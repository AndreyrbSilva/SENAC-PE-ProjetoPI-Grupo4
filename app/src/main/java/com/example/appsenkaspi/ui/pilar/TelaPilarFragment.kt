package com.example.appsenkaspi.ui.pilar

import android.animation.ObjectAnimator
import android.graphics.Paint
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appsenkaspi.R
import com.example.appsenkaspi.data.local.entity.AcaoEntity
import com.example.appsenkaspi.data.local.entity.PilarEntity
import com.example.appsenkaspi.data.local.enums.Cargo
import com.example.appsenkaspi.data.local.enums.StatusPilar
import com.example.appsenkaspi.databinding.FragmentTelaPilarBinding
import com.example.appsenkaspi.extensions.configurarBotaoVoltar
import com.example.appsenkaspi.extensions.configurarNotificacaoBadge
import com.example.appsenkaspi.ui.acao.AcaoAdapter
import com.example.appsenkaspi.ui.acao.CriarAcaoFragment
import com.example.appsenkaspi.ui.acao.TelaAcaoFragment
import com.example.appsenkaspi.viewmodel.AcaoViewModel
import com.example.appsenkaspi.viewmodel.FuncionarioViewModel
import com.example.appsenkaspi.viewmodel.NotificacaoViewModel
import com.example.appsenkaspi.viewmodel.PilarViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/**
 * Fragmento responsável por exibir os detalhes de um Pilar específico.
 *
 * Exibe nome, descrição, data de prazo, progresso e lista de ações associadas ao pilar.
 * A interface se adapta dinamicamente ao cargo do funcionário logado (Apoio, Coordenador ou Gestor),
 * mostrando ou ocultando botões de ação conforme permissões.
 *
 * Também permite concluir o pilar, editar suas informações ou adicionar novas ações.
 */
class TelaPilarFragment : Fragment() {

  private var _binding: FragmentTelaPilarBinding? = null
  private val binding get() = _binding!!

  private val pilarViewModel: PilarViewModel by activityViewModels()
  private val acaoViewModel: AcaoViewModel by activityViewModels()
  private val funcionarioViewModel: FuncionarioViewModel by activityViewModels()
  private val notificacaoViewModel: NotificacaoViewModel by activityViewModels()

  private var pilarId: Int = -1
  private lateinit var acaoAdapter: AcaoAdapter

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentTelaPilarBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    configurarBotaoVoltar(view)

    // Configura o ícone de notificações com badge dinâmico baseado no funcionário logado
    funcionarioViewModel.funcionarioLogado.observe(viewLifecycleOwner) { funcionario ->
      funcionario?.let {
        configurarNotificacaoBadge(
          rootView = view,
          lifecycleOwner = viewLifecycleOwner,
          fragmentManager = parentFragmentManager,
          funcionarioId = it.id,
          cargo = it.cargo,
          viewModel = notificacaoViewModel
        )
      }
    }

    // Ajusta a visibilidade dos botões com base no cargo do funcionário
    funcionarioViewModel.funcionarioLogado.observe(viewLifecycleOwner) { funcionario ->
      when (funcionario?.cargo) {
        Cargo.APOIO -> {
          binding.cardEditarPilar.visibility = View.GONE
          binding.cardAdicionarAcoes.visibility = View.VISIBLE
          binding.cardConcluirPilar.visibility = View.GONE
        }

        Cargo.COORDENADOR -> {
          binding.cardEditarPilar.visibility = View.VISIBLE
          binding.cardAdicionarAcoes.visibility = View.VISIBLE
        }

        Cargo.GESTOR -> {
          binding.cardEditarPilar.visibility = View.GONE
          binding.cardAdicionarAcoes.visibility = View.GONE
          binding.cardConcluirPilar.visibility = View.GONE
        }

        else -> {
          binding.cardEditarPilar.visibility = View.GONE
          binding.cardAdicionarAcoes.visibility = View.GONE
        }
      }
    }

    // Recupera o ID do Pilar passado via argumentos e inicia a observação de seus dados
    pilarId = arguments?.getInt("pilarId") ?: -1
    if (pilarId == -1) {
      Toast.makeText(requireContext(), "Pilar inválido!", Toast.LENGTH_SHORT).show()
      parentFragmentManager.popBackStack()
      return
    }

    pilarViewModel.getPilarById(pilarId)
      .observe(viewLifecycleOwner) { pilar ->
        if (pilar != null) {
          preencherCamposComPilar(pilar)
        } else {
          Toast.makeText(requireContext(), "Pilar não encontrado!", Toast.LENGTH_SHORT).show()
          parentFragmentManager.popBackStack()
        }
      }

    binding.cardConcluirPilar.setOnClickListener {
      lifecycleScope.launch {
        pilarViewModel.concluirPilar(pilarId)

        // Atualiza a UI após a conclusão do pilar
        pilarViewModel.getPilarById(pilarId).observe(viewLifecycleOwner) { pilar ->
          if (pilar != null) {
            preencherCamposComPilar(pilar)
            Toast.makeText(requireContext(), "Pilar concluído com sucesso!", Toast.LENGTH_SHORT).show()
          }
        }
      }
    }

