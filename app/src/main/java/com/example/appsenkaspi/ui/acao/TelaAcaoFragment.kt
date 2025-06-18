package com.example.appsenkaspi.ui.acao

import android.animation.ObjectAnimator
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appsenkaspi.R
import com.example.appsenkaspi.ui.atividade.TelaAtividadeFragment
import com.example.appsenkaspi.data.local.entity.AcaoEntity
import com.example.appsenkaspi.data.local.enums.Cargo
import com.example.appsenkaspi.databinding.FragmentTelaAcaoBinding
import com.example.appsenkaspi.extensions.configurarBotaoVoltar
import com.example.appsenkaspi.extensions.configurarNotificacaoBadge
import com.example.appsenkaspi.ui.atividade.AtividadeAdapter
import com.example.appsenkaspi.ui.atividade.CriarAtividadeFragment
import com.example.appsenkaspi.viewmodel.AcaoViewModel
import com.example.appsenkaspi.viewmodel.AtividadeViewModel
import com.example.appsenkaspi.viewmodel.FuncionarioViewModel
import com.example.appsenkaspi.viewmodel.NotificacaoViewModel
import com.example.appsenkaspi.viewmodel.PilarViewModel
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Fragmento responsável pela visualização de uma Ação específica.
 *
 * Exibe informações como nome, descrição, prazo, progresso das atividades e
 * permite edição da Ação e criação de novas atividades, dependendo do cargo do usuário logado.
 */
class TelaAcaoFragment : Fragment() {

  private var _binding: FragmentTelaAcaoBinding? = null
  private val binding get() = _binding!!

  private val pilarViewModel: PilarViewModel by activityViewModels()
  private val acaoViewModel: AcaoViewModel by activityViewModels()
  private val atividadeViewModel: AtividadeViewModel by activityViewModels()
  private val funcionarioViewModel: FuncionarioViewModel by activityViewModels()
  private val notificacaoViewModel: NotificacaoViewModel by activityViewModels()

  private var acaoId: Int = -1
  private lateinit var atividadeAdapter: AtividadeAdapter

