package com.example.appsenkaspi.ui.atividade

import androidx.recyclerview.widget.DiffUtil
import com.example.appsenkaspi.data.local.entity.AtividadeEntity

/**
 * Callback de diferenciação para uso com ListAdapter ou RecyclerView.
 *
 * Compara duas instâncias de [AtividadeEntity] para determinar se representam o mesmo item
 * e se seus conteúdos mudaram, otimizando atualizações de UI.
 */
class AtividadeDiffCallback : DiffUtil.ItemCallback<AtividadeEntity>() {

  /**
   * Verifica se os dois itens representam a mesma atividade com base no ID.
   */
  override fun areItemsTheSame(oldItem: AtividadeEntity, newItem: AtividadeEntity): Boolean {
    return oldItem.id == newItem.id
  }

  /**
   * Verifica se o conteúdo das atividades é igual.
   */
  override fun areContentsTheSame(oldItem: AtividadeEntity, newItem: AtividadeEntity): Boolean {
    return oldItem == newItem
  }
}
