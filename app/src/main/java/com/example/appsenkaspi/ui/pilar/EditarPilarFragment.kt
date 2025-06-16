package com.example.appsenkaspi.ui.pilar

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

class EditarPilarFragment : Fragment() {

    private var _binding: FragmentEditarPilarBinding? = null
    private val binding get() = _binding!!

    private val pilarViewModel: PilarViewModel by activityViewModels()
    private val funcionarioViewModel: FuncionarioViewModel by activityViewModels()

    private var novaDataSelecionada: Date? = null
    private var pilarId: Int = -1

    private val notificacaoViewModel: NotificacaoViewModel by activityViewModels()


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
                Cargo.GESTOR -> {
                    binding.confirmarButtonWrapperEdicao.visibility = View.GONE

                    binding.iconeMenuEdicao.visibility = View.GONE
                }
                else -> {
                    binding.confirmarButtonWrapperEdicao.visibility = View.GONE

                    binding.iconeMenuEdicao.visibility = View.GONE
                }
            }
        }

        binding.buttonPickDateEdicao.setOnClickListener { abrirDatePicker() }
        binding.confirmarButtonWrapperEdicao.setOnClickListener { confirmarEdicao() }

        binding.iconeMenuEdicao.setOnClickListener { exibirPopupMenu(it) }

        pilarId = arguments?.getInt("pilarId") ?: -1
        if (pilarId != -1) {
            carregarDadosPilar(pilarId)
        } else {
            Toast.makeText(requireContext(), "Erro ao carregar Pilar!", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    private fun carregarDadosPilar(id: Int) {
        lifecycleScope.launch {
            val dao = AppDatabase.Companion.getDatabase(requireContext()).pilarDao()
            val pilar = dao.buscarPilarPorId(id)
            if (pilar != null) {
                binding.inputNomeEdicao.setText(pilar.nome)
                binding.inputDescricaoEdicao.setText(pilar.descricao)
                novaDataSelecionada = pilar.dataPrazo
                novaDataSelecionada?.let {
                    val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    binding.buttonPickDateEdicao.text = formato.format(it)
                }
            }
        }
    }

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

    private fun confirmarEdicao() {
        val novoNome = binding.inputNomeEdicao.text.toString().trim()
        val novaDescricao = binding.inputDescricaoEdicao.text.toString().trim()

        if (novoNome.isEmpty()) {
            binding.inputNomeEdicao.error = "Digite o novo nome do Pilar"
            return
        }

        lifecycleScope.launch {
            val dao = AppDatabase.Companion.getDatabase(requireContext()).pilarDao()
            val pilarExistente = dao.buscarPilarPorId(pilarId)

            if (pilarExistente != null) {
                val pilarAtualizado = pilarExistente.copy(
                    nome = novoNome,
                    descricao = novaDescricao,
                    dataPrazo = novaDataSelecionada ?: pilarExistente.dataPrazo
                )
                dao.atualizarPilar(pilarAtualizado)
                Toast.makeText(requireContext(), "Pilar atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

  private fun excluirPilar() {
    lifecycleScope.launch {
      val dao = AppDatabase.Companion.getDatabase(requireContext()).pilarDao()
      val pilar = dao.buscarPilarPorId(pilarId)
      if (pilar != null) {
        pilarViewModel.excluirPilar(pilar) // ← usa a função do ViewModel
        Toast.makeText(requireContext(), "Pilar excluído com sucesso!", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
      } else {
        Toast.makeText(requireContext(), "Erro ao localizar Pilar!", Toast.LENGTH_SHORT).show()
      }
    }
  }


    private fun exibirDialogoConfirmacao() {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Confirmar exclusão")
            .setMessage("Deseja excluir este Pilar?")
            .setPositiveButton("Excluir") { _, _ -> excluirPilar() }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()

        // Aplica background customizado
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        // Ajusta largura
        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

        (dialog.findViewById<TextView>(android.R.id.message))?.textAlignment = View.TEXT_ALIGNMENT_VIEW_START

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.RED)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.WHITE)
    }


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
