package com.example.appsenkaspi.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.appsenkaspi.data.local.entity.RequisicaoEntity
import com.example.appsenkaspi.data.local.enums.StatusNotificacao
import com.example.appsenkaspi.data.local.enums.TipoDeNotificacao
import java.util.Date

/**
 * Representa uma notificação enviada a um funcionário, podendo estar associada a uma requisição,
 * atividade ou ação específica.
 *
 * Essa entidade é usada para controlar notificações internas entre usuários do sistema,
 * principalmente relacionadas a solicitações e status de atividades/ações.
 *
 * @property id Identificador único da notificação (autogerado).
 * @property tipo Tipo da notificação (ex: REQUISICAO_APROVADA, ATIVIDADE_VENCIDA), definido por enum [TipoDeNotificacao].
 * @property mensagem Texto principal da notificação exibido ao usuário.
 * @property remetenteId ID do funcionário que enviou a notificação (pode ser nulo se não aplicável).
 * @property destinatarioId ID do funcionário que recebe a notificação.
 * @property requisicaoId ID da requisição associada, se a notificação estiver vinculada a uma [RequisicaoEntity].
 * @property atividadeId ID da atividade relacionada, se houver.
 * @property acaoId ID da ação relacionada, se houver.
 * @property status Status atual da notificação (lida, não lida), definido por enum [StatusNotificacao].
 * @property dataCriacao Data de criação da notificação (padrão: agora).
 */
@Entity(
  tableName = "notificacoes",
  foreignKeys = [
    ForeignKey(
      entity = FuncionarioEntity::class,
      parentColumns = ["id"],
      childColumns = ["remetenteId"],
      onDelete = ForeignKey.SET_NULL
    ),
    ForeignKey(
      entity = FuncionarioEntity::class,
      parentColumns = ["id"],
      childColumns = ["destinatarioId"],
      onDelete = ForeignKey.SET_NULL
    ),
    ForeignKey(
      entity = RequisicaoEntity::class,
      parentColumns = ["id"],
      childColumns = ["requisicaoId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index("remetenteId"),
    Index("destinatarioId"),
    Index("requisicaoId")
  ]
)
data class NotificacaoEntity(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,

  @ColumnInfo(name = "tipo")
  val tipo: TipoDeNotificacao,

  @ColumnInfo(name = "mensagem")
  val mensagem: String,

  @ColumnInfo(name = "remetenteId")
  val remetenteId: Int?,

  @ColumnInfo(name = "destinatarioId")
  val destinatarioId: Int?,

  @ColumnInfo(name = "requisicaoId")
  val requisicaoId: Int? = null,

  @ColumnInfo(name = "vinculoAtividadeId")
  val atividadeId: Int? = null,

  @ColumnInfo(name = "vinculoAcaoId")
  val acaoId: Int? = null,

  @ColumnInfo(name = "status")
  val status: StatusNotificacao = StatusNotificacao.NAO_LIDA,

  @ColumnInfo(name = "dataCriacao")
  val dataCriacao: Date = Date()
)
