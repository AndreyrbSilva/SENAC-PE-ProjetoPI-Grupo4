package com.example.appsenkaspi.domain.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.appsenkaspi.data.local.entity.NotificacaoEntity
import com.example.appsenkaspi.data.local.entity.FuncionarioEntity

/**
 * Representa uma notificação com os dados completos do remetente associados.
 *
 * Essa estrutura permite acessar, a partir da entidade [NotificacaoEntity], os dados
 * do funcionário que enviou a notificação ([FuncionarioEntity]) de forma integrada,
 * facilitando a exibição em interfaces que exigem informações detalhadas do remetente.
 *
 * Útil principalmente em listas de notificações ou telas de detalhes onde o nome,
 * foto ou cargo do remetente precisam ser exibidos junto à mensagem.
 *
 * @property notificacao Instância da [NotificacaoEntity] contendo os dados da notificação.
 * @property remetente Instância de [FuncionarioEntity] correspondente ao remetente da notificação.
 */
data class NotificacaoComRemetente(
  @Embedded val notificacao: NotificacaoEntity,

  @Relation(
    parentColumn = "remetenteId",
    entityColumn = "id"
  )
  val remetente: FuncionarioEntity
)
