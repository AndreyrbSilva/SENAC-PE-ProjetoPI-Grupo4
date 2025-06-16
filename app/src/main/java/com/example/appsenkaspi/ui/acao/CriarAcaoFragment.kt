package com.example.appsenkaspi.ui.acao

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appsenkaspi.data.local.enums.Cargo
import com.example.appsenkaspi.data.local.entity.FuncionarioEntity
import com.example.appsenkaspi.ui.funcionario.FuncionarioSelecionadoAdapter
import com.example.appsenkaspi.viewmodel.FuncionarioViewModel
import com.example.appsenkaspi.viewmodel.NotificacaoViewModel
import com.example.appsenkaspi.data.local.entity.RequisicaoEntity
import com.example.appsenkaspi.ui.dialog.SelecionarResponsavelDialogFragment
import com.example.appsenkaspi.data.local.enums.StatusAcao
import com.example.appsenkaspi.data.local.enums.StatusRequisicao
import com.example.appsenkaspi.data.local.enums.TipoRequisicao
import com.example.appsenkaspi.extensions.configurarBotaoVoltar
import com.example.appsenkaspi.extensions.configurarNotificacaoBadge
import com.example.appsenkaspi.data.local.database.AppDatabase
import com.example.appsenkaspi.data.local.entity.AcaoEntity
import com.example.appsenkaspi.data.local.entity.AcaoFuncionarioEntity
import com.example.appsenkaspi.databinding.FragmentCriarAcaoBinding
import com.example.appsenkaspi.viewmodel.AcaoFuncionarioViewModel
import com.example.appsenkaspi.viewmodel.AcaoViewModel
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Fragmento responsável por permitir a criação de uma nova Ação, vinculada a um Pilar ou Subpilar.
 * Este fragmento trata tanto da lógica de criação direta (Coordenador) quanto da geração de requisição (Apoio).
 */
class CriarAcaoFragment : Fragment() {

  // ViewBinding do fragmento
  private var _binding: FragmentCriarAcaoBinding? = null
  private val binding get() = _binding!!

  /** ViewModel principal da Ação */
  private val acaoViewModel: AcaoViewModel by activityViewModels()

  /** ViewModel responsável pelo relacionamento Ação-Funcionário */
  private val acaoFuncionarioViewModel: AcaoFuncionarioViewModel by activityViewModels()

  /** ViewModel do Funcionário logado e dados de funcionários */
  private val funcionarioViewModel: FuncionarioViewModel by activityViewModels()

  /** ViewModel de notificações e requisições */
  private val requisicaoViewModel: NotificacaoViewModel by activityViewModels()

  /** Armazena a data de prazo selecionada pelo usuário */
  private var dataPrazoSelecionada: Date? = null

  /** Instância de calendário utilizada pelo DatePicker */
  private val calendario = Calendar.getInstance()

  /** Identificador do Subpilar (caso ação esteja vinculada a um) */
  private var subpilarId: Int? = null

  /** Identificador do Pilar (caso ação esteja vinculada a um) */
  private var pilarId: Int? = null

  /** Lista de funcionários responsáveis selecionados para a ação */
  private val funcionariosSelecionados = mutableListOf<FuncionarioEntity>()

  /** Adaptador que exibe os responsáveis selecionados */
  private lateinit var adapterSelecionados: FuncionarioSelecionadoAdapter

  /** Alias para o ViewModel de notificação para uso genérico */
  private val notificacaoViewModel: NotificacaoViewModel by activityViewModels()

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentCriarAcaoBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    configurarBotaoVoltar(view)

    // Observa e configura o badge de notificação para o funcionário logado
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

    // Recupera argumentos de Pilar/Subpilar
    pilarId = arguments?.getInt("pilarId")?.takeIf { it > 0 }
    subpilarId = arguments?.getInt("subpilarId")?.takeIf { it > 0 }

    if (pilarId == null && subpilarId == null) {
      Toast.makeText(requireContext(), "Erro: Pilar ou Subpilar não informado!", Toast.LENGTH_SHORT).show()
      parentFragmentManager.popBackStack()
      return
    }

