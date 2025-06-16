package com.example.appsenkaspi.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.appsenkaspi.data.local.entity.AtividadeFuncionarioEntity
import com.example.appsenkaspi.data.local.entity.AtividadeEntity
import com.example.appsenkaspi.data.local.enums.StatusAtividade
import com.example.appsenkaspi.domain.model.AtividadeComFuncionarios

/**
 * DAO (Data Access Object) responsável pelas operações relacionadas à entidade [AtividadeEntity].
 *
 * Gerencia inserções, atualizações, exclusões, contagens e relações entre atividades
 * e funcionários, além de queries específicas para geração de relatórios e status.
 */
@Dao
interface AtividadeDao {

  /**
   * Insere uma atividade no banco. Substitui caso já exista (pelo ID).
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun inserirAtividade(atividade: AtividadeEntity)

  /**
   * Atualiza os dados de uma atividade existente.
   */
  @Update
  suspend fun atualizarAtividade(atividade: AtividadeEntity)

  /**
   * Remove uma atividade do banco.
   */
  @Delete
  suspend fun deletarAtividade(atividade: AtividadeEntity)

  /**
   * Retorna uma lista observável de atividades com funcionários para uma ação.
   */
  @Transaction
  @Query("SELECT * FROM atividades WHERE acaoId = :acaoId")
  fun listarAtividadesComFuncionariosPorAcao(acaoId: Int): LiveData<List<AtividadeComFuncionarios>>

  /**
   * Deleta uma atividade diretamente pelo ID.
   */
  @Query("DELETE FROM atividades WHERE id = :id")
  suspend fun deletarPorId(id: Int)

  /**
   * Insere uma atividade e retorna o ID gerado.
   */
  @Insert
  suspend fun inserirComRetorno(atividade: AtividadeEntity): Long

  /**
   * Retorna uma [LiveData] com a atividade pelo ID.
   */
  @Query("SELECT * FROM atividades WHERE id = :atividadeId")
  fun getAtividadeById(atividadeId: Int): LiveData<AtividadeEntity>

  /**
   * Retorna uma atividade com funcionários por ID.
   */
  @Transaction
  @Query("SELECT * FROM atividades WHERE id = :atividadeId")
  fun buscarAtividadeComFuncionarios(atividadeId: Int): LiveData<AtividadeComFuncionarios>

  /**
   * Retorna todas as atividades de uma ação.
   */
  @Query("SELECT * FROM atividades WHERE acaoId = :acaoId")
  fun listarAtividadesPorAcao(acaoId: Int): LiveData<List<AtividadeEntity>>

  /**
   * Retorna uma atividade com seus responsáveis por ID.
   */
  @Transaction
  @Query("SELECT * FROM atividades WHERE id = :id")
  fun getAtividadeComFuncionariosPorId(id: Int): LiveData<AtividadeComFuncionarios>

  /**
   * Conta todas as atividades de uma ação (modo LiveData).
   */
  @Query("SELECT COUNT(*) FROM atividades WHERE acaoId = :acaoId")
  fun contarTotalPorAcao(acaoId: Int): LiveData<Int>

  /**
   * Conta atividades concluídas de uma ação (modo LiveData).
   */
  @Query("SELECT COUNT(*) FROM atividades WHERE acaoId = :acaoId AND status = 'concluida'")
  fun contarConcluidasPorAcao(acaoId: Int): LiveData<Int>

  /**
   * Conta total de atividades por ação (modo direto).
   */
  @Query("SELECT COUNT(*) FROM atividades WHERE acaoId = :acaoId")
  suspend fun contarTotalPorAcaoValor(acaoId: Int): Int

  /**
   * Conta atividades concluídas por ação (modo direto).
   */
  @Query("SELECT COUNT(*) FROM atividades WHERE acaoId = :acaoId AND status = 'concluida'")
  suspend fun contarConcluidasPorAcaoValor(acaoId: Int): Int

  /**
   * Lista atividades vinculadas a um funcionário por join.
   */
  @Query("""
        SELECT a.* FROM atividades a
        INNER JOIN atividades_funcionarios af ON af.atividadeId = a.id
        WHERE af.funcionarioId = :funcionarioId
    """)
  fun listarAtividadesPorFuncionario(funcionarioId: Int): LiveData<List<AtividadeEntity>>

