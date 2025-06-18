package com.example.appsenkaspi.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.appsenkaspi.data.local.enums.StatusRequisicao
import com.example.appsenkaspi.data.local.enums.TipoRequisicao
import java.util.Date

/**
 * Representa uma requisição feita por um funcionário (geralmente da categoria Apoio)
 * que precisa ser avaliada por um Coordenador. A requisição pode envolver
 * ações como criação, edição ou conclusão de uma atividade ou ação.
 *
 * Está associada diretamente a uma atividade via chave estrangeira (atividadeId),
 * mas também pode carregar informações sobre ações via JSON.
 *
 * @property id Identificador único da requisição (auto-incrementado).
 * @property tipo Enum que define o tipo da requisição (ex: CRIAR_ATIVIDADE, EDITAR_ACAO).
 * @property atividadeJson Representação em JSON da atividade, usada para persistir estado completo durante requisições.
 * @property acaoJson Representação em JSON da ação, usada quando a requisição envolve ações.
 * @property atividadeId ID da atividade associada (pode ser nulo, dependendo do tipo de requisição).
 * @property acaoId ID da ação associada (usado para requisições relacionadas a ações).
 * @property solicitanteId ID do funcionário que criou a requisição.
 * @property status Estado atual da requisição (PENDENTE, APROVADA, RECUSADA, etc.).
 * @property dataSolicitacao Data e hora em que a requisição foi feita.
 * @property dataResposta Data e hora em que a requisição foi respondida (se aplicável).
 * @property coordenadorId ID do coordenador que respondeu (se já foi respondida).
 * @property mensagemResposta Comentário ou justificativa do coordenador sobre a decisão.
 * @property foiVista Flag que indica se o solicitante já visualizou a resposta.
 * @property resolvida Flag lógica indicando se a requisição já foi processada por completo.
 * @property excluida Flag lógica para exclusão lógica (soft delete).
 */
@Entity(
  tableName = "requisicoes",
  foreignKeys = [
    ForeignKey(
      entity = AtividadeEntity::class,
      parentColumns = ["id"],
      childColumns = ["atividadeId"],
      onDelete = ForeignKey.Companion.CASCADE
    )
  ],
  indices = [Index("atividadeId")]
)
data class RequisicaoEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Int = 0,

  val tipo: TipoRequisicao,

  val atividadeJson: String? = null,
  val acaoJson: String? = null,

  val atividadeId: Int? = null,
  val acaoId: Long? = null,

  val solicitanteId: Int,

  val status: StatusRequisicao = StatusRequisicao.PENDENTE,

  val dataSolicitacao: Date = Date(),
  val dataResposta: Date? = null,

  val coordenadorId: Int? = null,
  val mensagemResposta: String? = null,

  val foiVista: Boolean = false,
  val resolvida: Boolean = false,
  val excluida: Boolean = false
)
