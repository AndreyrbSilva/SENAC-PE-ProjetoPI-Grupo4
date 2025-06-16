package com.example.appsenkaspi.ui.requisicao

import com.example.appsenkaspi.data.local.entity.AcaoEntity
import com.example.appsenkaspi.data.local.entity.AtividadeEntity

/**
 * Representa os diferentes tipos de requisições que podem ser enviadas no sistema,
 * como criação, edição ou conclusão de ações e atividades.
 *
 * Essa `sealed class` permite tratamento seguro e exaustivo dos diferentes casos
 * durante o processamento de requisições (ex: em ViewModel, Controller ou Fragment).
 */
sealed class DadosRequisicao {

  /**
   * Requisição para criar uma nova atividade.
   *
   * @property atividade A instância da [AtividadeEntity] a ser criada.
   */
  data class CriarAtividade(val atividade: AtividadeEntity) : DadosRequisicao()

  /**
   * Requisição para editar uma atividade existente.
   *
   * @property atividade A instância modificada da [AtividadeEntity].
   */
  data class EditarAtividade(val atividade: AtividadeEntity) : DadosRequisicao()

  /**
   * Requisição para marcar uma atividade como concluída.
   *
   * @property atividade A [AtividadeEntity] a ser marcada como concluída.
   */
  data class CompletarAtividade(val atividade: AtividadeEntity) : DadosRequisicao()

  /**
   * Requisição para criar uma nova ação.
   *
   * @property acao A instância da [AcaoEntity] a ser criada.
   */
  data class CriarAcao(val acao: AcaoEntity) : DadosRequisicao()

  /**
   * Requisição para editar uma ação existente.
   *
   * @property acao A instância modificada da [AcaoEntity].
   */
  data class EditarAcao(val acao: AcaoEntity) : DadosRequisicao()
}
