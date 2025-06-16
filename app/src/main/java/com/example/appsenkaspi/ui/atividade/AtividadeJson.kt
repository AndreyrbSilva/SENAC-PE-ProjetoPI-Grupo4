package com.example.appsenkaspi.ui.atividade

import com.example.appsenkaspi.data.local.enums.PrioridadeAtividade
import com.example.appsenkaspi.data.local.enums.StatusAtividade
import java.util.Date

/**
 * Estrutura de dados serializável usada para enviar ou receber informações de uma atividade
 * em operações de criação, edição ou requisição no sistema.
 *
 * Pode ser utilizada em conjunto com APIs, Room, Firebase ou qualquer sistema de persistência/transmissão.
 *
 * @property id Identificador único da atividade. Obrigatório para atualizações.
 * @property nome Título da atividade.
 * @property descricao Descrição detalhada do objetivo ou escopo da atividade.
 * @property dataInicio Data de criação/início da atividade.
 * @property dataPrazo Data limite para conclusão da atividade.
 * @property acaoId Identificador da ação à qual a atividade pertence.
 * @property status Estado atual da atividade (ex: PLANEJADA, EM_ANDAMENTO, CONCLUIDA).
 * @property prioridade Nível de prioridade da atividade (BAIXA, MÉDIA, ALTA).
 * @property criadoPor ID do funcionário que criou a atividade.
 * @property dataCriacao Data e hora em que a atividade foi registrada.
 * @property nomePilar Nome do Pilar ou Subpilar associado (usado para exibição contextual).
 * @property responsaveis Lista de IDs dos funcionários responsáveis pela atividade.
 */
data class AtividadeJson(
  val id: Int? = null,
  val nome: String,
  val descricao: String,
  val dataInicio: Date,
  val dataPrazo: Date,
  val acaoId: Int,
  val status: StatusAtividade,
  val prioridade: PrioridadeAtividade,
  val criadoPor: Int,
  val dataCriacao: Date,
  val nomePilar: String,
  val responsaveis: List<Int>
)
