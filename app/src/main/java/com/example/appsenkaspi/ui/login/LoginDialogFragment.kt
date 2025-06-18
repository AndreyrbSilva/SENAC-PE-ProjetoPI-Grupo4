package com.example.appsenkaspi.ui.login

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.appsenkaspi.R
import com.example.appsenkaspi.ui.principal.TelaPrincipalActivity
import com.example.appsenkaspi.data.local.database.AppDatabase
import com.example.appsenkaspi.helpers.salvarFuncionarioLogado
import com.example.appsenkaspi.viewmodel.FuncionarioViewModel
import kotlinx.coroutines.launch

/**
 * Diálogo de login que autentica um funcionário com base em ID e senha.
 *
 * Recursos incluídos:
 * - Armazenamento seguro com `EncryptedSharedPreferences` (opção "Lembrar de mim");
 * - Alternância de visibilidade da senha com ícones;
 * - Acesso ao diálogo de recuperação de senha (`EsqueciSenhaDialogFragment`);
 * - Navegação para a `TelaPrincipalActivity` em caso de sucesso.
 */
class LoginDialogFragment : DialogFragment() {

  /** ViewModel responsável por armazenar e propagar o estado do funcionário logado */
  private val funcionarioViewModel: FuncionarioViewModel by activityViewModels()

  @SuppressLint("ClickableViewAccessibility")
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val view = LayoutInflater.from(context).inflate(R.layout.login_popup, null)

    val editTextId = view.findViewById<EditText>(R.id.editTextId)
    val editTextSenha = view.findViewById<EditText>(R.id.editTextSenha)
    val checkBox = view.findViewById<CheckBox>(R.id.checkBoxLembrar)
    val buttonEntrar = view.findViewById<Button>(R.id.buttonEntrar)
    val textEsqueceu = view.findViewById<TextView>(R.id.textEsqueceuSenha)

    // Inicializa armazenamento criptografado usando a chave mestra da Android Keystore
    val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    val sharedPreferences = EncryptedSharedPreferences.create(
      "login_secure_prefs",
      masterKeyAlias,
      requireContext(),
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Recupera dados salvos se a opção "Lembrar de mim" estiver ativa
    val lembrarLogin = sharedPreferences.getBoolean("lembrarLogin", false)
    if (lembrarLogin) {
      editTextId.setText(sharedPreferences.getString("idAcesso", ""))
      editTextSenha.setText(sharedPreferences.getString("senha", ""))
      checkBox.isChecked = true
    }

    // Alternância de visibilidade do campo de senha com clique no ícone
    var senhaVisivel = false
    editTextSenha.setOnTouchListener { v, event ->
      val DRAWABLE_END = 2
      val drawable = editTextSenha.compoundDrawables[DRAWABLE_END]
      if (drawable != null && event.action == MotionEvent.ACTION_UP) {
        val touchAreaStart = editTextSenha.right - drawable.bounds.width() - 20
        if (event.rawX >= touchAreaStart) {
          senhaVisivel = !senhaVisivel
          editTextSenha.inputType = if (senhaVisivel) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
          } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
          }
          val icon = if (senhaVisivel) R.drawable.ic_open_eye else R.drawable.ic_closed_eye
          editTextSenha.setCompoundDrawablesWithIntrinsicBounds(0, 0, icon, 0)
          editTextSenha.setSelection(editTextSenha.text.length)
          v.performClick()
          return@setOnTouchListener true
        }
      }
      false
    }

    // Ação do botão Entrar: autentica o funcionário e inicia a tela principal
    buttonEntrar.setOnClickListener {
      val idAcesso = editTextId.text.toString().trim().toIntOrNull()
      val senha = editTextSenha.text.toString().trim()

      if (idAcesso == null || senha.isEmpty()) {
        Toast.makeText(context, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
        return@setOnClickListener
      }

      lifecycleScope.launch {
        val dao = AppDatabase.getDatabase(requireContext()).funcionarioDao()
        val funcionario = dao.autenticar(idAcesso, senha)

        if (funcionario != null) {
          // Armazena credenciais se "Lembrar de mim" estiver marcado
          val editor = sharedPreferences.edit()
          if (checkBox.isChecked) {
            editor.putString("idAcesso", idAcesso.toString())
            editor.putString("senha", senha)
            editor.putBoolean("lembrarLogin", true)
          } else {
            editor.clear()
          }
          editor.apply()

          salvarFuncionarioLogado(
            requireContext(),
            funcionario.id,
            funcionario.nomeUsuario
          )

          funcionarioViewModel.logarFuncionario(funcionario)
          startActivity(Intent(requireContext(), TelaPrincipalActivity::class.java))
          dismiss()
        } else {
          Toast.makeText(context, "Usuário ou senha inválidos", Toast.LENGTH_SHORT).show()
        }
      }
    }

    // Link para abrir o diálogo de recuperação de senha
    textEsqueceu.setOnClickListener {
      EsqueciSenhaDialogFragment().show(parentFragmentManager, "esqueciSenhaPopup")
    }

    return AlertDialog.Builder(requireContext(), R.style.LoginDialogTheme)
      .setView(view)
      .create()
  }

  /**
   * Define as dimensões e transparência do diálogo ao ser exibido.
   */
  override fun onStart() {
    super.onStart()
    dialog?.window?.setLayout(
      (320 * resources.displayMetrics.density).toInt(),
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
  }
}
