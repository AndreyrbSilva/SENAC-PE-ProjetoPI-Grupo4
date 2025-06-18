package com.example.appsenkaspi.data.local.enums



// Cargo do funcionário
enum class Cargo {
  COORDENADOR,
  GESTOR,
  APOIO
}

// Status de um Pilar
enum class StatusPilar {
  PLANEJADO,
  EM_ANDAMENTO,
  CONCLUIDO,
  EXCLUIDO,
  VENCIDO
}

// Status de uma Ação
enum class StatusAcao {
  PLANEJADA,
  EM_ANDAMENTO,
  CONCLUIDA,
  EXCLUIDA,
  VENCIDA
}

// Status de uma Atividade
enum class StatusAtividade {
  PENDENTE,
  EM_ANDAMENTO,
  CONCLUIDA,
  EXCLUIDA,
  VENCIDA
}

// Prioridade da Atividade
enum class PrioridadeAtividade {
  ALTA,
  MEDIA,
  BAIXA
}

// Status de uma Requisição
enum class StatusRequisicao {
  PENDENTE,
  ACEITA,
  RECUSADA
}

// Tipo de Requisição que pode ser feita no sistema
enum class TipoRequisicao {
  CRIAR_ATIVIDADE,
  EDITAR_ATIVIDADE,
  COMPLETAR_ATIVIDADE,
  CRIAR_ACAO,
  EDITAR_ACAO,
  ATIVIDADE_PARA_VENCER,
  ATIVIDADE_VENCIDA,
  ATIVIDADE_CONCLUIDA,
  PRAZO_ALTERADO,
  RESPONSAVEL_ADICIONADO,
  RESPONSAVEL_REMOVIDO
}

// Status de um Subpilar
enum class StatusSubPilar {
  PLANEJADO,
  EM_ANDAMENTO,
  CONCLUIDO,
  VENCIDO,
  EXCLUIDO
}

// Status de uma Notificação
enum class StatusNotificacao {
  NAO_LIDA,
  LIDA,
  ARQUIVIDA,
  NOVA
}

// Tipo de Notificação (enviada para coordenador ou apoio)
enum class TipoDeNotificacao {

  // Notificações enviadas ao Coordenador
  PEDIDO_CRIACAO_ACAO,
  PEDIDO_CRIACAO_ATIVIDADE,
  PEDIDO_EDICAO_ACAO,
  PEDIDO_EDICAO_ATIVIDADE,
  PEDIDO_CONFIRMACAO_ATIVIDADE,

  // Notificações enviadas ao Apoio
  CRIACAO_ACAO_ACEITA,
  CRIACAO_ATIVIDADE_ACEITA,
  EDICAO_ACAO_ACEITA,
  EDICAO_ATIVIDADE_ACEITA,
  CONFIRMACAO_ATIVIDADE_ACEITA
}
