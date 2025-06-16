package com.example.appsenkaspi.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.appsenkaspi.domain.model.AcaoComFuncionarios
import com.example.appsenkaspi.domain.model.AcaoComStatus
import com.example.appsenkaspi.ui.acao.ProgressoAcao
import com.example.appsenkaspi.ui.subpilares.ProgressoSubpilarDTO
import com.example.appsenkaspi.data.local.entity.AcaoEntity

/**
 * DAO responsável pelo gerenciamento de dados da entidade [AcaoEntity],
 * incluindo operações de CRUD, relações com Funcionários e Atividades,
 * e cálculos de progresso para uso em dashboards e relatórios.
 */
@Dao
interface AcaoDao {

  // --------------------- INSERÇÃO ---------------------

  /**
   * Insere uma ação sem retorno.
   * Conflitos são tratados conforme o padrão da anotação (default: ABORT).
   */
  @Insert
  suspend fun inserirAcao(acao: AcaoEntity)

  /**
   * Insere uma ação e retorna o ID gerado.
   */
  @Insert
  suspend fun inserirComRetorno(acao: AcaoEntity): Long

  /**
   * Insere uma ação e retorna o ID gerado (nome alternativo).
   */
  @Insert
  suspend fun inserirRetornandoId(acao: AcaoEntity): Long

  /**
   * Insere uma ação com estratégia REPLACE em caso de conflito.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(acao: AcaoEntity)

  // --------------------- ATUALIZAÇÃO ---------------------

  /**
   * Atualiza uma ação existente.
   */
  @Update
  suspend fun atualizarAcao(acao: AcaoEntity)

  /**
   * Atualiza uma ação existente (nome alternativo).
   */
  @Update
  suspend fun update(acao: AcaoEntity)

  // --------------------- REMOÇÃO ---------------------

  /**
   * Remove uma ação do banco.
   */
  @Delete
  suspend fun deletarAcao(acao: AcaoEntity)

  /**
   * Remove uma ação do banco (nome alternativo).
   */
  @Delete
  suspend fun delete(acao: AcaoEntity)

  // --------------------- CONSULTAS POR ID ---------------------

  @Query("SELECT * FROM acoes WHERE id = :id")
  suspend fun buscarAcaoPorId(id: Int): AcaoEntity?

  @Query("SELECT * FROM acoes WHERE id = :id")
  fun buscarPorId(id: Int): AcaoEntity?

  @Query("SELECT * FROM acoes WHERE id = :id")
  fun getAcaoById(id: Int): LiveData<AcaoEntity?>

  @Query("SELECT * FROM acoes WHERE id = :id")
  suspend fun getAcaoByIdNow(id: Int): AcaoEntity?

  @Query("SELECT * FROM acoes WHERE id = :id")
  suspend fun getById(id: Int): AcaoEntity

  @Query("SELECT * FROM acoes WHERE id = :acaoId LIMIT 1")
  suspend fun getAcaoPorIdDireto(acaoId: Int): AcaoEntity?

  @Query("SELECT * FROM acoes WHERE id = :acaoId")
  suspend fun getAcaoPorId(acaoId: Int): AcaoEntity?

  // --------------------- CONSULTAS COM RELACIONAMENTO ---------------------

  /**
   * Retorna uma Ação com todos os Funcionários relacionados.
   */
  @Transaction
  @Query("SELECT * FROM acoes WHERE id = :acaoId")
  suspend fun buscarComFuncionarios(acaoId: Int): AcaoComFuncionarios

  // --------------------- LISTAGEM POR PILAR ---------------------

  @Query("SELECT * FROM acoes WHERE pilarId = :pilarId")
  fun listarAcoesPorPilar(pilarId: Int): LiveData<List<AcaoEntity>>

  @Query("SELECT * FROM acoes WHERE pilarId = :pilarId")
  suspend fun getAcoesPorPilarDireto(pilarId: Int): List<AcaoEntity>

  @Query("SELECT * FROM acoes")
  fun listarTodas(): LiveData<List<AcaoEntity>>

  /**
   * Lista ações com contagem de atividades e número de concluídas.
   */
  @Query("""
        SELECT a.*, COUNT(at.id) AS totalAtividades,
               SUM(CASE WHEN at.status = 'concluida' THEN 1 ELSE 0 END) AS ativasConcluidas
        FROM acoes a
        LEFT JOIN atividades at ON at.acaoId = a.id
        WHERE a.pilarId = :pilarId
        GROUP BY a.id
    """)
  fun listarAcoesComStatusPorPilar(pilarId: Int): LiveData<List<AcaoComStatus>>

  @Query("""
        SELECT a.*, COUNT(at.id) AS totalAtividades,
               SUM(CASE WHEN at.status = 'concluida' THEN 1 ELSE 0 END) AS ativasConcluidas
        FROM acoes a
        LEFT JOIN atividades at ON at.acaoId = a.id
        WHERE a.pilarId = :pilarId
        GROUP BY a.id
    """)
  suspend fun listarAcoesComStatusPorPilarNow(pilarId: Int): List<AcaoComStatus>

