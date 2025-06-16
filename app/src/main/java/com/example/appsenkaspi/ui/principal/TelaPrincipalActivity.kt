package com.example.appsenkaspi.ui.principal

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.appsenkaspi.R
import com.example.appsenkaspi.data.local.database.AppDatabase
import com.example.appsenkaspi.databinding.ActivityTelaPrincipalBinding
import com.example.appsenkaspi.ui.home.HomeFragment
import com.example.appsenkaspi.ui.main.MainActivity
import com.example.appsenkaspi.ui.notificacao.DetalheNotificacaoFragment
import com.example.appsenkaspi.ui.notificacao.NotificacaoFragment
import com.example.appsenkaspi.util.NotificationUtils
import com.example.appsenkaspi.viewmodel.AtividadeViewModel
import com.example.appsenkaspi.viewmodel.FuncionarioViewModel
import com.example.appsenkaspi.workers.VerificacaoDePrazosWorker
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Tela principal da aplicação após login.
 *
 * Responsável por:
 * - Verificar permissões de notificação e alarmes
 * - Carregar os dados do funcionário logado a partir do `SharedPreferences`
 * - Agendar verificação diária de prazos usando WorkManager
 * - Redirecionar para a `MainActivity` caso não haja funcionário logado
 * - Carregar dinamicamente o fragmento inicial com base na intenção recebida
 */
class TelaPrincipalActivity : AppCompatActivity() {

  private lateinit var binding: ActivityTelaPrincipalBinding
  private lateinit var funcionarioViewModel: FuncionarioViewModel
  private val atividadeViewModel: AtividadeViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    // Criação dos canais de notificação para Android 8+
    NotificationUtils.criarCanais(applicationContext)

    // Agendamento da verificação de prazos diários (por volta das 8h)
    agendarVerificacaoDePrazosDiaria(applicationContext)

    super.onCreate(savedInstanceState)

    // Verifica e agenda notificações de atividades com prazos próximos
    atividadeViewModel.checarPrazos()

    // Solicita permissão de notificação em Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
      ) {
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
      }
    }

    // Verifica se a permissão para alarmes exatos está habilitada (Android 12+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
      if (!alarmManager.canScheduleExactAlarms()) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
          data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
      }
    }

    // Infla o layout principal
    binding = ActivityTelaPrincipalBinding.inflate(layoutInflater)
    setContentView(binding.root)

    // Recupera o ID do funcionário salvo localmente
    val prefs = getSharedPreferences("funcionario_prefs", MODE_PRIVATE)
    val funcionarioId = prefs.getInt("funcionario_id", -1)

    // Se não houver login salvo, retorna à tela inicial
    if (funcionarioId == -1) {
      startActivity(Intent(this, MainActivity::class.java))
      finish()
      return
    }

    // Inicializa o ViewModel do funcionário
    funcionarioViewModel = ViewModelProvider(this)[FuncionarioViewModel::class.java]

    lifecycleScope.launch {
      // Recupera o funcionário logado do banco
      val funcionario = AppDatabase.getDatabase(this@TelaPrincipalActivity)
        .funcionarioDao()
        .buscarPorId(funcionarioId)

      if (funcionario != null) {
        funcionarioViewModel.logarFuncionario(funcionario)
      } else {
        prefs.edit().clear().apply()
        startActivity(Intent(this@TelaPrincipalActivity, MainActivity::class.java))
        finish()
        return@launch
      }

      // Trata intent recebida, se houver
      val requisicaoId = intent.getIntExtra("requisicao_id", -1)
      val abrirNotificacoes = intent.getBooleanExtra("abrir_notificacoes", false)

      // Carrega fragmento inicial com base nos dados da intent
      if (savedInstanceState == null) {
        supportFragmentManager.popBackStack(
          null,
          FragmentManager.POP_BACK_STACK_INCLUSIVE
        )

        val fragment = when {
          abrirNotificacoes && requisicaoId != -1 -> DetalheNotificacaoFragment().apply {
            arguments = Bundle().apply { putInt("requisicaoId", requisicaoId) }
          }
          abrirNotificacoes -> NotificacaoFragment()
          else -> HomeFragment()
        }

        supportFragmentManager.beginTransaction()
          .replace(R.id.main_container, fragment)
          .commit()
      }
    }
  }

  /**
   * Calcula o tempo em milissegundos até as 08:00 da manhã do próximo dia.
   * Usado para agendar tarefas com `WorkManager` de forma sincronizada com o relógio biológico diário.
   */
  private fun calcularDelayPara8h(): Long {
    val agora = LocalDateTime.now()
    val proximaExecucao = agora.withHour(8).withMinute(0).withSecond(0)
      .let { if (it.isBefore(agora)) it.plusDays(1) else it }

    return Duration.between(agora, proximaExecucao).toMillis()
  }

  /**
   * Agenda uma verificação periódica de prazos com `WorkManager`, para ser executada diariamente às 08h.
   * Substitui qualquer agendamento anterior com a mesma chave (`VerificacaoDePrazosDiaria`).
   *
   * @param context Contexto necessário para inicializar o WorkManager.
   */
  private fun agendarVerificacaoDePrazosDiaria(context: Context) {
    val delay = calcularDelayPara8h()

    val workRequest = PeriodicWorkRequestBuilder<VerificacaoDePrazosWorker>(1, TimeUnit.DAYS)
      .setInitialDelay(delay, TimeUnit.MILLISECONDS)
      .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
      "VerificacaoDePrazosDiaria",
      ExistingPeriodicWorkPolicy.UPDATE,
      workRequest
    )
  }
}
