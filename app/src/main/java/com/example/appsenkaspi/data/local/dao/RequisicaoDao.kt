package com.example.appsenkaspi.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.appsenkaspi.data.local.entity.RequisicaoEntity
import com.example.appsenkaspi.data.local.enums.StatusRequisicao
import com.example.appsenkaspi.data.local.enums.TipoRequisicao

/**
 * DAO responsável pelas operações relacionadas às requisições no sistema.
 * Inclui inserções, atualizações, buscas, verificações e exclusões associadas a diferentes tipos de notificações e ações pendentes.
 */
@Dao
interface RequisicaoDao {

  /**
   * Insere uma nova requisição no banco. Substitui caso já exista uma com o mesmo ID.
   */
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun inserir(requisicao: RequisicaoEntity): Long

  /**
   * Atualiza uma requisição existente.
   */
  @Update
  suspend fun update(requisicao: RequisicaoEntity)

  /**
   * Retorna todas as requisições pendentes de aprovação que não sejam informativas.
   */
  @Query("""
        SELECT * FROM requisicoes
        WHERE status = :status
        AND tipo != 'atividade_para_vencer'
    """)
  fun getRequisicoesPendentes(status: StatusRequisicao = StatusRequisicao.PENDENTE): LiveData<List<RequisicaoEntity>>

  /**
   * Busca uma requisição específica pelo ID.
   */
  @Query("SELECT * FROM requisicoes WHERE id = :id")
  suspend fun getRequisicaoById(id: Int): RequisicaoEntity?

  /**
   * Retorna todas as requisições feitas por um usuário, ordenadas por data.
   */
  @Query("""
        SELECT * FROM requisicoes
        WHERE solicitanteId = :userId
        ORDER BY dataSolicitacao DESC
    """)
  fun getRequisicoesDoUsuario(userId: Int): LiveData<List<RequisicaoEntity>>

  /**
   * Retorna as notificações visíveis no painel do apoio (idêntico à anterior).
   */
  @Query("""
        SELECT * FROM requisicoes
        WHERE solicitanteId = :usuarioId
        ORDER BY dataSolicitacao DESC
    """)
  fun getNotificacoesDoApoio(usuarioId: Int): LiveData<List<RequisicaoEntity>>

  /**
   * Verifica se existe uma requisição pendente para uma atividade, usuário e tipo específico.
   */
  @Query("""
        SELECT * FROM requisicoes
        WHERE atividadeId = :atividadeId
        AND solicitanteId = :solicitanteId
        AND tipo = :tipo
        AND status = 'pendente'
        LIMIT 1
    """)
  suspend fun getRequisicaoPorAtividade(
    atividadeId: Int,
    solicitanteId: Int,
    tipo: TipoRequisicao
  ): RequisicaoEntity?

  /**
   * Retorna a quantidade de requisições não vistas por um usuário.
   */
  @Query("""
        SELECT COUNT(*) FROM requisicoes
        WHERE solicitanteId = :userId
        AND foiVista = 0
    """)
  fun getQuantidadeNaoVistas(userId: Int): LiveData<Int>

  /**
   * Marca todas as notificações como vistas para um usuário.
   */
  @Query("""
        UPDATE requisicoes
        SET foiVista = 1
        WHERE solicitanteId = :usuarioId
        AND foiVista = 0
    """)
  suspend fun marcarComoVista(usuarioId: Int)

  /**
   * Retorna a lista de notificações não vistas para o painel de apoio.
   */
  @Query("""
        SELECT * FROM requisicoes
        WHERE solicitanteId = :usuarioId
        AND foiVista = 0
    """)
  fun getNotificacoesNaoVistas(usuarioId: Int): LiveData<List<RequisicaoEntity>>

  /**
   * Verifica se há requisição pendente específica para uma atividade, usuário e tipo.
   */
  @Query("""
        SELECT EXISTS(
            SELECT 1 FROM requisicoes
            WHERE atividadeId = :atividadeId
            AND solicitanteId = :solicitanteId
            AND tipo = :tipo
            AND status = 'pendente'
        )
    """)
  suspend fun existeRequisicaoPendenteParaAtividade(
    atividadeId: Int,
    solicitanteId: Int,
    tipo: TipoRequisicao
  ): Boolean

  /**
   * Verifica se já foi feita uma requisição hoje para a atividade.
   */
  @Query("""
        SELECT EXISTS(
            SELECT 1 FROM requisicoes
            WHERE atividadeId = :atividadeId
            AND solicitanteId = :solicitanteId
            AND tipo = :tipo
            AND date(datetime(dataSolicitacao / 1000, 'unixepoch')) = date('now')
        )
    """)
  suspend fun existeRequisicaoHojeParaAtividade(
    atividadeId: Int,
    solicitanteId: Int,
    tipo: TipoRequisicao
  ): Boolean

  /**
   * Conta requisições pendentes que exigem decisão do coordenador.
   */
  @Query("""
        SELECT COUNT(*) FROM requisicoes
        WHERE status = 'pendente'
        AND tipo IN ('criar_atividade', 'editar_atividade', 'criar_acao', 'editar_acao', 'completar_atividade')
    """)
  fun getQuantidadePendentesParaCoordenador(): LiveData<Int>

