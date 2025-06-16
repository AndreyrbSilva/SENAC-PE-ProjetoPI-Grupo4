package com.example.appsenkaspi.ui.responsaveis

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.appsenkaspi.R
import com.example.appsenkaspi.data.local.entity.FuncionarioEntity
import com.example.appsenkaspi.databinding.ItemResponsavelBinding

/**
 * Adapter responsável por exibir uma lista de funcionários com opção de seleção visual.
 *
 * Pode operar em dois modos:
 * - Modo interativo (seleção): permite ao usuário selecionar e desselecionar responsáveis.
 * - Modo leitura: exibe os dados sem permitir interação.
 *
 * @param funcionarios Lista inicial de funcionários exibidos.
 * @param selecionados Lista de funcionários selecionados, usada para controlar seleção visual.
 * @param onSelecionadosAtualizados Callback chamado sempre que a lista de selecionados é alterada.
 * @param modoLeitura Indica se o adapter está no modo somente leitura (sem clique/seleção).
 */
class ResponsavelAdapter(
  funcionarios: List<FuncionarioEntity>,
  private val selecionados: MutableList<FuncionarioEntity>,
  private val onSelecionadosAtualizados: (List<FuncionarioEntity>) -> Unit,
  private val modoLeitura: Boolean = false
) : RecyclerView.Adapter<ResponsavelAdapter.ResponsavelViewHolder>() {

  /** Lista de funcionários exibidos no RecyclerView, podendo ser atualizada. */
  private var listaFuncionarios: MutableList<FuncionarioEntity> = funcionarios.toMutableList()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResponsavelViewHolder {
    val binding = ItemResponsavelBinding.inflate(
      LayoutInflater.from(parent.context), parent, false
    )
    return ResponsavelViewHolder(binding)
  }

  override fun onBindViewHolder(holder: ResponsavelViewHolder, position: Int) {
    holder.bind(listaFuncionarios[position])
  }

  override fun getItemCount(): Int = listaFuncionarios.size

  /**
   * ViewHolder que representa visualmente um funcionário na lista.
   * Exibe nome, e-mail e foto de perfil.
   */
  inner class ResponsavelViewHolder(
    private val binding: ItemResponsavelBinding
  ) : RecyclerView.ViewHolder(binding.root) {

    /**
     * Realiza o binding dos dados de um funcionário para a interface.
     * Também trata da lógica de seleção ou apenas exibição, dependendo do modo atual.
     *
     * @param funcionario Funcionário a ser exibido neste item.
     */
    fun bind(funcionario: FuncionarioEntity) {
      val contexto = binding.root.context

      binding.textNome.text = funcionario.nomeCompleto
      binding.textEmail.text = funcionario.email

      // Carrega a imagem de perfil com fallback para ícone padrão
      Glide.with(contexto)
        .load(funcionario.fotoPerfil)
        .placeholder(R.drawable.ic_person)
        .into(binding.imagemPerfil)

      if (!modoLeitura) {
        // Alterna a cor de fundo com base na seleção
        val isSelecionado = selecionados.contains(funcionario)
        binding.root.setCardBackgroundColor(
          if (isSelecionado) ContextCompat.getColor(contexto, R.color.selecionado)
          else ContextCompat.getColor(contexto, R.color.nao_selecionado)
        )

        // Lógica de seleção visual e notificação do callback
        binding.root.setOnClickListener {
          if (isSelecionado) {
            selecionados.remove(funcionario)
          } else {
            selecionados.add(funcionario)
          }
          this@ResponsavelAdapter.notifyItemChanged(adapterPosition)
          onSelecionadosAtualizados.invoke(selecionados)
        }
      } else {
        // No modo leitura, fundo transparente e clique desabilitado
        binding.root.setCardBackgroundColor(Color.TRANSPARENT)
        binding.root.setOnClickListener(null)
      }
    }
  }

  /**
   * Atualiza a lista de funcionários exibida.
   *
   * @param novaLista Nova lista de funcionários a ser exibida.
   */
  fun atualizarLista(novaLista: List<FuncionarioEntity>) {
    listaFuncionarios.clear()
    listaFuncionarios.addAll(novaLista)
    notifyDataSetChanged()
  }
}
