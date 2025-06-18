package com.example.appsenkaspi.ui.subpilares

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appsenkaspi.R
import com.example.appsenkaspi.data.local.entity.AcaoEntity
import com.example.appsenkaspi.data.local.entity.SubpilarEntity
import com.example.appsenkaspi.data.local.enums.Cargo
import com.example.appsenkaspi.databinding.FragmentTelaSubpilarBinding
import com.example.appsenkaspi.extensions.configurarBotaoVoltar
import com.example.appsenkaspi.extensions.configurarNotificacaoBadge
import com.example.appsenkaspi.ui.acao.AcaoAdapter
import com.example.appsenkaspi.ui.acao.CriarAcaoFragment
import com.example.appsenkaspi.ui.acao.TelaAcaoFragment
import com.example.appsenkaspi.viewmodel.AcaoViewModel
import com.example.appsenkaspi.viewmodel.FuncionarioViewModel
import com.example.appsenkaspi.viewmodel.NotificacaoViewModel
import com.example.appsenkaspi.viewmodel.SubpilarViewModel
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Fragmento responsável por exibir os detalhes de um Subpilar específico,
 * incluindo suas ações vinculadas e o progresso total.
 *
 * Permite que o usuário visualize, edite ou adicione ações dependendo do seu cargo.
 * Exibe também informações sobre o subpilar, como nome, descrição e prazo.
 */
class TelaSubpilarComAcoesFragment : Fragment() {

  private var _binding: FragmentTelaSubpilarBinding? = null
  private val binding get() = _binding!!

  private val subpilarViewModel: SubpilarViewModel by activityViewModels()
  private val acaoViewModel: AcaoViewModel by activityViewModels()
  private val funcionarioViewModel: FuncionarioViewModel by activityViewModels()
  private val notificacaoViewModel: NotificacaoViewModel by activityViewModels()

  private var subpilarId: Int = -1
  private lateinit var acaoAdapter: AcaoAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    subpilarId = arguments?.getInt("subpilarId") ?: -1
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentTelaSubpilarBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    if (subpilarId == -1) {
      Toast.makeText(requireContext(), "Subpilar inválido!", Toast.LENGTH_SHORT).show()
      parentFragmentManager.popBackStack()
      return
    }

    configurarBotaoVoltar(view)
    configurarRecycler()
    configurarBotoes()
    observarAcoes()

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

        // Controla os botões com base no cargo do usuário
        when (it.cargo) {
          Cargo.APOIO -> {
            binding.cardEditarSubpilar.visibility = View.GONE
            binding.cardAdicionarAcoes.visibility = View.VISIBLE
          }
          Cargo.COORDENADOR -> {
            binding.cardEditarSubpilar.visibility = View.VISIBLE
            binding.cardAdicionarAcoes.visibility = View.VISIBLE
          }
          Cargo.GESTOR, null -> {
            binding.cardEditarSubpilar.visibility = View.GONE
            binding.cardAdicionarAcoes.visibility = View.GONE
          }
        }
      }
    }

    // Observa dados do subpilar e atualiza UI
    subpilarViewModel.getSubpilarById(subpilarId).observe(viewLifecycleOwner) { subpilar ->
      if (subpilar != null) {
        preencherCamposComSubpilar(subpilar)
        subpilarViewModel.calcularProgressoDoSubpilar(subpilar.id) { progresso ->
          animarProgresso((progresso * 100).toInt())
        }
      } else {
        Toast.makeText(requireContext(), "Subpilar não encontrado!", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
      }
    }

    binding.iconeMenu.setOnClickListener { toggleSobre() }
  }

  /** Configura o RecyclerView com layout vertical e adapter de ações. */
  private fun configurarRecycler() {
    binding.recyclerAcoes.layoutManager = LinearLayoutManager(requireContext())
    acaoAdapter = AcaoAdapter { acao -> abrirTelaAcao(acao) }
    binding.recyclerAcoes.adapter = acaoAdapter
  }

  /** Observa lista de ações vinculadas ao subpilar e atualiza o estado da UI. */
  private fun observarAcoes() {
    val recycler = binding.recyclerAcoes
    val emptyView = binding.emptyStateView

    acaoViewModel.listarAcoesPorSubpilar(subpilarId).observe(viewLifecycleOwner) { lista ->
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
   * Preenche os campos da tela com os dados do subpilar.
   *
   * @param subpilar Objeto contendo os dados do subpilar atual.
   */
  private fun preencherCamposComSubpilar(subpilar: SubpilarEntity) {
    binding.tituloSubpilar.text = "${subpilar.id}° Subpilar"
    binding.subtituloSubpilar.apply {
      text = subpilar.nome.ifBlank { "Sem nome" }
      paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
    }

    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    binding.dataPrazoSubpilar.text = "Prazo: ${sdf.format(subpilar.dataPrazo)}"

    binding.textoSobre.text = if (subpilar.descricao.isNullOrBlank()) {
      "Nenhuma descrição adicionada."
    } else {
      subpilar.descricao
    }
  }

  /** Define o comportamento dos botões de editar subpilar e adicionar ação. */
  private fun configurarBotoes() {
    binding.cardEditarSubpilar.setOnClickListener {
      parentFragmentManager.beginTransaction()
        .setCustomAnimations(
          R.anim.slide_fade_in_right, R.anim.slide_fade_out_left,
          R.anim.slide_fade_in_left, R.anim.slide_fade_out_right
        )
        .replace(R.id.main_container, EditarSubpilarFragment().apply {
          arguments = Bundle().apply { putInt("subpilarId", subpilarId) }
        })
        .addToBackStack(null)
        .commit()
    }

    binding.cardAdicionarAcoes.setOnClickListener {
      parentFragmentManager.beginTransaction()
        .setCustomAnimations(
          R.anim.slide_fade_in_right, R.anim.slide_fade_out_left,
          R.anim.slide_fade_in_left, R.anim.slide_fade_out_right
        )
        .replace(R.id.main_container, CriarAcaoFragment().apply {
          arguments = Bundle().apply { putInt("subpilarId", subpilarId) }
        })
        .addToBackStack(null)
        .commit()
    }
  }

  /**
   * Abre a tela de detalhes da ação selecionada.
   *
   * @param acao Ação a ser exibida.
   */
  private fun abrirTelaAcao(acao: AcaoEntity) {
    val fragment = TelaAcaoFragment().apply {
      arguments = Bundle().apply { putInt("acaoId", acao.id!!) }
    }
    parentFragmentManager.beginTransaction()
      .setCustomAnimations(
        R.anim.slide_fade_in_right, R.anim.slide_fade_out_left,
        R.anim.slide_fade_in_left, R.anim.slide_fade_out_right
      )
      .replace(R.id.main_container, fragment)
      .addToBackStack(null)
      .commit()
  }

  /**
   * Alterna visibilidade da seção "Sobre", com animações e ajuste de elevação.
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
   * Anima a barra de progresso do subpilar com valor percentual suavizado.
   *
   * @param target Valor de progresso final (0–100).
   */
  private fun animarProgresso(target: Int) {
    ObjectAnimator.ofInt(binding.progressoPilar, "progress", target).apply {
      duration = 500L
      start()
    }
    binding.percentual.text = "$target%"
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  companion object {
    /**
     * Cria uma nova instância do fragmento para o subpilar informado.
     *
     * @param subpilarId ID do subpilar a ser exibido.
     */
    fun newInstance(subpilarId: Int): TelaSubpilarComAcoesFragment {
      return TelaSubpilarComAcoesFragment().apply {
        arguments = Bundle().apply { putInt("subpilarId", subpilarId) }
      }
    }
  }
}
