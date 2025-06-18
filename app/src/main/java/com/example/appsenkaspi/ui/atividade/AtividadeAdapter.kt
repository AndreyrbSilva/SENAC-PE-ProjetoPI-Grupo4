package com.example.appsenkaspi.ui.atividade

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.appsenkaspi.domain.model.AtividadeComFuncionarios
import com.example.appsenkaspi.data.local.enums.PrioridadeAtividade
import com.example.appsenkaspi.R
import com.example.appsenkaspi.data.local.enums.StatusAtividade
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Adapter personalizado para exibição de atividades dentro de um RecyclerView.
 *
 * Cada item representa uma atividade com seu título, prioridade, status,
 * data de prazo e responsáveis visuais (fotos). A aparência se adapta ao status
 * da atividade e à proximidade da data de prazo.
 *
 * @param onItemClick Função de callback chamada ao clicar em uma atividade.
 */
class AtividadeAdapter(
  private val onItemClick: (AtividadeComFuncionarios) -> Unit
) : ListAdapter<AtividadeComFuncionarios, AtividadeAdapter.ViewHolder>(
  AtividadeComFuncionariosDiffCallback()
) {

  /**
   * ViewHolder interno que referencia os componentes visuais do item.
   */
  inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val titulo: TextView = view.findViewById(R.id.textTitulo)
    val statusBolinha: View = view.findViewById(R.id.statusBolinha)
    val prioridadeQuadrado: View = view.findViewById(R.id.viewPrioridade)
    val containerResponsaveis: LinearLayout = view.findViewById(R.id.containerResponsaveis)
    val containerData: LinearLayout = view.findViewById(R.id.containerData)
    val textData: TextView = view.findViewById(R.id.textData)
    val iconClock: ImageView = view.findViewById(R.id.iconClock)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val item = LayoutInflater.from(parent.context)
      .inflate(R.layout.item_atividade, parent, false)
    return ViewHolder(item)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val atividadeComFuncionarios = getItem(position)
    val atividade = atividadeComFuncionarios.atividade

    // Define título
    holder.titulo.text = atividade.nome

    // Define cor do quadrado de prioridade
    val corPrioridade = when (atividade.prioridade) {
      PrioridadeAtividade.BAIXA -> Color.parseColor("#2ECC40")
      PrioridadeAtividade.MEDIA -> Color.parseColor("#F1C40F")
      PrioridadeAtividade.ALTA -> Color.parseColor("#E74C3C")
    }

    val layerDrawable = AppCompatResources.getDrawable(holder.itemView.context, R.drawable.bg_prioridade_layer) as? LayerDrawable
    layerDrawable?.let {
      val fundo = it.findDrawableByLayerId(R.id.fundo)
      val wrappedFundo = DrawableCompat.wrap(fundo.mutate())
      DrawableCompat.setTint(wrappedFundo, corPrioridade)
      holder.prioridadeQuadrado.background = it
    }

    // Define cor da bolinha de status
    val corStatus = when (atividade.status) {
      StatusAtividade.CONCLUIDA -> Color.parseColor("#2ECC40")
      StatusAtividade.EM_ANDAMENTO -> Color.parseColor("#F1C40F")
      else -> Color.TRANSPARENT
    }
    val drawableStatus = holder.statusBolinha.background as? GradientDrawable
    drawableStatus?.setColor(corStatus)

    // Define texto e cor do campo de data
    val diasRestantes = diasParaPrazo(atividade.dataPrazo)
    val dataFormatada = SimpleDateFormat("dd 'de' MMM, HH:mm", Locale("pt", "BR")).format(atividade.dataPrazo)
    holder.textData.text = dataFormatada

    val corFundoData = when {
      atividade.status == StatusAtividade.CONCLUIDA -> Color.parseColor("#2ECC40")
      diasRestantes <= 3 -> Color.parseColor("#E74C3C")
      diasRestantes <= 7 -> Color.parseColor("#F1C40F")
      else -> Color.parseColor("#CCCCCC")
    }

    val fundoDataDrawable = GradientDrawable().apply {
      cornerRadius = 24f
      setColor(corFundoData)
    }
    holder.containerData.background = fundoDataDrawable
    holder.textData.setTextColor(Color.BLACK)
    holder.iconClock.setColorFilter(Color.BLACK)

    // Renderiza as fotos dos responsáveis
    holder.containerResponsaveis.removeAllViews()
    val dimensao = holder.itemView.resources.getDimensionPixelSize(R.dimen.tamanho_foto_responsavel)

    atividadeComFuncionarios.funcionarios.forEach { funcionario ->
      val imageView = CircleImageView(holder.itemView.context).apply {
        layoutParams = ViewGroup.MarginLayoutParams(dimensao, dimensao).apply {
          marginEnd = 12
        }
        borderWidth = 2
        borderColor = Color.WHITE
      }
      Glide.with(imageView.context)
        .load(funcionario.fotoPerfil)
        .placeholder(R.drawable.ic_person)
        .into(imageView)
      holder.containerResponsaveis.addView(imageView)
    }

    // Define ação de clique no card
    holder.itemView.setOnClickListener {
      onItemClick(atividadeComFuncionarios)
    }
  }

  /**
   * Calcula o número de dias restantes até a data de prazo.
   */
  private fun diasParaPrazo(dataPrazo: Date): Int {
    val hoje = Calendar.getInstance().time
    val diff = dataPrazo.time - hoje.time
    return (diff / (1000 * 60 * 60 * 24)).toInt()
  }
}
