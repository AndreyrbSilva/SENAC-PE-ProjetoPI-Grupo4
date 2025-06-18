package com.example.appsenkaspi

import android.app.Application
import androidx.room.Room
import com.example.appsenkaspi.data.local.database.AppDatabase

/**
 * Classe principal da aplicação Android.
 *
 * Estende [Application] e é responsável por inicializar e expor uma instância única do
 * banco de dados [AppDatabase] para uso em toda a aplicação.
 *
 * A instância do banco é criada com `Room.databaseBuilder` e usa
 * `.fallbackToDestructiveMigration()` como estratégia de migração (isto é,
 * em caso de mudança de versão sem estratégia de migração explícita, os dados são apagados).
 *
 * **Importante:** Certifique-se de registrar esta classe no `AndroidManifest.xml`:
 *
 * ```xml
 * <application
 *     android:name=".App"
 *     ...>
 * </application>
 * ```
 *
 * @property database Instância singleton do banco de dados Room, acessível por outros componentes da aplicação.
 *
 * @see AppDatabase
 * @see Room.databaseBuilder
 */
class App : Application() {

  /**
   * Instância pública e única do banco de dados da aplicação.
   * Inicializada durante o ciclo de vida da aplicação.
   */
  lateinit var database: AppDatabase
    private set

  /**
   * Método chamado quando a aplicação é criada.
   * Inicializa o banco de dados usando o Room.
   */
  override fun onCreate() {
    super.onCreate()
    database = Room.databaseBuilder(
      applicationContext,
      AppDatabase::class.java,
      "appsenkaspi-db"
    )
      .fallbackToDestructiveMigration()
      .build()
  }
}
