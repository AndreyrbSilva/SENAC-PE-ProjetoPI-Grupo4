package com.example.appsenkaspi.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.appsenkaspi.data.local.entity.PilarEntity
import com.example.appsenkaspi.ui.pilar.PilarNomeDTO
import com.example.appsenkaspi.data.local.enums.StatusPilar
import java.util.Date

/**
 * DAO responsável pelas operações de acesso ao banco relacionadas à entidade Pilar.
 * Inclui inserções, buscas por ID, atualizações, exclusões e filtros por status.
 */
@Dao
interface PilarDao {

  /**
   * Insere um novo Pilar ou atualiza em caso de conflito.
   * @param pilar Pilar a ser inserido
   * @return ID do Pilar inserido
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun inserirPilar(pilar: PilarEntity): Long

  /** Busca um Pilar por ID. */
  @Query("SELECT * FROM pilares WHERE id = :id")
  suspend fun buscarPilarPorId(id: Int): PilarEntity?

  /** Busca um Pilar por ID (duplicado funcionalmente). */
  @Query("SELECT * FROM pilares WHERE id = :id")
  suspend fun getPilarPorId(id: Int): PilarEntity?

  /** Retorna um Pilar por ID como LiveData para observação reativa. */
  @Query("SELECT * FROM pilares WHERE id = :id")
  fun getPilarById(id: Int): LiveData<PilarEntity?>

  /** Atualiza um Pilar existente. */
  @Update
  suspend fun atualizarPilar(pilar: PilarEntity)

  /** Atualiza um Pilar e retorna a quantidade de linhas afetadas (uso em testes). */
  @Update
  suspend fun atualizarPilarTeste(pilar: PilarEntity): Int

  /** Remove um Pilar do banco de dados. */
  @Delete
  suspend fun deletarPilar(pilar: PilarEntity)

  /** Retorna todos os Pilares (forma suspensa). */
  @Query("SELECT * FROM pilares")
  suspend fun getTodosPilares(): List<PilarEntity>

  /** Retorna todos os Pilares como LiveData. */
  @Query("SELECT * FROM pilares")
  fun listarTodosPilares(): LiveData<List<PilarEntity>>

  /** Insere um Pilar e retorna o ID gerado. */
  @Insert
  suspend fun inserirRetornandoId(pilar: PilarEntity): Long

  /** Busca Pilar por ID. */
  @Query("SELECT * FROM pilares WHERE id = :id")
  suspend fun getById(id: Int): PilarEntity

  /** Busca Pilar por ID (duplicado funcionalmente). */
  @Query("SELECT * FROM pilares WHERE id = :id")
  suspend fun buscarPorId(id: Int): PilarEntity?

  /** Retorna apenas o nome de um Pilar a partir do ID. */
  @Query("SELECT nome FROM pilares WHERE id = :id")
  suspend fun getNomePilarPorId(id: Int): String?

  /** Lista Pilares com status dentro de uma lista específica (LiveData). */
  @Query("SELECT * FROM pilares WHERE status IN (:statusList)")
  fun listarPilaresPorStatus(statusList: List<StatusPilar>): LiveData<List<PilarEntity>>

  /** Marca um Pilar como excluído e registra a data. */
  @Query("UPDATE pilares SET status = :status, dataExcluido = :data WHERE id = :id")
  suspend fun excluirPilarPorId(id: Int, status: StatusPilar, data: Date): Int

  /** Lista IDs e nomes de Pilares com determinados status. */
  @Query("SELECT id, nome FROM pilares WHERE status IN (:status)")
  fun listarIdsENomesPorStatus(status: List<StatusPilar>): LiveData<List<PilarNomeDTO>>

  /** Retorna uma lista suspensa de Pilares filtrados por status. */
  @Query("SELECT * FROM pilares WHERE status IN (:statusList)")
  suspend fun getPilaresPorStatus(statusList: List<StatusPilar>): List<PilarEntity>

  /** Atualiza o status e a data de conclusão de um Pilar. */
  @Query("UPDATE pilares SET status = :novoStatus, dataConclusao = :dataConclusao WHERE id = :pilarId")
  suspend fun atualizarStatusEDataConclusao(
    pilarId: Int,
    novoStatus: StatusPilar,
    dataConclusao: Date
  ): Int

  /** Lista Pilares com status específico. */
  @Query("SELECT * FROM pilares WHERE status = :status")
  fun listarPilaresPorStatus(status: StatusPilar): LiveData<List<PilarEntity>>

  /** Lista Pilares filtrando por status e data de exclusão. */
  @Query("SELECT * FROM pilares WHERE status = :status AND dataExclusao = :dataExclusao")
  fun listarPilaresPorStatusEData(status: StatusPilar, dataExclusao: String): LiveData<List<PilarEntity>>
}
