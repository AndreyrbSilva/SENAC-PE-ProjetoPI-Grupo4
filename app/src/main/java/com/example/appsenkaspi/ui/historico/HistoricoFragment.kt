package com.example.appsenkaspi.ui.historico

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appsenkaspi.viewmodel.NotificacaoViewModel
import com.example.appsenkaspi.data.local.entity.PilarEntity
import com.example.appsenkaspi.viewmodel.PilarViewModel
import com.example.appsenkaspi.R
import com.example.appsenkaspi.data.local.enums.StatusPilar
import com.example.appsenkaspi.ui.pilar.TelaPilarComSubpilaresFragment
import com.example.appsenkaspi.ui.pilar.TelaPilarFragment
import com.example.appsenkaspi.extensions.configurarBotaoVoltar
import com.example.appsenkaspi.extensions.configurarNotificacaoBadge
import com.example.appsenkaspi.viewmodel.FuncionarioViewModel
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * Fragmento responsável por exibir o histórico de pilares concluídos, excluídos ou vencidos.
 *
 * Permite ao usuário aplicar filtros por status e por ano de prazo/exclusão, utilizando dois spinners interativos.
 * Ao selecionar um item da lista, navega dinamicamente para a tela de detalhes do Pilar, com suporte a subpilares.
 */
class HistoricoFragment : Fragment() {

  /** Adapter utilizado para renderizar os cartões de pilares filtrados no histórico */
  private lateinit var adapter: TelaHistoricoAdapter

  /** ViewModel compartilhado para acesso a dados dos pilares */
  private val pilarViewModel: PilarViewModel by activityViewModels()

  /** ViewModel para obter dados do funcionário logado */
  private val funcionarioViewModel: FuncionarioViewModel by activityViewModels()

  /** ViewModel compartilhado para controle de notificações e badges */
  private val notificacaoViewModel: NotificacaoViewModel by activityViewModels()

  /** ID do funcionário logado no sistema */
  private var funcionarioLogadoId: Int = -1

  /** Lista original de pilares, usada como base para os filtros aplicados */
  private var listaOriginal: List<PilarEntity> = emptyList()

  /** Valor selecionado no filtro por status */
  private var filtroStatusSelecionado: String = "Todos"

  /** Valor selecionado no filtro por ano */
  private var filtroAnoSelecionado: String = "Todos"

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return inflater.inflate(R.layout.fragment_historico, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    configurarBotaoVoltar(view)

    // Observa o funcionário logado para configurar notificações e capturar o ID
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
        funcionarioLogadoId = it.id
      }
    }

    val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerFiltroExclusao)
    recyclerView.layoutManager = LinearLayoutManager(requireContext())

    // Inicializa o adapter da lista de histórico
    adapter = TelaHistoricoAdapter(pilarViewModel, emptyList()) { pilar ->
      abrirTelaPilar(pilar)
    }
    recyclerView.adapter = adapter

    val spinnerStatusFilter = view.findViewById<MaterialAutoCompleteTextView>(R.id.spinnerStatusFilter)
    val spinnerAnoFiltro = view.findViewById<MaterialAutoCompleteTextView>(R.id.spinnerAnoFiltro)

    val itensStatus = resources.getStringArray(R.array.status_pilar_array).toMutableList()
    if (!itensStatus.contains("Todos")) {
      itensStatus.add(0, "Todos")
    }

    val adapterStatus = ArrayAdapter(requireContext(), R.layout.dropdown_item_white, itensStatus)
    spinnerStatusFilter.setAdapter(adapterStatus)
    spinnerStatusFilter.setText("Todos", false)

    // Observa lista de pilares e prepara filtros por ano
    pilarViewModel.listarTodosPilares().observe(viewLifecycleOwner) { lista ->
      listaOriginal = lista.filter {
        it.status == StatusPilar.CONCLUIDO ||
          it.status == StatusPilar.EXCLUIDO ||
          it.status == StatusPilar.VENCIDO
      }

      val anosSet = listaOriginal.mapNotNull {
        extrairAno(it.dataPrazo) ?: extrairAno(it.dataExclusao)
      }.toSet()

      val listaAnos = anosSet.sortedDescending().map { it.toString() }.toMutableList()
      if (!listaAnos.contains("Todos")) {
        listaAnos.add(0, "Todos")
      }

      val adapterAno = ArrayAdapter(requireContext(), R.layout.dropdown_item_white, listaAnos)
      spinnerAnoFiltro.setAdapter(adapterAno)
      spinnerAnoFiltro.setText("Todos", false)

      aplicarFiltros()
    }

    // Configura ações para exibir os dropdowns ao focar/clicar
    spinnerStatusFilter.setOnFocusChangeListener { _, hasFocus ->
      if (hasFocus) spinnerStatusFilter.showDropDown()
    }
    spinnerStatusFilter.setOnClickListener {
      spinnerStatusFilter.showDropDown()
    }

    spinnerAnoFiltro.setOnFocusChangeListener { _, hasFocus ->
      if (hasFocus) spinnerAnoFiltro.showDropDown()
    }
    spinnerAnoFiltro.setOnClickListener {
      spinnerAnoFiltro.showDropDown()
    }

    // Ações ao selecionar item de filtro por status
    spinnerStatusFilter.setOnItemClickListener { parent, _, position, _ ->
      filtroStatusSelecionado = parent.getItemAtPosition(position).toString()
      aplicarFiltros()
    }

    // Ações ao selecionar item de filtro por ano
    spinnerAnoFiltro.setOnItemClickListener { parent, _, position, _ ->
      filtroAnoSelecionado = parent.getItemAtPosition(position).toString()
      aplicarFiltros()
    }
  }

  /**
   * Aplica os filtros de status e ano à lista de pilares e atualiza o adapter.
   */
  private fun aplicarFiltros() {
    val filtrada = listaOriginal.filter { pilar ->
      val statusOk = filtroStatusSelecionado == "Todos" ||
        pilar.status.name == filtroStatusSelecionado.uppercase()

      val anoPilar = extrairAno(pilar.dataPrazo) ?: extrairAno(pilar.dataExclusao)
      val anoOk = filtroAnoSelecionado == "Todos" || anoPilar?.toString() == filtroAnoSelecionado

      statusOk && anoOk
    }
    adapter.atualizarLista(filtrada)
  }

  /**
   * Extrai o ano de uma data, retornando `null` caso a data seja nula.
   *
   * @param data Objeto `Date` do qual se deseja extrair o ano.
   * @return Ano no formato `Int`, ou `null` se a data for nula.
   */
  private fun extrairAno(data: Date?): Int? {
    data ?: return null
    val cal = Calendar.getInstance()
    cal.time = data
    return cal.get(Calendar.YEAR)
  }

  /**
   * Abre a tela do pilar correspondente, decidindo entre tela com ou sem subpilares.
   *
   * @param pilar Pilar selecionado pelo usuário.
   */
  private fun abrirTelaPilar(pilar: PilarEntity) {
    viewLifecycleOwner.lifecycleScope.launch {
      val temSubpilares = pilarViewModel.temSubpilares(pilar.id)

      val fragment = if (temSubpilares) {
        TelaPilarComSubpilaresFragment().apply {
          arguments = Bundle().apply {
            putInt("pilarId", pilar.id)
          }
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
        .replace(R.id.main_container, fragment)
        .addToBackStack(null)
        .commit()
    }
  }
}
