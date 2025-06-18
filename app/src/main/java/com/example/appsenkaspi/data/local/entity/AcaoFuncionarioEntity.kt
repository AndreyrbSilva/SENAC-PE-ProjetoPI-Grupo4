package com.example.appsenkaspi.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import com.example.appsenkaspi.data.local.entity.FuncionarioEntity

/**
 * Entidade intermediária que representa o relacionamento N:N entre Ações e Funcionários.
 *
 * Cada registro vincula uma `AcaoEntity` a um `FuncionarioEntity`, permitindo que uma ação
 * tenha múltiplos responsáveis e um funcionário participe de múltiplas ações.
 *
 * Utiliza chave composta (`acaoId`, `funcionarioId`) como primary key.
 *
 * @property acaoId ID da ação associada.
 * @property funcionarioId ID do funcionário responsável pela ação.
 */
@Entity(
  tableName = "acoes_funcionarios",
  primaryKeys = ["acaoId", "funcionarioId"],
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
    )
  ]
)
data class AcaoFuncionarioEntity(
  /** ID da ação relacionada. */
  val acaoId: Long,

  /** ID do funcionário relacionado. */
  val funcionarioId: Int
)
