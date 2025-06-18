package com.example.appsenkaspi.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.example.appsenkaspi.R
import com.example.appsenkaspi.data.local.enums.PrioridadeAtividade


/**
 * Diálogo para seleção da prioridade de uma atividade.
 *
 * Exibe três opções visuais (Alta, Média, Baixa) e retorna a prioridade escolhida ao fragmento pai
 * via `setFragmentResult`. Utiliza o enum `PrioridadeAtividade` como modelo de seleção.
 */
class EscolherPrioridadeDialogFragment : DialogFragment() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    // Infla o layout do diálogo contendo as opções de prioridade
    return inflater.inflate(R.layout.dialog_prioridade, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    // Elementos da interface: botão fechar e opções de prioridade
    val fechar = view.findViewById<ImageView>(R.id.fecharDialogPrioridade)
    val opcaoAlta = view.findViewById<View>(R.id.cardPrioridadeAlta)
    val opcaoMedia = view.findViewById<View>(R.id.cardPrioridadeMedia)
    val opcaoBaixa = view.findViewById<View>(R.id.cardPrioridadeBaixa)

    // Fecha o diálogo sem seleção
    fechar.setOnClickListener {
      dismiss()
    }

    // Função auxiliar para enviar a prioridade escolhida ao fragmento pai
    val selecionarPrioridade = { prioridade: PrioridadeAtividade ->
      parentFragmentManager.setFragmentResult(
        "prioridadeSelecionada",
        Bundle().apply {
          putString("valor", prioridade.name)
        }
      )
      dismiss()
    }

    // Define ações ao clicar nas opções visuais
    opcaoAlta.setOnClickListener { selecionarPrioridade(PrioridadeAtividade.ALTA) }
    opcaoMedia.setOnClickListener { selecionarPrioridade(PrioridadeAtividade.MEDIA) }
    opcaoBaixa.setOnClickListener { selecionarPrioridade(PrioridadeAtividade.BAIXA) }
  }

  override fun onStart() {
    super.onStart()
    // Define o tamanho do diálogo para ocupar toda a largura da tela
    dialog?.window?.setLayout(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
  }
}
