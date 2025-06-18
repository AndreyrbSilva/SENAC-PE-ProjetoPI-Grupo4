package com.example.appsenkaspi.ui.relatorio

/**
 * Representa um item do histórico de relatórios gerados ou recebidos pelo sistema.
 *
 * @property titulo Título do relatório exibido ao usuário.
 * @property data Data associada ao relatório (geralmente data de criação ou envio), formatada como String.
 * @property pilarNome Nome do pilar relacionado ao relatório, se aplicável. Pode ser nulo para relatórios gerais.
 * @property caminhoArquivo Caminho local do arquivo salvo no dispositivo, se houver.
 * @property tipoArquivo Tipo do arquivo (por exemplo, "PDF", "CSV", "DOCX").
 * @property urlArquivo URL de acesso ao arquivo remoto, se estiver armazenado na nuvem.
 */
data class HistoricoRelatorio(
  val titulo: String,
  val data: String,
  val pilarNome: String? = null,
  val caminhoArquivo: String? = null,
  val tipoArquivo: String,
  val urlArquivo: String? = null
)
