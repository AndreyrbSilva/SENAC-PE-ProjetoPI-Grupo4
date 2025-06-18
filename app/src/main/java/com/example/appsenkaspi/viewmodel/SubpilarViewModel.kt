package com.example.appsenkaspi.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.appsenkaspi.data.local.database.AppDatabase
import com.example.appsenkaspi.data.local.entity.SubpilarEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel responsável por gerenciar operações relacionadas aos Subpilares.
 *
 * Opera com inserção, atualização, exclusão, recuperação e cálculo de progresso
 * tanto individual quanto agregado a partir de ações associadas ao subpilar ou ao pilar pai.
 *
 * Utiliza DAOs do Room e corrotinas para chamadas assíncronas seguras e eficientes.
 */
class SubpilarViewModel(application: Application) : AndroidViewModel(application) {

  private val acaoDao = AppDatabase.getDatabase(application).acaoDao()
  private val subpilarDao = AppDatabase.getDatabase(application).subpilarDao()

  /**
   * Lista os subpilares associados a um determinado pilar (modo reativo).
   *
   * @param pilarId ID do pilar pai.
   * @return LiveData com a lista de subpilares associados.
   */
  fun listarSubpilaresPorPilar(pilarId: Int): LiveData<List<SubpilarEntity>> {
    return subpilarDao.listarSubpilaresPorPilar(pilarId)
  }

  /**
   * Insere um novo subpilar no banco.
   */
  fun inserir(subpilar: SubpilarEntity) = viewModelScope.launch {
    subpilarDao.inserirSubpilar(subpilar)
  }

  /**
   * Atualiza um subpilar existente.
   */
  fun atualizar(subpilar: SubpilarEntity) = viewModelScope.launch {
    subpilarDao.atualizarSubpilar(subpilar)
  }

  /**
   * Deleta um subpilar do banco.
   */
  fun deletar(subpilar: SubpilarEntity) = viewModelScope.launch {
    subpilarDao.deletarSubpilar(subpilar)
  }

  /**
   * Calcula o progresso de um subpilar e entrega via callback.
   *
   * @param pilarId ID do subpilar.
   * @param callback função lambda com resultado do progresso entre 0f e 1f.
   */
  fun calcularProgressoDoSubpilar(pilarId: Int, callback: (Float) -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
      val progresso = calcularProgressoInterno(pilarId)
      withContext(Dispatchers.Main) {
        callback(progresso)
      }
    }
  }

  /**
   * Recupera um subpilar pelo ID (modo reativo).
   */
  fun getSubpilarById(id: Int): LiveData<SubpilarEntity> {
    return subpilarDao.getSubpilarById(id)
  }

  /**
   * Calcula o progresso interno de um subpilar com base nas ações e atividades associadas.
   * O progresso é ponderado pelo número de atividades por ação.
   *
   * @param subpilarId ID do subpilar.
   * @return Progresso como `Float` entre 0.0 e 1.0.
   */
  suspend fun calcularProgressoInterno(subpilarId: Int): Float = withContext(Dispatchers.IO) {
    val lista = acaoDao.listarProgressoPorSubpilar(subpilarId)
    val (somaPesos, somaTotalAtividades) = lista.fold(0f to 0) { acc, item ->
      val pesoAtual = item.progresso * item.totalAtividades
      (acc.first + pesoAtual) to (acc.second + item.totalAtividades)
    }
    if (somaTotalAtividades > 0) (somaPesos / somaTotalAtividades).coerceIn(0f, 1f) else 0f
  }

  /**
   * Calcula o progresso médio de todos os subpilares de um pilar.
   * A média é ponderada com base no número de ações por subpilar.
   *
   * @param pilarId ID do pilar pai.
   * @param callback função com o progresso médio de seus subpilares.
   */
  fun calcularProgressoDoPilarComSubpilares(pilarId: Int, callback: (Float) -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
      val lista = acaoDao.listarProgressoPorSubpilaresDoPilar(pilarId)

      val (somaPesos, somaTotalAcoes) = lista.fold(0f to 0) { acc, item ->
        val pesoAtual = item.progresso * item.totalAcoes
        (acc.first + pesoAtual) to (acc.second + item.totalAcoes)
      }

      val progresso = if (somaTotalAcoes > 0) {
        (somaPesos / somaTotalAcoes).coerceIn(0f, 1f)
      } else {
        0f
      }

      withContext(Dispatchers.Main) {
        callback(progresso)
      }
    }
  }

  /**
   * Insere um subpilar e retorna seu ID gerado automaticamente pelo Room.
   *
   * @param subpilar Entidade a ser inserida.
   * @return ID longo do novo subpilar.
   */
  suspend fun inserirRetornandoId(subpilar: SubpilarEntity): Long {
    return subpilarDao.inserirRetornandoId(subpilar)
  }
}
