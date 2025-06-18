package com.example.appsenkaspi.ui.acao

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appsenkaspi.R
import com.example.appsenkaspi.data.local.entity.AcaoEntity
import com.example.appsenkaspi.domain.model.AcaoComStatus


/**
 * Adapter responsável por exibir uma lista de ações com status agregado de atividades.
 *
 * @property onClick Callback chamado quando uma ação é clicada.
 *
 * Utilizado na tela de listagem de ações, exibindo progresso e total de atividades.
 */
class AcaoAdapter(
  private val onClick: (AcaoEntity) -> Unit
) : ListAdapter<AcaoComStatus, AcaoAdapter.ViewHolder>(AcaoDiffCallback()) {

  /**
   * ViewHolder responsável por popular os dados de cada item da lista de ações.
   */
  inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val nomeTv = view.findViewById<TextView>(R.id.textTituloAcao)
    private val statusTv = view.findViewById<TextView>(R.id.textFracaoAcao)
    private val progressoBar = view.findViewById<ProgressBar>(R.id.progressoAcaoItem)
    private val textoPorcentagem = view.findViewById<TextView>(R.id.txtPorcentagemAcao)
    private val seta = view.findViewById<ImageView>(R.id.iconArrowAcao)

    /**
     * Associa os dados de uma ação à interface do item correspondente.
     *
     * @param acaoComStatus Objeto contendo a ação e seu progresso em atividades.
     */
    fun bind(acaoComStatus: AcaoComStatus) {
      val acao = acaoComStatus.acao
      nomeTv.text = acao.nome

      val total = acaoComStatus.totalAtividades
      val concluidas = acaoComStatus.ativasConcluidas
      val pct = if (total > 0) concluidas * 100 / total else 0

      statusTv.text = "$concluidas/$total ${if (total == 1) "Atividade" else "Atividades"} Concluída${if (concluidas == 1) "" else "s"}"

      progressoBar.max = 100
      progressoBar.progress = pct
      textoPorcentagem.text = "$pct%"

      seta.setColorFilter(Color.GREEN)

      itemView.setOnClickListener {
        onClick(acao)
      }
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.item_acao, parent, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bind(getItem(position))
  }
}

/**
 * Diferença entre itens da lista de ações para otimizar atualizações.
 *
 * Compara ações com base em seus identificadores e conteúdo completo.
 */
class AcaoDiffCallback : DiffUtil.ItemCallback<AcaoComStatus>() {
  override fun areItemsTheSame(oldItem: AcaoComStatus, newItem: AcaoComStatus): Boolean {
    return oldItem.acao.id == newItem.acao.id
  }

  override fun areContentsTheSame(oldItem: AcaoComStatus, newItem: AcaoComStatus): Boolean {
    return oldItem == newItem
  }
}
