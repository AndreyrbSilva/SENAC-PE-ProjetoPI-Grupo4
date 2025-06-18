package com.example.appsenkaspi.ui.requisicao

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appsenkaspi.R
import com.example.appsenkaspi.data.local.entity.RequisicaoEntity
import com.example.appsenkaspi.data.local.enums.StatusRequisicao
import com.example.appsenkaspi.data.local.enums.TipoRequisicao

/**
 * Representa uma tupla de quatro elementos. Útil para carregar múltiplos dados relacionados
 * a uma notificação em um único objeto (como título, ícone, cor e mensagem).
 */
data class Quadruple<A, B, C, D>(
  val first: A,
  val second: B,
  val third: C,
  val fourth: D
)

/**
 * Adapter responsável por exibir a lista de notificações e requisições de atividades/ações.
 *
 * Suporta dois modos distintos:
 * - Modo Coordenador: exibe ícones de aprovação pendente.
 * - Modo Apoio: exibe o status da resposta do coordenador.
 *
 * Também oferece suporte a modo de seleção múltipla com checkboxes para ações em lote.
 *
 * @property funcionarioIdLogado ID do funcionário logado, usado para filtrar notificações automáticas.
 * @property modoCoordenador Indica se o usuário visualizando é coordenador (com poderes de decisão).
 * @property onSelecaoMudou Callback invocado sempre que a seleção de itens muda.
 * @property onItemClick Callback chamado ao clicar em um item fora do modo de seleção.
 */
