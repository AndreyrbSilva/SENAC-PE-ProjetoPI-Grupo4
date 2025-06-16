package com.example.appsenkaspi.ui.relatorio

/**
 * Requisição enviada para a API de geração de relatórios.
 *
 * Define o tipo de relatório e os dados estruturados que serão transformados em documento.
 *
 * @property tipoRelatorio Tipo do relatório: "geral" ou "pilar"
 * @property pilarId ID do pilar selecionado (somente para relatórios por pilar)
 * @property pilares Lista de pilares com suas ações e atividades incluídas no relatório
 */
data class RelatorioRequest(
  val tipoRelatorio: String,
  val pilarId: Int? = null,
  val pilares: List<PilarDTO>
)

/**
 * Representação estruturada de um Pilar para geração de relatório.
 *
 * Inclui metadados e a lista completa de ações associadas.
 *
 * @property nome Nome do pilar
 * @property descricao Descrição do pilar
 * @property dataInicio Data de início (formato yyyy-MM-dd)
 * @property dataPrazo Data limite (formato yyyy-MM-dd)
 * @property status Status textual do pilar (ex: "EM_ANDAMENTO", "CONCLUIDO")
 * @property criadoPor Nome ou ID do criador do pilar
 * @property acoes Lista de ações pertencentes a este pilar
 */
data class PilarDTO(
  val nome: String,
  val descricao: String,
  val dataInicio: String,
  val dataPrazo: String,
  val status: String,
  val criadoPor: String,
  val acoes: List<AcaoDTO>
)
/**
 * Representação de uma Ação vinculada a um Pilar no relatório.
 *
 * Inclui nome, descrição, status e as atividades associadas.
 *
 * @property nome Nome da ação
 * @property descricao Descrição da ação
 * @property status Status da ação (ex: "EM_ANDAMENTO", "CONCLUIDA")
 * @property atividades Lista de atividades relacionadas à ação
 */
data class AcaoDTO(
  val nome: String,
  val descricao: String,
  val status: String,
  val atividades: List<AtividadeDTO>
)

/**
 * Representação de uma Atividade dentro de uma ação no relatório.
 *
 * @property nome Nome da atividade
 * @property descricao Descrição da atividade
 * @property status Status textual da atividade (ex: "EM_ANDAMENTO", "CONCLUIDA")
 * @property responsavel Nome ou ID do responsável vinculado à atividade
 */
data class AtividadeDTO(
  val nome: String,
  val descricao: String,
  val status: String,
  val responsavel: String
)
