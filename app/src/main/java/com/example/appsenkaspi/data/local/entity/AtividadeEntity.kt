package com.example.appsenkaspi.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.appsenkaspi.data.local.enums.PrioridadeAtividade
import com.example.appsenkaspi.data.local.enums.StatusAtividade
import java.util.Date

/**
 * Representa uma atividade específica dentro de uma ação.
 *
 * Cada atividade está associada a uma `AcaoEntity`, possui um responsável principal (`funcionarioId`),
 * e contém informações detalhadas como datas, prioridade, status e autor da criação.
 *
 * Essa entidade suporta vinculação com múltiplos responsáveis via `AtividadeFuncionarioEntity`.
 *
 * @property id Identificador único da atividade (autogerado).
 * @property nome Nome da atividade.
 * @property descricao Descrição detalhada da atividade.
 * @property dataInicio Data prevista de início da atividade.
 * @property dataPrazo Data limite para conclusão da atividade.
 * @property acaoId ID da ação à qual essa atividade pertence.
 * @property funcionarioId ID do responsável principal (relacionamento direto).
 * @property status Status atual da atividade (ex: PENDENTE, CONCLUIDA).
 * @property prioridade Grau de prioridade da atividade (ex: ALTA, MÉDIA, BAIXA).
 * @property criadoPor ID do funcionário que criou a atividade.
 * @property dataCriacao Data e hora de criação do registro da atividade.
 */
@Entity(
  tableName = "atividades",
  foreignKeys = [
    ForeignKey(
      entity = AcaoEntity::class,
      parentColumns = ["id"],
      childColumns = ["acaoId"],
      onDelete = ForeignKey.CASCADE
    ),
    ForeignKey(
      entity = FuncionarioEntity::class,
      parentColumns = ["id"],
      childColumns = ["funcionarioId"],
      onDelete = ForeignKey.CASCADE
    ),
    ForeignKey(
      entity = FuncionarioEntity::class,
      parentColumns = ["id"],
      childColumns = ["criado_por"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(value = ["acaoId"]),
    Index(value = ["funcionarioId"]),
    Index(value = ["criado_por"])
  ]
)
data class AtividadeEntity(

  /** ID da atividade (chave primária autogerada). */
  @PrimaryKey(autoGenerate = true) var id: Int? = null,

  /** Título da atividade. */
  val nome: String,

  /** Descrição detalhada da atividade. */
  val descricao: String,

  /** Data de início planejada. */
  val dataInicio: Date,

  /** Data limite para conclusão da atividade. */
  val dataPrazo: Date,

  /** Referência à ação à qual a atividade está vinculada. */
  val acaoId: Int,

  /** ID do responsável principal (pode coexistir com uma lista em relação N:N). */
  val funcionarioId: Int,

  /** Estado atual da atividade. */
  @ColumnInfo(name = "status")
  val status: StatusAtividade,

  /** Prioridade da atividade. */
  @ColumnInfo(name = "prioridade")
  val prioridade: PrioridadeAtividade,

  /** ID do criador da atividade. */
  @ColumnInfo(name = "criado_por")
  val criadoPor: Int,

  /** Data de criação do registro. */
  @ColumnInfo(name = "data_criacao")
  val dataCriacao: Date,
)
