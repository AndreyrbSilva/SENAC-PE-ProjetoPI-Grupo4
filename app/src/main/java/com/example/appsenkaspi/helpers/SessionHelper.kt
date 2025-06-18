package com.example.appsenkaspi.helpers

import android.content.Context



/**
 * Recupera o ID do funcionário atualmente logado, armazenado nas SharedPreferences.
 *
 * @param context Contexto da aplicação necessário para acessar as SharedPreferences.
 * @return ID do funcionário logado ou -1 caso nenhum ID esteja armazenado.
 *
 * Utilizado para manter persistência de sessão e permitir operações autenticadas no app.
 */
fun getFuncionarioLogadoId(context: Context): Int {
  val prefs = context.getSharedPreferences("funcionario_prefs", Context.MODE_PRIVATE)
  return prefs.getInt("funcionario_id", -1)
}

/**
 * Armazena os dados de login do funcionário logado nas SharedPreferences.
 *
 * @param context Contexto da aplicação.
 * @param id Identificador único do funcionário.
 * @param nomeUsuario Nome de usuário utilizado para login.
 *
 * Esse método persiste a sessão do usuário entre execuções do app.
 */
fun salvarFuncionarioLogado(context: Context, id: Int, nomeUsuario: String) {
  val prefs = context.getSharedPreferences("funcionario_prefs", Context.MODE_PRIVATE)
  prefs.edit()
    .putInt("funcionario_id", id)
    .putString("funcionario_nomeUsuario", nomeUsuario)
    .apply()
}

/**
 * Recupera o nome de usuário do funcionário logado.
 *
 * @param context Contexto necessário para acessar as SharedPreferences.
 * @return Nome de usuário salvo ou null se não houver valor persistido.
 *
 * Útil para personalizar telas ou identificar ações do usuário autenticado.
 */
fun getFuncionarioNomeUsuario(context: Context): String? {
  val prefs = context.getSharedPreferences("funcionario_prefs", Context.MODE_PRIVATE)
  return prefs.getString("funcionario_nomeUsuario", null)
}

/**
 * Remove as informações de login do funcionário armazenadas localmente.
 *
 * @param context Contexto necessário para editar as SharedPreferences.
 *
 * Este método é normalmente utilizado durante o logout para limpar a sessão.
 */
fun limparFuncionarioLogado(context: Context) {
  val prefs = context.getSharedPreferences("funcionario_prefs", Context.MODE_PRIVATE)
  prefs.edit()
    .remove("funcionario_id")
    .remove("funcionario_nomeUsuario")
    .apply()
}

