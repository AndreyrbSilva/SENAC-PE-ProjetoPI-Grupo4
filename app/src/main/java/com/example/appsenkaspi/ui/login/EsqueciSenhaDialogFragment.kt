package com.example.appsenkaspi.ui.login

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.appsenkaspi.R

/**
 * Diálogo de recuperação de senha por ID de funcionário.
 *
 * Exibe um campo de texto para digitar o ID e um botão para acionar o processo de recuperação.
 * A lógica real de recuperação ainda não está implementada — apenas o ID digitado é lido e o diálogo é fechado.
 */
class EsqueciSenhaDialogFragment : DialogFragment() {

  /**
   * Cria o diálogo com layout customizado para o popup de recuperação de senha.
   *
   * @return Diálogo estilizado com campo de entrada e botão.
   */
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val view = LayoutInflater.from(context).inflate(R.layout.popup_esqueci_senha, null)

    val campoId = view.findViewById<EditText>(R.id.editTextIdRecuperacao)
    val botaoRecuperar = view.findViewById<Button>(R.id.buttonRecuperar)

    // Quando o botão é clicado, lê o ID digitado e fecha o diálogo
    botaoRecuperar.setOnClickListener {
      val idRecuperado = campoId.text.toString()
      // Aqui seria o ponto ideal para acionar lógica de verificação, envio de e-mail ou SMS
      dismiss()
    }

    return AlertDialog.Builder(requireContext(), R.style.LoginDialogTheme)
      .setView(view)
      .create()
  }

  /**
   * Define tamanho e aparência visual do popup após sua criação.
   * O tamanho é proporcional à densidade de tela para manter a responsividade.
   */
  override fun onStart() {
    super.onStart()
    val density = resources.displayMetrics.density
    dialog?.window?.setLayout(
      (320 * density).toInt(), // Largura adaptável em dp
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
  }
}
