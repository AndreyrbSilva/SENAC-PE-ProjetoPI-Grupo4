package com.example.appsenkaspi.ui.subpilares

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import android.graphics.Color
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.appsenkaspi.viewmodel.FuncionarioViewModel
import com.example.appsenkaspi.viewmodel.NotificacaoViewModel
import com.example.appsenkaspi.viewmodel.PilarViewModel
import com.example.appsenkaspi.R
import com.example.appsenkaspi.viewmodel.SubpilarViewModel
import com.example.appsenkaspi.extensions.configurarBotaoVoltar
import com.example.appsenkaspi.extensions.configurarNotificacaoBadge
import com.example.appsenkaspi.data.local.database.AppDatabase
import com.example.appsenkaspi.databinding.FragmentEditarSubpilarBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragmento responsável por editar ou deletar um Subpilar existente.
 *
 * Permite atualizar o nome, descrição e prazo do subpilar, com validação
 * para garantir que a nova data não ultrapasse o prazo do Pilar pai.
 *
 * Também oferece opção de exclusão segura, com confirmação por diálogo.
 */
class EditarSubpilarFragment : Fragment() {

  private var _binding: FragmentEditarSubpilarBinding? = null
  private val binding get() = _binding!!

  /** ViewModel que manipula dados de subpilares. */
  private val subpilarViewModel: SubpilarViewModel by activityViewModels()

  /** ViewModel usado para obter o prazo máximo permitido pelo Pilar. */
  private val pilarViewModel: PilarViewModel by activityViewModels()

  /** ViewModel que provê os dados do usuário logado, para exibir notificações. */
  private val funcionarioViewModel: FuncionarioViewModel by activityViewModels()

  /** ViewModel que alimenta o badge de notificações. */
  private val notificacaoViewModel: NotificacaoViewModel by activityViewModels()

