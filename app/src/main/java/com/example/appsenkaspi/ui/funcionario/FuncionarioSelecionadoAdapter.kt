package com.example.appsenkaspi.ui.funcionario

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appsenkaspi.R
import com.example.appsenkaspi.data.local.entity.FuncionarioEntity

/**
 * Adapter responsável por exibir uma lista de funcionários previamente selecionados, em modo somente leitura.
 *
 * Esse adapter é útil para exibição em telas de detalhes ou confirmações, onde a lista de responsáveis não é interativa.
 * As imagens de perfil são propositalmente ocultadas por padrão, mas o layout mantém compatibilidade com `box_perfil`.
 *
 * @property listaFuncionarios Lista de objetos `FuncionarioEntity` a serem exibidos.
 */
class FuncionarioSelecionadoAdapter(
  private val listaFuncionarios: List<FuncionarioEntity>
) : RecyclerView.Adapter<FuncionarioSelecionadoAdapter.ViewHolder>() {

  /**
   * ViewHolder que representa visualmente cada funcionário com nome e (opcionalmente) foto.
   */
  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val imagePerfil: ImageView = itemView.findViewById(R.id.imageViewFotoPerfil)
    private val textNome: TextView = itemView.findViewById(R.id.textViewNomeFuncionario)

    /**
     * Associa os dados do funcionário à interface.
     *
     * @param funcionario Funcionário exibido nesta célula.
     */
    fun bind(funcionario: FuncionarioEntity) {
      textNome.text = funcionario.nomeCompleto

      // Oculta a imagem de perfil por padrão
      imagePerfil.visibility = View.GONE
    }
  }

  /**
   * Infla o layout `box_perfil` para representar visualmente um funcionário.
   */
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.box_perfil, parent, false)
    return ViewHolder(view)
  }

  /**
   * Realiza a ligação dos dados do funcionário com o ViewHolder correspondente.
   */
  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bind(listaFuncionarios[position])
  }

  /**
   * Retorna o número total de funcionários a serem exibidos.
   */
  override fun getItemCount(): Int = listaFuncionarios.size
}
