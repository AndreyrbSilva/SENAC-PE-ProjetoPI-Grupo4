package com.example.appsenkaspi.domain.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.appsenkaspi.data.local.entity.AtividadeFuncionarioEntity
import com.example.appsenkaspi.data.local.entity.FuncionarioEntity
import com.example.appsenkaspi.data.local.entity.AtividadeEntity

/**
 * Relação entre uma atividade e seus respectivos responsáveis (funcionários).
 *
 * Utilizada para obter, em uma única consulta, tanto os dados da [AtividadeEntity] quanto a lista
 * de [FuncionarioEntity]s vinculados à atividade por meio da tabela de junção [AtividadeFuncionarioEntity].
 *
 * Ideal para exibições detalhadas em telas como "Detalhes da Atividade", onde é necessário mostrar
 * a atividade e todos os seus responsáveis associados.
 *
 * @property atividade Instância da [AtividadeEntity] representando a atividade principal.
 * @property funcionarios Lista de [FuncionarioEntity] que são responsáveis pela atividade.
 */
data class AtividadeComFuncionarios(
  @Embedded val atividade: AtividadeEntity,
  @Relation(
    parentColumn = "id",
    entityColumn = "id",
    associateBy = Junction(
      value = AtividadeFuncionarioEntity::class,
      parentColumn = "atividadeId",
      entityColumn = "funcionarioId"
    )
  )
  val funcionarios: List<FuncionarioEntity>
)

