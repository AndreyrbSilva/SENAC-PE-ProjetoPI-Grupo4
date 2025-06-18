package com.example.appsenkaspi.ui.dashboard

import android.R
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.appsenkaspi.viewmodel.FuncionarioViewModel
import com.example.appsenkaspi.viewmodel.NotificacaoViewModel
import com.example.appsenkaspi.viewmodel.PilarViewModel
import com.example.appsenkaspi.ui.dashboard.ResumoDashboard
import com.example.appsenkaspi.extensions.configurarNotificacaoBadge
import com.example.appsenkaspi.data.local.database.AppDatabase
import com.example.appsenkaspi.databinding.FragmentDashboardBinding
import com.example.appsenkaspi.viewmodel.AcaoViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragmento responsável por exibir o painel geral de progresso de pilares, ações e atividades.
 * Inclui gráfico de pizza (donut) com progresso agregado e gráfico de barras para comparação detalhada.
 * Permite filtragem por Pilar através de um Spinner.
 */


class DashboardFragment : Fragment() {
  /** Binding da View associada ao Fragmento */

  private var _binding: FragmentDashboardBinding? = null
  private val binding get() = _binding!!

  /** ViewModel para operações com ações */

  private val acaoViewModel: AcaoViewModel by viewModels()

  /** ViewModel para operações com pilares */

  private val pilarViewModel: PilarViewModel by viewModels()

  /** ViewModel compartilhado para dados do funcionário logado */

  private val funcionarioViewModel: FuncionarioViewModel by activityViewModels()

  /** ViewModel compartilhado para gerenciar notificações */

  private val notificacaoViewModel: NotificacaoViewModel by activityViewModels()

  /** Mapeamento entre nomes exibidos no Spinner e os IDs dos pilares */

  private val mapaNomesParaIds = mutableMapOf<String, Int?>()

  /** ID do funcionário logado */

  private var funcionarioLogadoId: Int = -1

