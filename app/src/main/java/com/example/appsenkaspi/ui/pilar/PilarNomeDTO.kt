package com.example.appsenkaspi.ui.pilar

/**
 * Data Transfer Object (DTO) usado para representar apenas o ID e o nome de um Pilar.
 *
 * Essa classe é útil em contextos onde não é necessário carregar todos os campos da entidade
 * `PilarEntity`, como em listagens simplificadas, seletores ou filtros.
 *
 * @property id Identificador único do Pilar.
 * @property nome Nome do Pilar.
 */
data class PilarNomeDTO(
  val id: Int,
  val nome: String
)