    // Alterna botões conforme cargo
    funcionarioViewModel.funcionarioLogado.observe(viewLifecycleOwner) { funcionario ->
      when (funcionario?.cargo) {
        Cargo.APOIO -> {
          binding.buttonConfirmacaoAcao.visibility = View.GONE
          binding.buttonPedirConfirmacaoAcao.visibility = View.VISIBLE
        }
        Cargo.COORDENADOR -> {
          binding.buttonConfirmacaoAcao.visibility = View.VISIBLE
          binding.buttonPedirConfirmacaoAcao.visibility = View.GONE
        }
        else -> {
          binding.buttonConfirmacaoAcao.visibility = View.GONE
          binding.buttonPedirConfirmacaoAcao.visibility = View.GONE
        }
      }
    }

    // Inicializa RecyclerView para exibir os funcionários responsáveis
    adapterSelecionados = FuncionarioSelecionadoAdapter(funcionariosSelecionados)
    binding.recyclerViewFuncionariosSelecionados.layoutManager =
      LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    binding.recyclerViewFuncionariosSelecionados.adapter = adapterSelecionados

    // Abertura do seletor de data
    binding.buttonPickDateAcao.setOnClickListener { abrirDatePicker() }

    // Abre diálogo para seleção de responsáveis
    binding.iconSelecionarFuncionario.setOnClickListener {
      SelecionarResponsavelDialogFragment().show(childFragmentManager, "SelecionarFuncionariosDialog")
    }

    // Caso o usuário for Apoio, envia requisição
    binding.buttonPedirConfirmacaoAcao.setOnClickListener {
      val nome = binding.inputNomeAcao.text.toString().trim()
      val descricao = binding.inputDescricaoAcao.text.toString().trim()
      val funcionarioLogado = funcionarioViewModel.funcionarioLogado.value

      if (nome.isEmpty() || dataPrazoSelecionada == null || funcionariosSelecionados.isEmpty() || funcionarioLogado == null) {
        Toast.makeText(requireContext(), "Preencha todos os campos obrigatórios!", Toast.LENGTH_SHORT).show()
        return@setOnClickListener
      }

      viewLifecycleOwner.lifecycleScope.launch {
        if (!validarPrazoAcao()) return@launch

        val nomeEstrutura = when {
          subpilarId != null -> acaoViewModel.buscarNomeSubpilarPorId(subpilarId!!) ?: "Subpilar"
          pilarId != null -> acaoViewModel.buscarPilarPorId(pilarId!!)?.nome ?: "Pilar"
          else -> "Estrutura não identificada"
        }

        val acaoJson = AcaoJson(
          nome = nome,
          descricao = descricao,
          dataPrazo = dataPrazoSelecionada!!,
          dataInicio = Date(),
          criadoPor = funcionarioLogado.id,
          status = StatusAcao.PLANEJADA,
          dataCriacao = Date(),
          pilarId = if (subpilarId == null) pilarId else null,
          subpilarId = subpilarId,
          nomePilar = nomeEstrutura,
          responsaveis = funcionariosSelecionados.map { it.id }
        )

        val requisicao = RequisicaoEntity(
          tipo = TipoRequisicao.CRIAR_ACAO,
          acaoJson = Gson().toJson(acaoJson),
          status = StatusRequisicao.PENDENTE,
          solicitanteId = funcionarioLogado.id,
          dataSolicitacao = Date()
        )

        AppDatabase.getDatabase(requireContext()).requisicaoDao().inserir(requisicao)

        Toast.makeText(requireContext(), "Requisição de ação enviada para aprovação!", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
      }
    }

    // Coordenador cria a ação diretamente
    binding.buttonConfirmacaoAcao.setOnClickListener {
      viewLifecycleOwner.lifecycleScope.launch {
        if (!validarPrazoAcao()) return@launch
        confirmarCriacaoAcao()
      }
    }

    // Recebe funcionários selecionados no diálogo
    childFragmentManager.setFragmentResultListener("funcionariosSelecionados", viewLifecycleOwner) { _, bundle ->
      val lista = bundle.getParcelableArrayList<FuncionarioEntity>("listaFuncionarios") ?: arrayListOf()
      funcionariosSelecionados.clear()
      funcionariosSelecionados.addAll(lista.filterNotNull())
      adapterSelecionados.notifyDataSetChanged()
    }
  }