  override fun onCreateView(
      inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentDashboardBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    // Observa o funcionário logado para configurar badges de notificação
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

        // Se quiser controlar algum botão pela permissão do cargo:
        // Exemplo (se tiver botão no layout):
        // binding.algumBotao.visibility = if (it.cargo == Cargo.COORDENADOR) View.VISIBLE else View.GONE
      }
    }

    configurarSpinner()
  }

  /**
   * Configura o spinner de seleção de Pilar e vincula ações aos eventos de seleção.
   */
  private fun configurarSpinner() {
    pilarViewModel.listarIdsENomes().observe(viewLifecycleOwner) { pilares ->
      val nomes = pilares.map { it.nome }
      val opcoes = listOf("Visão Geral") + nomes

      mapaNomesParaIds.clear()
      mapaNomesParaIds["Visão Geral"] = null
      pilares.forEach { mapaNomesParaIds[it.nome] = it.id }

      val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, opcoes)
      adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
      binding.spinnerFiltro.adapter = adapter
    }

    binding.spinnerFiltro.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        val nome = parent.getItemAtPosition(position).toString()
        val pilarId = mapaNomesParaIds[nome]
        carregarResumo(pilarId)
        carregarGraficoDeBarras(pilarId)
      }

      override fun onNothingSelected(parent: AdapterView<*>) {}
    }
  }
  /**
   * Carrega os dados agregados de atividades e progresso para um pilar específico ou para todos (visão geral).
   * @param pilarId ID do Pilar ou `null` para visão geral.
   */
  private fun carregarResumo(pilarId: Int?) {
    lifecycleScope.launch {
      if (pilarId == null) {
        // Visão geral (todos os pilares)
        val pilares = withContext(Dispatchers.IO) {
            pilarViewModel.getPilaresParaDashboard()
        }

        var totalAcoes = 0
        var totalAtividades = 0
        var atividadesConcluidas = 0
        var atividadesAndamento = 0
        var atividadesAtraso = 0

        val listaProgresso = mutableListOf<Float>()

        for (pilar in pilares) {
          val possuiSubpilares = withContext(Dispatchers.IO) {
              pilarViewModel.temSubpilaresDireto(pilar.id)
          }

          val resumo = if (possuiSubpilares) {
              withContext(Dispatchers.IO) {
                  pilarViewModel.gerarResumoPorSubpilaresDireto(pilar.id)
              }
          } else {
              withContext(Dispatchers.IO) {
                  acaoViewModel.gerarResumoDashboardDireto(pilar.id)
              }
          }

          val progresso = withContext(Dispatchers.IO) {
              pilarViewModel.calcularProgressoInterno(pilar.id)
          }

          listaProgresso.add(progresso)

          totalAcoes += resumo.totalAcoes
          totalAtividades += resumo.totalAtividades
          atividadesConcluidas += resumo.atividadesConcluidas
          atividadesAndamento += resumo.atividadesAndamento
          atividadesAtraso += resumo.atividadesAtraso
        }

        val mediaProgresso = if (listaProgresso.isNotEmpty()) {
          listaProgresso.average().toFloat()
        } else 0f

        val resumoGeral = ResumoDashboard(
            totalAcoes,
            totalAtividades,
            atividadesConcluidas,
            atividadesAndamento,
            atividadesAtraso
        )

          withContext(Dispatchers.Main) {
              atualizarResumoEDonut(resumoGeral, "Progressão geral dos pilares", mediaProgresso)
          }

      } else {
        // Pilar específico
        val possuiSubpilares = withContext(Dispatchers.IO) {
            pilarViewModel.temSubpilaresDireto(pilarId)
        }

        val resumo = if (possuiSubpilares) {
            withContext(Dispatchers.IO) {
                pilarViewModel.gerarResumoPorSubpilaresDireto(pilarId)
            }
        } else {
            withContext(Dispatchers.IO) {
                acaoViewModel.gerarResumoDashboardDireto(pilarId)
            }
        }

        val progressoReal = withContext(Dispatchers.IO) {
            pilarViewModel.calcularProgressoInterno(pilarId)
        }

          withContext(Dispatchers.Main) {
              atualizarResumoEDonut(resumo, "Progressão do Pilar", progressoReal)
          }
      }
    }
  }



  /**
   * Atualiza os valores do resumo de atividades e configura o gráfico de pizza com o progresso.
   * @param resumo Dados agregados das atividades
   * @param titulo Título descritivo do gráfico
   * @param progressoReal Valor do progresso [0,1] opcional para sobrescrever o cálculo padrão
   */
  private fun atualizarResumoEDonut(
      resumo: ResumoDashboard,
      titulo: String,
      progressoReal: Float? = null // novo parâmetro opcional
  ) {
    binding.valorTotal.text = resumo.totalAcoes.toString()
    binding.valorConcluidas.text = resumo.atividadesConcluidas.toString()
    binding.valorAndamento.text = resumo.atividadesAndamento.toString()
    binding.valorAtraso.text = resumo.atividadesAtraso.toString()
    binding.labelDonut.text = titulo

    val progressoUsado = progressoReal?.coerceIn(0f, 1f) ?: run {
      if (resumo.totalAtividades > 0)
        resumo.atividadesConcluidas.toFloat() / resumo.totalAtividades
      else 0f
    }

    // Agora o donut usará o progressoReal se disponível
    atualizarDonutChart(progressoUsado)
  }
  /**
   * Atualiza o gráfico de pizza (donut chart) com o progresso percentual.
   * @param progresso Valor entre 0 e 1 representando a fração concluída
   */
  private fun atualizarDonutChart(progresso: Float) {
    val progressoPercentual = (progresso * 100f).coerceIn(0f, 100f)
    val restante = 100f - progressoPercentual

    val entries = listOf(
        PieEntry(progressoPercentual),
        PieEntry(restante)
    )

    val dataSet = PieDataSet(entries, "").apply {
      setDrawValues(false)
      colors = listOf(
        Color.parseColor("#164773"), // Azul do progresso
        Color.parseColor("#181818")  // Cinza escuro para o restante
      )
    }

    val data = PieData(dataSet)

    binding.donutChart.apply {
      this.data = data
      description.isEnabled = false
      legend.isEnabled = false
      setUsePercentValues(false)
      setDrawHoleEnabled(true)
      setHoleColor(Color.TRANSPARENT)
      holeRadius = 70f
      setTransparentCircleAlpha(0)
      setDrawEntryLabels(false)

      centerText = "${progressoPercentual.toInt()}%"
      setCenterTextSize(18f)
      setCenterTextColor(Color.WHITE)

      invalidate()
      animateY(800)
    }
  }


  /**
   * Decide qual versão do gráfico de barras carregar com base na presença de subpilares.
   * @param pilarId ID do pilar ou `null` para visão geral
   */

  private fun carregarGraficoDeBarras(pilarId: Int?) {
    if (pilarId == null) {
      carregarGraficoDeBarrasVisaoGeral()
    } else {
      lifecycleScope.launch {
        val possuiSubpilares = withContext(Dispatchers.IO) {
            AppDatabase.Companion.getDatabase(requireContext())
                .subpilarDao()
                .existeSubpilarParaPilar(pilarId)
        }

        if (possuiSubpilares) {
          carregarGraficoDeBarrasPorSubpilares(pilarId)
        } else {
          carregarGraficoDeBarrasPorAcoes(pilarId)
        }
      }
    }
  }

  /**
   * Carrega o gráfico de barras com o progresso de cada pilar (modo visão geral).
   */
  private fun carregarGraficoDeBarrasVisaoGeral() {
    lifecycleScope.launch {
      withContext(Dispatchers.Main) {
          binding.labelBarChart.text = "Progresso de Cada Pilar"
      }

      val pilares = withContext(Dispatchers.IO) {
          pilarViewModel.getPilaresParaDashboard()
      }

      val nomes = mutableListOf<String>()
      val entradas = mutableListOf<BarEntry>()

      pilares.forEachIndexed { index, pilar ->
        val progresso = withContext(Dispatchers.IO) {
            pilarViewModel.calcularProgressoInterno(pilar.id)
        }
        nomes.add(pilar.nome)
        entradas.add(BarEntry(index.toFloat(), progresso * 100f))
      }

      if (entradas.isEmpty()) {
        binding.barChart.clear()
        binding.barChart.setNoDataText("Nenhum dado disponível para os pilares.")
        return@launch
      }

      val dataSet = BarDataSet(entradas, "Progresso por Pilar (%)").apply {
        valueTextColor = Color.WHITE
        valueTextSize = 12f
        color = Color.parseColor("#164773")
      }

      val barData = BarData(dataSet).apply {
        barWidth = 0.5f
      }

      with(binding.barChart) {
        data = barData
        setFitBars(true)
        description.isEnabled = false
        legend.isEnabled = false
        setDrawValueAboveBar(true)
        setTouchEnabled(false)
        animateY(800)
        setScaleEnabled(false)

        axisLeft.apply {
          axisMinimum = 0f
          axisMaximum = 100f
          granularity = 10f
          textColor = Color.WHITE
          textSize = 12f
        }

        axisRight.isEnabled = false

        xAxis.apply {
          position = XAxis.XAxisPosition.BOTTOM
          valueFormatter = IndexAxisValueFormatter(nomes)
          granularity = 1f
          isGranularityEnabled = true
          setDrawGridLines(false)
          textColor = Color.WHITE
          textSize = 10f
          labelRotationAngle = 0f
          setLabelCount(nomes.size, false)
          setAvoidFirstLastClipping(true)
          yOffset = 2f
        }

        invalidate()
      }
    }
  }

  /**
   * Carrega o gráfico de barras com o progresso de cada ação dentro do pilar.
   * @param pilarId ID do pilar sem subpilares
   */
  private fun carregarGraficoDeBarrasPorAcoes(pilarId: Int) {
    lifecycleScope.launch {
      binding.labelBarChart.text = "Progresso de Cada Ação"
      val progressoAcoes = withContext(Dispatchers.IO) {
          AppDatabase.Companion.getDatabase(requireContext())
              .acaoDao()
              .listarProgressoPorPilar(pilarId)
      }

      if (progressoAcoes.isEmpty()) {
        binding.barChart.clear()
        binding.barChart.setNoDataText("Nenhuma ação encontrada para este pilar.")
        return@launch
      }

      val entries = progressoAcoes.mapIndexed { index, acao ->
          BarEntry(index.toFloat(), acao.progresso * 100f)
      }

      val labels = progressoAcoes.map { it.nome }

      val dataSet = BarDataSet(entries, "Progresso (%)").apply {
        valueTextColor = Color.WHITE
        valueTextSize = 12f
        color = Color.parseColor("#164773")
      }

      val barData = BarData(dataSet).apply {
        barWidth = 0.5f
      }

      with(binding.barChart) {
        data = barData
        setFitBars(true)
        description.isEnabled = false
        legend.isEnabled = false
        setDrawValueAboveBar(true)
        setTouchEnabled(false)
        animateY(800)
        setScaleEnabled(false)

        axisLeft.apply {
          axisMinimum = 0f
          axisMaximum = 100f
          granularity = 10f
          textColor = Color.WHITE
          textSize = 12f
        }

        axisRight.isEnabled = false

        xAxis.apply {
          position = XAxis.XAxisPosition.BOTTOM
          valueFormatter = IndexAxisValueFormatter(labels)
          granularity = 1f
          isGranularityEnabled = true
          setDrawGridLines(false)
          textColor = Color.WHITE
          textSize = 10f
          labelRotationAngle = 0f
          setLabelCount(labels.size, false)
          setAvoidFirstLastClipping(true)
          yOffset = 2f
        }

        invalidate()
      }
    }
  }
  /**
   * Carrega o gráfico de barras com o progresso de cada subpilar dentro do pilar.
   * @param pilarId ID do pilar com subpilares
   */

  private fun carregarGraficoDeBarrasPorSubpilares(pilarId: Int) {
    lifecycleScope.launch {
      binding.labelBarChart.text = "Progresso de Cada Subpilar"
      val progressoSubpilares = withContext(Dispatchers.IO) {
          pilarViewModel.calcularProgressoDosSubpilares(pilarId)
      }

      if (progressoSubpilares.isEmpty()) {
        binding.barChart.clear()
        binding.barChart.setNoDataText("Nenhum subpilar encontrado para este pilar.")
        return@launch
      }

      val entries = progressoSubpilares.mapIndexed { index, (_, progresso) ->
          BarEntry(index.toFloat(), progresso * 100f)
      }

      val labels = progressoSubpilares.map { it.first }

      val dataSet = BarDataSet(entries, "Progresso por Subpilar (%)").apply {
        valueTextColor = Color.WHITE
        valueTextSize = 12f
        color = Color.parseColor("#164773")
      }

      val barData = BarData(dataSet).apply {
        barWidth = 0.5f
      }

      with(binding.barChart) {
        data = barData
        setFitBars(true)
        description.isEnabled = false
        legend.isEnabled = false
        setDrawValueAboveBar(true)
        setTouchEnabled(false)
        animateY(800)
        setScaleEnabled(false)

        axisLeft.apply {
          axisMinimum = 0f
          axisMaximum = 100f
          granularity = 10f
          textColor = Color.WHITE
          textSize = 12f
        }

        axisRight.isEnabled = false

        xAxis.apply {
          position = XAxis.XAxisPosition.BOTTOM
          valueFormatter = IndexAxisValueFormatter(labels)
          granularity = 1f
          isGranularityEnabled = true
          setDrawGridLines(false)
          textColor = Color.WHITE
          textSize = 10f
          labelRotationAngle = 0f
          setLabelCount(labels.size, false)
          setAvoidFirstLastClipping(true)
          yOffset = 2f
        }

        invalidate()
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
