package com.example.appsenkaspi.ui.relatorio

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Serviço de API Retrofit responsável pela geração de relatórios nos formatos PDF, Word e Excel.
 *
 * Cada método envia um corpo do tipo [RelatorioRequest] e recebe a resposta como um arquivo binário ([ResponseBody]).
 *
 * Os endpoints são esperados em formato REST com conteúdo JSON.
 */
interface RelatorioApiService {

  /**
   * Gera um relatório em formato PDF a partir dos dados enviados.
   *
   * @param dados Dados estruturados da requisição que definem o conteúdo do relatório.
   * @return Resposta contendo o corpo binário do arquivo PDF.
   */
  @Headers("Content-Type: application/json")
  @POST("/relatorio/pdf")
  suspend fun gerarPdf(@Body dados: RelatorioRequest): Response<ResponseBody>

  /**
   * Gera um relatório em formato Word (DOCX) a partir dos dados enviados.
   *
   * @param dados Dados estruturados da requisição que definem o conteúdo do relatório.
   * @return Resposta contendo o corpo binário do arquivo Word.
   */
  @Headers("Content-Type: application/json")
  @POST("/relatorio/word")
  suspend fun gerarWord(@Body dados: RelatorioRequest): Response<ResponseBody>

  /**
   * Gera um relatório em formato Excel (XLSX) a partir dos dados enviados.
   *
   * @param dados Dados estruturados da requisição que definem o conteúdo do relatório.
   * @return Resposta contendo o corpo binário do arquivo Excel.
   */
  @Headers("Content-Type: application/json")
  @POST("/relatorio/excel")
  suspend fun gerarExcel(@Body dados: RelatorioRequest): Response<ResponseBody>
}
