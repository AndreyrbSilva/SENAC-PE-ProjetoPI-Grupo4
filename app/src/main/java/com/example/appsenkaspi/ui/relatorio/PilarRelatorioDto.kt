package com.example.appsenkaspi.ui.relatorio
/**
 * Data Transfer Object (DTO) usado para representar os dados essenciais de um Pilar
 * ao gerar relatórios ou realizar exportações.
 *
 * Essa estrutura é útil para serialização (ex: geração de PDF/CSV) sem depender da
 * entidade completa do banco de dados.
 *
 * @property nome Nome do pilar, utilizado como título no relatório.
 * @property descricao Texto descritivo sobre o objetivo ou escopo do pilar.
 * @property dataInicio Data de início das atividades do pilar, formatada como String.
 * @property dataPrazo Data-limite ou estimativa de conclusão, formatada como String.
 * @property status Estado atual do pilar (ex: "Em andamento", "Concluído").
 * @property criadoPor Nome do funcionário ou responsável que criou o pilar.
 */
data class PilarRelatorioDto(
  val nome: String,
  val descricao: String,
  val dataInicio: String,
  val dataPrazo: String,
  val status: String,
  val criadoPor: String
)
