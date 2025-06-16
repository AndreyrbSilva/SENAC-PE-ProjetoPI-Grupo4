package com.example.appsenkaspi.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.appsenkaspi.data.local.enums.StatusSubPilar
import java.util.Date

/**
 * Representa um Subpilar dentro da hierarquia de planejamento. Um Subpilar é uma subdivisão de um Pilar
 * e pode conter várias Ações associadas. Permite uma organização mais granular das atividades.
 *
 * Está ligado diretamente a um `PilarEntity` através da chave estrangeira `pilarId`.
 *
 * @property id Identificador único do subpilar (auto-incrementado).
 * @property nome Nome do subpilar, utilizado para exibição e identificação.
 * @property descricao Descrição opcional do subpilar, com mais detalhes sobre seu propósito.
 * @property dataInicio Data planejada de início das atividades do subpilar.
 * @property dataPrazo Data limite planejada para conclusão do subpilar.
 * @property pilarId Chave estrangeira que referencia o `PilarEntity` ao qual este subpilar pertence.
 * @property dataCriacao Data em que o subpilar foi criado no sistema.
 * @property status Enum indicando o estado atual do subpilar (ex: ATIVO, CONCLUIDO, CANCELADO).
 * @property criadoPor ID do funcionário (`FuncionarioEntity`) responsável pela criação do subpilar.
 */
@Entity(
  tableName = "subpilares",
  foreignKeys = [
    ForeignKey(
      entity = PilarEntity::class,
      parentColumns = ["id"],
      childColumns = ["pilarId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [Index("pilarId")]
)
data class SubpilarEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Int = 0,

  val nome: String,

  val descricao: String?,

  val dataInicio: Date,

  val dataPrazo: Date,

  val pilarId: Int,

  val dataCriacao: Date,

  @ColumnInfo(name = "status")
  val status: StatusSubPilar,

  @ColumnInfo(name = "criado_por")
  val criadoPor: Int
)

