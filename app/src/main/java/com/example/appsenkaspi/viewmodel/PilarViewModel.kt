package com.example.appsenkaspi.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.appsenkaspi.ui.pilar.PilarNomeDTO
import com.example.appsenkaspi.ui.subpilares.ProgressoSubpilar
import com.example.appsenkaspi.ui.dashboard.ResumoDashboard
import com.example.appsenkaspi.data.local.enums.StatusAtividade
import com.example.appsenkaspi.data.local.enums.StatusPilar
import com.example.appsenkaspi.data.local.database.AppDatabase
import com.example.appsenkaspi.data.local.entity.PilarEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Calendar
import java.util.Date

/**
 * ViewModel responsável pela lógica de controle dos pilares no sistema.
 *
 * Gerencia ações de criação, atualização, exclusão lógica e conclusão de pilares,
 * cálculo de progresso (direto e por subpilares), atualização automática de status,
 * e geração de resumos para dashboards.
 *
 * Utiliza DAOs do Room, corrotinas e LiveData para manter reatividade e consistência nos dados.
 */
class PilarViewModel(application: Application) : AndroidViewModel(application) {

  private val pilarDao = AppDatabase.getDatabase(application).pilarDao()
  private val acaoDao = AppDatabase.getDatabase(application).acaoDao()
  private val subpilarDao = AppDatabase.getDatabase(application).subpilarDao()

  val statusAtivos = listOf(StatusPilar.EM_ANDAMENTO, StatusPilar.PLANEJADO)
  val statusHistorico = listOf(StatusPilar.CONCLUIDO, StatusPilar.VENCIDO, StatusPilar.EXCLUIDO)
  val statusParaDashboard = listOf(StatusPilar.PLANEJADO, StatusPilar.EM_ANDAMENTO, StatusPilar.CONCLUIDO)

  /** Retorna um pilar pelo ID (modo reativo). */
  fun getPilarById(id: Int): LiveData<PilarEntity?> = pilarDao.getPilarById(id)

  /** Lista todos os pilares (modo reativo). */
  fun listarTodosPilares(): LiveData<List<PilarEntity>> = pilarDao.listarTodosPilares()

  /** Insere um novo pilar no banco. */
  fun inserir(pilar: PilarEntity) = viewModelScope.launch {
    pilarDao.inserirPilar(pilar)
  }

  /** Atualiza os dados de um pilar. */
  fun atualizar(pilar: PilarEntity) = viewModelScope.launch {
    pilarDao.atualizarPilar(pilar)
  }

  /** Remove um pilar definitivamente do banco. */
  fun deletar(pilar: PilarEntity) = viewModelScope.launch {
    pilarDao.deletarPilar(pilar)
  }

  /**
   * Marca um pilar como excluído, alterando seu status e data de exclusão.
   */
  fun excluirPilar(pilar: PilarEntity) = viewModelScope.launch(Dispatchers.IO) {
    val hoje = Calendar.getInstance().time
    val linhasAfetadas = pilarDao.excluirPilarPorId(pilar.id, StatusPilar.EXCLUIDO, hoje)
    Log.d("ExcluirPilar", "Linhas afetadas: $linhasAfetadas")
  }

  /** Insere um pilar e retorna o ID gerado. */
  suspend fun inserirRetornandoId(pilar: PilarEntity): Long = pilarDao.inserirPilar(pilar)

