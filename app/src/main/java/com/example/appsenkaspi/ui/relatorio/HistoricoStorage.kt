package com.example.appsenkaspi.ui.relatorio

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Utilitário para persistência local do histórico de relatórios por usuário.
 *
 * Usa `SharedPreferences` para armazenar e recuperar listas de [HistoricoRelatorio]
 * serializadas em JSON via Gson.
 */
object HistoricoStorage {

  /** Nome do arquivo de preferências utilizado para salvar os históricos. */
  private const val PREFS_NAME = "historico_prefs"

  /**
   * Salva uma lista de históricos de relatório no armazenamento local (SharedPreferences).
   *
   * A lista é associada a um identificador de usuário específico, permitindo histórico individualizado.
   *
   * @param context Contexto da aplicação.
   * @param historico Lista de objetos [HistoricoRelatorio] a ser salva.
   * @param usuario Identificador único do usuário (ex: login, ID).
   */
  fun salvar(context: Context, historico: List<HistoricoRelatorio>, usuario: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val editor = prefs.edit()
    val gson = Gson()
    val json = gson.toJson(historico)
    editor.putString("historico_$usuario", json)
    editor.apply()
  }

  /**
   * Carrega a lista de históricos de relatório armazenada localmente para um usuário específico.
   *
   * @param context Contexto da aplicação.
   * @param usuario Identificador único do usuário (ex: login, ID).
   * @return Lista de [HistoricoRelatorio] recuperada, ou uma lista vazia se não houver dados.
   */
  fun carregar(context: Context, usuario: String): List<HistoricoRelatorio> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val gson = Gson()
    val json = prefs.getString("historico_$usuario", null)
    return if (json != null) {
      val type = object : TypeToken<List<HistoricoRelatorio>>() {}.type
      gson.fromJson(json, type)
    } else {
      emptyList()
    }
  }
}