  /**
   * Retorna quantidade de notificações de prazo não vistas.
   */
  @Query("""
        SELECT COUNT(*) FROM requisicoes
        WHERE solicitanteId = :userId
        AND foiVista = 0
        AND tipo IN ('atividade_para_vencer', 'atividade_vencida', 'prazo_alterado', 'atividade_concluida', 'responsavel_adicionado', 'responsavel_removido')
    """)
  fun getQuantidadeNotificacoesPrazoNaoVistas(userId: Int): LiveData<Int>

  /**
   * Marca todas as notificações de prazo como vistas.
   */
  @Query("""
        UPDATE requisicoes
        SET foiVista = 1
        WHERE solicitanteId = :usuarioId
        AND foiVista = 0
        AND tipo in ('atividade_para_vencer', 'atividade_vencida', 'prazo_alterado', 'atividade_concluida', 'responsavel_adicionado', 'responsavel_removido')
    """)
  suspend fun marcarNotificacoesDePrazoComoVistas(usuarioId: Int)

  /**
   * Verifica se existe notificação de atividade vencida para o responsável.
   */
  @Query("""
        SELECT EXISTS (
            SELECT 1 FROM requisicoes
            WHERE atividadeId = :atividadeId
            AND solicitanteId = :responsavelId
            AND tipo = 'atividade_vencida'
        )
    """)
  suspend fun existeRequisicaoDeVencida(
    atividadeId: Int,
    responsavelId: Int
  ): Boolean

  /**
   * Busca notificações de tipo prazo por atividade.
   */
  @Query("SELECT * FROM requisicoes WHERE atividadeId = :atividadeId AND (tipo = 'atividade_para_vencer' OR tipo = 'atividade_vencidade')")
  suspend fun getRequisicoesDePrazoPorAtividade(atividadeId: Int): List<RequisicaoEntity>

  /**
   * Marca requisição como oculta (foiVista = 1).
   */
  @Query("""
        UPDATE requisicoes
        SET foiVista = 1
        WHERE id = :requisicaoId
    """)
  suspend fun marcarComoOculta(requisicaoId: Int)

  /**
   * Marca requisição como resolvida.
   */
  @Query("UPDATE requisicoes SET resolvida = 1 WHERE id = :requisicaoId")
  suspend fun marcarComoResolvida(requisicaoId: Int)

  /**
   * Deleta requisições de prazo específicas por tipo.
   */
  @Query("DELETE FROM requisicoes WHERE atividadeId = :atividadeId AND tipo = :tipo")
  suspend fun deletarRequisicoesDePrazoPorAtividade(
    atividadeId: Int,
    tipo: TipoRequisicao = TipoRequisicao.ATIVIDADE_PARA_VENCER
  )

  /**
   * Deleta requisições de um determinado tipo por atividade.
   */
  @Query("DELETE FROM requisicoes WHERE atividadeId = :atividadeId AND tipo = :tipo")
  suspend fun deletarRequisicoesDeTipoPorAtividade(
    atividadeId: Int,
    tipo: TipoRequisicao
  )

  /**
   * Retorna todas as notificações de um funcionário ordenadas pela data.
   */
  @Query("SELECT * FROM requisicoes WHERE solicitanteId = :id ORDER BY dataSolicitacao DESC")
  fun getNotificacoesDoFuncionario(id: Int): LiveData<List<RequisicaoEntity>>

  /**
   * Verifica se há notificação com dias restantes já enviada.
   */
  @Query("""
        SELECT COUNT(*) > 0 FROM requisicoes
        WHERE atividadeId = :atividadeId
        AND solicitanteId = :responsavelId
        AND tipo = :tipo
        AND mensagemResposta LIKE '%' || :diasRestantes || '%'
    """)
  suspend fun existeNotificacaoComDiasRestantes(
    atividadeId: Int,
    responsavelId: Int,
    tipo: TipoRequisicao,
    diasRestantes: Int
  ): Boolean

  /**
   * Verifica se há notificação com mensagem exatamente igual à enviada.
   */
  @Query("""
        SELECT EXISTS(
            SELECT 1 FROM requisicoes
            WHERE atividadeId = :atividadeId
            AND solicitanteId = :responsavelId
            AND tipo = :tipo
            AND mensagemResposta = :mensagem
        )
    """)
  suspend fun existeMensagemExata(
    atividadeId: Int,
    responsavelId: Int,
    tipo: TipoRequisicao,
    mensagem: String
  ): Boolean

  /**
   * Retorna todas as requisições ordenadas por data.
   */
  @Query("SELECT * FROM requisicoes ORDER BY dataSolicitacao DESC")
  fun getTodasNotificacoes(): LiveData<List<RequisicaoEntity>>

  /**
   * Remove requisições específicas por tipo, atividade e funcionário.
   */
  @Query("DELETE FROM requisicoes WHERE atividadeId = :atividadeId AND tipo = :tipo AND solicitanteId = :responsavelId")
  suspend fun deletarRequisicoesDeTipoPorAtividadeEFuncionario(
    atividadeId: Int,
    tipo: TipoRequisicao,
    responsavelId: Int
  )

  /**
   * Marca requisições como excluídas (soft delete).
   */
  @Query("UPDATE requisicoes SET excluida = 1 WHERE id IN (:ids)")
  suspend fun marcarComoExcluidas(ids: List<Int>)

  /**
   * Retorna requisições que não foram excluídas.
   */
  @Query("SELECT * FROM requisicoes WHERE excluida = 0")
  fun getNaoExcluidas(): LiveData<List<RequisicaoEntity>>
}