    configurarRecycler()
    configurarBotoes()
    binding.iconeMenu.setOnClickListener { toggleSobre() }
    observarAcoes()
  }

  /**
   * Preenche os campos visuais com os dados do Pilar.
   *
   * Também calcula e anima o progresso, e define a visibilidade do botão de conclusão.
   *
   * @param pilar Entidade contendo os dados do Pilar atual.
   */
  private fun preencherCamposComPilar(pilar: PilarEntity) {
    binding.tituloPilar.text = "${pilar.id}° Pilar"
    binding.subtituloPilar.apply {
      text = pilar.nome.ifBlank { "Sem nome" }
      paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
    }

    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    binding.dataPrazoPilar.text = "Prazo: ${sdf.format(pilar.dataPrazo)}"
    binding.textoSobre.text = pilar.descricao.ifBlank { "Nenhuma descrição adicionada." }

    viewLifecycleOwner.lifecycleScope.launch {
      val progresso = pilarViewModel.calcularProgressoInterno(pilar.id)
      pilarViewModel.atualizarStatusAutomaticamente(pilar.id)
      animarProgresso((progresso * 100).toInt())

      val pilarAtualizado = pilarViewModel.getPilarById(pilar.id).value ?: pilar

      binding.cardConcluirPilar.visibility = when {
        pilarAtualizado.status == StatusPilar.CONCLUIDO -> View.GONE
        pilarViewModel.podeConcluirPilar(pilarId, pilar.dataPrazo.toLocalDate()) -> View.VISIBLE
        else -> View.GONE
      }
    }
  }

  /**
   * Configura os botões de edição e adição de ações com animações e navegação entre fragments.
   */
  private fun configurarBotoes() {
    binding.cardEditarPilar.setOnClickListener {
      parentFragmentManager.beginTransaction()
        .setCustomAnimations(R.anim.slide_fade_in_right, R.anim.slide_fade_out_left, R.anim.slide_fade_in_left, R.anim.slide_fade_out_right)
        .replace(R.id.main_container, EditarPilarFragment().apply {
          arguments = Bundle().apply { putInt("pilarId", pilarId) }
        })
        .addToBackStack(null)
        .commit()
    }
    binding.cardAdicionarAcoes.setOnClickListener {
      parentFragmentManager.beginTransaction()
        .setCustomAnimations(R.anim.slide_fade_in_right, R.anim.slide_fade_out_left, R.anim.slide_fade_in_left, R.anim.slide_fade_out_right)
        .replace(R.id.main_container, CriarAcaoFragment().apply {
          arguments = Bundle().apply { putInt("pilarId", pilarId) }
        })
        .addToBackStack(null)
        .commit()
    }
  }

  /**
   * Expande ou colapsa a seção "Sobre" com animação.
   */
  private fun toggleSobre() {
    val transition = AutoTransition().apply { duration = 300 }
    TransitionManager.beginDelayedTransition(binding.cabecalhoCard, transition)
    val visivel = binding.sobreWrapper.visibility == View.VISIBLE
    binding.sobreWrapper.visibility = if (visivel) View.GONE else View.VISIBLE
    binding.iconeMenu.animate().rotation(if (visivel) 0f else 180f).setDuration(300).start()
    binding.headerLayout.elevation = if (visivel) 8f else 16f
  }

  /**
   * Converte um objeto Date para LocalDate, respeitando o fuso horário local.
   */
  private fun Date.toLocalDate(): LocalDate {
    return this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
  }

  /**
   * Anima visualmente a barra de progresso e atualiza o valor textual do progresso.
   *
   * @param target Valor alvo da progressão, de 0 a 100.
   */
  private fun animarProgresso(target: Int) {
    ObjectAnimator.ofInt(binding.progressoPilar, "progress", target).apply {
      duration = 500L
      start()
    }
    binding.percentual.text = "$target%"
  }

  /**
   * Inicializa o RecyclerView com o adaptador de ações.
   */
  private fun configurarRecycler() {
    binding.recyclerAcoes.layoutManager = LinearLayoutManager(requireContext())
    acaoAdapter = AcaoAdapter { acao -> abrirTelaAcao(acao) }
    binding.recyclerAcoes.adapter = acaoAdapter
  }

  /**
   * Observa as ações vinculadas ao pilar atual e exibe-as no RecyclerView.
   * Exibe estado vazio caso não haja ações.
   */
  private fun observarAcoes() {
    val recycler = binding.recyclerAcoes
    val emptyView = binding.emptyStateView

    acaoViewModel.listarAcoesPorPilar(pilarId).observe(viewLifecycleOwner) { lista ->
      if (lista.isNullOrEmpty()) {
        recycler.visibility = View.GONE
        emptyView.visibility = View.VISIBLE
      } else {
        recycler.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
        acaoAdapter.submitList(lista)
      }
    }
  }

  /**
   * Navega para a tela de detalhes da Ação selecionada.
   *
   * @param acao Ação cujo ID será usado para navegação.
   */
  private fun abrirTelaAcao(acao: AcaoEntity) {
    val fragment = TelaAcaoFragment().apply {
      arguments = Bundle().apply { putInt("acaoId", acao.id!!) }
    }
    parentFragmentManager.beginTransaction()
      .setCustomAnimations(R.anim.slide_fade_in_right, R.anim.slide_fade_out_left, R.anim.slide_fade_in_left, R.anim.slide_fade_out_right)
      .replace(R.id.main_container, fragment)
      .addToBackStack(null)
      .commit()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
