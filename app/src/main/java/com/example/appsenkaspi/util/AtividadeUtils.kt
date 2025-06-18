package com.example.appsenkaspi.util


import com.example.appsenkaspi.data.local.entity.AtividadeEntity
import com.example.appsenkaspi.data.local.enums.StatusAtividade
import java.util.*

/**
 * Verifica se a atividade está vencida **neste momento**.
 *
 * A atividade é considerada vencida se:
 * - Sua data de prazo for anterior ao dia atual (ignora horas).
 * - Ela ainda **não estiver marcada como concluída**.
 *
 * @return `true` se a atividade estiver vencida e não concluída, `false` caso contrário.
 */
fun AtividadeEntity.estaVencidaAgora(): Boolean {
  val hoje = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }.time

  return dataPrazo?.before(hoje) == true && status != StatusAtividade.CONCLUIDA
}
