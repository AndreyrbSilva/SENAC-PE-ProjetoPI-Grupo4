package com.example.appsenkaspi.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.appsenkaspi.data.local.entity.NotificacaoEntity

/**
 * DAO responsável pelas operações de acesso a dados da entidade Notificacao.
 * Permite inserir, atualizar, deletar e buscar notificações no banco de dados.
 */
@Dao
interface NotificacaoDao {

  /**
   * Insere uma nova notificação no banco de dados. Substitui se houver conflito de chave primária.
   *
   * @param notificacao a entidade de notificação a ser inserida
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun inserir(notificacao: NotificacaoEntity)

  /**
   * Atualiza os dados de uma notificação existente no banco de dados.
   *
   * @param notificacao a entidade de notificação atualizada
   */
  @Update
  suspend fun atualizar(notificacao: NotificacaoEntity)

  /**
   * Remove uma notificação do banco de dados.
   *
   * @param notificacao a notificação a ser removida
   */
  @Delete
  suspend fun deletar(notificacao: NotificacaoEntity)

  /**
   * Busca uma notificação pelo seu ID.
   *
   * @param id o identificador da notificação
   * @return a notificação correspondente ou null se não encontrada
   */
  @Query("SELECT * FROM notificacoes WHERE id = :id")
  suspend fun buscarPorId(id: Int): NotificacaoEntity?

  /**
   * Remove todas as notificações cujo status esteja marcado como 'ARQUIVIDA'.
   */
  @Query("DELETE FROM notificacoes WHERE status = 'ARQUIVIDA'")
  suspend fun deletarArquivadas()

  /**
   * Atualiza o status de uma notificação específica com base no seu ID.
   *
   * @param id o ID da notificação
   * @param status o novo status a ser atribuído
   */
  @Query("UPDATE notificacoes SET status = :status WHERE id = :id")
  fun atualizarStatus(id: Int, status: String)
}
