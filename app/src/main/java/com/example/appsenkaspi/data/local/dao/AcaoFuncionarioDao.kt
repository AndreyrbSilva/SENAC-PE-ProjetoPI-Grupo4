package com.example.appsenkaspi.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.appsenkaspi.data.local.entity.AcaoFuncionarioEntity
import com.example.appsenkaspi.data.local.entity.FuncionarioEntity

@Dao
interface AcaoFuncionarioDao {

  /**
   * Insere ou substitui um vínculo entre uma Ação e um Funcionário.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun inserirAcaoFuncionario(acaoFuncionario: AcaoFuncionarioEntity)

  /**
   * Remove um vínculo entre Ação e Funcionário.
   */
  @Delete
  suspend fun deletarAcaoFuncionario(acaoFuncionario: AcaoFuncionarioEntity)

  /**
   * Lista os IDs dos funcionários vinculados a uma determinada Ação.
   *
   * @param acaoId ID da ação
   * @return LiveData com lista de IDs dos funcionários
   */
  @Query("SELECT funcionarioId FROM acoes_funcionarios WHERE acaoId = :acaoId")
  fun listarFuncionariosPorAcao(acaoId: Int): LiveData<List<Int>>

  /**
   * Lista os IDs das ações às quais um funcionário está vinculado.
   *
   * @param funcionarioId ID do funcionário
   * @return LiveData com lista de IDs das ações
   */
  @Query("SELECT acaoId FROM acoes_funcionarios WHERE funcionarioId = :funcionarioId")
  fun listarAcoesPorFuncionario(funcionarioId: Int): LiveData<List<Int>>

  /**
   * Retorna a lista de entidades Funcionario vinculadas a uma ação específica.
   *
   * @param acaoId ID da ação
   * @return Lista de objetos FuncionarioEntity responsáveis pela ação
   */
  @Query("""
        SELECT f.* FROM funcionarios f
        INNER JOIN acoes_funcionarios af ON af.funcionarioId = f.id
        WHERE af.acaoId = :acaoId
    """)
  suspend fun getResponsaveisByAcaoId(acaoId: Int): List<FuncionarioEntity>

  /**
   * Remove todos os vínculos entre funcionários e uma ação específica.
   *
   * @param acaoId ID da ação
   */
  @Query("DELETE FROM acoes_funcionarios WHERE acaoId = :acaoId")
  suspend fun deletarResponsaveisPorAcao(acaoId: Int)
}
