package com.example.appsenkaspi.ui.atividade

import androidx.recyclerview.widget.DiffUtil
import com.example.appsenkaspi.domain.model.AtividadeComFuncionarios

/**
 * Callback de comparação para uso com ListAdapter.
 *
 * Responsável por otimizar atualizações na lista de atividades,
 * verificando mudanças entre itens antigos e novos para evitar renderizações desnecessárias.
 */
class AtividadeComFuncionariosDiffCallback : DiffUtil.ItemCallback<AtividadeComFuncionarios>() {

  /**
   * Verifica se dois itens representam a mesma atividade com base no ID único.
   */
  override fun areItemsTheSame(
    oldItem: AtividadeComFuncionarios,
    newItem: AtividadeComFuncionarios
  ): Boolean {
    return oldItem.atividade.id == newItem.atividade.id
  }

  /**
   * Verifica se o conteúdo dos itens antigos e novos é idêntico.
   * Compara todos os campos, incluindo a lista de responsáveis.
   */
  override fun areContentsTheSame(
    oldItem: AtividadeComFuncionarios,
    newItem: AtividadeComFuncionarios
  ): Boolean {
    return oldItem == newItem
  }
}