  private var subpilarId: Int = -1
  private var pilarId: Int = -1
  private var novaDataSelecionada: Date? = null

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentEditarSubpilarBinding.inflate(inflater, container, false)
    return binding.root
  }

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

    subpilarId = arguments?.getInt("subpilarId") ?: -1
    if (subpilarId == -1) {
      Toast.makeText(requireContext(), "Subpilar inválido!", Toast.LENGTH_SHORT).show()
      parentFragmentManager.popBackStack()
      return
    }

    carregarDadosSubpilar(subpilarId)

    binding.buttonPickDateEdicao.setOnClickListener { abrirDatePicker() }
    binding.confirmarButtonWrapperEdicao.setOnClickListener {
      lifecycleScope.launch {
        if (validarPrazoComPilar()) {
          confirmarEdicao()
        }
      }
    }
    binding.iconeMenuEdicao.setOnClickListener { exibirPopupMenu(it) }
  }

  /**
   * Busca no banco os dados do subpilar e preenche os campos da interface.
   *
   * @param id ID do subpilar a ser carregado.
   */
  private fun carregarDadosSubpilar(id: Int) {
    lifecycleScope.launch {
      val dao = AppDatabase.getDatabase(requireContext()).subpilarDao()
      val subpilar = dao.buscarSubpilarPorId(id)
      if (subpilar != null) {
        binding.inputNomeEdicao.setText(subpilar.nome)
        binding.inputDescricaoEdicao.setText(subpilar.descricao)
        novaDataSelecionada = subpilar.dataPrazo
        pilarId = subpilar.pilarId

        novaDataSelecionada?.let {
          val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
          binding.buttonPickDateEdicao.text = formato.format(it)
        }
      }
    }
  }

  /**
   * Exibe o seletor de data (DatePicker) para atualização do prazo.
   */
  private fun abrirDatePicker() {
    val calendario = Calendar.getInstance()
    val datePicker = DatePickerDialog(
      requireContext(),
      { _, year, month, dayOfMonth ->
        calendario.set(year, month, dayOfMonth)
        novaDataSelecionada = calendario.time
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.buttonPickDateEdicao.text = formato.format(novaDataSelecionada!!)
      },
      calendario.get(Calendar.YEAR),
      calendario.get(Calendar.MONTH),
      calendario.get(Calendar.DAY_OF_MONTH)
    )
    datePicker.show()
  }

  /**
   * Verifica se a nova data do subpilar está dentro do prazo máximo permitido pelo Pilar pai.
   *
   * @return true se a data for válida; false caso contrário, com mensagens de erro apropriadas.
   */
  private suspend fun validarPrazoComPilar(): Boolean {
    if (novaDataSelecionada == null) {
      binding.buttonPickDateEdicao.error = "Escolha um prazo"
      return false
    }

    val dataLimite = pilarViewModel.getDataPrazoDoPilar(pilarId)

    if (dataLimite == null) {
      withContext(Dispatchers.Main) {
        Toast.makeText(requireContext(), "Erro ao buscar data do pilar.", Toast.LENGTH_SHORT).show()
      }
      return false
    }

    val selecionadaTruncada = truncarData(novaDataSelecionada!!)
    val limiteTruncado = truncarData(dataLimite)

    if (selecionadaTruncada.after(limiteTruncado)) {
      val dataFormatada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(limiteTruncado)
      withContext(Dispatchers.Main) {
        Toast.makeText(
          requireContext(),
          "A nova data não pode ultrapassar o prazo do pilar ($dataFormatada).",
          Toast.LENGTH_LONG
        ).show()
      }
      return false
    }

    return true
  }

  /**
   * Remove hora/minuto/segundo de uma data para comparação apenas por dia.
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
   * Realiza a atualização do subpilar no banco após validação dos campos.
   * Persiste alterações e volta para a tela anterior.
   */
  private fun confirmarEdicao() {
    val novoNome = binding.inputNomeEdicao.text.toString().trim()
    val novaDescricao = binding.inputDescricaoEdicao.text.toString().trim()

    if (novoNome.isEmpty()) {
      binding.inputNomeEdicao.error = "Digite o nome do Subpilar"
      return
    }

    lifecycleScope.launch {
      val dao = AppDatabase.getDatabase(requireContext()).subpilarDao()
      val subpilarExistente = dao.buscarSubpilarPorId(subpilarId)
      if (subpilarExistente != null) {
        val atualizado = subpilarExistente.copy(
          nome = novoNome,
          descricao = novaDescricao,
          dataPrazo = novaDataSelecionada ?: subpilarExistente.dataPrazo
        )
        dao.atualizarSubpilar(atualizado)
        Toast.makeText(requireContext(), "Subpilar atualizado com sucesso!", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
      }
    }
  }

  /**
   * Remove permanentemente o subpilar selecionado do banco de dados.
   */
  private fun deletarSubpilar() {
    lifecycleScope.launch {
      val dao = AppDatabase.getDatabase(requireContext()).subpilarDao()
      val subpilar = dao.buscarSubpilarPorId(subpilarId)
      if (subpilar != null) {
        dao.deletarSubpilar(subpilar)
        Toast.makeText(requireContext(), "Subpilar deletado com sucesso!", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
      } else {
        Toast.makeText(requireContext(), "Erro ao localizar Subpilar!", Toast.LENGTH_SHORT).show()
      }
    }
  }

  /**
   * Exibe diálogo de confirmação para excluir o subpilar de forma segura.
   */
  private fun exibirDialogoConfirmacao() {
    val dialog = AlertDialog.Builder(requireContext())
      .setTitle("Confirmar exclusão")
      .setMessage("Deseja deletar este Subpilar?")
      .setPositiveButton("Deletar") { _, _ -> deletarSubpilar() }
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
   * Exibe menu suspenso com ações extras (como deletar).
   *
   * @param anchor View usada como âncora para o popup.
   */
  private fun exibirPopupMenu(anchor: View) {
    val popup = PopupMenu(requireContext(), anchor)
    popup.menuInflater.inflate(R.menu.menu_pilar, popup.menu)
    popup.setOnMenuItemClickListener { item ->
      when (item.itemId) {
        R.id.action_deletar -> {
          exibirDialogoConfirmacao()
          true
        }
        else -> false
      }
    }
    popup.show()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
