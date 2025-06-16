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
import com.example.appsenkaspi.ui.subpilares.TelaSubpilarAdapter
import com.example.appsenkaspi.ui.subpilares.TelaSubpilarComAcoesFragment
import com.example.appsenkaspi.data.local.entity.PilarEntity
import com.example.appsenkaspi.data.local.entity.SubpilarEntity
import com.example.appsenkaspi.data.local.enums.Cargo
import com.example.appsenkaspi.data.local.enums.StatusPilar
import com.example.appsenkaspi.databinding.FragmentTelaPilarComSubpilaresBinding
import com.example.appsenkaspi.extensions.configurarBotaoVoltar
import com.example.appsenkaspi.extensions.configurarNotificacaoBadge
import com.example.appsenkaspi.ui.subpilares.CriarSubpilarFragment
import com.example.appsenkaspi.viewmodel.FuncionarioViewModel
import com.example.appsenkaspi.viewmodel.NotificacaoViewModel
import com.example.appsenkaspi.viewmodel.PilarViewModel
import com.example.appsenkaspi.viewmodel.SubpilarViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

/**
 * Fragmento responsável por exibir os detalhes de um Pilar que contém Subpilares.
 *
 * Este fragmento exibe as seguintes informações e funcionalidades:
 * - Nome, descrição, data de prazo e progresso do Pilar.
 * - Lista dos Subpilares associados, com barra de progresso individual.
 * - Botões de ação para editar o Pilar, adicionar novos Subpilares ou concluir o Pilar (apenas para Coordenadores).
 * - Expansão da descrição com animação e toggle visual.
 * - Integração com `PilarViewModel`, `SubpilarViewModel`, `FuncionarioViewModel` e `NotificacaoViewModel`.
 *
 * Funcionalidades inteligentes:
 * - A visibilidade dos botões de ação varia de acordo com o cargo do usuário (Coordenação, Apoio, Gestor).
 * - O progresso é recalculado dinamicamente com base nas ações e subpilares.
 * - A conclusão do Pilar é condicional ao progresso total e à data de vencimento.
 *
 * Requisitos do layout XML:
 * - IDs esperados: `tituloPilar`, `subtituloPilar`, `dataPrazoPilar`, `progressoPilar`, `percentual`, `recyclerSubpilares`,
 *   `cardEditarPilar`, `cardAdicionarSubPilares`, `cardConcluirPilar`, `iconeMenu`, `sobreWrapper`, `headerLayout`.
 *
 * @see PilarViewModel
 * @see SubpilarViewModel
 * @see FuncionarioViewModel
 * @see NotificacaoViewModel
 * @see TelaSubpilarComAcoesFragment
 */
class TelaPilarComSubpilaresFragment : Fragment() {

  // Binding da View
  private var _binding: FragmentTelaPilarComSubpilaresBinding? = null
  private val binding get() = _binding!!

  // ViewModels compartilhados via activity
  private val pilarViewModel: PilarViewModel by activityViewModels()
  private val subpilarViewModel: SubpilarViewModel by activityViewModels()
  private val funcionarioViewModel: FuncionarioViewModel by activityViewModels()
  private val notificacaoViewModel: NotificacaoViewModel by activityViewModels()

  // ID do Pilar carregado
  private var pilarId: Int = -1

  // Adapter para o RecyclerView de Subpilares
  private lateinit var subpilarAdapter: TelaSubpilarAdapter

  // Mapa com progresso por Subpilar
  private val progressoMap = mutableMapOf<Int, Float>()

