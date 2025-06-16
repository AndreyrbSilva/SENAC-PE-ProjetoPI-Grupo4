package com.example.appsenkaspi.workers


import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.appsenkaspi.data.local.database.AppDatabase
import com.example.appsenkaspi.ui.atividade.AtividadeRepository


/**
 * Worker responsável por verificar e atualizar notificações de prazos e status de atividades vencidas.
 *
 * Esta classe é executada periodicamente pelo WorkManager e utiliza `CoroutineWorker` para
 * realizar tarefas em background com suporte a corrotinas. O objetivo principal é:
 *
 * 1. Emitir notificações para atividades com prazos próximos (D-30, D-15, D-7, D-1, etc.).
 * 2. Atualizar o status de atividades cujo prazo foi ultrapassado para `VENCIDA`.
 *
 * A lógica de verificação e envio é delegada ao `AtividadeRepository`, promovendo separação de responsabilidades.
 *
 * @param context O contexto da aplicação.
 * @param workerParams Parâmetros fornecidos automaticamente pelo WorkManager.
 *
 * @return `Result.success()` se tudo ocorreu normalmente; `Result.failure()` em caso de exceções.
 *
 * @see AtividadeRepository.verificarNotificacoesDePrazo
 * @see AtividadeRepository.verificarAtividadesVencidas
 */
class VerificacaoDePrazosWorker(
  context: Context,
  workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

  /**
   * Método chamado automaticamente pelo WorkManager.
   * Executa verificações de prazo e atualização de status em segundo plano.
   */
  override suspend fun doWork(): Result {
    val db = AppDatabase.getDatabase(applicationContext)

    val atividadeRepository = AtividadeRepository(
      context = applicationContext,
      atividadeDao = db.atividadeDao(),
      atividadeFuncionarioDao = db.atividadeFuncionarioDao(),
      requisicaoDao = db.requisicaoDao()
    )

    return try {
      atividadeRepository.verificarNotificacoesDePrazo()
      atividadeRepository.verificarAtividadesVencidas()
      Result.success()
    } catch (e: Exception) {
      Log.e("Worker", "Erro ao verificar prazos", e)
      Result.failure()
    }
  }
}

