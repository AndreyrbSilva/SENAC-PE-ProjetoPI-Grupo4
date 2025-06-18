package com.example.appsenkaspi.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.appsenkaspi.data.local.entity.SubpilarEntity

/**
 * DAO responsável pelas operações relacionadas à entidade Subpilar.
 * Permite inserção, atualização, deleção e diversas formas de consulta por ID ou por Pilar.
 */
@Dao
interface SubpilarDao {

  /**
   * Insere um subpilar no banco de dados, substituindo em caso de conflito.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun inserirSubpilar(subpilar: SubpilarEntity)

  /**
   * Lista os subpilares associados a um determinado pilar como LiveData (reativo).
   */
  @Query("SELECT * FROM subpilares WHERE pilarId = :pilarId")
  fun listarSubpilaresPorPilar(pilarId: Int): LiveData<List<SubpilarEntity>>

  /**
   * Lista os subpilares associados a um determinado pilar para uso direto em coroutines.
   */
  @Query("SELECT * FROM subpilares WHERE pilarId = :pilarId")
  suspend fun listarSubpilaresPorTelaPilar(pilarId: Int): List<SubpilarEntity>

  /**
   * Busca um subpilar pelo seu ID.
   */
  @Query("SELECT * FROM subpilares WHERE id = :id")
  suspend fun buscarSubpilarPorId(id: Int): SubpilarEntity?

  /**
   * Atualiza os dados de um subpilar existente.
   */
  @Update
  suspend fun atualizarSubpilar(subpilar: SubpilarEntity)

  /**
   * Remove um subpilar do banco.
   */
  @Delete
  suspend fun deletarSubpilar(subpilar: SubpilarEntity)

  /**
   * Retorna o nome de um subpilar com base no ID fornecido.
   */
  @Query("SELECT nome FROM subpilares WHERE id = :subpilarId LIMIT 1")
  suspend fun buscarNomeSubpilarPorId(subpilarId: Int): String?

  /**
   * Recupera um subpilar por ID como LiveData.
   */
  @Query("SELECT * FROM subpilares WHERE id = :id LIMIT 1")
  fun getSubpilarById(id: Int): LiveData<SubpilarEntity>

  /**
   * Insere um subpilar e retorna o ID gerado.
   */
  @Insert
  suspend fun inserirRetornandoId(subpilar: SubpilarEntity): Long

  /**
   * Retorna a quantidade de subpilares associados a um determinado pilar.
   */
  @Query("SELECT COUNT(*) FROM subpilares WHERE pilarId = :pilarId")
  suspend fun contarSubpilaresDoPilar(pilarId: Int): Int

  /**
   * Retorna a quantidade de subpilares de um pilar (alternativo).
   */
  @Query("SELECT COUNT(*) FROM subpilares WHERE pilarId = :pilarId")
  suspend fun getQuantidadePorPilar(pilarId: Int): Int

  /**
   * Verifica se existe pelo menos um subpilar associado ao pilar informado.
   */
  @Query("SELECT EXISTS (SELECT 1 FROM subpilares WHERE pilarId = :pilarId)")
  suspend fun existeSubpilarParaPilar(pilarId: Int): Boolean

  /**
   * Lista todos os subpilares associados ao pilar fornecido.
   */
  @Query("SELECT * FROM subpilares WHERE pilarId = :pilarId")
  suspend fun listarPorPilar(pilarId: Int): List<SubpilarEntity>

  /**
   * Versão duplicada de listagem direta por pilar (possível redundância).
   */
  @Query("SELECT * FROM subpilares WHERE pilarId = :pilarId")
  suspend fun listarPorPilarDireto(pilarId: Int): List<SubpilarEntity>

  /**
   * Recupera um subpilar diretamente pelo ID.
   */
  @Query("SELECT * FROM subpilares WHERE id = :id LIMIT 1")
  suspend fun getSubpilarPorId(id: Int): SubpilarEntity?
}

