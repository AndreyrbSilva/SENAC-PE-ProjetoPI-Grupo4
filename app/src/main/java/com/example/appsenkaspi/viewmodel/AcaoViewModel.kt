package com.example.appsenkaspi.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.appsenkaspi.data.local.database.AppDatabase
import com.example.appsenkaspi.data.local.entity.PilarEntity
import com.example.appsenkaspi.ui.dashboard.ResumoDashboard
import com.example.appsenkaspi.data.local.enums.StatusAcao
import com.example.appsenkaspi.data.local.enums.StatusPilar
import com.example.appsenkaspi.data.local.entity.SubpilarEntity
import com.example.appsenkaspi.data.local.entity.AcaoEntity
import com.example.appsenkaspi.domain.model.AcaoComStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * ViewModel responsável pela lógica de negócios e acesso aos dados da entidade `AcaoEntity`.
 *
 * Realiza operações CRUD, atualizações de status, validações de vínculo com pilares ou subpilares
 * e geração de resumos para dashboards, utilizando DAOs do Room e corrotinas para execução segura
 * fora da thread principal.
 */
class AcaoViewModel(application: Application) : AndroidViewModel(application) {

  private val pilarDao = AppDatabase.getDatabase(application).pilarDao()
  private val dao = AppDatabase.getDatabase(application).acaoDao()
  private val acaoDao = AppDatabase.getDatabase(application).acaoDao()
  private val atividadeDao = AppDatabase.getDatabase(application).atividadeDao()
  private val subpilarDao = AppDatabase.getDatabase(application).subpilarDao()

  /**
   * Recupera uma ação em tempo real a partir de seu ID.
   *
   * @param id ID da ação.
   * @return [LiveData] contendo a entidade `AcaoEntity` ou null.
   */
  fun getAcaoById(id: Int): LiveData<AcaoEntity?> = acaoDao.getAcaoById(id)

  /**
   * Recupera uma ação de forma síncrona (em coroutine) pelo seu ID.
   *
   * @param id ID da ação.
   * @return Instância da ação ou null.
   */
  suspend fun getAcaoByIdNow(id: Int): AcaoEntity? = acaoDao.getAcaoByIdNow(id)

  /**
   * Lista ações associadas a um pilar, com status computado.
   *
   * @param pilarId ID do pilar.
   * @return Lista observável de ações com status.
   */
  fun listarAcoesPorPilar(pilarId: Int): LiveData<List<AcaoComStatus>> =
    acaoDao.listarPorPilar(pilarId)

  /**
   * Lista ações associadas a um subpilar, com status computado.
   *
   * @param subpilarId ID do subpilar.
   * @return Lista observável de ações com status.
   */
  fun listarAcoesPorSubpilar(subpilarId: Int): LiveData<List<AcaoComStatus>> =
    acaoDao.listarPorSubpilar(subpilarId)

  /**
   * Insere uma nova ação no banco de dados.
   *
   * @param acao Ação a ser inserida.
   */
  fun inserir(acao: AcaoEntity) = viewModelScope.launch {
    acaoDao.inserirAcao(acao)
  }

  /**
   * Atualiza os dados de uma ação existente.
   *
   * @param acao Ação com dados atualizados.
   */
  fun atualizar(acao: AcaoEntity) = viewModelScope.launch {
    acaoDao.atualizarAcao(acao)
  }

  /**
   * Remove uma ação do banco.
   *
   * @param acao Ação a ser deletada.
   */
  fun deletar(acao: AcaoEntity) = viewModelScope.launch {
    acaoDao.deletarAcao(acao)
  }

  /**
   * Insere uma ação e retorna o ID gerado automaticamente.
   *
   * @param acao Entidade a ser inserida.
   * @return ID da nova ação.
   */
  suspend fun inserirRetornandoId(acao: AcaoEntity): Int =
    acaoDao.inserirComRetorno(acao).toInt()

  /**
   * Gera estatísticas de ações e atividades para um pilar específico.
   *
   * @param pilarId ID do pilar a ser analisado.
   * @return Instância de [ResumoDashboard] com totais e status.
   */
  suspend fun gerarResumoDashboardDireto(pilarId: Int): ResumoDashboard = withContext(Dispatchers.IO) {
    val acoes = acaoDao.listarAcoesComStatusPorPilarNow(pilarId)
    val totalAcoes = acoes.size
    val totalAtividades = acoes.sumOf { it.totalAtividades }
    val concluidas = acoes.sumOf { it.ativasConcluidas }
    val vencidas = atividadeDao.contarVencidasPorPilar(pilarId)
    val andamento = totalAtividades - concluidas - vencidas

    ResumoDashboard(
      totalAcoes = totalAcoes,
      totalAtividades = totalAtividades,
      atividadesConcluidas = concluidas,
      atividadesAndamento = andamento,
      atividadesAtraso = vencidas
    )
  }

