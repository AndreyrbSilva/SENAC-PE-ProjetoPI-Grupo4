package com.example.appsenkaspi.ui.pilar

import androidx.recyclerview.widget.DiffUtil
import com.example.appsenkaspi.data.local.entity.PilarEntity

/**
 * Callback utilizada pelo ListAdapter para otimizar atualizações na lista de PilarEntity.
 *
 * Essa classe define como o DiffUtil deve comparar os itens da lista antiga e nova,
 * permitindo que o RecyclerView atualize apenas os elementos modificados, sem recarregar tudo.
 *
 * Isso melhora o desempenho e a fluidez visual da interface.
 */
class PilarDiffCallback : DiffUtil.ItemCallback<PilarEntity>() {

  /**
   * Verifica se dois objetos representam o mesmo item da lista, com base no ID.
   *
   * @param oldItem Pilar anterior na lista.
   * @param newItem Pilar novo na lista.
   * @return true se os itens têm o mesmo ID (ou seja, representam o mesmo Pilar).
   */
  override fun areItemsTheSame(oldItem: PilarEntity, newItem: PilarEntity): Boolean {
    return oldItem.id == newItem.id
  }

  /**
   * Verifica se o conteúdo dos dois objetos é o mesmo.
   * Essa verificação considera os campos visíveis/relevantes do Pilar (nome, descrição e datas).
   *
   * @param oldItem Pilar anterior.
   * @param newItem Pilar novo.
   * @return true se todos os campos relevantes forem iguais.
   */
  override fun areContentsTheSame(oldItem: PilarEntity, newItem: PilarEntity): Boolean {
    return oldItem.nome == newItem.nome &&
      oldItem.descricao == newItem.descricao &&
      oldItem.dataInicio == newItem.dataInicio &&
      oldItem.dataPrazo == newItem.dataPrazo
  }
}