  /**
   * Infla o layout do fragmento.
   */
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentTelaPilarComSubpilaresBinding.inflate(inflater, container, false)
    return binding.root
  }

  /**
   * Inicializa a interface e observa os dados do Pilar e seus Subpilares.
   */
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    // Validação do Pilar
    pilarId = arguments?.getInt("pilarId") ?: -1
    if (pilarId == -1) {
      Toast.makeText(requireContext(), "Pilar inválido!", Toast.LENGTH_SHORT).show()
      parentFragmentManager.popBackStack()
      return
    }

    configurarBotaoVoltar(view)
    configurarRecycler()
    configurarBotoes()
    observarCargoFuncionario(view)
    observarPilar()
    observarSubpilares()
    binding.iconeMenu.setOnClickListener { toggleSobre() }

    // Anima progresso inicial
    viewLifecycleOwner.lifecycleScope.launch {
      val progresso = pilarViewModel.calcularProgressoInterno(pilarId)
      pilarViewModel.atualizarStatusAutomaticamente(pilarId)
      animarProgresso((progresso * 100).toInt())
    }
  }

  /**
   * Inicializa ações de botões (editar, criar subpilar).
   */
  private fun configurarBotoes() {
    binding.cardEditarPilar.setOnClickListener {
      parentFragmentManager.beginTransaction()
        .setCustomAnimations(
          R.anim.slide_fade_in_right,
          R.anim.slide_fade_out_left,
          R.anim.slide_fade_in_left,
          R.anim.slide_fade_out_right
        )
        .replace(R.id.main_container, EditarPilarFragment().apply {
          arguments = Bundle().apply { putInt("pilarId", pilarId) }
        })
        .addToBackStack(null)
        .commit()
    }

    binding.cardAdicionarSubPilares.setOnClickListener {
      parentFragmentManager.beginTransaction()
        .setCustomAnimations(
          R.anim.slide_fade_in_right,
          R.anim.slide_fade_out_left,
          R.anim.slide_fade_in_left,
          R.anim.slide_fade_out_right
        )
        .replace(R.id.main_container, CriarSubpilarFragment.newInstance(pilarId))
        .addToBackStack(null)
        .commit()
    }

    binding.cardConcluirPilar.setOnClickListener {
      lifecycleScope.launch {
        pilarViewModel.concluirPilar(pilarId)
        pilarViewModel.getPilarById(pilarId).observe(viewLifecycleOwner) { pilar ->
          if (pilar != null) {
            preencherCamposComPilar(pilar)
            Toast.makeText(requireContext(), "Pilar concluído com sucesso!", Toast.LENGTH_SHORT).show()
          }
        }
      }
    }
  }

  /**
   * Observa o cargo do funcionário e ajusta visibilidade dos cards.
   */
  private fun observarCargoFuncionario(view: View) {
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

        binding.cardEditarPilar.visibility =
          if (it.cargo == Cargo.COORDENADOR) View.VISIBLE else View.GONE
        binding.cardAdicionarSubPilares.visibility =
          if (it.cargo == Cargo.COORDENADOR) View.VISIBLE else View.GONE
        binding.cardConcluirPilar.visibility =
          if (it.cargo == Cargo.COORDENADOR) View.VISIBLE else View.GONE
      }
    }
  }

  /**
   * Observa o Pilar e preenche os campos na tela.
   */
  private fun observarPilar() {
    pilarViewModel.getPilarById(pilarId).observe(viewLifecycleOwner) { pilar ->
      if (pilar != null) {
        preencherCamposComPilar(pilar)
      } else {
        Toast.makeText(requireContext(), "Pilar não encontrado!", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
      }
    }
  }

  /**
   * Preenche os dados do Pilar na tela.
   */
  private fun preencherCamposComPilar(pilar: PilarEntity) {
    binding.tituloPilar.text = "${pilar.id}° Pilar"
    binding.subtituloPilar.apply {
      text = pilar.nome.ifBlank { "Sem nome" }
      paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
    }
    binding.dataPrazoPilar.text = "Prazo: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(pilar.dataPrazo)}"
    binding.textoSobre.text = pilar.descricao.ifBlank { "Nenhuma descrição adicionada." }

    lifecycleScope.launch {
      val progresso = withContext(Dispatchers.IO) {
        pilarViewModel.calcularProgressoInterno(pilar.id).also {
          pilarViewModel.atualizarStatusAutomaticamente(pilar.id)
        }
      }
      animarProgresso((progresso * 100).toInt())

      val atualizado = pilarViewModel.getPilarById(pilar.id).value ?: pilar
      val podeConcluir = pilarViewModel.podeConcluirPilar(pilar.id, pilar.dataPrazo.toLocalDate())

      binding.cardConcluirPilar.visibility = if (
        atualizado.status != StatusPilar.CONCLUIDO && podeConcluir
      ) View.VISIBLE else View.GONE
    }
  }

  /**
   * Observa os Subpilares do Pilar e atualiza a lista com progresso.
   */
  private fun observarSubpilares() {
    subpilarViewModel.listarSubpilaresPorPilar(pilarId).observe(viewLifecycleOwner) { subpilares ->
      lifecycleScope.launch {
        progressoMap.clear()
        val progressoList = subpilares.map { subpilar ->
          async { subpilar.id to subpilarViewModel.calcularProgressoInterno(subpilar.id) }
        }.awaitAll()
        progressoMap.putAll(progressoList.toMap())
        subpilarAdapter.submitList(subpilares.toList())
      }

      pilarViewModel.calcularProgressoDoPilar(pilarId) { progresso ->
        animarProgresso((progresso * 100).toInt())
      }
    }
  }

  /**
   * Configura RecyclerView para exibir Subpilares com progresso.
   */
  private fun configurarRecycler() {
    binding.recyclerSubpilares.layoutManager = LinearLayoutManager(requireContext())
    subpilarAdapter = TelaSubpilarAdapter(
      onClick = { abrirTelaSubpilar(it) },
      progressoMap = progressoMap
    )
    binding.recyclerSubpilares.adapter = subpilarAdapter
  }

  /**
   * Abre tela de visualização detalhada de um Subpilar.
   */
  private fun abrirTelaSubpilar(subpilar: SubpilarEntity) {
    parentFragmentManager.beginTransaction()
      .setCustomAnimations(
        R.anim.slide_fade_in_right,
        R.anim.slide_fade_out_left,
        R.anim.slide_fade_in_left,
        R.anim.slide_fade_out_right
      )
      .replace(R.id.main_container, TelaSubpilarComAcoesFragment.newInstance(subpilar.id))
      .addToBackStack(null)
      .commit()
  }

  /**
   * Alterna visibilidade da descrição do Pilar.
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
   * Anima a barra de progresso com valor inteiro de 0 a 100.
   */
  private fun animarProgresso(target: Int) {
    ObjectAnimator.ofInt(binding.progressoPilar, "progress", target).apply {
      duration = 60L
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
     * Cria uma nova instância do fragmento com o ID do Pilar.
     */
    fun newInstance(pilarId: Int): TelaPilarComSubpilaresFragment {
      return TelaPilarComSubpilaresFragment().apply {
        arguments = Bundle().apply {
          putInt("pilarId", pilarId)
        }
      }
    }
  }

  private fun Date.toLocalDate(): LocalDate {
    return this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
  }
}