  @Query("""
        SELECT a.id, a.nome, a.descricao, a.dataInicio, a.dataPrazo,
               a.pilarId, a.status, a.criado_por, a.data_criacao,
               COUNT(at.id) AS totalAtividades,
               SUM(CASE WHEN at.status = 'concluida' THEN 1 ELSE 0 END) AS ativasConcluidas
        FROM acoes a
        LEFT JOIN atividades at ON at.acaoId = a.id
        WHERE a.pilarId = :pilarId
        GROUP BY a.id
    """)
  fun listarPorPilar(pilarId: Int): LiveData<List<AcaoComStatus>>

  // --------------------- LISTAGEM POR SUBPILAR ---------------------

  @Query("SELECT * FROM acoes WHERE subpilarId = :subpilarId")
  fun getAcoesPorSubpilar(subpilarId: Int): LiveData<List<AcaoEntity>>

  @Query("SELECT * FROM acoes WHERE subpilarId = :subpilarId")
  suspend fun listarPorSubpilares(subpilarId: Int): List<AcaoEntity>

  @Query("""
        SELECT a.*, COUNT(at.id) AS totalAtividades,
               SUM(CASE WHEN at.status = 'concluida' THEN 1 ELSE 0 END) AS ativasConcluidas
        FROM acoes a
        LEFT JOIN atividades at ON at.acaoId = a.id
        WHERE a.subpilarId = :subpilarId
        GROUP BY a.id
    """)
  fun listarPorSubpilar(subpilarId: Int): LiveData<List<AcaoComStatus>>

  // --------------------- PROGRESSO ---------------------

  @Query("""
        SELECT a.id AS acaoId, a.nome AS nome,
               COALESCE(SUM(CASE WHEN at.status = 'concluida' THEN 1 ELSE 0 END) * 1.0 / COUNT(at.id), 0) AS progresso,
               COUNT(at.id) AS totalAtividades
        FROM acoes a
        LEFT JOIN atividades at ON at.acaoId = a.id
        WHERE a.pilarId = :pilarId
        GROUP BY a.id
    """)
  suspend fun listarProgressoPorPilar(pilarId: Int): List<ProgressoAcao>

  @Query("""
        SELECT a.id AS acaoId, a.nome AS nome,
               COUNT(at.id) AS totalAtividades,
               COALESCE(SUM(CASE WHEN at.status = 'concluida' THEN 1 ELSE 0 END) * 1.0 / COUNT(at.id), 0) AS progresso
        FROM acoes a
        LEFT JOIN atividades at ON at.acaoId = a.id
        WHERE a.subpilarId = :subpilarId
        GROUP BY a.id
    """)
  suspend fun listarProgressoPorSubpilar(subpilarId: Int): List<ProgressoAcao>

  @Query("""
        SELECT subpilarId AS subpilarId,
               COUNT(id) AS totalAcoes,
               CASE
                 WHEN COUNT(id) = 0 THEN 0.0
                 ELSE CAST(SUM(CASE WHEN status = 'concluida' THEN 1 ELSE 0 END) AS FLOAT) / COUNT(id)
               END AS progresso
        FROM acoes
        WHERE pilarId = :pilarId AND subpilarId IS NOT NULL
        GROUP BY subpilarId
    """)
  suspend fun listarProgressoPorSubpilaresDoPilar(pilarId: Int): List<ProgressoSubpilarDTO>

  // --------------------- TUDO COM STATUS ---------------------

  @Query("""
        SELECT a.*, COUNT(at.id) AS totalAtividades,
               SUM(CASE WHEN at.status = 'concluida' THEN 1 ELSE 0 END) AS ativasConcluidas
        FROM acoes a
        LEFT JOIN atividades at ON at.acaoId = a.id
        GROUP BY a.id
    """)
  fun listarTodasAcoesComStatus(): LiveData<List<AcaoComStatus>>

  @Query("""
        SELECT a.*, COUNT(at.id) AS totalAtividades,
               SUM(CASE WHEN at.status = 'concluida' THEN 1 ELSE 0 END) AS ativasConcluidas
        FROM acoes a
        LEFT JOIN atividades at ON at.acaoId = a.id
        GROUP BY a.id
    """)
  suspend fun listarTodasAcoesComStatusNow(): List<AcaoComStatus>

  @Query("""
        SELECT a.*, COUNT(at.id) AS totalAtividades,
               SUM(CASE WHEN at.status = 'concluida' THEN 1 ELSE 0 END) AS ativasConcluidas
        FROM acoes a
        LEFT JOIN atividades at ON at.acaoId = a.id
        WHERE a.pilarId IN (:pilarIds)
        GROUP BY a.id
    """)
  suspend fun listarAcoesComStatusPorPilares(pilarIds: List<Int>): List<AcaoComStatus>
}
