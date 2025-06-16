package com.example.appsenkaspi.ui.acao

import com.example.appsenkaspi.data.local.enums.StatusAcao
import java.util.Date

/**
 * Representa uma Ação em formato serializado para comunicação (requisições, notificações ou persistência temporária).
 *
 * Utilizado especialmente em requisições que envolvem criação ou edição de ações antes da confirmação pelo coordenador.
 *
 * @property id Identificador único da ação; obrigatório para atualizações.
 * @property responsaveis Lista de IDs dos funcionários responsáveis pela ação.
 * @property nome Nome da ação.
 * @property descricao Descrição detalhada da ação.
 * @property dataInicio Data prevista de início da ação.
 * @property dataPrazo Data limite para a conclusão da ação.
 * @property status Status atual da ação (planejada, em andamento, concluída, etc.).
 * @property criadoPor ID do funcionário que criou a ação.
 * @property dataCriacao Data em que a ação foi criada.
 * @property nomePilar Nome do pilar associado à ação (usado para exibição).
 * @property pilarId ID do pilar pai da ação (se aplicável).
 * @property subpilarId ID do subpilar associado à ação, se a estrutura hierárquica exigir.
 */
data class AcaoJson(
  val id: Int? = null,
  val responsaveis: List<Int>,
  val nome: String,
  val descricao: String,
  val dataInicio: Date,
  val dataPrazo: Date,
  val status: StatusAcao,
  val criadoPor: Int,
  val dataCriacao: Date,
  val nomePilar: String,
  val pilarId: Int? = null,
  val subpilarId: Int? = null
)