  /**
   * Atualiza o status da ação com base na data de prazo e progresso das atividades.
   *
   * @param acaoId ID da ação a ser verificada e atualizada.
   */
  fun atualizarStatusAcaoAutomaticamente(acaoId: Int) {
    viewModelScope.launch(Dispatchers.IO) {
      val total = atividadeDao.contarTotalPorAcaoValor(acaoId)
      val concluidas = atividadeDao.contarConcluidasPorAcaoValor(acaoId)
      val acao = acaoDao.getAcaoPorIdDireto(acaoId) ?: return@launch
      val hoje = Calendar.getInstance().time

      val novoStatus = when {
        total == 0 -> StatusAcao.PLANEJADA
        concluidas == total && hoje.before(acao.dataPrazo) -> StatusAcao.CONCLUIDA
        hoje.after(acao.dataPrazo) -> StatusAcao.VENCIDA
        else -> StatusAcao.EM_ANDAMENTO
      }

      if (acao.status != novoStatus) {
        acaoDao.atualizarAcao(acao.copy(status = novoStatus))
      }
    }
  }

  /**
   * Cria uma ação vinculada a um único pilar ou subpilar. Garante que não haja ambiguidade.
   *
   * @param acao Ação a ser criada.
   * @throws IllegalArgumentException se for associada a ambos ou nenhum.
   * @return ID da nova ação.
   */
  suspend fun criarAcaoSegura(acao: AcaoEntity): Long {
    val valido = (acao.pilarId != null) xor (acao.subpilarId != null)
    if (!valido) {
      throw IllegalArgumentException("A ação deve estar ligada a um pilar OU subpilar, nunca ambos ou nenhum.")
    }
    return dao.inserirRetornandoId(acao)
  }

  /**
   * Recupera uma ação específica a partir do ID.
   *
   * @param id ID da ação.
   * @return Instância da ação ou null.
   */
  suspend fun buscarAcaoPorId(id: Int): AcaoEntity? = acaoDao.getAcaoPorId(id)

  /**
   * Retorna o nome de um subpilar a partir do seu ID.
   *
   * @param subpilarId ID do subpilar.
   * @return Nome do subpilar ou null.
   */
  suspend fun buscarNomeSubpilarPorId(subpilarId: Int): String? =
    subpilarDao.buscarNomeSubpilarPorId(subpilarId)

  /**
   * Busca os dados de um pilar específico.
   *
   * @param id ID do pilar.
   * @return Pilar correspondente ou null.
   */
  suspend fun buscarPilarPorId(id: Int): PilarEntity? = pilarDao.getPilarPorId(id)

  /**
   * Gera estatísticas de ações e atividades para o dashboard geral ou por pilar.
   *
   * @param pilarId ID do pilar (ou null para todos).
   * @param callback Função que recebe o [ResumoDashboard] gerado.
   */
  fun gerarResumoDashboard(pilarId: Int?, callback: (ResumoDashboard) -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
      val acoes = if (pilarId == null) {
        val statusValidos = listOf(StatusPilar.PLANEJADO, StatusPilar.EM_ANDAMENTO, StatusPilar.CONCLUIDO)
        val pilaresValidos = pilarDao.getPilaresPorStatus(statusValidos)
        val idsValidos = pilaresValidos.map { it.id }
        acaoDao.listarAcoesComStatusPorPilares(idsValidos)
      } else {
        acaoDao.listarAcoesComStatusPorPilarNow(pilarId)
      }

      val totalAcoes = acoes.size
      val totalAtividades = acoes.sumOf { it.totalAtividades }
      val concluidas = acoes.sumOf { it.ativasConcluidas }
      val vencidas = if (pilarId == null) {
        atividadeDao.contarVencidasGeral()
      } else {
        atividadeDao.contarVencidasPorPilar(pilarId)
      }
      val andamento = totalAtividades - concluidas - vencidas

      withContext(Dispatchers.Main) {
        callback(
          ResumoDashboard(
            totalAcoes = totalAcoes,
            totalAtividades = totalAtividades,
            atividadesConcluidas = concluidas,
            atividadesAndamento = andamento,
            atividadesAtraso = vencidas
          )
        )
      }
    }
  }

  /**
   * Recupera os dados completos de um subpilar.
   *
   * @param id ID do subpilar.
   * @return Subpilar correspondente ou null.
   */
  suspend fun buscarSubpilarPorId(id: Int): SubpilarEntity? =
    subpilarDao.getSubpilarPorId(id)
}
