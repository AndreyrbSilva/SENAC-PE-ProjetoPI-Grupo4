package com.example.appsenkaspi.ui.subpilares

import com.example.appsenkaspi.data.local.dao.AcaoDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Objeto utilitário responsável por calcular o progresso de um subpilar
 * com base no progresso agregado de suas ações e atividades associadas.
 */
object ProgressoSubpilar {

  /**
   * Calcula o progresso percentual de um subpilar de forma ponderada.
   *
   * O progresso é obtido como a média ponderada dos progressos das ações
   * vinculadas ao subpilar, ponderadas pelo número de atividades de cada ação.
   *
   * Fórmula:
   * ```
   * progresso = soma(por_acao: progresso * totalAtividades) / soma(totalAtividades)
   * ```
   *
   * Exemplo: Se uma ação tem progresso 1.0 com 2 atividades e outra tem 0.5 com 4 atividades,
   * o progresso será: `(1.0*2 + 0.5*4)/(2+4) = (2+2)/6 = 0.66`
   *
   * @param subpilarId ID do subpilar cujo progresso será calculado.
   * @param acaoDao DAO responsável por fornecer os dados agregados por ação.
   * @return Progresso percentual entre 0f (nenhum progresso) e 1f (completo).
   */
  suspend fun calcularProgressoDoSubpilarInterno(
    subpilarId: Int,
    acaoDao: AcaoDao
  ): Float = withContext(Dispatchers.IO) {
    val lista = acaoDao.listarProgressoPorSubpilar(subpilarId)

    // Acumula soma ponderada dos progressos e total de atividades
    val (somaPesos, somaTotalAtividades) = lista.fold(0f to 0) { acc, item ->
      val pesoAtual = item.progresso * item.totalAtividades
      (acc.first + pesoAtual) to (acc.second + item.totalAtividades)
    }

    // Calcula a média ponderada se houver atividades; senão, retorna 0
    if (somaTotalAtividades > 0) {
      (somaPesos / somaTotalAtividades).coerceIn(0f, 1f)
    } else {
      0f
    }
  }
}
