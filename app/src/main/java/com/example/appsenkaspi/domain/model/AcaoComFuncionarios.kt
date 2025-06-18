package com.example.appsenkaspi.domain.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.appsenkaspi.data.local.entity.AcaoFuncionarioEntity
import com.example.appsenkaspi.data.local.entity.FuncionarioEntity
import com.example.appsenkaspi.data.local.entity.AcaoEntity

/**
 * Modelo que representa uma Ação acompanhada da lista de Funcionários associados a ela.
 * Esta estrutura é utilizada para recuperar relações many-to-many entre Ações e Funcionários,
 * mediadas pela entidade de junção [AcaoFuncionarioEntity].
 *
 * @property acao Instância da ação principal.
 * @property funcionarios Lista de funcionários vinculados a essa ação.
 */
data class AcaoComFuncionarios(
  @Embedded val acao: AcaoEntity,

  @Relation(
    parentColumn = "id", // Coluna da AcaoEntity usada na junção
    entityColumn = "id", // Coluna da FuncionarioEntity usada na junção
    associateBy = Junction(
      value = AcaoFuncionarioEntity::class, // Entidade de junção que representa a tabela N:N
      parentColumn = "acaoId",              // Chave da ação na entidade de junção
      entityColumn = "funcionarioId"        // Chave do funcionário na entidade de junção
    )
  )
  val funcionarios: List<FuncionarioEntity>
)
