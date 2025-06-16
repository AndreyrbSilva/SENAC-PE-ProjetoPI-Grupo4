package com.example.appsenkaspi.ui.subpilares

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appsenkaspi.R
import com.example.appsenkaspi.ui.pilar.CriarPilarFragment

/**
 * Adapter responsável por exibir uma lista temporária de subpilares durante a criação de um Pilar.
 *
 * Cada item da lista mostra apenas o nome do subpilar.
 * Os subpilares são representados por instâncias da classe [CriarPilarFragment.SubpilarTemp],
 * que armazenam os dados locais inseridos pelo usuário antes da persistência no banco.
 *
 * @param subpilares Lista de subpilares temporários a serem exibidos.
 */
class SubpilarAdapter(
  private val subpilares: List<CriarPilarFragment.SubpilarTemp>
) : RecyclerView.Adapter<SubpilarAdapter.SubpilarViewHolder>() {

  /**
   * ViewHolder que representa visualmente um subpilar com seu nome.
   */
  class SubpilarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    /** Campo de texto que exibe o nome do subpilar. */
    val textNomeSubpilar: TextView = itemView.findViewById(R.id.textNomeSubpilar)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubpilarViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.item_subpilar, parent, false)
    return SubpilarViewHolder(view)
  }

  override fun onBindViewHolder(holder: SubpilarViewHolder, position: Int) {
    holder.textNomeSubpilar.text = subpilares[position].nome
  }

  override fun getItemCount(): Int = subpilares.size
}
