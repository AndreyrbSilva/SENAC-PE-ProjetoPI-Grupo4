package com.example.appsenkaspi.ui.historico

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appsenkaspi.R
import com.example.appsenkaspi.data.local.entity.PilarEntity
import com.example.appsenkaspi.data.local.enums.StatusPilar
import com.example.appsenkaspi.viewmodel.PilarViewModel
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter utilizado no `HistoricoFragment` para exibir uma lista de Pilares agrupados por ano.
 *
 * Suporta múltiplos tipos de itens:
 * - Cabeçalhos (`HeaderViewHolder`) que indicam o ano;
 * - Cartões (`PilarViewHolder`) que representam os pilares com informações de status, data e progresso.
 *
 * O progresso de cada pilar é calculado de forma assíncrona com `ViewModelScope`.
 *
 * @param viewModel ViewModel utilizado para cálculo de progresso dos pilares.
 * @param pilaresOriginais Lista inicial de pilares a serem exibidos.
 * @param onClickPilar Função executada quando um pilar é clicado.
 */
class TelaHistoricoAdapter(
  private val viewModel: PilarViewModel,
  pilaresOriginais: List<PilarEntity>,
  private val onClickPilar: (PilarEntity) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  companion object {
    private const val TIPO_HEADER = 0
    private const val TIPO_PILAR = 1
  }

  /** Lista de itens intercalando cabeçalhos de ano (String) e pilares (PilarEntity) */
  private var listaItens: List<Any> = agruparPorAno(pilaresOriginais)

  /** CoroutineScope próprio do adapter para chamadas assíncronas */
  private val adapterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  /**
   * ViewHolder para exibir o cabeçalho de agrupamento por ano.
   */
  inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val txtAno: TextView = itemView.findViewById(R.id.txtAno)
  }

  /**
   * ViewHolder que representa um Pilar com dados visuais de título, status, data e progresso.
   */
  inner class PilarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val textTitulo: TextView = itemView.findViewById(R.id.textTitulo)
    val statusOverlay: ImageView = itemView.findViewById(R.id.statusOverlay)
    val textStatus: TextView = itemView.findViewById(R.id.textStatus)
    val textData: TextView = itemView.findViewById(R.id.textData)
    val progressoPilar: ProgressBar = itemView.findViewById(R.id.progressoPilar)
    val percentual: TextView = itemView.findViewById(R.id.percentual)
  }

  override fun getItemViewType(position: Int): Int {
    return if (listaItens[position] is String) TIPO_HEADER else TIPO_PILAR
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return if (viewType == TIPO_HEADER) {
      val view = inflater.inflate(R.layout.item_header_ano, parent, false)
      HeaderViewHolder(view)
    } else {
      val view = inflater.inflate(R.layout.item_pilar_historico, parent, false)
      PilarViewHolder(view)
    }
  }

  override fun getItemCount(): Int = listaItens.size

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    val item = listaItens[position]

    if (holder is HeaderViewHolder && item is String) {
      holder.txtAno.text = item
    } else if (holder is PilarViewHolder && item is PilarEntity) {
      val pilar = item
      holder.textTitulo.text = pilar.nome ?: "Sem título"

      // Define data formatada com base no status
      val dataFormatada = formatarData(
        when (pilar.status) {
          StatusPilar.EXCLUIDO -> pilar.dataExclusao
          StatusPilar.CONCLUIDO -> pilar.dataConclusao
          StatusPilar.VENCIDO -> pilar.dataPrazo
          else -> null
        }
      )

      // Configura status visual
      when (pilar.status) {
        StatusPilar.CONCLUIDO -> {
          holder.statusOverlay.visibility = View.VISIBLE
          holder.statusOverlay.setImageResource(R.drawable.bverde)
          holder.textStatus.text = "Concluído"
          holder.textData.text = dataFormatada?.let { "Concluído em: $it" } ?: ""
        }
        StatusPilar.EXCLUIDO -> {
          holder.statusOverlay.visibility = View.VISIBLE
          holder.statusOverlay.setImageResource(R.drawable.bvermelho)
          holder.textStatus.text = "Excluído"
          holder.textData.text = dataFormatada?.let { "Excluído em: $it" } ?: ""
        }
        StatusPilar.VENCIDO -> {
          holder.statusOverlay.visibility = View.VISIBLE
          holder.statusOverlay.setImageResource(R.drawable.blaranja)
          holder.textStatus.text = "Vencido"
          holder.textData.text = dataFormatada?.let { "Venceu em: $it" } ?: ""
        }
        else -> {
          holder.statusOverlay.visibility = View.GONE
          holder.textStatus.text = ""
          holder.textData.text = ""
        }
      }

      // Inicializa visualmente a barra de progresso com zero
      holder.progressoPilar.progress = 0
      holder.percentual.text = "0%"

      // Calcula o progresso real de forma assíncrona
      adapterScope.launch {
        val progressoFloat = viewModel.calcularProgressoInterno(pilar.id)
        val progresso = (progressoFloat * 100).toInt()
        holder.progressoPilar.progress = progresso
        holder.percentual.text = "$progresso%"
      }

      // Define clique para abrir tela de detalhes
      holder.itemView.setOnClickListener {
        onClickPilar(pilar)
      }
    }
  }

  /**
   * Atualiza a lista interna de itens com novo agrupamento por ano.
   *
   * @param novaLista Lista atualizada de pilares.
   */
  fun atualizarLista(novaLista: List<PilarEntity>) {
    listaItens = agruparPorAno(novaLista)
    notifyDataSetChanged()
  }

  /**
   * Agrupa os pilares por ano da data de prazo ou exclusão, retornando lista intercalada com cabeçalhos.
   */
  private fun agruparPorAno(pilares: List<PilarEntity>): List<Any> {
    val dfAno = SimpleDateFormat("yyyy", Locale.getDefault())

    return pilares
      .sortedByDescending {
        it.dataPrazo ?: it.dataExclusao ?: Date(0)
      }
      .groupBy {
        val data = it.dataPrazo ?: it.dataExclusao ?: Date(0)
        dfAno.format(data)
      }
      .toSortedMap(compareByDescending { it.toInt() })
      .flatMap { (ano, pilaresDoAno) ->
        listOf(ano) + pilaresDoAno
      }
  }

  /**
   * Formata a data para o padrão `dd/MM/yyyy` ou retorna `null` se a data for nula.
   */
  private fun formatarData(data: Date?): String? {
    return data?.let {
      val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
      sdf.format(it)
    }
  }
}
