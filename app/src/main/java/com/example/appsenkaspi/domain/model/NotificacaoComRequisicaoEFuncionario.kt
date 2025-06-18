package com.example.appsenkaspi.domain.model

import com.example.appsenkaspi.ui.requisicao.DadosRequisicao
import androidx.room.Embedded
import androidx.room.Relation
import com.example.appsenkaspi.data.local.entity.NotificacaoEntity
import com.example.appsenkaspi.data.local.entity.RequisicaoEntity
import com.example.appsenkaspi.data.local.entity.FuncionarioEntity

/**
 * Representa uma notificação completa com os dados da requisição associada e
 * os funcionários envolvidos (remetente e destinatário).
 *
 * Essa estrutura é usada para exibir notificações que envolvem requisições
 * (como criação, edição ou conclusão de ações/atividades) e permite o acesso
 * direto aos dados do remetente e destinatário da mensagem, além da entidade
 * de requisição relacionada.
 *
 * Também inclui o campo [dadosAcaoOuAtividade], que pode ser preenchido
 * dinamicamente no ViewModel para exibir os dados deserializados da ação
 * ou atividade vinculada.
 *
 * @property notificacao Instância da entidade [NotificacaoEntity].
 * @property requisicao Instância da entidade [RequisicaoEntity] associada à notificação.
 * @property remetente Funcionário que enviou a notificação.
 * @property destinatario Funcionário que recebeu a notificação (opcional).
 * @property dadosAcaoOuAtividade Dados deserializados da ação ou atividade, preenchidos em tempo de execução.
 */
data class NotificacaoComRequisicaoEFuncionario(
  @Embedded val notificacao: NotificacaoEntity,

  @Relation(
    parentColumn = "requisicaoId",
    entityColumn = "id"
  )
  val requisicao: RequisicaoEntity,

  @Relation(
    parentColumn = "remetenteId",
    entityColumn = "id"
  )
  val remetente: FuncionarioEntity,

  @Relation(
    parentColumn = "destinatarioId",
    entityColumn = "id"
  )
  val destinatario: FuncionarioEntity? = null, // opcional

  @Transient
  var dadosAcaoOuAtividade: DadosRequisicao? = null
)
