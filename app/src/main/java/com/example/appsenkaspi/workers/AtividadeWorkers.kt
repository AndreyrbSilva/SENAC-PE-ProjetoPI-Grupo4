package com.example.appsenkaspi.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.appsenkaspi.data.local.database.AppDatabase
import com.example.appsenkaspi.ui.atividade.AtividadeRepository

/**
 * Worker que verifica periodicamente se há atividades vencidas e atualiza seus status no banco de dados.
 *
 * Esta classe é executada de forma assíncrona com `WorkManager` e utiliza `CoroutineWorker` para
 * acessar o banco de dados via corrotinas. É ideal para agendamentos periódicos como tarefas em background
 * relacionadas a prazos e notificações.
 *
 * @param context Contexto da aplicação.
 * @param workerParams Parâmetros de configuração fornecidos pelo `WorkManager`.
 *
 * @see AtividadeRepository.verificarAtividadesVencidas
 */
class VerificarAtividadesVencidasWorker(
  context: Context,
  workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

  /**
   * Executa a verificação de atividades vencidas chamando o método correspondente no repositório.
   * Este método será chamado automaticamente pelo WorkManager conforme a periodicidade agendada.
   *
   * @return Resultado do trabalho: sucesso em caso de execução sem erros.
   */
  override suspend fun doWork(): Result {
    // Inicializa instância do banco de dados
    val db = AppDatabase.getDatabase(applicationContext)

    // Cria repositório com DAOs necessários para verificação
    val repository = AtividadeRepository(
      context = applicationContext,
      atividadeDao = db.atividadeDao(),
      atividadeFuncionarioDao = db.atividadeFuncionarioDao(),
      requisicaoDao = db.requisicaoDao()
    )

    // Executa verificação de prazos vencidos
    repository.verificarAtividadesVencidas()

    // Retorna sucesso para o WorkManager
    return Result.success()
  }
}
