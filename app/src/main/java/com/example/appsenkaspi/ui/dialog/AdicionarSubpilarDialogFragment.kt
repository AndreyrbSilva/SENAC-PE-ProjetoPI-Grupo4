package com.example.appsenkaspi.ui.dialog

import android.app.DatePickerDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.appsenkaspi.viewmodel.NotificacaoViewModel
import com.example.appsenkaspi.R
import com.example.appsenkaspi.viewmodel.SubpilarViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Diálogo personalizado para adicionar um novo Subpilar a um Pilar existente.
 *
 * Exibe campos para nome, descrição e prazo (com validação contra o prazo máximo do Pilar).
 * Os dados inseridos são enviados ao fragmento pai via `setFragmentResult`.
 */
class AdicionarSubpilarDialogFragment : DialogFragment() {

  /** ID do pilar ao qual o subpilar será vinculado */
  private var pilarId: Int = -1

  /** Data máxima permitida para o prazo do subpilar (prazo do pilar pai) */
  private var prazoMaximo: Date? = null

  /** ViewModel responsável por notificações (uso futuro ou estendido) */
  private val notificacaoViewModel: NotificacaoViewModel by activityViewModels()

  /** ViewModel compartilhado para operações relacionadas a subpilares */
  private val subpilarViewModel: SubpilarViewModel by activityViewModels()

  /** Campo de entrada para o nome do subpilar */
  private lateinit var inputNomeSubpilar: EditText

  /** Campo de entrada para a descrição do subpilar */
  private lateinit var inputDescricaoSubpilar: EditText

  /** Botão para escolher a data de prazo do subpilar */
  private lateinit var buttonPickDateSubpilar: Button

  /** Botão para confirmar e salvar o subpilar */
  private lateinit var buttonConfirmarSubpilar: Button

  /** Data selecionada pelo usuário no DatePicker */
  private var dataSelecionada: Date? = null

  /** Instância do calendário usada no controle do DatePicker */
  private val calendario = Calendar.getInstance()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    arguments?.let { bundle ->
      pilarId = bundle.getInt(ARG_PILAR_ID)
      prazoMaximo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        bundle.getSerializable(ARG_PRAZO, Date::class.java)
      } else {
        @Suppress("DEPRECATION")
        bundle.getSerializable(ARG_PRAZO) as? Date
      }
    }
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val view = LayoutInflater.from(context).inflate(R.layout.popup_adicionar_subpilar, null)

    inputNomeSubpilar = view.findViewById(R.id.inputNomeSubpilar)
    inputDescricaoSubpilar = view.findViewById(R.id.inputDescricaoSubpilar)
    buttonPickDateSubpilar = view.findViewById(R.id.buttonPickDateSubpilar)
    buttonConfirmarSubpilar = view.findViewById(R.id.buttonConfirmarSubpilar)

    // Ações dos botões
    buttonPickDateSubpilar.setOnClickListener { abrirDatePicker() }
    buttonConfirmarSubpilar.setOnClickListener { confirmarSubpilar() }

    return AlertDialog.Builder(requireContext()).setView(view).create()
  }

  override fun onStart() {
    super.onStart()
    // Ajusta a largura do diálogo para ocupar a largura da tela
    dialog?.window?.apply {
      setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
      setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
  }

  /**
   * Exibe um diálogo de seleção de data com verificação de validade em relação ao prazo do pilar.
   */
  private fun abrirDatePicker() {
    DatePickerDialog(
      requireContext(),
      { _, year, month, dayOfMonth ->
        calendario.set(year, month, dayOfMonth)
        val dataEscolhida = calendario.time

        // Verifica se a data escolhida excede o prazo máximo permitido
        if (prazoMaximo != null && dataEscolhida.after(prazoMaximo)) {
          buttonPickDateSubpilar.error = "Data deve ser igual ou anterior ao prazo do Pilar"
        } else {
          dataSelecionada = dataEscolhida
          val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
          buttonPickDateSubpilar.text = formato.format(dataEscolhida)
          buttonPickDateSubpilar.error = null
        }
      },
      calendario.get(Calendar.YEAR),
      calendario.get(Calendar.MONTH),
      calendario.get(Calendar.DAY_OF_MONTH)
    ).show()
  }

  /**
   * Valida os campos preenchidos e envia os dados para o fragmento pai.
   */
  private fun confirmarSubpilar() {
    val nome = inputNomeSubpilar.text.toString().trim()
    val descricao = inputDescricaoSubpilar.text.toString().trim()

    if (nome.isEmpty()) {
      inputNomeSubpilar.error = "Nome obrigatório"
      return
    }

    if (dataSelecionada == null) {
      buttonPickDateSubpilar.error = "Selecione uma data válida"
      return
    }

    // Envia os dados ao fragmento pai via setFragmentResult
    parentFragmentManager.setFragmentResult(
      "novoSubpilar",
      bundleOf(
        "nomeSubpilar" to nome,
        "descricaoSubpilar" to descricao,
        "prazoSubpilar" to dataSelecionada
      )
    )

    dismiss()
  }

  companion object {
    private const val ARG_PILAR_ID = "pilarId"
    private const val ARG_PRAZO = "prazoMaximo"

    /**
     * Cria uma nova instância do diálogo com os argumentos esperados.
     *
     * @param pilarId ID do Pilar ao qual o Subpilar será vinculado.
     * @param prazoMaximo Data máxima permitida para o prazo do Subpilar.
     */
    fun newInstance(pilarId: Int, prazoMaximo: Date): AdicionarSubpilarDialogFragment {
      return AdicionarSubpilarDialogFragment().apply {
        arguments = Bundle().apply {
          putInt(ARG_PILAR_ID, pilarId)
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            putSerializable(ARG_PRAZO, prazoMaximo)
          } else {
            @Suppress("DEPRECATION")
            putSerializable(ARG_PRAZO, prazoMaximo)
          }
        }
      }
    }
  }
}
