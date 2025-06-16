package com.example.appsenkaspi.ui.perfil

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.cardview.widget.CardView
import com.example.appsenkaspi.ui.pilar.PilarDiffCallback
import com.example.appsenkaspi.data.local.entity.PilarEntity
import com.example.appsenkaspi.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Adapter para exibir uma lista de [PilarEntity]s em um RecyclerView.
 *
 * Cada item representa um pilar, identificado por sua ordem e nome.
 * O adapter também verifica se o pilar possui subpilares, exibindo um ícone indicativo caso existam.
 *
 * @param onClickPilar Callback chamado quando o item do pilar é clicado.
 * @param verificarSubpilares Função de suspensão que recebe o ID do pilar e retorna `true` se ele tiver subpilares.
 */
class PilarAdapter(
  private val onClickPilar: (PilarEntity) -> Unit,
  private val verificarSubpilares: suspend (Int) -> Boolean
) : ListAdapter<PilarEntity, PilarAdapter.PilarViewHolder>(PilarDiffCallback()) {

  /**
   * ViewHolder responsável por vincular os dados do [PilarEntity] ao layout do item.
   */
  inner class PilarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val textNomePilar: TextView = itemView.findViewById(R.id.textNomePilar)
    private val cardPilar: CardView = itemView.findViewById(R.id.cardPilar)
    private val iconeSubpilares: ImageView = itemView.findViewById(R.id.iconeSubpilares)

    /**
     * Associa os dados de um pilar ao layout, incluindo verificação de subpilares.
     *
     * @param pilar O objeto [PilarEntity] a ser exibido.
     * @param position A posição do item na lista, usada para rotular o pilar como "1º Pilar", "2º Pilar", etc.
     */
    fun bind(pilar: PilarEntity, position: Int) {
      textNomePilar.text = "${position + 1}º Pilar"

      cardPilar.setOnClickListener {
        onClickPilar(pilar)
      }

      // Verifica de forma assíncrona se há subpilares e exibe o ícone caso existam
      CoroutineScope(Dispatchers.Main).launch {
        val temSubpilares = verificarSubpilares(pilar.id)
        iconeSubpilares.visibility = if (temSubpilares) View.VISIBLE else View.GONE
      }
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PilarViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.item_pilar, parent, false)
    return PilarViewHolder(view)
  }

  override fun onBindViewHolder(holder: PilarViewHolder, position: Int) {
    holder.bind(getItem(position), position)
  }
}
