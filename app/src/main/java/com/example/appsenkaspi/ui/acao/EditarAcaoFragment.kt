package com.example.appsenkaspi.ui.acao

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.Color
import android.widget.TextView
import android.widget.ImageView
import android.widget.PopupMenu
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
import com.example.appsenkaspi.R
import com.example.appsenkaspi.data.local.entity.RequisicaoEntity
import com.example.appsenkaspi.ui.dialog.SelecionarResponsavelDialogFragment
import com.example.appsenkaspi.data.local.enums.StatusRequisicao
import com.example.appsenkaspi.data.local.enums.TipoRequisicao
import com.example.appsenkaspi.extensions.configurarBotaoVoltar
import com.example.appsenkaspi.extensions.configurarNotificacaoBadge
import com.example.appsenkaspi.data.local.database.AppDatabase
import com.example.appsenkaspi.databinding.FragmentEditarAcaoBinding
import com.example.appsenkaspi.viewmodel.AcaoFuncionarioViewModel
import com.example.appsenkaspi.viewmodel.AcaoViewModel
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Fragmento responsável pela edição de uma Ação existente.
 *
 * Esta classe permite que usuários com diferentes cargos (Coordenador, Apoio) editem ações,
 * verifiquem prazos, atribuam responsáveis e solicitem confirmação de alterações.
 * Coordenadores podem editar diretamente, enquanto usuários de apoio enviam requisições.
 */
class EditarAcaoFragment : Fragment() {

  private var _binding: FragmentEditarAcaoBinding? = null
  private val binding get() = _binding!!

  private val acaoViewModel: AcaoViewModel by activityViewModels()
  private val acaoFuncionarioViewModel: AcaoFuncionarioViewModel by activityViewModels()
  private val funcionarioViewModel: FuncionarioViewModel by activityViewModels()
  private val notificacaoViewModel: NotificacaoViewModel by activityViewModels()

  private var dataPrazoSelecionada: Date? = null
  private val calendario = Calendar.getInstance()

  private var acaoId: Int = -1
  private var pilarId: Int? = null
  private var subpilarId: Int? = null

  private val funcionariosSelecionados = mutableListOf<FuncionarioEntity>()
  private lateinit var adapterSelecionados: FuncionarioSelecionadoAdapter

