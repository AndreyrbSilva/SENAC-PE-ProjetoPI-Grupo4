package com.example.appsenkaspi.ui.dashboard

/**
 * Representa um resumo quantitativo das ações e atividades em um Pilar ou conjunto de Pilares.
 *
 * @property totalAcoes Quantidade total de ações associadas.
 * @property totalAtividades Quantidade total de atividades vinculadas às ações.
 * @property atividadesConcluidas Número de atividades marcadas como concluídas.
 * @property atividadesAndamento Número de atividades em andamento (status intermediário).
 * @property atividadesAtraso Número de atividades cujo prazo foi ultrapassado.
 */
data class ResumoDashboard(
  val totalAcoes: Int,
  val totalAtividades: Int,
  val atividadesConcluidas: Int,
  val atividadesAndamento: Int,
  val atividadesAtraso: Int
)
