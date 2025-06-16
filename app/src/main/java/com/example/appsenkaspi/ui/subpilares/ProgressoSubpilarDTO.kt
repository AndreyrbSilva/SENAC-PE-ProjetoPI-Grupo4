package com.example.appsenkaspi.ui.subpilares

/**
 * DTO (Data Transfer Object) que representa o progresso consolidado de um Subpilar.
 *
 * Essa estrutura é usada para transmitir o status de acompanhamento de um subpilar
 * em visualizações como dashboards, relatórios ou gráficos de progresso.
 *
 * @property subpilarId Identificador único do subpilar ao qual esse progresso se refere.
 * @property totalAcoes Número total de ações vinculadas ao subpilar.
 * @property progresso Valor percentual do progresso geral do subpilar (entre 0.0f e 1.0f).
 */
data class ProgressoSubpilarDTO(
  val subpilarId: Int,
  val totalAcoes: Int,
  val progresso: Float
)
