package com.example.appsenkaspi.ui.pilar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appsenkaspi.R
import com.example.appsenkaspi.ui.pilar.TelaPilarFragment
import com.example.appsenkaspi.ui.perfil.PilarAdapter
import com.example.appsenkaspi.viewmodel.PilarViewModel

/**
 * Fragmento responsável por exibir uma lista de Pilares em um RecyclerView.
 *
 * Esta classe utiliza o `PilarAdapter` para renderizar visualmente cada Pilar na lista,
 * e o `PilarViewModel` para obter os dados da base via LiveData.
 *
 * A navegação ocorre automaticamente para a tela de detalhes do Pilar (`TelaPilarFragment`)
 * ao clicar em um item da lista.
 *
 * Funcionalidades principais:
 * - Observa a lista de Pilares em tempo real.
 * - Verifica dinamicamente se o Pilar possui Subpilares (ícone condicional).
 * - Gerencia o clique no item para navegação.
 *
 * Requisitos:
 * - O layout XML deve conter um RecyclerView com o ID `recyclerViewPilares`.
 * - O container de fragmentos principal deve possuir o ID `main_container`.
 */
class RecyclerViewPilar : Fragment() {

  private lateinit var recyclerView: RecyclerView
  private lateinit var adapter: PilarAdapter

  private val pilarViewModel: PilarViewModel by activityViewModels()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return inflater.inflate(R.layout.fragment_recycler_view_pilar, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    recyclerView = view.findViewById(R.id.recyclerViewPilares)

    adapter = PilarAdapter(
      onClickPilar = { pilar -> abrirTelaDoPilar(pilar.id) },
      verificarSubpilares = { id -> pilarViewModel.temSubpilaresDireto(id) }
    )

    recyclerView.layoutManager = LinearLayoutManager(requireContext())
    recyclerView.adapter = adapter

    // Observa mudanças na lista de pilares e atualiza o adapter
    pilarViewModel.listarTodosPilares().observe(viewLifecycleOwner) { pilares ->
      adapter.submitList(pilares)
    }
  }

  /**
   * Navega para a tela do Pilar selecionado.
   *
   * @param pilarId ID do Pilar a ser exibido.
   */
  private fun abrirTelaDoPilar(pilarId: Int) {
    parentFragmentManager.beginTransaction()
      .replace(
        R.id.main_container,
        TelaPilarFragment().apply {
          arguments = Bundle().apply { putInt("pilarId", pilarId) }
        }
      )
      .addToBackStack(null)
      .commit()
  }
}
