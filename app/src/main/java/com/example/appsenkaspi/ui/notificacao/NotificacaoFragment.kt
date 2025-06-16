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
import android.graphics.Color
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

class NotificacaoFragment : Fragment() {

  private val viewModel: NotificacaoViewModel by activityViewModels()
  private val funcionarioViewModel: FuncionarioViewModel by activityViewModels()
  private lateinit var adapter: RequisicaoAdapter
  private var funcionarioIdAtual: Int? = null
  private var modoSelecaoAtivo = false
  private lateinit var trashIcon: ImageView
  private var iconeAtual: Int = R.drawable.ic_delete

  override fun onCreateView(
      inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    return inflater.inflate(R.layout.fragment_notificacoes, container, false)
  }

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

      adapter = RequisicaoAdapter(
          funcionarioIdLogado = funcionarioId,
          modoCoordenador = modoCoordenador
      ).apply {
        onItemClick = { requisicao ->
          if (modoCoordenador && requisicao.tipo !in listOf(
              TipoRequisicao.ATIVIDADE_PARA_VENCER,
              TipoRequisicao.ATIVIDADE_VENCIDA,
              TipoRequisicao.PRAZO_ALTERADO,
              TipoRequisicao.RESPONSAVEL_ADICIONADO,
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
        onSelecaoMudou = {
          atualizarIconeLixeira()
        }
      }

      recyclerView.adapter = adapter

      adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
          vazio.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
        }
      })

      if (modoCoordenador) {
        val pendentesLiveData = viewModel.getRequisicoesPendentes()
        val pessoaisLiveData = viewModel.getNotificacoesDoApoio(funcionarioId)

        pendentesLiveData.observe(viewLifecycleOwner) { pendentes ->
          pessoaisLiveData.observe(viewLifecycleOwner) { pessoais ->
            val notificacoesAuto = pessoais.filter {
              it.tipo in listOf(
                TipoRequisicao.ATIVIDADE_PARA_VENCER,
                TipoRequisicao.ATIVIDADE_VENCIDA,
                TipoRequisicao.PRAZO_ALTERADO,
                TipoRequisicao.ATIVIDADE_CONCLUIDA,
                TipoRequisicao.RESPONSAVEL_REMOVIDO,
                TipoRequisicao.RESPONSAVEL_ADICIONADO
              ) && it.solicitanteId == funcionarioId && !it.excluida
            }
            val listaFinal = (pendentes + notificacoesAuto)
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

          if (listaFiltrada.isNotEmpty()) {
            recyclerView.post {
              val notificationManager = NotificationManagerCompat.from(requireContext())

              listaFiltrada.filter {
                it.tipo == TipoRequisicao.ATIVIDADE_VENCIDA && !it.foiVista && it.solicitanteId == funcionarioId
              }.forEach { requisicao ->
                notificationManager.cancel(requisicao.id)
                viewModel.marcarTodasComoVistas(requisicao.id)
              }

              viewModel.marcarTodasComoVistas(funcionarioId)
            }
          }
        }
      }

      trashIcon.setOnClickListener {
        if (!modoSelecaoAtivo) {
          modoSelecaoAtivo = true
          adapter.modoSelecao = true
          adapter.selecionadas.clear()
          adapter.notifyDataSetChanged()
          atualizarIconeLixeira()
        } else {
          if (adapter.selecionadas.isNotEmpty()) {
            val dialog = AlertDialog.Builder(requireContext())
              .setTitle("Excluir notificações")
              .setMessage("Tem certeza que deseja excluir ${adapter.selecionadas.size} notificações selecionadas?")
              .setPositiveButton("Excluir") { dialogInterface, _ ->
                viewModel.excluirRequisicoes(adapter.selecionadas.toList())
                Snackbar.make(view, "Notificações excluídas", Snackbar.LENGTH_SHORT).show()

                modoSelecaoAtivo = false
                adapter.modoSelecao = false
                adapter.selecionadas.clear()
                adapter.notifyDataSetChanged()
                atualizarIconeLixeira()
                dialogInterface.dismiss()
              }
              .setNegativeButton("Cancelar") { dialogInterface, _ ->
                dialogInterface.dismiss()
              }
              .create()

            dialog.show()

            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

            val width = (requireContext().resources.displayMetrics.widthPixels * 0.85).toInt()
            dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

            dialog.findViewById<TextView>(android.R.id.message)?.textAlignment = View.TEXT_ALIGNMENT_VIEW_START

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.RED)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.WHITE)

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

  private fun atualizarListaNotificacoes() {
    val funcionarioId = funcionarioIdAtual ?: return
    val modoCoordenador = funcionarioViewModel.funcionarioLogado.value?.cargo == Cargo.COORDENADOR

    if (modoCoordenador) {
      val pendentesLiveData = viewModel.getRequisicoesPendentes()
      val pessoaisLiveData = viewModel.getNotificacoesDoApoio(funcionarioId)

      pendentesLiveData.observe(viewLifecycleOwner) { pendentes ->
        pessoaisLiveData.observe(viewLifecycleOwner) { pessoais ->
          val notificacoesDePrazoOuVencida = pessoais.filter {
            it.tipo in listOf(
              TipoRequisicao.ATIVIDADE_PARA_VENCER,
              TipoRequisicao.ATIVIDADE_VENCIDA,
              TipoRequisicao.PRAZO_ALTERADO,
              TipoRequisicao.ATIVIDADE_CONCLUIDA,
              TipoRequisicao.RESPONSAVEL_REMOVIDO,
              TipoRequisicao.RESPONSAVEL_ADICIONADO,
            ) && it.solicitanteId == funcionarioId && !it.excluida
          }
          val listaFinal = (pendentes + notificacoesDePrazoOuVencida)
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

  override fun onStop() {
    super.onStop()
    funcionarioIdAtual?.let { id ->
      viewModel.marcarNotificacoesDePrazoComoVistas(id)
    }
  }

  override fun onResume() {
    super.onResume()
    atualizarListaNotificacoes()
  }
}
