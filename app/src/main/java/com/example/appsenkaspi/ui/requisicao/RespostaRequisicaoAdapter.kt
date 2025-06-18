package com.example.appsenkaspi.ui.requisicao

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appsenkaspi.data.local.entity.RequisicaoEntity
import com.example.appsenkaspi.databinding.ItemRespostaBinding

/**
 * Adapter para exibir uma lista resumida de respostas às requisições realizadas.
 *
 * Essa visualização é usada, por exemplo, em telas de histórico ou detalhes de notificações resolvidas,
 * exibindo informações como tipo da requisição, status final, coordenador responsável e data de resposta.
 *
 * @param lista Lista de objetos [RequisicaoEntity] a serem exibidos.
 */
class RespostaRequisicaoAdapter(
  private val lista: List<RequisicaoEntity>
) : RecyclerView.Adapter<RespostaRequisicaoAdapter.RespostaViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RespostaViewHolder {
    val binding = ItemRespostaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return RespostaViewHolder(binding)
  }

  override fun onBindViewHolder(holder: RespostaViewHolder, position: Int) {
    holder.bind(lista[position])
  }

  override fun getItemCount(): Int = lista.size

  /**
   * ViewHolder responsável por associar os dados de uma [RequisicaoEntity] aos elementos visuais do item.
   *
   * @property binding Binding gerado para o layout XML do item de resposta.
   */
  inner class RespostaViewHolder(private val binding: ItemRespostaBinding) : RecyclerView.ViewHolder(binding.root) {

    /**
     * Associa os dados de uma requisição aos campos visuais do item.
     *
     * @param requisicao Objeto contendo os dados da requisição a serem exibidos.
     */
    fun bind(requisicao: RequisicaoEntity) {
      binding.textTipo.text = requisicao.tipo.name
      binding.textStatus.text = requisicao.status.name
      binding.textCoordenador.text = "Coordenador ID: ${requisicao.coordenadorId}"
      binding.textDataResposta.text = requisicao.dataResposta?.toString() ?: "Sem data"
    }
  }
}
