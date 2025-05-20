package com.example.appsenkaspi

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

class RequisicaoAdapter(
  private val funcionarioIdLogado: Int,
  var modoCoordenador: Boolean,
  var onItemClick: (RequisicaoEntity) -> Unit = {}
) : ListAdapter<RequisicaoEntity, RequisicaoAdapter.ViewHolder>(DIFF_CALLBACK) {

  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val iconStatus: ImageView = itemView.findViewById(R.id.iconStatus)
    val textoTitulo: TextView = itemView.findViewById(R.id.textoTitulo)
    val textoMensagem: TextView = itemView.findViewById(R.id.textoMensagem)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.item_notificacao, parent, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val requisicao = getItem(position)

    // 🔔 Notificações automáticas de prazo
    if (requisicao.tipo == TipoRequisicao.ATIVIDADE_PARA_VENCER || requisicao.tipo == TipoRequisicao.ATIVIDADE_VENCIDA || requisicao.tipo == TipoRequisicao.PRAZO_ALTERADO || requisicao.tipo == TipoRequisicao.ATIVIDADE_CONCLUIDA || requisicao.tipo == TipoRequisicao.RESPONSAVEL_ADICIONADO || requisicao.tipo == TipoRequisicao.RESPONSAVEL_REMOVIDO) {
      if (requisicao.solicitanteId != funcionarioIdLogado) {
        holder.itemView.visibility = View.GONE
        holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
        return
      }

      if (requisicao.resolvida) {
        val tituloResolvido = when (requisicao.tipo) {
          TipoRequisicao.ATIVIDADE_PARA_VENCER -> "Prazo Resolvido"
          TipoRequisicao.ATIVIDADE_VENCIDA -> "Vencimento Resolvido"
          else -> "Atividade Resolvida"
        }

        holder.textoTitulo.text = "✔ $tituloResolvido"
        holder.textoMensagem.text = requisicao.mensagemResposta ?: "Esta notificação foi resolvida."
        holder.iconStatus.setImageResource(R.drawable.ic_check_circle)
        holder.iconStatus.setColorFilter(Color.parseColor("#9E9E9E")) // cinza claro
        holder.itemView.alpha = 0.6f
        holder.itemView.setOnClickListener(null)
        return
      }

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
          "Atividade Concluida", R.drawable.ic_check_circle, "#4CAF50", "A atividade foi concluida"
        )
        TipoRequisicao.RESPONSAVEL_ADICIONADO -> Quadruple(
          "Responsavel Adicionado", R.drawable.ic_warning, "#FF9800", "Responsavel Adicionado"
        )
        TipoRequisicao.RESPONSAVEL_REMOVIDO -> Quadruple(
          "Responsavel Adicionado", R.drawable.ic_warning, "#FF9800", "Responsavel Removido"
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
      holder.itemView.alpha = 1f
      holder.itemView.setOnClickListener(null)
      return
    }

    // 🔧 Requisições formais: criar, editar, completar ações/atividades
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
      holder.iconStatus.setImageResource(R.drawable.ic_help)
      holder.iconStatus.setColorFilter(Color.parseColor("#FFC107"))
      holder.textoMensagem.text = requisicao.mensagemResposta?.takeIf { it.isNotBlank() }
        ?: "Solicitação pendente de aprovação"
      holder.itemView.setOnClickListener { onItemClick(requisicao) }
    } else {
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
      holder.itemView.setOnClickListener(null)
    }
  }

  companion object {
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
