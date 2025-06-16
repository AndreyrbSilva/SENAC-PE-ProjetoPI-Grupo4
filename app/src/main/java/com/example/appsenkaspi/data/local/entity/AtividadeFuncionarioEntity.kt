package com.example.appsenkaspi.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import com.example.appsenkaspi.data.local.entity.FuncionarioEntity

/**
 * Entidade de associação entre atividades e seus responsáveis.
 *
 * Representa um relacionamento N:N entre `AtividadeEntity` e `FuncionarioEntity`,
 * permitindo múltiplos responsáveis por atividade, e múltiplas atividades atribuídas a um funcionário.
 *
 * @property atividadeId ID da atividade associada.
 * @property funcionarioId ID do funcionário responsável.
 */
@Entity(
  tableName = "atividades_funcionarios",
  primaryKeys = ["atividadeId", "funcionarioId"],
  foreignKeys = [
    ForeignKey(
      entity = AtividadeEntity::class,
      parentColumns = ["id"],
      childColumns = ["atividadeId"],
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
data class AtividadeFuncionarioEntity(

  /** ID da atividade atribuída. */
  val atividadeId: Int,

  /** ID do funcionário responsável. */
  val funcionarioId: Int
)

