package com.example.appsenkaspi.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


/**
 * Entidade que representa um item de checklist vinculado a uma atividade.
 *
 * Cada item é associado a uma `AtividadeEntity`, permitindo o controle granular
 * de subtarefas ou etapas necessárias para conclusão de uma atividade maior.
 *
 * @property id Identificador único do item (autogerado).
 * @property descricao Descrição textual do item de checklist.
 * @property concluido Indica se o item foi concluído (`true`) ou não (`false`).
 * @property atividadeId ID da atividade à qual o item pertence.
 */
@Entity(
  tableName = "checklist_itens",
  foreignKeys = [
    ForeignKey(
      entity = AtividadeEntity::class,
      parentColumns = ["id"],
      childColumns = ["atividadeId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [Index("atividadeId")]
)
data class ChecklistItemEntity(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val descricao: String,
  val concluido: Boolean = false,
  val atividadeId: Int
)
