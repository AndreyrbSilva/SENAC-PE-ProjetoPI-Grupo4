package com.example.appsenkaspi.ui.atividade

import android.app.DatePickerDialog
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.appsenkaspi.data.local.enums.Cargo
import com.example.appsenkaspi.data.local.entity.FuncionarioEntity
import com.example.appsenkaspi.viewmodel.FuncionarioViewModel
import com.example.appsenkaspi.viewmodel.NotificacaoViewModel
import com.example.appsenkaspi.data.local.enums.PrioridadeAtividade
import com.example.appsenkaspi.R
import com.example.appsenkaspi.data.local.entity.RequisicaoEntity
import com.example.appsenkaspi.ui.dialog.SelecionarResponsavelDialogFragment
import com.example.appsenkaspi.data.local.enums.StatusRequisicao
import com.example.appsenkaspi.data.local.enums.TipoRequisicao
import com.example.appsenkaspi.extensions.configurarNotificacaoBadge
import com.example.appsenkaspi.data.local.database.AppDatabase
import com.example.appsenkaspi.data.local.entity.AtividadeEntity
import com.example.appsenkaspi.data.local.entity.AtividadeFuncionarioEntity
import com.example.appsenkaspi.databinding.FragmentEditarAtividadeBinding
import com.example.appsenkaspi.domain.model.AtividadeComFuncionarios
import com.example.appsenkaspi.ui.dialog.EscolherPrioridadeDialogFragment
import com.example.appsenkaspi.viewmodel.AcaoViewModel
import com.example.appsenkaspi.viewmodel.AtividadeViewModel
import com.google.gson.Gson
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditarAtividadeFragment : Fragment() {

  private var _binding: FragmentEditarAtividadeBinding? = null
  private val binding get() = _binding!!
  private var dataPrazoAcao: Date? = null
  private val atividadeViewModel: AtividadeViewModel by activityViewModels()
  private val funcionarioViewModel: FuncionarioViewModel by activityViewModels()
  private val acaoViewModel: AcaoViewModel by activityViewModels()

  private var atividadeId: Int = -1
  private lateinit var atividadeOriginal: AtividadeEntity
  private var dataInicio: Date? = null
  private var dataFim: Date? = null
  private var prioridadeSelecionada: PrioridadeAtividade? = null
  private val funcionariosSelecionados = mutableListOf<FuncionarioEntity>()
  private var funcionariosOriginais: List<FuncionarioEntity> = emptyList()

  private val notificacaoViewModel: NotificacaoViewModel by activityViewModels()

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    _binding = FragmentEditarAtividadeBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

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

    // 🔁 Observa notificações (ex.: após alteração de prazo)


    funcionarioViewModel.funcionarioLogado.observe(viewLifecycleOwner) { funcionario ->
      when (funcionario?.cargo) {
        Cargo.APOIO -> {
          binding.botaoConfirmarAtividade.visibility = View.GONE
          binding.botaoPedirConfirmarAtividade.visibility = View.VISIBLE
        }
        Cargo.COORDENADOR -> {
          binding.botaoConfirmarAtividade.visibility = View.VISIBLE
          binding.botaoPedirConfirmarAtividade.visibility = View.GONE
        }
        else -> {
          binding.botaoConfirmarAtividade.visibility = View.GONE
          binding.botaoPedirConfirmarAtividade.visibility = View.GONE
        }
      }
    }

    atividadeId = arguments?.getInt("atividadeId") ?: -1
    if (atividadeId == -1) {
      Toast.makeText(requireContext(), "Erro: atividade inválida!", Toast.LENGTH_SHORT).show()
      parentFragmentManager.popBackStack()
      return
    }

    atividadeViewModel.getAtividadeComFuncionariosById(atividadeId).observe(viewLifecycleOwner) { atividadeComFuncionarios ->
      if (atividadeComFuncionarios != null) {
        atividadeOriginal = atividadeComFuncionarios.atividade
        funcionariosSelecionados.clear()
        funcionariosSelecionados.addAll(atividadeComFuncionarios.funcionarios)
        funcionariosOriginais = atividadeComFuncionarios.funcionarios
        preencherCampos(atividadeComFuncionarios)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
          val acao = AppDatabase.Companion.getDatabase(requireContext())
            .acaoDao().getAcaoPorIdDireto(atividadeOriginal.acaoId)

          if (acao != null) {
            dataPrazoAcao = acao.dataPrazo
          } else {
              withContext(Dispatchers.Main) {
                  Toast.makeText(
                      requireContext(),
                      "Erro ao carregar prazo da ação associada.",
                      Toast.LENGTH_SHORT
                  ).show()
              }
          }
        }

      } else {
        Toast.makeText(requireContext(), "Atividade não encontrada", Toast.LENGTH_SHORT).show()
      }
    }

    binding.areaPrioridade.setOnClickListener {
      EscolherPrioridadeDialogFragment().show(parentFragmentManager, "EscolherPrioridade")
    }

    binding.areaResponsaveis.setOnClickListener {
      SelecionarResponsavelDialogFragment().show(parentFragmentManager, "EscolherResponsaveis")
    }

    binding.textDataInicio.setOnClickListener { abrirDatePicker(true) }
    binding.textDataFim.setOnClickListener { abrirDatePicker(false) }

    binding.botaoConfirmarAtividade.setOnClickListener {
      salvarAlteracoes()
    }

    binding.botaoPedirConfirmarAtividade.setOnClickListener {
      val nome = binding.inputNomeAtividade.text.toString().trim()
      val descricao = binding.inputDescricao.text.toString().trim()
      val funcionarioCriador = funcionarioViewModel.funcionarioLogado.value

      if (nome.isEmpty() || dataInicio == null || dataFim == null || prioridadeSelecionada == null || funcionariosSelecionados.isEmpty() || funcionarioCriador == null) {
        Toast.makeText(requireContext(), "Preencha todos os campos obrigatórios.", Toast.LENGTH_SHORT).show()
        return@setOnClickListener
      }

      viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
        if (!validarDatasComAcao()) return@launch
        val nomePilar = AppDatabase.Companion.getDatabase(requireContext())
          .pilarDao()
          .getNomePilarPorId(atividadeOriginal.acaoId) ?: "Indefinido"

        val atividadeEditada = AtividadeJson(
          id = atividadeOriginal.id,
          nome = nome,
          descricao = descricao,
          dataInicio = dataInicio!!,
          dataPrazo = dataFim!!,
          status = atividadeOriginal.status,
          prioridade = prioridadeSelecionada!!,
          criadoPor = funcionarioCriador.id,
          dataCriacao = atividadeOriginal.dataCriacao,
          acaoId = atividadeOriginal.acaoId,
          nomePilar = nomePilar,
          responsaveis = funcionariosSelecionados.map { it.id }
        )

        val json = Gson().toJson(atividadeEditada)

        val requisicao = RequisicaoEntity(
            tipo = TipoRequisicao.EDITAR_ATIVIDADE,
            atividadeJson = json,
            status = StatusRequisicao.PENDENTE,
            solicitanteId = funcionarioCriador.id,
            dataSolicitacao = Date()
        )

        AppDatabase.Companion.getDatabase(requireContext()).requisicaoDao().inserir(requisicao)

        launch(Dispatchers.Main) {
          Toast.makeText(requireContext(), "Requisição de edição enviada para aprovação.", Toast.LENGTH_SHORT).show()
          parentFragmentManager.popBackStack()
        }
      }
    }

    parentFragmentManager.setFragmentResultListener("prioridadeSelecionada", viewLifecycleOwner) { _, bundle ->
      val valor = bundle.getString("valor")
      prioridadeSelecionada = PrioridadeAtividade.values().find { it.name == valor }
      atualizarVisualPrioridade()
    }

    parentFragmentManager.setFragmentResultListener("funcionariosSelecionados", viewLifecycleOwner) { _, bundle ->
      val selecionados = bundle.getParcelableArrayList<FuncionarioEntity>("listaFuncionarios") ?: return@setFragmentResultListener
      funcionariosSelecionados.clear()
      funcionariosSelecionados.addAll(selecionados)
      exibirFotosSelecionadas(funcionariosSelecionados)
    }
  }

  private fun salvarAlteracoes() {
    val nome = binding.inputNomeAtividade.text.toString().trim()
    val descricao = binding.inputDescricao.text.toString().trim()

    if (nome.isEmpty() || descricao.isEmpty() || dataInicio == null || dataFim == null || prioridadeSelecionada == null || funcionariosSelecionados.isEmpty()) {
      Toast.makeText(requireContext(), "Preencha todos os campos obrigatórios.", Toast.LENGTH_SHORT).show()
      return
    }

    viewLifecycleOwner.lifecycleScope.launch {
      if (!validarDatasComAcao()) return@launch

      val atividadeAtualizada = atividadeOriginal.copy(
        nome = nome,
        descricao = descricao,
        dataInicio = dataInicio!!,
        dataPrazo = dataFim!!,
        prioridade = prioridadeSelecionada!!,
        funcionarioId = funcionariosSelecionados.first().id,
        status = atividadeOriginal.status
      )

      val appDb = AppDatabase.Companion.getDatabase(requireContext())
      val atividadeDao = appDb.atividadeDao()
      val funcionarioDao = appDb.funcionarioDao()
      val requisicaoDao = appDb.requisicaoDao()
      val atividadeFuncionarioDao = appDb.atividadeFuncionarioDao()

      val atividadeRepository = AtividadeRepository(
          requireContext(),
          atividadeDao,
          atividadeFuncionarioDao,
          requisicaoDao
      )

      // Salva alterações principais da atividade
      atividadeViewModel.salvarEdicaoAtividade(atividadeAtualizada, atividadeOriginal)

      // Verifica alterações nos responsáveis
      val idsOriginais = funcionariosOriginais.map { it.id }
      val idsNovos = funcionariosSelecionados.map { it.id }

      val adicionados = idsNovos - idsOriginais
      val removidos = idsOriginais - idsNovos

      if (adicionados.isNotEmpty() || removidos.isNotEmpty()) {
        atividadeViewModel.deletarRelacoesPorAtividade(atividadeId)

        for (funcionario in funcionariosSelecionados) {
          val relacao = AtividadeFuncionarioEntity(
              atividadeId = atividadeId,
              funcionarioId = funcionario.id
          )
          atividadeViewModel.inserirRelacaoFuncionario(relacao)
        }

        // Busca todos os dados completos dos funcionários adicionados e removidos
        val adicionadosEntities = funcionarioDao.getFuncionariosPorIds(adicionados)
        val removidosEntities = funcionarioDao.getFuncionariosPorIds(removidos)

          delay(200) // Garante que os relacionamentos foram salvos antes da notificação
        atividadeRepository.notificarMudancaResponsaveis(atividadeAtualizada, adicionadosEntities, removidosEntities)
      }

      // Atualiza o status da ação associada (caso necessário)
      acaoViewModel.atualizarStatusAcaoAutomaticamente(atividadeAtualizada.acaoId)

      Toast.makeText(requireContext(), "Atividade atualizada com sucesso!", Toast.LENGTH_SHORT).show()
      parentFragmentManager.popBackStack()
    }
  }




  private fun preencherCampos(atividade: AtividadeComFuncionarios) {
    binding.inputNomeAtividade.setText(atividade.atividade.nome)
    binding.inputDescricao.setText(atividade.atividade.descricao)

    dataInicio = atividade.atividade.dataInicio
    dataFim = atividade.atividade.dataPrazo

    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    binding.textDataInicio.text = "Data de início: ${sdf.format(dataInicio!!)}"
    binding.textDataFim.text = "Data de término: ${sdf.format(dataFim!!)}"

    prioridadeSelecionada = atividade.atividade.prioridade
    atualizarVisualPrioridade()
    exibirFotosSelecionadas(atividade.funcionarios)
  }

  private fun atualizarVisualPrioridade() {
    val corFundo = when (prioridadeSelecionada) {
      PrioridadeAtividade.BAIXA -> 0xFF2ECC40.toInt()
      PrioridadeAtividade.MEDIA -> 0xFFF1C40F.toInt()
      PrioridadeAtividade.ALTA -> 0xFFE74C3C.toInt()
      null -> 0xFFAAAAAA.toInt()
    }

    val corTexto = when (prioridadeSelecionada) {
      PrioridadeAtividade.MEDIA -> 0xFF000000.toInt()
      else -> 0xFFFFFFFF.toInt()
    }

    val bg = GradientDrawable().apply {
      shape = GradientDrawable.RECTANGLE
      cornerRadius = 16f
      setColor(corFundo)
    }

    binding.textPrioridade.apply {
      background = bg
      setTextColor(corTexto)
      text = when (prioridadeSelecionada) {
        PrioridadeAtividade.BAIXA -> "Prioridade Baixa"
        PrioridadeAtividade.MEDIA -> "Prioridade Média"
        PrioridadeAtividade.ALTA -> "Prioridade Alta"
        null -> "Prioridade"
      }
      setPadding(32, 16, 32, 16)
      textAlignment = View.TEXT_ALIGNMENT_CENTER
    }
  }

  private fun abrirDatePicker(isInicio: Boolean) {
    val calendario = Calendar.getInstance()
    DatePickerDialog(
        requireContext(),
        { _, year, month, day ->
            calendario.set(year, month, day)
            val data = calendario.time
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            if (isInicio) {
                dataInicio = data
                binding.textDataInicio.text = "Data de início: ${format.format(data)}"
            } else {
                dataFim = data
                binding.textDataFim.text = "Data de término: ${format.format(data)}"
            }
        },
        calendario.get(Calendar.YEAR),
        calendario.get(Calendar.MONTH),
        calendario.get(Calendar.DAY_OF_MONTH)
    ).show()
  }

  private fun exibirFotosSelecionadas(lista: List<FuncionarioEntity>) {
    val container = binding.containerFotosResponsaveis
    container.removeAllViews()

    val dimensao = resources.getDimensionPixelSize(R.dimen.tamanho_foto_responsavel)

    lista.forEach { funcionario ->
      val imageView = CircleImageView(requireContext()).apply {
        layoutParams = ViewGroup.MarginLayoutParams(dimensao, dimensao).apply {
          marginEnd = 16
        }
        borderWidth = 2
        borderColor = ContextCompat.getColor(context, android.R.color.white)

        Glide.with(this)
          .load(funcionario.fotoPerfil)
          .placeholder(R.drawable.ic_person)
          .into(this)
      }

      container.addView(imageView)
    }
  }
  private fun validarDatasComAcao(): Boolean {
    if (dataInicio == null || dataFim == null || dataPrazoAcao == null) {
      Toast.makeText(requireContext(), "Erro ao validar datas: valores nulos.", Toast.LENGTH_SHORT).show()
      return false
    }

    val inicio = truncarData(dataInicio!!)
    val fim = truncarData(dataFim!!)
    val prazoAcao = truncarData(dataPrazoAcao!!)

    if (inicio.after(prazoAcao)) {
      Toast.makeText(requireContext(), "A data de início deve ser antes do prazo da ação.", Toast.LENGTH_SHORT).show()
      return false
    }

    if (fim.before(inicio)) {
      Toast.makeText(requireContext(), "A data de término deve ser igual ou depois da data de início.", Toast.LENGTH_SHORT).show()
      return false
    }

    if (fim.after(prazoAcao)) {
      Toast.makeText(requireContext(), "A data de término deve ser no máximo até o prazo da ação.", Toast.LENGTH_SHORT).show()
      return false
    }

    return true
  }

  private fun truncarData(data: Date): Date {
    return Calendar.getInstance().apply {
      time = data
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.time
  }


  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