  /**
   * Calcula o progresso de um pilar (ações ou subpilares) e envia via callback.
   */
  fun calcularProgressoDoPilar(pilarId: Int, callback: (Float) -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
      val progresso = calcularProgressoInterno(pilarId)
      withContext(Dispatchers.Main) { callback(progresso) }
    }
  }

  /** Verifica se o pilar possui subpilares (modo suspend). */
  suspend fun temSubpilares(pilarId: Int): Boolean =
    subpilarDao.contarSubpilaresDoPilar(pilarId) > 0

  /** Alternativa direta para verificar existência de subpilares. */
  suspend fun temSubpilaresDireto(pilarId: Int): Boolean =
    subpilarDao.getQuantidadePorPilar(pilarId) > 0

  /**
   * Calcula o progresso interno de um pilar:
   * - Se houver subpilares, calcula média do progresso deles.
   * - Se não, usa as ações diretamente.
   */
  suspend fun calcularProgressoInterno(pilarId: Int): Float = coroutineScope {
    val subpilares = subpilarDao.listarSubpilaresPorTelaPilar(pilarId)

    if (subpilares.isNotEmpty()) {
      val subProgressoList = subpilares.map { sub ->
        async { ProgressoSubpilar.calcularProgressoDoSubpilarInterno(sub.id, acaoDao) }
      }.awaitAll()
      subProgressoList.average().toFloat()
    } else {
      val lista = acaoDao.listarProgressoPorPilar(pilarId)
      val (somaPesos, somaTotal) = lista.fold(0f to 0) { acc, item ->
        val peso = item.progresso * item.totalAtividades
        (acc.first + peso) to (acc.second + item.totalAtividades)
      }
      if (somaTotal > 0) somaPesos / somaTotal else 0f
    }
  }

  /**
   * Atualiza automaticamente o status de um pilar com base em seu progresso e prazo.
   */
  suspend fun atualizarStatusAutomaticamente(pilarId: Int) {
    val pilar = pilarDao.getById(pilarId) ?: return
    val progresso = calcularProgressoInterno(pilarId)
    val hoje = Calendar.getInstance().time

    if (pilar.status == StatusPilar.EXCLUIDO) return
    if (pilar.status == StatusPilar.CONCLUIDO && progresso >= 1f) return

    val passouPrazo = hoje.after(pilar.dataPrazo)

    val novoStatus = when {
      pilar.status == StatusPilar.VENCIDO && !passouPrazo -> StatusPilar.EM_ANDAMENTO
      progresso >= 1f && passouPrazo -> StatusPilar.CONCLUIDO
      progresso < 1f && passouPrazo -> StatusPilar.VENCIDO
      progresso < 1f && pilar.status == StatusPilar.CONCLUIDO -> StatusPilar.EM_ANDAMENTO
      progresso == 0f -> StatusPilar.PLANEJADO
      else -> StatusPilar.EM_ANDAMENTO
    }

    if (novoStatus != pilar.status) {
      val atualizado = if (novoStatus == StatusPilar.CONCLUIDO) {
        pilar.copy(status = novoStatus, dataConclusao = hoje)
      } else {
        pilar.copy(status = novoStatus)
      }
      pilarDao.atualizarPilar(atualizado)
    }
  }

  /** Retorna a data de prazo de um pilar pelo ID. */
  suspend fun getDataPrazoDoPilar(id: Int): Date? = pilarDao.getById(id).dataPrazo

  /** Atualiza o status de todos os pilares no banco. */
  fun atualizarStatusDeTodosOsPilares() = viewModelScope.launch(Dispatchers.IO) {
    pilarDao.getTodosPilares().forEach { atualizarStatusAutomaticamente(it.id) }
  }

  /**
   * Verifica se o pilar pode ser concluído com base no progresso e na data de vencimento.
   */
  suspend fun podeConcluirPilar(pilarId: Int, dataVencimento: LocalDate): Boolean {
    val progresso = calcularProgressoInterno(pilarId)
    val hoje = LocalDate.now()
    return progresso >= 1f && (hoje <= dataVencimento)
  }

  /** Marca um pilar como concluído com data atual. */
  fun concluirPilar(pilarId: Int) = viewModelScope.launch(Dispatchers.IO) {
    val hoje = Calendar.getInstance().time
    pilarDao.atualizarStatusEDataConclusao(pilarId, StatusPilar.CONCLUIDO, hoje)
  }

  /** Lista os pilares ativos (em andamento ou planejados). */
  fun listarPilaresAtivos() = pilarDao.listarPilaresPorStatus(statusAtivos)

  /** Lista os pilares concluídos, vencidos ou excluídos. */
  fun listarPilaresHistorico() = pilarDao.listarPilaresPorStatus(statusHistorico)

  /** Lista IDs e nomes de pilares para dashboards. */
  fun listarIdsENomes(): LiveData<List<PilarNomeDTO>> =
    pilarDao.listarIdsENomesPorStatus(statusParaDashboard)

  fun listarAcaoIdsENomes(): LiveData<List<PilarNomeDTO>> =
    pilarDao.listarIdsENomesPorStatus(statusParaDashboard)

  /** Retorna pilares com status relevantes para dashboard. */
  suspend fun getPilaresParaDashboard(): List<PilarEntity> =
    pilarDao.getPilaresPorStatus(statusParaDashboard)

  /** Retorna todos os pilares no banco (modo suspend). */
  suspend fun getTodosPilares(): List<PilarEntity> = withContext(Dispatchers.IO) {
    pilarDao.getTodosPilares()
  }

  /** Lista pilares por status e opcionalmente por data de exclusão. */
  fun listarPilaresPorStatusEData(status: StatusPilar, dataExclusao: String? = null): LiveData<List<PilarEntity>> {
    return if (dataExclusao.isNullOrEmpty()) {
      pilarDao.listarPilaresPorStatus(status)
    } else {
      pilarDao.listarPilaresPorStatusEData(status, dataExclusao)
    }
  }

  /**
   * Calcula o progresso individual de cada subpilar (nome e percentual).
   */
  suspend fun calcularProgressoDosSubpilares(pilarId: Int): List<Pair<String, Float>> {
    val subpilares = subpilarDao.listarPorPilar(pilarId)
    val acaoDao = AppDatabase.getDatabase(getApplication()).acaoDao()
    val atividadeDao = AppDatabase.getDatabase(getApplication()).atividadeDao()

    return subpilares.map { sub ->
      val acoes = acaoDao.listarPorSubpilares(sub.id)
      val atividades = acoes.flatMap { it.id?.let { id -> atividadeDao.listarPorAcao(id) } ?: emptyList() }
      val total = atividades.size
      val concluidas = atividades.count { it.status == StatusAtividade.CONCLUIDA }
      val progresso = if (total > 0) concluidas.toFloat() / total else 0f
      sub.nome to progresso
    }
  }

  /**
   * Gera um resumo completo (ações + atividades) com callback para exibição no dashboard.
   */
  fun gerarResumoPorSubpilares(pilarId: Int, callback: (ResumoDashboard) -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
      val resumo = gerarResumoPorSubpilaresDireto(pilarId)
      withContext(Dispatchers.Main) { callback(resumo) }
    }
  }

  /**
   * Gera um resumo completo de atividades a partir dos subpilares (modo direto).
   */
  suspend fun gerarResumoPorSubpilaresDireto(pilarId: Int): ResumoDashboard {
    val subpilares = subpilarDao.listarPorPilarDireto(pilarId)
    val acaoDao = acaoDao
    val atividadeDao = AppDatabase.getDatabase(getApplication()).atividadeDao()

    var totalAcoes = 0
    var totalAtividades = 0
    var atividadesConcluidas = 0
    var atividadesAndamento = 0
    var atividadesAtraso = 0

    for (sub in subpilares) {
      val acoes = acaoDao.listarPorSubpilares(sub.id)
      totalAcoes += acoes.size

      for (acao in acoes) {
        val atividades = acao.id?.let { atividadeDao.listarPorAcao(it) } ?: emptyList()
        totalAtividades += atividades.size
        atividadesConcluidas += atividades.count { it.status == StatusAtividade.CONCLUIDA }
        atividadesAndamento += atividades.count { it.status == StatusAtividade.PENDENTE }
        atividadesAtraso += atividades.count { it.status == StatusAtividade.VENCIDA }
      }
    }

    return ResumoDashboard(
      totalAcoes,
      totalAtividades,
      atividadesConcluidas,
      atividadesAndamento,
      atividadesAtraso
    )
  }
}