  /**
   * Lista atividades atribuídas a um funcionário via subquery.
   */
  @Query("""
        SELECT * FROM atividades
        WHERE id IN (
            SELECT atividadeId FROM atividades_funcionarios
            WHERE funcionarioId = :funcionarioId
        )
    """)
  fun listarAtividadesDoFuncionario(funcionarioId: Int): LiveData<List<AtividadeEntity>>

  /**
   * Lista atividades com responsáveis associadas a um funcionário.
   */
  @Transaction
  @Query("""
        SELECT * FROM atividades
        WHERE id IN (
            SELECT atividadeId FROM atividades_funcionarios
            WHERE funcionarioId = :funcionarioId
        )
    """)
  fun listarAtividadesComResponsaveis(funcionarioId: Int): LiveData<List<AtividadeComFuncionarios>>

  /**
   * Busca uma atividade pelo ID (modo direto, não reativo).
   */
  @Query("SELECT * FROM atividades WHERE id = :id")
  fun buscarPorId(id: Int): AtividadeEntity?

  /**
   * Obtém uma atividade pelo ID (modo suspend).
   */
  @Query("SELECT * FROM atividades WHERE id = :id")
  suspend fun getById(id: Int): AtividadeEntity

  /**
   * Retorna uma atividade com funcionários (modo direto com corrotina).
   */
  @Transaction
  @Query("SELECT * FROM atividades WHERE id = :atividadeId")
  suspend fun buscarComFuncionarios(atividadeId: Int): AtividadeComFuncionarios

  /**
   * Insere uma atividade substituindo em caso de conflito.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(atividade: AtividadeEntity)

  @Update
  suspend fun update(atividade: AtividadeEntity)

  @Delete
  suspend fun delete(atividade: AtividadeEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertComRetorno(atividade: AtividadeEntity): Long

  /**
   * Insere relação entre atividade e funcionário.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun inserirRelacaoFuncionario(relacao: AtividadeFuncionarioEntity)

  /**
   * Obtém lista de atividades com prazo para um funcionário.
   */
  @Query("""
        SELECT a.* FROM atividades a
        INNER JOIN atividades_funcionarios af ON a.id = af.atividadeId
        WHERE af.funcionarioId = :funcionarioId
    """)
  suspend fun getAtividadesComPrazoPorFuncionario(funcionarioId: Int): List<AtividadeEntity>

  /**
   * Obtém todas as atividades que possuem data de prazo preenchida.
   */
  @Query("SELECT * FROM atividades WHERE dataPrazo IS NOT NULL")
  suspend fun getTodasAtividadesComDataPrazo(): List<AtividadeEntity>

  /**
   * Deleta todos os vínculos entre uma atividade e seus funcionários.
   */
  @Query("DELETE FROM atividades_funcionarios WHERE atividadeId = :atividadeId")
  suspend fun deletarRelacoesPorAtividade(atividadeId: Int)

  /**
   * Busca uma atividade diretamente por ID (modo suspend).
   */
  @Query("SELECT * FROM atividades WHERE id = :id")
  suspend fun getAtividadePorIdDireto(id: Int): AtividadeEntity

  /**
   * Lista atividades associadas a uma ação (modo direto).
   */
  @Query("SELECT * FROM atividades WHERE acaoId = :acaoId")
  suspend fun getAtividadesPorAcaoDireto(acaoId: Int): List<AtividadeEntity>

  /**
   * Conta quantas atividades estão com status 'vencida' no geral.
   */
  @Query("SELECT COUNT(*) FROM atividades WHERE status = 'vencida'")
  suspend fun contarVencidasGeral(): Int

  /**
   * Conta quantas atividades vencidas estão ligadas a ações de um pilar.
   */
  @Query("""
        SELECT COUNT(*)
        FROM atividades AS at
        INNER JOIN acoes AS ac ON ac.id = at.acaoId
        WHERE at.status = 'vencida' AND ac.pilarId = :pilarId
    """)
  suspend fun contarVencidasPorPilar(pilarId: Int): Int

  /**
   * Lista atividades diretamente por ID da ação (modo suspend).
   */
  @Query("SELECT * FROM atividades WHERE acaoId = :acaoId")
  suspend fun listarPorAcao(acaoId: Int): List<AtividadeEntity>

  /**
   * Retorna todas as atividades com status específico.
   */
  @Query("SELECT * FROM atividades WHERE status = :status")
  suspend fun getAtividadesPorStatus(status: StatusAtividade): List<AtividadeEntity>
}
