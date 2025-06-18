package com.example.appsenkaspi.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appsenkaspi.data.local.enums.Cargo
import com.example.appsenkaspi.R
import com.example.appsenkaspi.data.local.enums.StatusPilar
import com.example.appsenkaspi.ui.pilar.TelaPilarComSubpilaresFragment
import com.example.appsenkaspi.ui.pilar.TelaPilarFragment
import com.example.appsenkaspi.data.local.entity.PilarEntity
import com.example.appsenkaspi.databinding.FragmentHomeBinding
import com.example.appsenkaspi.extensions.configurarNotificacaoBadge
import com.example.appsenkaspi.ui.historico.HistoricoFragment
import com.example.appsenkaspi.ui.perfil.PilarAdapter
import com.example.appsenkaspi.ui.pilar.CriarPilarFragment
import com.example.appsenkaspi.viewmodel.AtividadeViewModel
import com.example.appsenkaspi.viewmodel.FuncionarioViewModel
import com.example.appsenkaspi.viewmodel.NotificacaoViewModel
import com.example.appsenkaspi.viewmodel.PilarViewModel
import kotlinx.coroutines.launch

/**
 * Fragmento principal da aplicação, exibindo os pilares ativos (planejados ou em andamento).
 *
 * Oferece opções de:
 * - Criação de novos pilares (somente para coordenadores);
 * - Acesso ao histórico de pilares concluídos, vencidos ou excluídos;
 * - Abertura de telas específicas conforme a estrutura do pilar (com ou sem subpilares).
 * Integra animações e observadores de LiveData para atualizações em tempo real.
 */
class HomeFragment : Fragment() {

  /** Binding para o layout do fragmento */
  private var _binding: FragmentHomeBinding? = null
  private val binding get() = _binding ?: throw IllegalStateException("Binding is null")

  private lateinit var recyclerView: RecyclerView
  private lateinit var cardAdicionarPilar: CardView
  private lateinit var adapter: PilarAdapter

  /** ViewModel com dados do funcionário logado */
  private val funcionarioViewModel: FuncionarioViewModel by activityViewModels()

  /** ViewModel para gerenciamento dos pilares */
  private val pilarViewModel: PilarViewModel by activityViewModels()

  /** ViewModel para atividades (usado indiretamente por ações futuras) */
  private val atividadeViewModel: AtividadeViewModel by activityViewModels()

  /** ViewModel para notificações e badges */
  private val notificacaoViewModel: NotificacaoViewModel by activityViewModels()

  /** ID do funcionário logado no sistema */
  private var funcionarioLogadoId: Int = -1

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentHomeBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val shakeAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.shake)
    val scaleAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.button_scale)

    // Observa o funcionário logado e inicia a lógica de interface com base no cargo
    funcionarioViewModel.funcionarioLogado.observe(viewLifecycleOwner) { funcionario ->
      funcionario?.let {
        funcionarioLogadoId = it.id

        configurarNotificacaoBadge(
          rootView = view,
          lifecycleOwner = viewLifecycleOwner,
          fragmentManager = parentFragmentManager,
          funcionarioId = it.id,
          cargo = it.cargo,
          viewModel = notificacaoViewModel
        )

        // Exibe botão de adicionar pilar apenas para coordenadores
        binding.cardAdicionarPilar.visibility =
          if (it.cargo == Cargo.COORDENADOR) View.VISIBLE else View.GONE

        recyclerView = binding.recyclerViewPilares
        cardAdicionarPilar = binding.cardAdicionarPilar

        adapter = PilarAdapter(
          onClickPilar = { pilar -> abrirTelaPilar(pilar) },
          verificarSubpilares = { pilarId -> pilarViewModel.temSubpilaresDireto(pilarId) }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Atualiza status dos pilares automaticamente
        lifecycleScope.launch {
          pilarViewModel.atualizarStatusDeTodosOsPilares()
        }

        // Exibe apenas pilares ativos (planejados ou em andamento)
        pilarViewModel.listarTodosPilares().observe(viewLifecycleOwner) { lista ->
          val listaFiltrada = lista.filter {
            it.status == StatusPilar.PLANEJADO || it.status == StatusPilar.EM_ANDAMENTO
          }
          adapter.submitList(listaFiltrada)
        }

        // Clique curto: animação e navegação para criação de pilar
        cardAdicionarPilar.setOnClickListener {
          it.startAnimation(scaleAnim)
          it.postDelayed({
            val fragment = CriarPilarFragment().apply {
              arguments = Bundle().apply {
                putInt("funcionarioId", funcionarioLogadoId)
              }
            }
            parentFragmentManager.beginTransaction()
              .setCustomAnimations(
                R.anim.pull_fade_in,
                R.anim.pull_fade_out,
                R.anim.pull_fade_in,
                R.anim.pull_fade_out
              )
              .replace(R.id.main_container, fragment)
              .addToBackStack(null)
              .commit()
          }, 200)
        }

        // Clique longo: efeito shake + scale
        cardAdicionarPilar.setOnLongClickListener {
          it.startAnimation(shakeAnim)
          it.startAnimation(scaleAnim)
          true
        }

        // Botão para acessar o histórico de pilares
        val boxHistorico = view.findViewById<View>(R.id.box_historico)
        boxHistorico.setOnClickListener {
          val historicoFragment = HistoricoFragment()
          parentFragmentManager.beginTransaction()
            .setCustomAnimations(
              R.anim.pull_fade_in,
              R.anim.pull_fade_out,
              R.anim.pull_fade_in,
              R.anim.pull_fade_out
            )
            .replace(R.id.main_container, historicoFragment)
            .addToBackStack(null)
            .commit()
        }

        // Define cor da status bar do app
        requireActivity().window.statusBarColor =
          ContextCompat.getColor(requireContext(), R.color.graybar)
      }
    }
  }

  /**
   * Abre a tela associada a um pilar, com ou sem subpilares.
   *
   * @param pilar Pilar selecionado pelo usuário.
   */
  private fun abrirTelaPilar(pilar: PilarEntity) {
    viewLifecycleOwner.lifecycleScope.launch {
      val temSubpilares = pilarViewModel.temSubpilares(pilar.id)

      val fragment = if (temSubpilares) {
        TelaPilarComSubpilaresFragment().apply {
          arguments = Bundle().apply { putInt("pilarId", pilar.id) }
        }
      } else {
        TelaPilarFragment().apply {
          arguments = Bundle().apply {
            putInt("pilarId", pilar.id)
            putInt("funcionarioId", funcionarioLogadoId)
          }
        }
      }

      parentFragmentManager.beginTransaction()
        .setCustomAnimations(
          R.anim.pull_fade_in,
          R.anim.pull_fade_out,
          R.anim.pull_fade_in,
          R.anim.pull_fade_out
        )
        .replace(R.id.main_container, fragment)
        .addToBackStack(null)
        .commit()
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}