package com.example.appsenkaspi.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.appsenkaspi.data.local.entity.ChecklistItemEntity



/**
 * DAO responsável pelas operações na tabela de checklist vinculada a uma Atividade.
 * Permite gerenciar os itens de checklist de forma individual.
 */
@Dao
interface ChecklistDao {

  /**
   * Retorna a lista de itens do checklist vinculados a uma atividade.
   *
   * @param atividadeId ID da atividade
   * @return LiveData com a lista de itens do checklist
   */
  @Query("SELECT * FROM checklist_itens WHERE atividadeId = :atividadeId")
  fun getChecklist(atividadeId: Int): LiveData<List<ChecklistItemEntity>>

  /**
   * Insere um novo item de checklist.
   *
   * @param item item a ser inserido
   */
  @Insert
  suspend fun inserir(item: ChecklistItemEntity)

  /**
   * Atualiza um item de checklist existente.
   *
   * @param item item atualizado
   */
  @Update
  suspend fun atualizar(item: ChecklistItemEntity)

  /**
   * Remove um item de checklist.
   *
   * @param item item a ser removido
   */
  @Delete
  suspend fun deletar(item: ChecklistItemEntity)
}
