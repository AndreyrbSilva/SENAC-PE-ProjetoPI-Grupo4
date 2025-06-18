package com.example.appsenkaspi.ui.pilar

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.appsenkaspi.R
import com.example.appsenkaspi.ui.pilar.TelaPilarFragment

/**
 * Activity responsável por exibir a tela de Pilar principal.
 *
 * Esta activity é acionada após o login, recebendo o ID do funcionário autenticado via `Intent`.
 * Caso seja a primeira criação da tela (`savedInstanceState == null`), carrega o fragmento `TelaPilarFragment`,
 * passando o ID do funcionário por meio de argumentos.
 */
class TelaPilarActivity : AppCompatActivity() {

  /** ID do funcionário logado, recuperado da Intent de login. */
  private var funcionarioLogadoId: Int = -1

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.atv_tela_pilar)

    // Recupera o ID do funcionário da Intent
    funcionarioLogadoId = intent.getIntExtra("funcionarioId", -1)

    // Se é a primeira criação (não é recriação automática do sistema), carrega o fragmento inicial
    if (savedInstanceState == null) {
      val fragment = TelaPilarFragment().apply {
        arguments = Bundle().apply {
          putInt("funcionarioId", funcionarioLogadoId)
        }
      }

      supportFragmentManager.beginTransaction()
        .replace(R.id.fragment_container, fragment)
        .commit()
    }
  }
}

