package com.example.appsenkaspi.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.appsenkaspi.data.local.entity.FuncionarioEntity

/**
 * DAO responsável pelas operações relacionadas à entidade Funcionario.
 * Inclui autenticação, busca por ID, inserções em lote e observação reativa da lista de funcionários.
 */
@Dao
interface FuncionarioDao {

  /**
   * Realiza a autenticação de um funcionário com base no ID de acesso e senha.
   *
   * @param idAcesso identificador de login
   * @param senha senha fornecida
   * @return Funcionário autenticado ou null se inválido
   */
  @Query("SELECT * FROM funcionarios WHERE id_acesso = :idAcesso AND senha = :senha LIMIT 1")
  suspend fun autenticar(idAcesso: Int, senha: String): FuncionarioEntity?

  /**
   * Retorna todos os funcionários cadastrados.
   */
  @Query("SELECT * FROM funcionarios")
  suspend fun buscarTodos(): List<FuncionarioEntity>

  /**
   * Insere um novo funcionário ou substitui se já existir.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun inserirFuncionario(funcionario: FuncionarioEntity)

  /**
   * Busca um funcionário por ID com resultado limitado a 1.
   */
  @Query("SELECT * FROM funcionarios WHERE id = :id LIMIT 1")
  suspend fun buscarPorId(id: Int): FuncionarioEntity?

  /**
   * Busca um funcionário por ID (sem LIMIT, mas função equivalente).
   */
  @Query("SELECT * FROM funcionarios WHERE id = :id")
  suspend fun buscarFuncionarioPorId(id: Int): FuncionarioEntity?

  /**
   * Lista todos os funcionários como LiveData para observação reativa.
   */
  @Query("SELECT * FROM funcionarios")
  fun listarTodosFuncionarios(): LiveData<List<FuncionarioEntity>>

  /**
   * Retorna uma lista de funcionários cujos IDs estejam contidos na lista fornecida.
   */
  @Query("SELECT * FROM funcionarios WHERE id IN (:ids)")
  suspend fun getByIds(ids: List<Int>): List<FuncionarioEntity>

  /**
   * Insere um funcionário (substituindo em caso de conflito).
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun inserir(funcionario: FuncionarioEntity)

  /**
   * Insere múltiplos funcionários de uma vez.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun inserirTodos(funcionarios: List<FuncionarioEntity>)

  /**
   * Atualiza os dados de um funcionário existente.
   */
  @Update
  suspend fun atualizarFuncionario(funcionario: FuncionarioEntity)

  /**
   * Remove um funcionário do banco.
   */
  @Delete
  suspend fun deletarFuncionario(funcionario: FuncionarioEntity)

  /**
   * Retorna os funcionários cujos IDs estão na lista especificada.
   */
  @Query("SELECT * FROM funcionarios WHERE id IN (:ids)")
  suspend fun getFuncionariosPorIds(ids: List<Int>): List<FuncionarioEntity>

  /**
   * Retorna um único funcionário por ID.
   */
  @Query("SELECT * FROM funcionarios WHERE id = :id LIMIT 1")
  suspend fun getFuncionarioById(id: Int): FuncionarioEntity?
}