  /**
   * Infla a visualização do layout do fragmento.
   */
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    _binding = FragmentEditarAcaoBinding.inflate(inflater, container, false)
    return binding.root
  }

  /**
   * Inicializa os componentes da interface e define o comportamento dos botões conforme o cargo do usuário.
   */
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    configurarBotaoVoltar(view)

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

    adapterSelecionados = FuncionarioSelecionadoAdapter(funcionariosSelecionados)
    binding.recyclerViewFuncionariosSelecionados.layoutManager =
      LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    binding.recyclerViewFuncionariosSelecionados.adapter = adapterSelecionados

    binding.buttonPickDateEdicao.setOnClickListener { abrirDatePicker() }
    binding.iconSelecionarFuncionario.setOnClickListener { abrirDialogSelecionarFuncionarios() }
    binding.confirmarButtonWrapperEdicao.setOnClickListener { confirmarEdicaoAcao() }

    funcionarioViewModel.funcionarioLogado.observe(viewLifecycleOwner) { funcionario ->
      when (funcionario?.cargo) {
        Cargo.APOIO -> {
          binding.confirmarButtonWrapperEdicao.visibility = View.GONE
          binding.cardPedirConfirmacao.visibility = View.VISIBLE
          binding.iconeMenuEdicao.visibility = View.GONE
        }
        Cargo.COORDENADOR -> {
          binding.confirmarButtonWrapperEdicao.visibility = View.VISIBLE
          binding.cardPedirConfirmacao.visibility = View.GONE
        }
        Cargo.GESTOR, null -> {
          binding.confirmarButtonWrapperEdicao.visibility = View.GONE
          binding.cardPedirConfirmacao.visibility = View.GONE
          binding.iconeMenuEdicao.visibility = View.GONE
        }
      }
    }

    binding.cardPedirConfirmacao.setOnClickListener { enviarRequisicaoEdicao() }

    val menuIcon = view.findViewById<ImageView>(R.id.iconeMenuEdicao)
    menuIcon.setOnClickListener { exibirPopupMenu(it) }

    acaoId = arguments?.getInt("acaoId") ?: -1
    if (acaoId == -1) {
      Toast.makeText(requireContext(), "Erro: Ação inválida!", Toast.LENGTH_SHORT).show()
      parentFragmentManager.popBackStack()
      return
    }

    acaoViewModel.getAcaoById(acaoId).observe(viewLifecycleOwner) { acao ->
      acao?.let {
        binding.inputNomeEdicao.setText(it.nome)
        binding.inputDescricaoEdicao.setText(it.descricao)
        dataPrazoSelecionada = it.dataPrazo
        binding.buttonPickDateEdicao.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it.dataPrazo)

        pilarId = it.pilarId
        subpilarId = it.subpilarId
      }
    }

    childFragmentManager.setFragmentResultListener("funcionariosSelecionados", viewLifecycleOwner) { _, bundle ->
      val lista = bundle.getParcelableArrayList<FuncionarioEntity>("listaFuncionarios") ?: arrayListOf()
      funcionariosSelecionados.clear()
      funcionariosSelecionados.addAll(lista.filterNotNull())
      adapterSelecionados.notifyDataSetChanged()
    }
  }

  /**
   * Envia uma requisição de edição da ação para aprovação por um Coordenador.
   */
  private fun enviarRequisicaoEdicao() {
    val nome = binding.inputNomeEdicao.text.toString().trim()
    val descricao = binding.inputDescricaoEdicao.text.toString().trim()
    val funcionarioLogado = funcionarioViewModel.funcionarioLogado.value

    if (nome.isEmpty() || dataPrazoSelecionada == null || funcionarioLogado == null) {
      Toast.makeText(requireContext(), "Preencha todos os campos obrigatórios!", Toast.LENGTH_SHORT).show()
      return
    }

    lifecycleScope.launch(Dispatchers.IO) {
      if (!validarPrazoEdicao()) return@launch
      val acao = AppDatabase.getDatabase(requireContext()).acaoDao().getAcaoPorIdDireto(acaoId) ?: return@launch

      val nomeAlvo = when {
        acao.subpilarId != null -> AppDatabase.getDatabase(requireContext()).subpilarDao().buscarNomeSubpilarPorId(acao.subpilarId!!)
        acao.pilarId != null -> AppDatabase.getDatabase(requireContext()).pilarDao().getNomePilarPorId(acao.pilarId!!)
        else -> "Destino não identificado"
      }

      val acaoJson = AcaoJson(
        id = acao.id,
        nome = nome,
        descricao = descricao,
        dataInicio = acao.dataInicio,
        dataPrazo = dataPrazoSelecionada!!,
        status = acao.status,
        criadoPor = acao.criadoPor,
        dataCriacao = acao.dataCriacao,
        pilarId = acao.pilarId,
        subpilarId = acao.subpilarId,
        nomePilar = nomeAlvo!!,
        responsaveis = funcionariosSelecionados.map { it.id }
      )

      val requisicao = RequisicaoEntity(
        tipo = TipoRequisicao.EDITAR_ACAO,
        acaoJson = Gson().toJson(acaoJson),
        status = StatusRequisicao.PENDENTE,
        solicitanteId = funcionarioLogado.id,
        dataSolicitacao = Date()
      )

      AppDatabase.getDatabase(requireContext()).requisicaoDao().inserir(requisicao)

      withContext(Dispatchers.Main) {
        Toast.makeText(
          requireContext(),
          "Requisição enviada para aprovação!",
          Toast.LENGTH_SHORT
        ).show()
        parentFragmentManager.popBackStack()
      }
    }
  }

  /**
   * Aplica alterações diretamente no banco de dados local se o usuário for Coordenador.
   */
  private fun confirmarEdicaoAcao() {
    val nome = binding.inputNomeEdicao.text.toString().trim()
    val descricao = binding.inputDescricaoEdicao.text.toString().trim()

    if (nome.isEmpty()) {
      binding.inputNomeEdicao.error = "Digite o nome da ação"
      return
    }
    if (dataPrazoSelecionada == null) {
      binding.buttonPickDateEdicao.error = "Selecione uma data de prazo"
      return
    }

    lifecycleScope.launch {
      if (!validarPrazoEdicao()) return@launch
      val acaoExistente = acaoViewModel.getAcaoByIdNow(acaoId)
      if (acaoExistente != null) {
        if (acaoExistente.pilarId != null && acaoExistente.subpilarId != null) {
          Toast.makeText(requireContext(), "Erro: Ação não pode estar vinculada a Pilar e Subpilar ao mesmo tempo", Toast.LENGTH_LONG).show()
          return@launch
        }

        val acaoAtualizada = acaoExistente.copy(
          nome = nome,
          descricao = descricao,
          dataPrazo = dataPrazoSelecionada!!
        )
        acaoViewModel.atualizar(acaoAtualizada)

        Toast.makeText(requireContext(), "Ação atualizada com sucesso!", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
      } else {
        Toast.makeText(requireContext(), "Erro ao atualizar a ação!", Toast.LENGTH_SHORT).show()
      }
    }
  }

  /**
   * Verifica se o novo prazo definido ultrapassa o prazo máximo permitido da estrutura pai.
   */
  private suspend fun validarPrazoEdicao(): Boolean {
    if (dataPrazoSelecionada == null) {
      binding.buttonPickDateEdicao.error = "Selecione uma data de prazo"
      return false
    }

    val dataLimite: Date? = when {
      subpilarId != null -> acaoViewModel.buscarSubpilarPorId(subpilarId!!)?.dataPrazo
      pilarId != null -> acaoViewModel.buscarPilarPorId(pilarId!!)?.dataPrazo
      else -> null
    }

    dataLimite?.let {
      val selecionada = truncarData(dataPrazoSelecionada!!)
      val limite = truncarData(it)

      if (selecionada.after(limite)) {
        val nomeEstrutura = if (subpilarId != null) "subpilar" else "pilar"
        val dataFormatada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
        Toast.makeText(requireContext(), "A nova data não pode ultrapassar o prazo do $nomeEstrutura ($dataFormatada).", Toast.LENGTH_LONG).show()
        return false
      }
    }

    return true
  }

  /**
   * Remove componentes de tempo de uma data, preservando apenas o dia.
   */
  private fun truncarData(data: Date): Date {
    return Calendar.getInstance().apply {
      time = data
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.time
  }

  /**
   * Exibe um seletor de data para alteração do prazo da ação.
   */
  private fun abrirDatePicker() {
    DatePickerDialog(
      requireContext(),
      { _, ano, mes, dia ->
        calendario.set(ano, mes, dia)
        dataPrazoSelecionada = calendario.time
        val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.buttonPickDateEdicao.text = fmt.format(dataPrazoSelecionada!!)
      },
      calendario.get(Calendar.YEAR),
      calendario.get(Calendar.MONTH),
      calendario.get(Calendar.DAY_OF_MONTH)
    ).show()
  }

  /**
   * Abre o diálogo de seleção de responsáveis.
   */
  private fun abrirDialogSelecionarFuncionarios() {
    SelecionarResponsavelDialogFragment().show(childFragmentManager, "SelecionarFuncionariosDialog")
  }

  /**
   * Exibe menu de opções com ações como exclusão da Ação.
   */
  private fun exibirPopupMenu(anchor: View) {
    val popup = PopupMenu(requireContext(), anchor)
    popup.menuInflater.inflate(R.menu.menu_pilar, popup.menu)
    popup.setOnMenuItemClickListener {
      when (it.itemId) {
        R.id.action_deletar -> {
          exibirDialogoConfirmacao()
          true
        }
        else -> false
      }
    }
    popup.show()
  }

  /**
   * Exibe diálogo de confirmação antes de excluir a Ação.
   */
  private fun exibirDialogoConfirmacao() {
    val dialog = AlertDialog.Builder(requireContext())
      .setTitle("Confirmar exclusão")
      .setMessage("Deseja deletar esta Ação?")
      .setPositiveButton("Deletar") { _, _ -> deletarAcao() }
      .setNegativeButton("Cancelar", null)
      .create()

    dialog.show()

    dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

    val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
    dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

    (dialog.findViewById<TextView>(android.R.id.message))?.textAlignment = View.TEXT_ALIGNMENT_VIEW_START

    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.RED)
    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.WHITE)
  }

  /**
   * Exclui permanentemente a ação do banco de dados.
   */
  private fun deletarAcao() {
    lifecycleScope.launch {
      val dao = AppDatabase.getDatabase(requireContext()).acaoDao()
      val acao = dao.buscarAcaoPorId(acaoId)
      if (acao != null) {
        dao.deletarAcao(acao)
        Toast.makeText(requireContext(), "Ação deletada com sucesso!", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
      } else {
        Toast.makeText(requireContext(), "Erro ao localizar a ação!", Toast.LENGTH_SHORT).show()
      }
    }
  }

  /**
   * Libera a memória do binding ao destruir a view.
   */
  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
