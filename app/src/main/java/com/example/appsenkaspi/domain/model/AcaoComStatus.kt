package com.example.appsenkaspi.domain.model

import androidx.room.Embedded
import com.example.appsenkaspi.data.local.entity.AcaoEntity

/**
 * Projeção de uma Ação com informações agregadas sobre suas atividades associadas.
 *
 * Essa estrutura é útil para exibição em dashboards, resumos ou visões gerais na interface do usuário,
 * permitindo apresentar não apenas os dados da ação, mas também o progresso ou estado geral das atividades vinculadas.
 *
 * @property acao Instância da entidade [AcaoEntity] representando a ação principal.
 * @property totalAtividades Número total de atividades associadas a essa ação.
 * @property ativasConcluidas Número de atividades que estão ativas e já foram concluídas (status = CONCLUIDA).
 */
data class AcaoComStatus(
  @Embedded
  val acao: AcaoEntity,

  val totalAtividades: Int,
  val ativasConcluidas: Int
)