class RequisicaoAdapter(
  private val funcionarioIdLogado: Int,
  var modoCoordenador: Boolean,
  var onSelecaoMudou: (() -> Unit)? = null,
  var onItemClick: (RequisicaoEntity) -> Unit = {}
) : ListAdapter<RequisicaoEntity, RequisicaoAdapter.ViewHolder>(DIFF_CALLBACK) {

  /** Indica se o adapter está no modo de seleção múltipla. */
  var modoSelecao = false

  /** Conjunto de requisições atualmente selecionadas. */
  val selecionadas = mutableSetOf<RequisicaoEntity>()

  /**
   * ViewHolder que representa a visualização de uma notificação individual.
   */
  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val iconStatus: ImageView = itemView.findViewById(R.id.iconStatus)
    val textoTitulo: TextView = itemView.findViewById(R.id.textoTitulo)
    val textoMensagem: TextView = itemView.findViewById(R.id.textoMensagem)
    val checkBox: CheckBox = itemView.findViewById(R.id.checkboxSelecionar)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.item_notificacao, parent, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val requisicao = getItem(position)

    // Verifica se a notificação é automática (ex: prazos, alterações)
    val isNotificacaoAutomatica = requisicao.tipo in listOf(
      TipoRequisicao.ATIVIDADE_PARA_VENCER,
      TipoRequisicao.ATIVIDADE_VENCIDA,
      TipoRequisicao.PRAZO_ALTERADO,
      TipoRequisicao.ATIVIDADE_CONCLUIDA,
      TipoRequisicao.RESPONSAVEL_ADICIONADO,
      TipoRequisicao.RESPONSAVEL_REMOVIDO
    )

    // Reset da view antes da vinculação (evita efeitos colaterais de reuso)
    holder.itemView.clearAnimation()
    holder.itemView.alpha = 1f
    holder.itemView.visibility = View.VISIBLE
    holder.checkBox.visibility = View.GONE
    holder.checkBox.alpha = 1f
    holder.checkBox.setOnCheckedChangeListener(null)
    holder.checkBox.isChecked = false

    if (isNotificacaoAutomatica) {
      // Oculta notificações automáticas destinadas a outros funcionários
      if (requisicao.solicitanteId != funcionarioIdLogado) {
        holder.itemView.visibility = View.GONE
        holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
        return
      }

      if (requisicao.resolvida) {
        // Exibe estado resolvido com ícone e texto informativo
        val tituloResolvido = when (requisicao.tipo) {
          TipoRequisicao.ATIVIDADE_PARA_VENCER -> "Prazo Resolvido"
          TipoRequisicao.ATIVIDADE_VENCIDA -> "Vencimento Resolvido"
          else -> "Atividade Resolvida"
        }

        holder.textoTitulo.text = "✔ $tituloResolvido"
        holder.textoMensagem.text = requisicao.mensagemResposta ?: "Esta notificação foi resolvida."
        holder.iconStatus.setImageResource(R.drawable.ic_check_circle)
        holder.iconStatus.setColorFilter(Color.parseColor("#9E9E9E"))

        holder.itemView.setOnClickListener(null)
        holder.checkBox.visibility = View.GONE
      } else {
        // Monta conteúdo da notificação automática com base no tipo
        val tipoInfo = when (requisicao.tipo) {
          TipoRequisicao.ATIVIDADE_PARA_VENCER -> Quadruple(
            "Prazo Próximo", R.drawable.ic_info, "#2196F3", "A atividade está próxima do prazo final."
          )
          TipoRequisicao.ATIVIDADE_VENCIDA -> Quadruple(
            "Atividade Vencida", R.drawable.ic_warning, "#F44336", "A atividade ultrapassou o prazo."
          )
          TipoRequisicao.PRAZO_ALTERADO -> Quadruple(
            "Prazo Alterado", R.drawable.ic_update, "#FF9800", "A data de prazo da atividade foi modificada."
          )
          TipoRequisicao.ATIVIDADE_CONCLUIDA -> Quadruple(
            "Atividade Concluída", R.drawable.ic_check_circle, "#4CAF50", "A atividade foi concluída."
          )
          TipoRequisicao.RESPONSAVEL_ADICIONADO -> Quadruple(
            "Responsável Adicionado", R.drawable.ic_warning, "#FF9800", "Responsável adicionado."
          )
          TipoRequisicao.RESPONSAVEL_REMOVIDO -> Quadruple(
            "Responsável Removido", R.drawable.ic_warning, "#FF9800", "Responsável removido."
          )
          else -> Quadruple(
            "Notificação", R.drawable.ic_info, "#607D8B", "Notificação automática."
          )
        }

        val (titulo, icone, corHex, mensagemPadrao) = tipoInfo

        holder.textoTitulo.text = titulo
        holder.textoMensagem.text = requisicao.mensagemResposta ?: mensagemPadrao
        holder.iconStatus.setImageResource(icone)
        holder.iconStatus.setColorFilter(Color.parseColor(corHex))
        holder.itemView.setOnClickListener(null)
      }
    } else {
      // Notificação de requisição enviada por um usuário
      val titulo = when (requisicao.tipo) {
        TipoRequisicao.CRIAR_ATIVIDADE -> "Criação de Atividade"
        TipoRequisicao.EDITAR_ATIVIDADE -> "Edição de Atividade"
        TipoRequisicao.COMPLETAR_ATIVIDADE -> "Conclusão de Atividade"
        TipoRequisicao.CRIAR_ACAO -> "Criação de Ação"
        TipoRequisicao.EDITAR_ACAO -> "Edição de Ação"
        else -> "Requisição"
      }

      holder.textoTitulo.text = titulo

      if (modoCoordenador) {
        // Coordenador visualiza pedidos pendentes
        holder.iconStatus.setImageResource(R.drawable.ic_help)
        holder.iconStatus.setColorFilter(Color.parseColor("#FFC107"))
        holder.textoMensagem.text = requisicao.mensagemResposta?.takeIf { it.isNotBlank() }
          ?: "Solicitação pendente de aprovação"
      } else {
        // Apoio visualiza o status da própria requisição
        val (icone, cor) = when (requisicao.status) {
          StatusRequisicao.ACEITA -> R.drawable.ic_check_circle to "#4CAF50"
          StatusRequisicao.RECUSADA -> R.drawable.ic_cancel to "#F44336"
          StatusRequisicao.PENDENTE -> R.drawable.ic_help to "#FFC107"
        }

        val mensagem = requisicao.mensagemResposta?.takeIf { it.isNotBlank() } ?: when (requisicao.status) {
          StatusRequisicao.ACEITA -> "Sua solicitação foi aprovada pelo coordenador."
          StatusRequisicao.RECUSADA -> "Sua solicitação foi recusada pelo coordenador."
          StatusRequisicao.PENDENTE -> "Sua solicitação está aguardando aprovação."
        }

        holder.iconStatus.setImageResource(icone)
        holder.iconStatus.setColorFilter(Color.parseColor(cor))
        holder.textoMensagem.text = mensagem
      }
    }

    // Animação e visibilidade da checkbox no modo seleção
    if (modoSelecao) {
      holder.checkBox.alpha = 0f
      holder.checkBox.visibility = View.VISIBLE
      holder.checkBox.animate().alpha(1f).setDuration(200).start()
    } else {
      holder.checkBox.animate().alpha(0f).setDuration(200).withEndAction {
        holder.checkBox.visibility = View.GONE
      }.start()
    }

    // Controle de seleção
    holder.checkBox.setOnCheckedChangeListener(null)
    holder.checkBox.isChecked = selecionadas.contains(requisicao)

    holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
      if (isChecked) {
        selecionadas.add(requisicao)
      } else {
        selecionadas.remove(requisicao)
      }
      onSelecaoMudou?.invoke()
    }

    // Clique normal no item
    holder.itemView.setOnClickListener {
      if (modoSelecao) {
        val novoEstado = !holder.checkBox.isChecked
        holder.checkBox.isChecked = novoEstado
      } else {
        onItemClick(requisicao)
      }
    }

    // Suavização da entrada com animação e opacidade condicional
    holder.itemView.translationY = 20f
    val targetAlpha = if (isNotificacaoAutomatica && requisicao.resolvida) 0.6f else 1f
    holder.itemView.animate()
      .translationY(0f)
      .alpha(targetAlpha)
      .setDuration(250)
      .start()
  }

  companion object {
    /**
     * Callback usado pelo ListAdapter para calcular atualizações eficientes na lista.
     */
    val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RequisicaoEntity>() {
      override fun areItemsTheSame(oldItem: RequisicaoEntity, newItem: RequisicaoEntity): Boolean {
        return oldItem.id == newItem.id
      }

      override fun areContentsTheSame(oldItem: RequisicaoEntity, newItem: RequisicaoEntity): Boolean {
        return oldItem == newItem
      }
    }
  }
}