  /**
   * Exibe um DatePicker e atualiza o campo com a data selecionada.
   */
  private fun abrirDatePicker() {
    DatePickerDialog(
      requireContext(),
      { _, ano, mes, dia ->
        calendario.set(ano, mes, dia)
        dataPrazoSelecionada = calendario.time
        val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.buttonPickDateAcao.text = fmt.format(dataPrazoSelecionada!!)
      },
      calendario.get(Calendar.YEAR),
      calendario.get(Calendar.MONTH),
      calendario.get(Calendar.DAY_OF_MONTH)
    ).show()
  }

  /**
   * Confirma diretamente a criação de uma nova Ação no banco de dados local.
   * Disponível apenas para Coordenadores.
   */
  private fun confirmarCriacaoAcao() {
    val nome = binding.inputNomeAcao.text.toString().trim()
    val descricao = binding.inputDescricaoAcao.text.toString().trim()
    val funcionarioLogado = funcionarioViewModel.funcionarioLogado.value

    if (funcionarioLogado == null) {
      Toast.makeText(context, "Erro: usuário não autenticado!", Toast.LENGTH_SHORT).show()
      return
    }

    if (nome.isEmpty()) {
      binding.inputNomeAcao.error = "Digite o nome da ação"
      return
    }

    if (dataPrazoSelecionada == null) {
      binding.buttonPickDateAcao.error = "Selecione uma data de prazo"
      return
    }

    if (funcionariosSelecionados.isEmpty()) {
      Toast.makeText(requireContext(), "Selecione pelo menos um funcionário!", Toast.LENGTH_SHORT).show()
      return
    }

    viewLifecycleOwner.lifecycleScope.launch {
      try {
        val idNovaAcao = acaoViewModel.criarAcaoSegura(
          AcaoEntity(
            nome = nome,
            descricao = descricao,
            dataInicio = Date(),
            dataPrazo = dataPrazoSelecionada!!,
            pilarId = if (subpilarId == null) pilarId else null,
            subpilarId = subpilarId,
            status = StatusAcao.PLANEJADA,
            criadoPor = funcionarioLogado.id,
            dataCriacao = Date()
          )
        )

        // Relaciona funcionários à nova ação
        funcionariosSelecionados.forEach { func ->
          acaoFuncionarioViewModel.inserir(
            AcaoFuncionarioEntity(
              acaoId = idNovaAcao,
              funcionarioId = func.id
            )
          )
        }

        Toast.makeText(requireContext(), "Ação criada com sucesso!", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
      } catch (e: IllegalArgumentException) {
        Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
      }
    }
  }

  /**
   * Valida se a data de prazo selecionada está dentro dos limites do Pilar/Subpilar.
   * @return true se a data for válida, false caso contrário.
   */
  private suspend fun validarPrazoAcao(): Boolean {
    if (dataPrazoSelecionada == null) {
      binding.buttonPickDateAcao.error = "Selecione uma data de prazo"
      return false
    }

    val dataLimite: Date? = when {
      subpilarId != null -> acaoViewModel.buscarSubpilarPorId(subpilarId!!)?.dataPrazo
      pilarId != null -> acaoViewModel.buscarPilarPorId(pilarId!!)?.dataPrazo
      else -> null
    }

    dataLimite?.let {
      val selecionadaTruncada = truncarData(dataPrazoSelecionada!!)
      val limiteTruncado = truncarData(it)

      if (selecionadaTruncada.after(limiteTruncado)) {
        val nomeEstrutura = if (subpilarId != null) "subpilar" else "pilar"
        val dataFormatada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(limiteTruncado)
        Toast.makeText(
          requireContext(),
          "A data da ação não pode ultrapassar o prazo do $nomeEstrutura ($dataFormatada).",
          Toast.LENGTH_LONG
        ).show()
        return false
      }
    }

    return true
  }

  /**
   * Remove horas, minutos e segundos de uma data, para comparação apenas por dia.
   */
  private fun truncarData(data: Date): Date {
    val cal = Calendar.getInstance()
    cal.time = data
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.time
  }

  companion object {
    /**
     * Cria uma nova instância do fragmento, associada a um Pilar ou Subpilar.
     * @param pilarId ID do Pilar (opcional)
     * @param subpilarId ID do Subpilar (opcional)
     */
    fun newInstance(pilarId: Int? = null, subpilarId: Int? = null): CriarAcaoFragment {
      return CriarAcaoFragment().apply {
        arguments = Bundle().apply {
          pilarId?.let { putInt("pilarId", it) }
          subpilarId?.let { putInt("subpilarId", it) }
        }
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
