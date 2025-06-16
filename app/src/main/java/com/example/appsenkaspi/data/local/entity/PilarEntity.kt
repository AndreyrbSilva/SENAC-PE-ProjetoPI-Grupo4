package com.example.appsenkaspi.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.appsenkaspi.data.local.enums.StatusPilar
import java.util.Date

/**
 * Representa um Pilar, que é a entidade base de organização no sistema.
 * Cada Pilar pode conter Subpilares e/ou Ações, definindo uma estrutura hierárquica de planejamento.
 *
 * @property id Identificador único do Pilar (autogerado).
 * @property nome Nome do Pilar (título principal).
 * @property descricao Descrição detalhada do Pilar.
 * @property dataInicio Data de início prevista para o Pilar.
 * @property dataPrazo Data limite ou estimada de conclusão.
 * @property dataCriacao Data de criação do Pilar no sistema.
 * @property dataConclusao Data real de conclusão (pode ser nula até que o Pilar seja finalizado).
 * @property dataExcluido [DEPRECATED] Campo antigo de exclusão, substituído por [dataExclusao].
 * @property criadoPor ID do funcionário criador do Pilar (relacionado a [FuncionarioEntity]).
 * @property status Status atual do Pilar (ex: EM_ANDAMENTO, CONCLUIDO, DELETADO), definido por [StatusPilar].
 * @property dataExclusao Data em que o Pilar foi efetivamente marcado como excluído.
 */
@Entity(
  tableName = "pilares",
  foreignKeys = [
    ForeignKey(
      entity = FuncionarioEntity::class,
      parentColumns = ["id"],
      childColumns = ["criado_por"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [Index(value = ["criado_por"])]
)
data class PilarEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Int = 0,

  val nome: String,
  val descricao: String,

  val dataInicio: Date,
  val dataPrazo: Date,
  val dataCriacao: Date,

  val dataConclusao: Date? = null, // Preenchido apenas quando concluído

  val dataExcluido: Date? = null, // OBS: campo mantido por compatibilidade, mas evite uso

  @ColumnInfo(name = "criado_por")
  val criadoPor: Int,

  @ColumnInfo(name = "status")
  val status: StatusPilar,

  val dataExclusao: Date? = null // Novo campo preferido para marcação de exclusão lógica
)

