package com.example.appsenkaspi.data.local.converter

import androidx.room.TypeConverter
import com.example.appsenkaspi.data.local.enums.Cargo
import com.example.appsenkaspi.data.local.enums.PrioridadeAtividade
import com.example.appsenkaspi.data.local.enums.StatusAcao
import com.example.appsenkaspi.data.local.enums.StatusAtividade
import com.example.appsenkaspi.data.local.enums.StatusPilar
import com.example.appsenkaspi.data.local.enums.StatusRequisicao
import com.example.appsenkaspi.data.local.enums.StatusSubPilar
import com.example.appsenkaspi.data.local.enums.TipoRequisicao
import com.example.appsenkaspi.data.local.entity.AcaoEntity
import com.example.appsenkaspi.data.local.entity.AtividadeEntity
import com.google.gson.Gson
import java.util.Date

/**
 * Classe que centraliza todos os conversores personalizados utilizados pelo Room
 * para persistência de tipos complexos que não são suportados diretamente pelo banco de dados SQLite.
 *
 * Essa abordagem permite mapear enums, datas e objetos complexos (via JSON) para tipos primitivos compatíveis.
 */
class Converters {

  // ---------- CONVERSORES DE DATA ----------

  /**
   * Converte um timestamp (Long) para um objeto Date.
   *
   * Utilizado para armazenar datas em formato longo no banco.
   */
  @TypeConverter
  fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

  /**
   * Converte um objeto Date para timestamp (Long).
   *
   * Permite a persistência de datas como inteiros no banco.
   */
  @TypeConverter
  fun dateToTimestamp(date: Date?): Long? = date?.time

  // ---------- ENUM: Cargo ----------

  /**
   * Converte enum Cargo para String minúscula.
   */
  @TypeConverter
  fun fromCargo(cargo: Cargo): String = cargo.name.lowercase()

  /**
   * Converte String para enum Cargo. Caso inválido, retorna APOIO.
   */
  @TypeConverter
  fun toCargo(value: String): Cargo = try {
    Cargo.valueOf(value.uppercase())
  } catch (e: IllegalArgumentException) {
    Cargo.APOIO
  }

  // ---------- ENUM: StatusPilar ----------

  @TypeConverter
  fun fromStatusPilar(status: StatusPilar): String = status.name.lowercase()

  @TypeConverter
  fun toStatusPilar(value: String): StatusPilar = try {
    StatusPilar.valueOf(value.uppercase())
  } catch (e: IllegalArgumentException) {
    StatusPilar.PLANEJADO
  }

  // ---------- ENUM: StatusSubPilar ----------

  @TypeConverter
  fun fromStatusSubPilar(status: StatusSubPilar): String = status.name.lowercase()

  @TypeConverter
  fun toStatusSubPilar(value: String): StatusSubPilar = try {
    StatusSubPilar.valueOf(value.uppercase())
  } catch (e: IllegalArgumentException) {
    StatusSubPilar.PLANEJADO
  }

  // ---------- ENUM: StatusAcao ----------

  @TypeConverter
  fun fromStatusAcao(status: StatusAcao): String = status.name.lowercase()

  @TypeConverter
  fun toStatusAcao(value: String): StatusAcao = try {
    StatusAcao.valueOf(value.uppercase())
  } catch (e: IllegalArgumentException) {
    StatusAcao.PLANEJADA
  }

  // ---------- ENUM: StatusAtividade ----------

  @TypeConverter
  fun fromStatusAtividade(status: StatusAtividade): String = status.name.lowercase()

  @TypeConverter
  fun toStatusAtividade(value: String): StatusAtividade = try {
    StatusAtividade.valueOf(value.uppercase())
  } catch (e: IllegalArgumentException) {
    StatusAtividade.PENDENTE
  }

  // ---------- ENUM: PrioridadeAtividade ----------

  @TypeConverter
  fun fromPrioridade(prioridade: PrioridadeAtividade): String = prioridade.name.lowercase()

  @TypeConverter
  fun toPrioridade(value: String): PrioridadeAtividade = try {
    PrioridadeAtividade.valueOf(value.uppercase())
  } catch (e: IllegalArgumentException) {
    PrioridadeAtividade.MEDIA
  }

  // ---------- ENUM: StatusRequisicao ----------

  @TypeConverter
  fun fromStatusRequisicao(status: StatusRequisicao): String = status.name.lowercase()

  @TypeConverter
  fun toStatusRequisicao(value: String): StatusRequisicao = try {
    StatusRequisicao.valueOf(value.uppercase())
  } catch (e: IllegalArgumentException) {
    StatusRequisicao.PENDENTE
  }

  // ---------- ENUM: TipoRequisicao ----------

  @TypeConverter
  fun fromTipoRequisicao(tipo: TipoRequisicao): String = tipo.name.lowercase()

  @TypeConverter
  fun toTipoRequisicao(value: String): TipoRequisicao = try {
    TipoRequisicao.valueOf(value.uppercase())
  } catch (e: IllegalArgumentException) {
    TipoRequisicao.COMPLETAR_ATIVIDADE
  }

  // ---------- JSON: Entidades complexas ----------

  private val gson = Gson()

  /**
   * Converte objeto AcaoEntity para JSON.
   *
   * Pode ser útil em campos de string no banco contendo a entidade serializada.
   */
  @TypeConverter
  fun fromAcao(acao: AcaoEntity?): String? = gson.toJson(acao)

  /**
   * Converte JSON para objeto AcaoEntity.
   */
  @TypeConverter
  fun toAcao(json: String?): AcaoEntity? = json?.let {
    gson.fromJson(it, AcaoEntity::class.java)
  }

  /**
   * Converte objeto AtividadeEntity para JSON.
   */
  @TypeConverter
  fun fromAtividade(atividade: AtividadeEntity?): String? = gson.toJson(atividade)

  /**
   * Converte JSON para objeto AtividadeEntity.
   */
  @TypeConverter
  fun toAtividade(json: String?): AtividadeEntity? = json?.let {
    gson.fromJson(it, AtividadeEntity::class.java)
  }
}