  /**
   * Infla o layout do fragmento.
   */
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentTelaAcaoBinding.inflate(inflater, container, false)
    return binding.root
  }

  /**
   * Inicializa os componentes da tela, define visibilidade de botões e observa a Ação e suas atividades.
   */
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    configurarBotaoVoltar(view)

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

    funcionarioViewModel.funcionarioLogado.observe(viewLifecycleOwner) { funcionario ->
      when (funcionario?.cargo) {
        Cargo.APOIO, Cargo.COORDENADOR -> {
          binding.cardEditarAcao.visibility = View.VISIBLE
          binding.cardAdicionarAtividade.visibility = View.VISIBLE
        }
        else -> {
          binding.cardEditarAcao.visibility = View.GONE
          binding.cardAdicionarAtividade.visibility = View.GONE
        }
      }
    }

    acaoId = arguments?.getInt("acaoId") ?: -1
    if (acaoId == -1) {
      Toast.makeText(requireContext(), "Ação Inválida!", Toast.LENGTH_SHORT).show()
      parentFragmentManager.popBackStack()
      return
    }

    acaoViewModel.getAcaoById(acaoId).observe(viewLifecycleOwner) { acao ->
      if (acao != null) {
        preencherCamposComAcao(acao)
      } else {
        Toast.makeText(requireContext(), "Ação não encontrada!", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
      }
    }

    configurarBotoes()
    binding.iconeMenu.setOnClickListener { toggleSobre() }
    observarAtividades()
    acaoViewModel.atualizarStatusAcaoAutomaticamente(acaoId)
  }

  /**
   * Preenche os campos de nome, descrição e data da Ação.
   */
  private fun preencherCamposComAcao(acao: AcaoEntity) {
    binding.tituloAcao.text = acao.nome.ifBlank { "Ação sem nome" }

    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    binding.dataPrazoPilar.text = "Prazo: ${sdf.format(acao.dataPrazo)}"

    binding.textoSobre.text = acao.descricao.ifBlank { "Nenhuma descrição adicionada." }
  }

  /**
   * Define comportamento dos botões de editar ação e criar atividade.
   */
  private fun configurarBotoes() {
    binding.cardEditarAcao.setOnClickListener {
      parentFragmentManager.beginTransaction()
        .setCustomAnimations(
          R.anim.slide_fade_in_right,
          R.anim.slide_fade_out_left,
          R.anim.slide_fade_in_left,
          R.anim.slide_fade_out_right
        )
        .replace(
          R.id.main_container,
          EditarAcaoFragment().apply {
            arguments = Bundle().apply { putInt("acaoId", acaoId) }
          }
        )
        .addToBackStack(null)
        .commit()
    }

    binding.cardAdicionarAtividade.setOnClickListener {
      parentFragmentManager.beginTransaction()
        .setCustomAnimations(
          R.anim.slide_fade_in_right,
          R.anim.slide_fade_out_left,
          R.anim.slide_fade_in_left,
          R.anim.slide_fade_out_right
        )
        .replace(
          R.id.main_container,
          CriarAtividadeFragment().apply {
            arguments = Bundle().apply { putInt("acaoId", acaoId) }
          }
        )
        .addToBackStack(null)
        .commit()
    }
  }

  /**
   * Alterna a visibilidade da seção "Sobre" com animação.
   */
  private fun toggleSobre() {
    val transition = AutoTransition().apply { duration = 300 }
    TransitionManager.beginDelayedTransition(binding.cabecalhoCard, transition)
    if (binding.sobreWrapper.visibility == View.VISIBLE) {
      binding.sobreWrapper.visibility = View.GONE
      binding.iconeMenu.animate().rotation(0f).setDuration(300).start()
      binding.headerLayout.elevation = 8f
    } else {
      binding.sobreWrapper.visibility = View.VISIBLE
      binding.iconeMenu.animate().rotation(180f).setDuration(300).start()
      binding.headerLayout.elevation = 16f
    }
  }

  /**
   * Atualiza a barra de progresso da ação com animação e exibe o percentual.
   */
  private fun animarProgresso(target: Int) {
    ObjectAnimator.ofInt(binding.progressoAcao, "progress", target).apply {
      duration = 500L
      start()
    }
    binding.percentual.text = "$target%"
  }

  /**
   * Observa a lista de atividades associadas à ação e atualiza a UI com progresso.
   */
  private fun observarAtividades() {
    val recycler = binding.recyclerAtividades
    val emptyView = binding.emptyStateView

    atividadeAdapter = AtividadeAdapter { atividadeComFuncionarios ->
      val fragment = TelaAtividadeFragment().apply {
        arguments = Bundle().apply {
          putInt("atividadeId", atividadeComFuncionarios.atividade.id!!)
        }
      }

      parentFragmentManager.beginTransaction()
        .setCustomAnimations(
          R.anim.slide_fade_in_right,
          R.anim.slide_fade_out_left,
          R.anim.slide_fade_in_left,
          R.anim.slide_fade_out_right
        )
        .replace(R.id.main_container, fragment)
        .addToBackStack(null)
        .commit()
    }

    recycler.layoutManager = LinearLayoutManager(requireContext())
    recycler.adapter = atividadeAdapter

    atividadeViewModel.listarAtividadesComFuncionariosPorAcao(acaoId)
      .observe(viewLifecycleOwner) { atividades ->
        if (atividades.isNullOrEmpty()) {
          recycler.visibility = View.GONE
          emptyView.visibility = View.VISIBLE
          animarProgresso(0)
        } else {
          recycler.visibility = View.VISIBLE
          emptyView.visibility = View.GONE
          atividadeAdapter.submitList(atividades)

          val total = atividades.size
          val concluidas = atividades.count { it.atividade.status.name == "CONCLUIDA" }
          val progresso = if (total > 0) (concluidas * 100 / total) else 0
          animarProgresso(progresso)
        }
      }
  }

  /**
   * Libera o binding para evitar vazamentos de memória.
   */
  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
