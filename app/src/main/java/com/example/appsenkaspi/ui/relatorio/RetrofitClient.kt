package com.example.appsenkaspi.ui.relatorio

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Cliente HTTP singleton responsável por configurar e expor a instância da API de relatórios.
 *
 * Utiliza Retrofit com Gson para serialização JSON e OkHttp com logging completo para debug.
 *
 * Base URL: https://Matheusmoura19.pythonanywhere.com
 *
 * A instância da API [RelatorioApiService] é criada de forma preguiçosa e reutilizável.
 */
object RetrofitClient {

  /** URL base da API externa responsável pela geração e fornecimento de relatórios. */
  private const val BASE_URL = "https://Matheusmoura19.pythonanywhere.com"

  /**
   * Instância única da interface [RelatorioApiService], pronta para consumo.
   *
   * Configura:
   * - Retrofit com Gson
   * - OkHttp com logging de corpo de requisição/resposta
   */
  val apiService: RelatorioApiService by lazy {
    val logging = HttpLoggingInterceptor().apply {
      level = HttpLoggingInterceptor.Level.BODY
    }

    val client = OkHttpClient.Builder()
      .addInterceptor(logging)
      .build()

    Retrofit.Builder()
      .baseUrl(BASE_URL)
      .client(client)
      .addConverterFactory(GsonConverterFactory.create())
      .build()
      .create(RelatorioApiService::class.java)
  }
}
