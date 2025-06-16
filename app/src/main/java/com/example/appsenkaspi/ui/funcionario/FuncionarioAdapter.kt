package com.example.appsenkaspi.ui.funcionario

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appsenkaspi.data.local.entity.FuncionarioEntity
import com.example.appsenkaspi.R

/**
 * Adapter responsável por exibir uma lista de funcionários em um `RecyclerView`.
 *
 * Utiliza o layout `box_perfil` para representar visualmente cada item, incluindo nome e foto de perfil.
 * Este adapter é adequado para exibição somente leitura de funcionários (sem interação ou seleção).
 *
 * @property listaFuncionarios Lista de objetos `FuncionarioEntity` a serem exibidos.
 */
class FuncionarioAdapter(
  private val listaFuncionarios: List<FuncionarioEntity>
) : RecyclerView.Adapter<FuncionarioAdapter.FuncionarioViewHolder>() {

  /**
   * ViewHolder interno que representa cada item da lista de funcionários.
   *
   * @param view A View inflada do layout `box_perfil` contendo os elementos visuais.
   */
  inner class FuncionarioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    /** TextView que exibe o nome completo do funcionário */
    val nomeTextView: TextView = view.findViewById(R.id.textViewNomeFuncionario)

    /** ImageView que exibe a foto de perfil (ou imagem padrão) do funcionário */
    val fotoImageView: ImageView = view.findViewById(R.id.imageViewFotoPerfil)
  }

  /**
   * Infla o layout `box_perfil` e cria um novo `FuncionarioViewHolder`.
   */
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FuncionarioViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.box_perfil, parent, false)
    return FuncionarioViewHolder(view)
  }

  /**
   * Associa os dados do funcionário à interface do item correspondente.
   *
   * @param holder ViewHolder que será populado
   * @param position Posição do item na lista
   */
  override fun onBindViewHolder(holder: FuncionarioViewHolder, position: Int) {
    val funcionario = listaFuncionarios[position]
    holder.nomeTextView.text = funcionario.nomeCompleto
    holder.fotoImageView.setImageResource(R.drawable.ic_perfil_exemplo) // Exibe imagem padrão
  }

  /**
   * Retorna a quantidade total de funcionários na lista.
   */
  override fun getItemCount() = listaFuncionarios.size
}
