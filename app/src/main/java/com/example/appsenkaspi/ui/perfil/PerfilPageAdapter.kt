package com.example.appsenkaspi.ui.perfil

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.appsenkaspi.ui.perfil.TrabalhosFragment

/**
 * Adapter responsável por fornecer os fragmentos correspondentes às abas
 * do perfil do usuário dentro do `PerfilFragment`.
 *
 * Abas disponíveis:
 *  - Posição 0: [DetalhesFragment] — mostra informações pessoais do funcionário.
 *  - Posição 1: [TrabalhosFragment] — mostra as atividades atribuídas ao funcionário.
 *
 * @param fragment Fragment pai que hospeda o ViewPager2 (normalmente o PerfilFragment)
 */
class PerfilPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

  /**
   * Define a quantidade de abas disponíveis no perfil.
   * Atualmente: 2 abas.
   */
  override fun getItemCount(): Int = 2

  /**
   * Cria e retorna o fragmento correspondente à aba da posição especificada.
   *
   * @param position Índice da aba (0 para Detalhes, 1 para Trabalhos)
   * @return Fragment correspondente à posição
   * @throws IllegalStateException Se a posição for inválida
   */
  override fun createFragment(position: Int): Fragment {
    return when (position) {
      0 -> DetalhesFragment()
      1 -> TrabalhosFragment()
      else -> throw IllegalStateException("Posição inesperada: $position")
    }
  }
}
