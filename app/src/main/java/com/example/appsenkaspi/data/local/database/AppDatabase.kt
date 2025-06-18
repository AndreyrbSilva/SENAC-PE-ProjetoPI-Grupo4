package com.example.appsenkaspi.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.appsenkaspi.data.local.dao.AtividadeDao
import com.example.appsenkaspi.data.local.dao.AtividadeFuncionarioDao
import com.example.appsenkaspi.data.local.entity.AtividadeFuncionarioEntity
import com.example.appsenkaspi.data.local.enums.Cargo
import com.example.appsenkaspi.data.local.dao.ChecklistDao
import com.example.appsenkaspi.data.local.entity.ChecklistItemEntity
import com.example.appsenkaspi.data.local.converter.Converters
import com.example.appsenkaspi.data.local.dao.FuncionarioDao
import com.example.appsenkaspi.data.local.entity.FuncionarioEntity
import com.example.appsenkaspi.data.local.dao.PilarDao
import com.example.appsenkaspi.data.local.entity.PilarEntity
import com.example.appsenkaspi.data.local.dao.RequisicaoDao
import com.example.appsenkaspi.data.local.entity.RequisicaoEntity
import com.example.appsenkaspi.data.local.dao.SubpilarDao
import com.example.appsenkaspi.data.local.entity.SubpilarEntity
import com.example.appsenkaspi.data.local.dao.AcaoDao
import com.example.appsenkaspi.data.local.dao.AcaoFuncionarioDao
import com.example.appsenkaspi.data.local.entity.AcaoEntity
import com.example.appsenkaspi.data.local.entity.AcaoFuncionarioEntity
import com.example.appsenkaspi.data.local.entity.AtividadeEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Classe abstrata que define a base de dados principal da aplicação usando Room.
 * Contém a configuração de entidades, conversores de tipo e DAOs.
 *
 * - Inclui entidades como Pilar, Subpilar, Ação, Atividade, Funcionário, Requisição, entre outras.
 * - Utiliza um singleton para garantir instância única da base de dados.
 * - Insere funcionários iniciais no banco de dados ao ser criado.
 */
@Database(
  entities = [
    PilarEntity::class,
    SubpilarEntity::class,
    AcaoEntity::class,
    AtividadeEntity::class,
    FuncionarioEntity::class,
    ChecklistItemEntity::class,
    AcaoFuncionarioEntity::class,
    AtividadeFuncionarioEntity::class,
    RequisicaoEntity::class
  ],
  version = 3,
  exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

  /** DAO para acessar dados dos pilares. */
  abstract fun pilarDao(): PilarDao

  /** DAO para acessar dados dos subpilares. */
  abstract fun subpilarDao(): SubpilarDao

  /** DAO para acessar dados das ações. */
  abstract fun acaoDao(): AcaoDao

  /** DAO para acessar dados das atividades. */
  abstract fun atividadeDao(): AtividadeDao

  /** DAO para acessar dados dos funcionários. */
  abstract fun funcionarioDao(): FuncionarioDao

  /** DAO para acessar os itens de checklist das atividades. */
  abstract fun checklistDao(): ChecklistDao

  /** DAO para acessar e manipular as requisições do sistema. */
  abstract fun requisicaoDao(): RequisicaoDao

  /** DAO para a relação N:N entre Ação e Funcionário. */
  abstract fun acaoFuncionarioDao(): AcaoFuncionarioDao

  /** DAO para a relação N:N entre Atividade e Funcionário. */
  abstract fun atividadeFuncionarioDao(): AtividadeFuncionarioDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    /**
     * Retorna a instância única do banco de dados.
     * Caso ainda não exista, cria e inicializa com dados de funcionários.
     */
    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "appsenkaspi.db"
        )
          .addCallback(object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
              super.onCreate(db)
              // Popula a tabela de funcionários com registros iniciais
              CoroutineScope(Dispatchers.IO).launch {
                val dao = getDatabase(context).funcionarioDao()
                dao.inserirTodos(
                  listOf(
                    FuncionarioEntity(
                      nomeCompleto = "Ana Beatriz Souza",
                      email = "ana.souza@example.com",
                      cargo = Cargo.COORDENADOR,
                      fotoPerfil = "https://i.pravatar.cc/150?img=1",
                      nomeUsuario = "ana.souza",
                      senha = "senha123",
                      idAcesso = 1,
                      numeroTel = "(84)91191-9291",
                      fotoBanner = ""
                    ),
                    FuncionarioEntity(
                      nomeCompleto = "Usuario Teste",
                      email = "usuario.teste@example.com",
                      cargo = Cargo.COORDENADOR,
                      fotoPerfil = "https://i.pravatar.cc/150?img=1",
                      nomeUsuario = "teste",
                      senha = "senha123",
                      idAcesso = 5,
                      numeroTel = "(84)91191-9291",
                      fotoBanner = ""
                    ),
                    FuncionarioEntity(
                      nomeCompleto = "Fernanda Oliveira",
                      email = "fernanda.oliveira@example.com",
                      cargo = Cargo.APOIO,
                      fotoPerfil = "https://i.pravatar.cc/150?img=3",
                      nomeUsuario = "fernanda.oliveira",
                      senha = "senha123",
                      idAcesso = 3,
                      numeroTel = "(84)91191-9291",
                      fotoBanner = ""
                    ),
                    FuncionarioEntity(
                      nomeCompleto = "Carlos Eduardo Silva",
                      email = "carlos.silva@example.com",
                      cargo = Cargo.GESTOR,
                      fotoPerfil = "https://i.pravatar.cc/150?img=2",
                      nomeUsuario = "carlos.silva",
                      senha = "senha123",
                      idAcesso = 2,
                      numeroTel = "(84)91191-9291",
                      fotoBanner = ""
                    ),
                    FuncionarioEntity(
                      nomeCompleto = "Eu mesmo",
                      email = "eumesmo.oliveira@example.com",
                      cargo = Cargo.APOIO,
                      fotoPerfil = "https://i.pravatar.cc/150?img=3",
                      nomeUsuario = "fernanda.oliveira",
                      senha = "senha123",
                      idAcesso = 4,
                      numeroTel = "(84)91191-9291",
                      fotoBanner = ""
                    )
                  )
                )
              }
            }
          })
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
