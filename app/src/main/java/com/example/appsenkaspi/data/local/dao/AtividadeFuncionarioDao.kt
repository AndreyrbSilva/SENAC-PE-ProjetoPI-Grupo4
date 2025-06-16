package com.example.appsenkaspi.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.appsenkaspi.data.local.entity.AtividadeFuncionarioEntity
import com.example.appsenkaspi.data.local.entity.FuncionarioEntity

/**
 * DAO responsável pelas operações na tabela de relacionamento entre Atividades e Funcionários.
 * Permite criar, consultar e remover vínculos entre responsáveis e atividades.
 */
@Dao
interface AtividadeFuncionarioDao {

  /**
   * Insere ou substitui um vínculo entre uma Atividade e um Funcionário.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun inserirAtividadeFuncionario(atividadeFuncionario: AtividadeFuncionarioEntity)

  /**
   * Remove um vínculo específico entre Atividade e Funcionário.
   */
  @Delete
  suspend fun deletarAtividadeFuncionario(atividadeFuncionario: AtividadeFuncionarioEntity)

  /**
   * Retorna os IDs dos funcionários vinculados a uma Atividade.
   *
   * @param atividadeId ID da atividade
   * @return LiveData com IDs dos funcionários
   */
  @Query("SELECT funcionarioId FROM atividades_funcionarios WHERE atividadeId = :atividadeId")
  fun listarFuncionariosPorAtividade(atividadeId: Int): LiveData<List<Int>>

  /**
   * Retorna os IDs das atividades em que um funcionário está vinculado.
   *
   * @param funcionarioId ID do funcionário
   * @return LiveData com IDs das atividades
   */
  @Query("SELECT atividadeId FROM atividades_funcionarios WHERE funcionarioId = :funcionarioId")
  fun listarAtividadesPorFuncionario(funcionarioId: Int): LiveData<List<Int>>

  /**
   * Remove todos os vínculos de uma atividade com seus responsáveis.
   *
   * @param atividadeId ID da atividade
   */
  @Query("DELETE FROM atividades_funcionarios WHERE atividadeId = :atividadeId")
  suspend fun deletarPorAtividade(atividadeId: Int)

  /**
   * Recupera os dados dos funcionários vinculados a uma atividade específica.
   *
   * @param atividadeId ID da atividade
   * @return Lista de objetos FuncionarioEntity
   */
  @Query("""
        SELECT f.* FROM funcionarios f
        INNER JOIN atividades_funcionarios af ON af.funcionarioId = f.id
        WHERE af.atividadeId = :atividadeId
    """)
  suspend fun getResponsaveisByAtividadeId(atividadeId: Int): List<FuncionarioEntity>

  /**
   * Alias do método acima: recupera os responsáveis de uma atividade.
   *
   * @param atividadeId ID da atividade
   * @return Lista de objetos FuncionarioEntity
   */
  @Query("""
        SELECT f.* FROM funcionarios f
        INNER JOIN atividades_funcionarios af ON af.funcionarioId = f.id
        WHERE af.atividadeId = :atividadeId
    """)
  suspend fun getResponsaveisDaAtividade(atividadeId: Int): List<FuncionarioEntity>
}
