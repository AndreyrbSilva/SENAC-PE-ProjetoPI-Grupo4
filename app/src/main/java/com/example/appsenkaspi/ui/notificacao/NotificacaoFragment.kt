package com.example.appsenkaspi.ui.notificacao

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appsenkaspi.data.local.enums.Cargo
import com.example.appsenkaspi.viewmodel.NotificacaoViewModel
import com.example.appsenkaspi.R
import com.example.appsenkaspi.ui.requisicao.RequisicaoAdapter
import com.example.appsenkaspi.data.local.enums.TipoRequisicao
import com.example.appsenkaspi.extensions.configurarBotaoVoltar
import com.example.appsenkaspi.viewmodel.FuncionarioViewModel
import com.google.android.material.snackbar.Snackbar

/**
 * Fragmento responsável por exibir e gerenciar notificações/requisições.
 *
 * Exibe uma lista de notificações (ações pendentes, alertas de prazo, alterações e conclusões)
 * de acordo com o cargo do usuário logado:
 *
 * - **Coordenador**: vê requisições pendentes e notificações automáticas vinculadas a ações/atividades.
 * - **Apoio**: vê apenas notificações relacionadas às suas atividades.
 *
 * Permite ativar modo de seleção múltipla para excluir notificações.
 */
class NotificacaoFragment : Fragment() {

  /** ViewModel responsável por buscar e manipular notificações */
  private val viewModel: NotificacaoViewModel by activityViewModels()

  /** ViewModel que armazena o funcionário logado */
  private val funcionarioViewModel: FuncionarioViewModel by activityViewModels()

  /** Adaptador que renderiza a lista de notificações */
  private lateinit var adapter: RequisicaoAdapter

  /** ID do funcionário logado (armazenado para reutilização) */
  private var funcionarioIdAtual: Int? = null

  /** Flag que indica se o modo de seleção está ativo */
  private var modoSelecaoAtivo = false

  /** Referência para o ícone de lixeira no topo */
  private lateinit var trashIcon: ImageView

