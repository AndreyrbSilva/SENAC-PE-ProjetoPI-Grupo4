package com.example.appsenkaspi.ui.perfil

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.appsenkaspi.R
import com.example.appsenkaspi.data.local.database.AppDatabase
import kotlinx.coroutines.launch

/**
 * Fragmento responsável por exibir os detalhes do funcionário logado no sistema,
 * como cargo, e-mail e telefone.
 *
 * Os dados são carregados do `SharedPreferences` (ID do funcionário) e consultados
 * no banco de dados local via Room.
 *
 * Exibe as informações nos seguintes campos:
 * - Cargo formatado (com inicial maiúscula) + "na empresa SENAC"
 * - E-mail cadastrado
 * - Telefone (quando disponível)
 */
class DetalhesFragment : Fragment() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = inflater.inflate(R.layout.fragment_detalhes, container, false)

    // Recupera ID do funcionário logado a partir do SharedPreferences
    val prefs = requireContext().getSharedPreferences("funcionario_prefs", Context.MODE_PRIVATE)
    val funcionarioId = prefs.getInt("funcionario_id", -1)

    // Referência para os elementos visuais do layout
    val cargoTextView = view.findViewById<TextView>(R.id.textCargo)
    val emailTextView = view.findViewById<TextView>(R.id.textEmail)
    val telefoneTextView = view.findViewById<TextView>(R.id.textTelefone)

    // Carrega os dados do funcionário de forma assíncrona com lifecycleScope
    lifecycleScope.launch {
      val dao = AppDatabase.getDatabase(requireContext()).funcionarioDao()
      val funcionario = dao.getFuncionarioById(funcionarioId)

      funcionario?.let {
        // Formata o cargo com a primeira letra maiúscula e exibe no texto
        val nomeCargoFormatado = it.cargo.name.lowercase()
          .replaceFirstChar { ch -> ch.uppercase() }

        cargoTextView.text = "$nomeCargoFormatado(a) na empresa SENAC"
        emailTextView.text = it.email
        telefoneTextView.text = it.numeroTel // Substitua por outro campo se for necessário
      }
    }

    return view
  }
}
