package com.example.appsenkaspi.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appsenkaspi.data.local.entity.FuncionarioEntity
import com.example.appsenkaspi.databinding.DialogEscolherResponsaveisBinding
import com.example.appsenkaspi.ui.responsaveis.ResponsavelAdapter
import com.example.appsenkaspi.viewmodel.FuncionarioViewModel

/**
 * Diálogo que permite selecionar múltiplos responsáveis a partir da lista de funcionários disponíveis.
 *
 * Os responsáveis selecionados são retornados ao fragmento pai via `setFragmentResult` com a chave "funcionariosSelecionados".
 * Utiliza um `ResponsavelAdapter` customizado com suporte à seleção múltipla.
 */
class SelecionarResponsavelDialogFragment : DialogFragment() {

  /** Binding do layout do diálogo */
  private var _binding: DialogEscolherResponsaveisBinding? = null
  private val binding get() = _binding!!

  /** ViewModel compartilhado contendo a lista de funcionários */
  private val funcionarioViewModel: FuncionarioViewModel by activityViewModels()

  /** Lista de funcionários selecionados durante a interação */
  private val selecionados = mutableListOf<FuncionarioEntity>()

  /** Adapter responsável por exibir e gerenciar a seleção dos funcionários */
  private lateinit var adapter: ResponsavelAdapter

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    _binding = DialogEscolherResponsaveisBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    // Inicializa o adapter com lista vazia e com suporte à seleção múltipla
    adapter = ResponsavelAdapter(
      funcionarios = listOf(),
      selecionados = selecionados,
      onSelecionadosAtualizados = { listaAtualizada ->
        // Callback opcional caso deseje tratar mudanças
      }
    )

    binding.recyclerMembros.layoutManager = LinearLayoutManager(requireContext())
    binding.recyclerMembros.adapter = adapter

    // Observa e atualiza a lista de funcionários no adapter
    funcionarioViewModel.listasFuncionarios.observe(viewLifecycleOwner) { lista ->
      adapter.atualizarLista(lista)
    }

    // Fecha o diálogo ao clicar no botão "Fechar"
    binding.fecharDialog.setOnClickListener {
      dismiss()
    }

    // Retorna os selecionados ao fragmento pai ao clicar em "Confirmar"
    binding.buttonConfirmarSelecionados.setOnClickListener {
      val result = Bundle().apply {
        putParcelableArrayList("listaFuncionarios", ArrayList(selecionados))
      }
      parentFragmentManager.setFragmentResult("funcionariosSelecionados", result)
      dismiss()
    }
  }

  /**
   * Remove a barra de título padrão do diálogo.
   */
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog = super.onCreateDialog(savedInstanceState)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    return dialog
  }

  /**
   * Libera o binding para evitar memory leaks.
   */
  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
