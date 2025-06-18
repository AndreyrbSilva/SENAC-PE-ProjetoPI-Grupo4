package com.example.appsenkaspi.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.appsenkaspi.R
import com.example.appsenkaspi.ui.relatorio.RelatorioFragment
import com.example.appsenkaspi.databinding.FragmentBottomNavBinding
import com.example.appsenkaspi.ui.dashboard.DashboardFragment
import com.example.appsenkaspi.ui.home.HomeFragment
import com.example.appsenkaspi.ui.perfil.PerfilFragment
import com.example.appsenkaspi.viewmodel.FuncionarioViewModel

/**
 * Fragmento responsável por gerenciar a barra de navegação inferior (Bottom Navigation).
 *
 * Este fragmento:
 * - Controla o deslocamento do indicador visual animado abaixo do item selecionado;
 * - Realiza a navegação entre fragmentos (Home, Dashboard, Relatório, Perfil);
 * - Aplica animações suaves de transição entre as telas;
 * - Escuta o estado do funcionário logado para iniciar a navegação.
 */
class BottomNavFragment : Fragment() {

  /** Binding do layout do fragmento que contém os itens de navegação inferior */
  private lateinit var binding: FragmentBottomNavBinding

  /** ViewModel compartilhado com os dados do funcionário logado */
  private val funcionarioViewModel: FuncionarioViewModel by activityViewModels()

  /** ID do item atualmente selecionado na barra de navegação */
  private var currentSelectedId: Int = R.id.nav_home

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentBottomNavBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupClickListeners()

    // Inicia a navegação ao observar o funcionário logado
    funcionarioViewModel.funcionarioLogado.observe(viewLifecycleOwner) { funcionario ->
      funcionario?.let {
        updateIndicator(R.id.nav_home)

        val atual = parentFragmentManager.findFragmentById(R.id.main_container)
        if (atual == null) {
          navigateTo(HomeFragment(), R.id.nav_home)
        }
      }
    }
  }

  /**
   * Configura os ouvintes de clique para cada item da barra de navegação inferior.
   */
  private fun setupClickListeners() {
    binding.navHome.setOnClickListener {
      updateIndicator(R.id.nav_home)
      navigateTo(HomeFragment(), R.id.nav_home)
    }

    binding.navDashboard.setOnClickListener {
      updateIndicator(R.id.nav_dashboard)
      navigateTo(DashboardFragment(), R.id.nav_dashboard)
    }

    binding.navRelatorio.setOnClickListener {
      updateIndicator(R.id.nav_relatorio)
      navigateTo(RelatorioFragment(), R.id.nav_relatorio)
    }

    binding.navPerfil.setOnClickListener {
      updateIndicator(R.id.nav_perfil)
      navigateTo(PerfilFragment(), R.id.nav_perfil)
    }
  }

  /**
   * Atualiza o posicionamento do indicador deslizante e o estado visual dos ícones.
   *
   * @param selectedId ID do item atualmente selecionado.
   */
  private fun updateIndicator(selectedId: Int) {
    val itemViews = mapOf(
      R.id.nav_relatorio to binding.navRelatorio,
      R.id.nav_dashboard to binding.navDashboard,
      R.id.nav_home to binding.navHome,
      R.id.nav_perfil to binding.navPerfil
    )

    val icons = mapOf(
      R.id.nav_relatorio to binding.iconRelatorio,
      R.id.nav_dashboard to binding.iconDashboard,
      R.id.nav_home to binding.iconHome,
      R.id.nav_perfil to binding.iconPerfil
    )

    val selectedView = itemViews[selectedId] ?: return
    val indicator = binding.indicatorSlider

    selectedView.post {
      val targetX = selectedView.left + selectedView.width / 2 - indicator.width / 2
      indicator.animate()
        .translationX(targetX.toFloat())
        .setDuration(250)
        .start()
    }

    icons.forEach { (id, icon) ->
      icon.isSelected = (id == selectedId)
    }
  }

  /**
   * Realiza a transição entre fragmentos de acordo com o item selecionado.
   * Aplica animações diferentes para navegação à direita ou à esquerda.
   *
   * @param fragment Fragmento que será exibido.
   * @param selectedId ID do item selecionado.
   */
  private fun navigateTo(fragment: Fragment, selectedId: Int) {
    if (selectedId == currentSelectedId) return

    val orderMap = mapOf(
      R.id.nav_home to 0,
      R.id.nav_dashboard to 1,
      R.id.nav_relatorio to 2,
      R.id.nav_perfil to 3
    )

    val currentIndex = orderMap[currentSelectedId] ?: 0
    val newIndex = orderMap[selectedId] ?: 0

    val (enterAnim, exitAnim, popEnterAnim, popExitAnim) = if (newIndex > currentIndex) {
      listOf(
        R.anim.slide_fade_in_right,
        R.anim.slide_fade_out_left,
        R.anim.slide_fade_in_left,
        R.anim.slide_fade_out_right
      )
    } else {
      listOf(
        R.anim.slide_fade_in_left,
        R.anim.slide_fade_out_right,
        R.anim.slide_fade_in_right,
        R.anim.slide_fade_out_left
      )
    }

    currentSelectedId = selectedId

    requireActivity().supportFragmentManager.beginTransaction()
      .setCustomAnimations(enterAnim, exitAnim, popEnterAnim, popExitAnim)
      .replace(R.id.main_container, fragment)
      .addToBackStack(null)
      .commit()
  }
}
