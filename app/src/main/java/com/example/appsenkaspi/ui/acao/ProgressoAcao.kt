package com.example.appsenkaspi.ui.acao

/**
 * Representa o progresso de execução de uma Ação, incluindo percentual de conclusão
 * e quantidade total de atividades associadas.
 *
 * Utilizada para compor visões de dashboard, relatórios ou indicadores de desempenho.
 *
 * @property acaoId Identificador único da ação.
 * @property nome Nome descritivo da ação.
 * @property progresso Valor percentual de progresso da ação, no intervalo de 0.0 a 1.0.
 * @property totalAtividades Número total de atividades vinculadas à ação.
 */
data class ProgressoAcao(
  val acaoId: Int,
  val nome: String,
  val progresso: Float,
  val totalAtividades: Int
)
