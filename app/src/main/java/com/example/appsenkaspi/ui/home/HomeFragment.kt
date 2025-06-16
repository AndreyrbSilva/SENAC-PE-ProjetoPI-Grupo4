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

class HomeFragment : Fragment() {

  private var _binding: FragmentHomeBinding? = null
  private val binding get() = _binding ?: throw IllegalStateException("Binding is null")

  private lateinit var recyclerView: RecyclerView
  private lateinit var cardAdicionarPilar: CardView
  private lateinit var adapter: PilarAdapter

  private val funcionarioViewModel: FuncionarioViewModel by activityViewModels()
  private val pilarViewModel: PilarViewModel by activityViewModels()
  private val atividadeViewModel: AtividadeViewModel by activityViewModels()
  private val notificacaoViewModel: NotificacaoViewModel by activityViewModels()

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

        lifecycleScope.launch {
          pilarViewModel.atualizarStatusDeTodosOsPilares()
        }

        pilarViewModel.listarTodosPilares().observe(viewLifecycleOwner) { lista ->
          val listaFiltrada = lista.filter {
            it.status == StatusPilar.PLANEJADO || it.status == StatusPilar.EM_ANDAMENTO
          }
          adapter.submitList(listaFiltrada)
        }

        // Clique curto com animação + navegação
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

        // Clique longo: shake + scale
        cardAdicionarPilar.setOnLongClickListener {
          it.startAnimation(shakeAnim)
          it.startAnimation(scaleAnim)
          true
        }

        // Botão de histórico com animação
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

        requireActivity().window.statusBarColor =
          ContextCompat.getColor(requireContext(), R.color.graybar)
      }
    }
  }

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