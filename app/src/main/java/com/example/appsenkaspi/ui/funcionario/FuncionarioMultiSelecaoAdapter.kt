package com.example.appsenkaspi.ui.funcionario

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.appsenkaspi.R
import com.example.appsenkaspi.data.local.entity.FuncionarioEntity

/**
 * Adapter para exibição e seleção múltipla de funcionários com `CheckBox` em uma lista do tipo `RecyclerView`.
 *
 * Utiliza `ListAdapter` com `DiffUtil` para performance eficiente em grandes listas.
 * Permite carregar a imagem de perfil (quando disponível) com Glide, e mantém controle dos selecionados.
 *
 * @property selecionados Lista mutável externa contendo os funcionários atualmente selecionados.
 */
class FuncionarioMultiSelecaoAdapter(
  private val selecionados: MutableList<FuncionarioEntity>
) : ListAdapter<FuncionarioEntity, FuncionarioMultiSelecaoAdapter.ViewHolder>(DiffCallback()) {

  /**
   * ViewHolder que representa um funcionário com um CheckBox e imagem de perfil.
   */
  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val checkBox: CheckBox = itemView.findViewById(R.id.checkFuncionario)
    private val imagePerfil: ImageView = itemView.findViewById(R.id.imageViewFotoPerfil)

    /**
     * Liga os dados do funcionário à View, controlando a seleção e exibição da imagem.
     *
     * @param funcionario Funcionário a ser exibido na posição correspondente.
     */
    fun bind(funcionario: FuncionarioEntity) {
      checkBox.text = funcionario.nomeCompleto
      checkBox.setOnCheckedChangeListener(null)
      checkBox.isChecked = selecionados.contains(funcionario)

      checkBox.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) selecionados.add(funcionario)
        else selecionados.remove(funcionario)
      }

      // Exibe imagem de perfil se disponível
      if (!funcionario.fotoPerfil.isNullOrEmpty()) {
        Glide.with(itemView).load(funcionario.fotoPerfil).circleCrop().into(imagePerfil)
        imagePerfil.visibility = View.VISIBLE
      } else {
        imagePerfil.visibility = View.GONE
      }
    }
  }

  /**
   * Infla o layout `item_funcionario_selecao` e instancia o ViewHolder correspondente.
   */
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.item_funcionario_selecao, parent, false)
    return ViewHolder(view)
  }

  /**
   * Associa os dados do funcionário ao ViewHolder.
   *
   * @param holder ViewHolder a ser populado.
   * @param position Posição atual na lista.
   */
  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bind(getItem(position))
  }

  /**
   * Callback para otimização da atualização da lista usando DiffUtil.
   */
  class DiffCallback : DiffUtil.ItemCallback<FuncionarioEntity>() {
    override fun areItemsTheSame(a: FuncionarioEntity, b: FuncionarioEntity) = a.id == b.id
    override fun areContentsTheSame(a: FuncionarioEntity, b: FuncionarioEntity) = a == b
  }
}
