package com.example.appsenkaspi.ui.main

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.appsenkaspi.R
import com.example.appsenkaspi.ui.login.LoginDialogFragment

/**
 * Atividade principal da aplicação — ponto de entrada inicial.
 *
 * Exibe a tela com botão de login e configurações visuais básicas da status bar.
 * Ao clicar no botão, exibe o `LoginDialogFragment` para autenticação de usuário.
 */
class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // Define a cor da status bar conforme o tema visual do app
    window.statusBarColor = ContextCompat.getColor(this, R.color.graycont)

    // Garante que os ícones da status bar fiquem brancos (ideal para fundo escuro)
    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

    // Inicializa o botão de login e exibe o diálogo ao ser clicado
    val botaoLogin = findViewById<Button>(R.id.loginButtonPrincipal)
    botaoLogin.setOnClickListener {
      LoginDialogFragment().show(supportFragmentManager, "loginPopup")
    }
  }
}
