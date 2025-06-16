package com.example.appsenkaspi.ui.pilar

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.appsenkaspi.data.local.enums.Cargo
import com.example.appsenkaspi.viewmodel.FuncionarioViewModel
import com.example.appsenkaspi.viewmodel.NotificacaoViewModel
import com.example.appsenkaspi.viewmodel.PilarViewModel
import com.example.appsenkaspi.R
import com.example.appsenkaspi.extensions.configurarBotaoVoltar
import com.example.appsenkaspi.extensions.configurarNotificacaoBadge
import com.example.appsenkaspi.data.local.database.AppDatabase
import com.example.appsenkaspi.databinding.FragmentEditarPilarBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Fragmento responsável por editar um Pilar existente.
 *
 * Este fragmento permite a atualização do nome, descrição e data de prazo de um Pilar.
 * A visibilidade dos botões é controlada de acordo com o cargo do funcionário logado.
 *
 * Funcionalidades:
 * - Carrega os dados do Pilar selecionado.
 * - Permite edição e confirmação da alteração.
 * - Oferece opção de exclusão com diálogo de confirmação.
 * - Integra com ViewModels: PilarViewModel, FuncionarioViewModel e NotificacaoViewModel.
 */
class EditarPilarFragment : Fragment() {

  private var _binding: FragmentEditarPilarBinding? = null
  private val binding get() = _binding!!

  private val pilarViewModel: PilarViewModel by activityViewModels()
  private val funcionarioViewModel: FuncionarioViewModel by activityViewModels()
  private val notificacaoViewModel: NotificacaoViewModel by activityViewModels()

  private var novaDataSelecionada: Date? = null
  private var pilarId: Int = -1

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentEditarPilarBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    configurarBotaoVoltar(view)

    // Badge de notificação
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

    // Controla visibilidade dos botões conforme cargo
    funcionarioViewModel.funcionarioLogado.observe(viewLifecycleOwner) { funcionario ->
      when (funcionario?.cargo) {
        Cargo.APOIO -> {
          binding.confirmarButtonWrapperEdicao.visibility = View.GONE
          binding.iconeMenuEdicao.visibility = View.VISIBLE
        }
        Cargo.COORDENADOR -> {
          binding.confirmarButtonWrapperEdicao.visibility = View.VISIBLE
          binding.iconeMenuEdicao.visibility = View.VISIBLE
        }
        Cargo.GESTOR, null -> {
          binding.confirmarButtonWrapperEdicao.visibility = View.GONE
          binding.iconeMenuEdicao.visibility = View.GONE
        }
      }
    }

    binding.buttonPickDateEdicao.setOnClickListener { abrirDatePicker() }
    binding.confirmarButtonWrapperEdicao.setOnClickListener { confirmarEdicao() }
    binding.iconeMenuEdicao.setOnClickListener { exibirPopupMenu(it) }

    // Carrega dados do Pilar a partir do ID passado por argumento
    pilarId = arguments?.getInt("pilarId") ?: -1
    if (pilarId != -1) {
      carregarDadosPilar(pilarId)
    } else {
      Toast.makeText(requireContext(), "Erro ao carregar Pilar!", Toast.LENGTH_SHORT).show()
      parentFragmentManager.popBackStack()
    }
  }

  /** Carrega os dados do Pilar no formulário. */
  private fun carregarDadosPilar(id: Int) {
    lifecycleScope.launch {
      val dao = AppDatabase.getDatabase(requireContext()).pilarDao()
      dao.buscarPilarPorId(id)?.let { pilar ->
        binding.inputNomeEdicao.setText(pilar.nome)
        binding.inputDescricaoEdicao.setText(pilar.descricao)
        novaDataSelecionada = pilar.dataPrazo
        binding.buttonPickDateEdicao.text =
          SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(pilar.dataPrazo)
      }
    }
  }

  /** Abre o seletor de data para definir novo prazo. */
  private fun abrirDatePicker() {
    val calendario = Calendar.getInstance()
    DatePickerDialog(
      requireContext(),
      { _, year, month, day ->
        calendario.set(year, month, day)
        novaDataSelecionada = calendario.time
        binding.buttonPickDateEdicao.text =
          SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(novaDataSelecionada!!)
      },
      calendario.get(Calendar.YEAR),
      calendario.get(Calendar.MONTH),
      calendario.get(Calendar.DAY_OF_MONTH)
    ).show()
  }

  /** Confirma a edição do Pilar e atualiza o banco de dados. */
  private fun confirmarEdicao() {
    val novoNome = binding.inputNomeEdicao.text.toString().trim()
    val novaDescricao = binding.inputDescricaoEdicao.text.toString().trim()

    if (novoNome.isEmpty()) {
      binding.inputNomeEdicao.error = "Digite o novo nome do Pilar"
      return
    }

    lifecycleScope.launch {
      val dao = AppDatabase.getDatabase(requireContext()).pilarDao()
      val pilarExistente = dao.buscarPilarPorId(pilarId)

      pilarExistente?.let {
        val pilarAtualizado = it.copy(
          nome = novoNome,
          descricao = novaDescricao,
          dataPrazo = novaDataSelecionada ?: it.dataPrazo
        )
        dao.atualizarPilar(pilarAtualizado)
        Toast.makeText(requireContext(), "Pilar atualizado com sucesso!", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
      }
    }
  }

  /** Exclui o Pilar usando o ViewModel. */
  private fun excluirPilar() {
    lifecycleScope.launch {
      val dao = AppDatabase.getDatabase(requireContext()).pilarDao()
      dao.buscarPilarPorId(pilarId)?.let {
        pilarViewModel.excluirPilar(it)
        Toast.makeText(requireContext(), "Pilar excluído com sucesso!", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
      } ?: Toast.makeText(requireContext(), "Erro ao localizar Pilar!", Toast.LENGTH_SHORT).show()
    }
  }

  /** Exibe caixa de diálogo para confirmar a exclusão do Pilar. */
  private fun exibirDialogoConfirmacao() {
    AlertDialog.Builder(requireContext())
      .setTitle("Confirmar exclusão")
      .setMessage("Deseja excluir este Pilar?")
      .setPositiveButton("Excluir") { _, _ -> excluirPilar() }
      .setNegativeButton("Cancelar", null)
      .show()
  }

  /** Exibe menu suspenso com ação de exclusão. */
  private fun exibirPopupMenu(anchor: View) {
    PopupMenu(requireContext(), anchor).apply {
      menuInflater.inflate(R.menu.menu_pilar, menu)
      setOnMenuItemClickListener {
        when (it.itemId) {
          R.id.action_deletar -> {
            exibirDialogoConfirmacao()
            true
          }
          else -> false
        }
      }
    }.show()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
