package com.example.appsenkaspi.data.local.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.appsenkaspi.data.local.enums.Cargo
import kotlinx.parcelize.Parcelize

/**
 * Representa um funcionário do sistema, podendo assumir diferentes cargos (Coordenador, Gestor, Apoio).
 *
 * Essa entidade é fundamental para o controle de permissões, autenticação e vinculação
 * de ações/atividades a usuários específicos.
 *
 * @property id Identificador único do funcionário (autogerado).
 * @property nomeCompleto Nome completo do funcionário.
 * @property email Endereço de e-mail para contato e autenticação.
 * @property cargo Enum definindo o tipo de cargo (COORDENADOR, GESTOR, APOIO).
 * @property fotoPerfil URL da foto de perfil do funcionário.
 * @property nomeUsuario Nome de usuário utilizado no login.
 * @property senha Senha de acesso (armazenada de forma não segura nesta estrutura).
 * @property idAcesso Campo adicional para controle interno de acesso, opcional.
 * @property numeroTel Número de telefone do funcionário.
 * @property fotoBanner URL da imagem de banner associada ao perfil do funcionário (opcional).
 */
@Parcelize
@Entity(tableName = "funcionarios")
data class FuncionarioEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Int = 0,

  val nomeCompleto: String,

  val email: String,

  @ColumnInfo(name = "cargo")
  val cargo: Cargo,

  val fotoPerfil: String,

  val nomeUsuario: String,

  val senha: String,

  @ColumnInfo(name = "id_acesso")
  val idAcesso: Int = 0,

  val numeroTel: String,

  val fotoBanner: String? = null
) : Parcelable
