package com.example.appsenkaspi.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.appsenkaspi.data.local.enums.StatusAcao
import com.example.appsenkaspi.data.local.entity.SubpilarEntity
import java.util.Date

/**
 * Entidade que representa uma Ação dentro de um Pilar ou Subpilar.
 *
 * Cada ação está relacionada a um criador (funcionário), pode estar vinculada diretamente a um `Pilar`
 * ou, alternativamente, a um `Subpilar`. Utiliza controle de integridade referencial via chaves estrangeiras.
 *
 * Ações também armazenam datas importantes para controle de execução e prazos.
 *
 * @property id ID único da ação (auto gerado).
 * @property nome Nome da ação.
 * @property descricao Descrição detalhada da ação.
 * @property dataInicio Data de início da ação.
 * @property dataPrazo Data limite para execução.
 * @property pilarId ID do pilar relacionado (opcional se estiver em subpilar).
 * @property subpilarId ID do subpilar relacionado (opcional se estiver diretamente em pilar).
 * @property status Status atual da ação (em andamento, concluída, vencida, etc).
 * @property criadoPor ID do funcionário responsável pela criação da ação.
 * @property dataCriacao Data de criação da ação.
 */
@Entity(
  tableName = "acoes",
  foreignKeys = [
    ForeignKey(
      entity = PilarEntity::class,
      parentColumns = ["id"],
      childColumns = ["pilarId"],
      onDelete = ForeignKey.CASCADE
    ),
    ForeignKey(
      entity = FuncionarioEntity::class,
      parentColumns = ["id"],
      childColumns = ["criado_por"],
      onDelete = ForeignKey.CASCADE
    ),
    ForeignKey(
      entity = SubpilarEntity::class,
      parentColumns = ["id"],
      childColumns = ["subpilarId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(value = ["pilarId"]),
    Index(value = ["criado_por"]),
    Index(value = ["subpilarId"])
  ]
)
data class AcaoEntity(
  /** Identificador único da ação, gerado automaticamente. */
  @PrimaryKey(autoGenerate = true) val id: Int? = null,

  /** Nome da ação. */
  val nome: String,

  /** Descrição detalhada da ação. */
  val descricao: String,

  /** Data de início da ação. */
  val dataInicio: Date,

  /** Data limite para execução da ação. */
  val dataPrazo: Date,

  /** ID do Pilar associado à ação (pode ser nulo se vinculada a um Subpilar). */
  val pilarId: Int? = null,

  /** ID do Subpilar associado à ação (pode ser nulo se vinculada diretamente ao Pilar). */
  val subpilarId: Int? = null,

  /** Status da ação (ex: EM_ANDAMENTO, CONCLUIDA, VENCIDA). */
  @ColumnInfo(name = "status") val status: StatusAcao,

  /** ID do funcionário que criou a ação. */
  @ColumnInfo(name = "criado_por") val criadoPor: Int,

  /** Data em que a ação foi criada. */
  @ColumnInfo(name = "data_criacao") val dataCriacao: Date
)