  /** Referência para o recurso atual da lixeira */
  private var iconeAtual: Int = R.drawable.ic_delete

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    return inflater.inflate(R.layout.fragment_notificacoes, container, false)
  }

  /**
   * Inicializa o RecyclerView, adapta os dados conforme o cargo do usuário e define
   * os comportamentos de clique, modo seleção e exclusão.
   */
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewNotificacoes)
    val vazio = view.findViewById<TextView>(R.id.textVazioNotificacoes)
    trashIcon = view.findViewById(R.id.trashIcon)
    configurarBotaoVoltar(view)

    recyclerView.layoutManager = LinearLayoutManager(requireContext())

    funcionarioViewModel.funcionarioLogado.observe(viewLifecycleOwner) { funcionario ->
      val funcionarioId = funcionario?.id ?: return@observe
      funcionarioIdAtual = funcionarioId
      val modoCoordenador = funcionario.cargo == Cargo.COORDENADOR

      adapter = RequisicaoAdapter(funcionarioId, modoCoordenador).apply {
        // Clique em uma notificação (somente Coordenador com notificações interativas)
        onItemClick = { requisicao ->
          if (modoCoordenador && requisicao.tipo !in listOf(
              TipoRequisicao.ATIVIDADE_PARA_VENCER, TipoRequisicao.ATIVIDADE_VENCIDA,
              TipoRequisicao.PRAZO_ALTERADO, TipoRequisicao.RESPONSAVEL_ADICIONADO,
              TipoRequisicao.ATIVIDADE_CONCLUIDA
            )
          ) {
            val fragment = DetalheNotificacaoFragment().apply {
              arguments = Bundle().apply {
                putInt("requisicaoId", requisicao.id)
              }
            }
            requireActivity().supportFragmentManager.beginTransaction()
              .replace(R.id.main_container, fragment)
              .addToBackStack(null)
              .commit()
          }
        }

        onSelecaoMudou = { atualizarIconeLixeira() }
      }

      recyclerView.adapter = adapter

      adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
          vazio.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
        }
      })

      if (modoCoordenador) {
        val pendentes = viewModel.getRequisicoesPendentes()
        val pessoais = viewModel.getNotificacoesDoApoio(funcionarioId)

        pendentes.observe(viewLifecycleOwner) { listaPendentes ->
          pessoais.observe(viewLifecycleOwner) { listaPessoais ->
            val notificacoesAuto = listaPessoais.filter {
              it.tipo in listOf(
                TipoRequisicao.ATIVIDADE_PARA_VENCER, TipoRequisicao.ATIVIDADE_VENCIDA,
                TipoRequisicao.PRAZO_ALTERADO, TipoRequisicao.ATIVIDADE_CONCLUIDA,
                TipoRequisicao.RESPONSAVEL_REMOVIDO, TipoRequisicao.RESPONSAVEL_ADICIONADO
              ) && it.solicitanteId == funcionarioId && !it.excluida
            }

            val listaFinal = (listaPendentes + notificacoesAuto)
              .filter { !it.excluida }
              .sortedBy { it.resolvida }

            if (adapter.currentList != listaFinal) {
              adapter.submitList(listaFinal)
            }
          }
        }
      } else {
        viewModel.getNotificacoesDoApoio(funcionarioId).observe(viewLifecycleOwner) { lista ->
          val listaFiltrada = lista.filter { !it.excluida }
          adapter.submitList(listaFiltrada)

          // Marcação automática de notificações vencidas como "vistas"
          if (listaFiltrada.isNotEmpty()) {
            recyclerView.post {
              val manager = NotificationManagerCompat.from(requireContext())
              listaFiltrada.filter {
                it.tipo == TipoRequisicao.ATIVIDADE_VENCIDA && !it.foiVista && it.solicitanteId == funcionarioId
              }.forEach {
                manager.cancel(it.id)
                viewModel.marcarTodasComoVistas(it.id)
              }

              viewModel.marcarTodasComoVistas(funcionarioId)
            }
          }
        }
      }

      // Ações do ícone da lixeira
      trashIcon.setOnClickListener {
        if (!modoSelecaoAtivo) {
          modoSelecaoAtivo = true
          adapter.modoSelecao = true
          adapter.selecionadas.clear()
          adapter.notifyDataSetChanged()
          atualizarIconeLixeira()
        } else {
          if (adapter.selecionadas.isNotEmpty()) {
            AlertDialog.Builder(requireContext())
              .setTitle("Excluir notificações")
              .setMessage("Tem certeza que deseja excluir ${adapter.selecionadas.size} notificações selecionadas?")
              .setPositiveButton("Excluir") { dialog, _ ->
                viewModel.excluirRequisicoes(adapter.selecionadas.toList())
                Snackbar.make(view, "Notificações excluídas", Snackbar.LENGTH_SHORT).show()
                modoSelecaoAtivo = false
                adapter.modoSelecao = false
                adapter.selecionadas.clear()
                adapter.notifyDataSetChanged()
                atualizarIconeLixeira()
                dialog.dismiss()
              }
              .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
              .show()
          } else {
            modoSelecaoAtivo = false
            adapter.modoSelecao = false
            adapter.notifyDataSetChanged()
            atualizarIconeLixeira()
          }
        }
      }
    }
  }

  /**
   * Atualiza dinamicamente o ícone da lixeira de acordo com o estado do modo seleção.
   */
  fun atualizarIconeLixeira() {
    val novaImagem = when {
      !modoSelecaoAtivo -> R.drawable.ic_delete
      adapter.selecionadas.isEmpty() -> R.drawable.ic_delete_x
      else -> R.drawable.ic_delete_confirm
    }

    if (novaImagem == iconeAtual) return

    iconeAtual = novaImagem
    trashIcon.animate()
      .alpha(0f)
      .setDuration(150)
      .withEndAction {
        trashIcon.setImageResource(novaImagem)
        trashIcon.animate().alpha(1f).setDuration(150).start()
      }
      .start()
  }

  /**
   * Recarrega as notificações ao retomar o fragmento.
   */
  override fun onResume() {
    super.onResume()
    atualizarListaNotificacoes()
  }

  /**
   * Marca as notificações de prazo como vistas ao sair da tela.
   */
  override fun onStop() {
    super.onStop()
    funcionarioIdAtual?.let { id ->
      viewModel.marcarNotificacoesDePrazoComoVistas(id)
    }
  }

  /**
   * Atualiza a lista de notificações com base no cargo e exclusões.
   */
  private fun atualizarListaNotificacoes() {
    val funcionarioId = funcionarioIdAtual ?: return
    val modoCoordenador = funcionarioViewModel.funcionarioLogado.value?.cargo == Cargo.COORDENADOR

    if (modoCoordenador) {
      val pendentes = viewModel.getRequisicoesPendentes()
      val pessoais = viewModel.getNotificacoesDoApoio(funcionarioId)

      pendentes.observe(viewLifecycleOwner) { listaPendentes ->
        pessoais.observe(viewLifecycleOwner) { listaPessoais ->
          val notificacoesAuto = listaPessoais.filter {
            it.tipo in listOf(
              TipoRequisicao.ATIVIDADE_PARA_VENCER, TipoRequisicao.ATIVIDADE_VENCIDA,
              TipoRequisicao.PRAZO_ALTERADO, TipoRequisicao.ATIVIDADE_CONCLUIDA,
              TipoRequisicao.RESPONSAVEL_REMOVIDO, TipoRequisicao.RESPONSAVEL_ADICIONADO
            ) && it.solicitanteId == funcionarioId && !it.excluida
          }

          val listaFinal = (listaPendentes + notificacoesAuto)
            .filter { !it.excluida }
            .sortedBy { it.resolvida }

          adapter.submitList(listaFinal)
        }
      }
    } else {
      viewModel.getNotificacoesDoApoio(funcionarioId).observe(viewLifecycleOwner) { lista ->
        adapter.submitList(lista.filter { !it.excluida })
      }
    }
  }
}
