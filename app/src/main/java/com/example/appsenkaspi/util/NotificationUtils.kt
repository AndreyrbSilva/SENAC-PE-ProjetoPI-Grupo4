package com.example.appsenkaspi.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.appsenkaspi.R

/**
 * Utilitário responsável por configurar e exibir notificações no aplicativo.
 *
 * Este objeto contém métodos para:
 * - Criar canais de notificação (necessário no Android 8+)
 * - Exibir notificações com base em canal, título, mensagem e prioridade
 *
 * Canais utilizados:
 * - [CANAL_SISTEMA]: Para eventos gerais do sistema (ex: criação, edição, conclusões)
 * - [CANAL_PRAZO]: Para alertas relacionados a prazos de atividades
 */
object NotificationUtils {

  /** Canal usado para notificações gerais do sistema */
  const val CANAL_SISTEMA = "sistema_notificacoes"

  /** Canal usado especificamente para notificações de prazos */
  const val CANAL_PRAZO = "canal_prazo"

  private const val NOME_CANAL_SISTEMA = "Notificações do App Senkaspi"
  private const val NOME_CANAL_PRAZO = "Notificações de Prazo"

  /**
   * Cria os canais de notificação do app se estiver em Android 8.0 (API 26) ou superior.
   *
   * Isso garante que notificações sejam entregues com a prioridade e configurações apropriadas.
   *
   * @param context Contexto necessário para acessar o serviço de notificações.
   */
  fun criarCanais(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

      val canalSistema = NotificationChannel(
        CANAL_SISTEMA,
        NOME_CANAL_SISTEMA,
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Notificações automáticas do sistema (conclusão, alteração, etc.)"
        enableLights(true)
        lightColor = Color.BLUE
        enableVibration(true)
      }

      val canalPrazo = NotificationChannel(
        CANAL_PRAZO,
        NOME_CANAL_PRAZO,
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Notificações sobre prazos de atividades"
        enableLights(true)
        lightColor = Color.RED
        enableVibration(true)
      }

      manager.createNotificationChannel(canalSistema)
      manager.createNotificationChannel(canalPrazo)
    }
  }

  /**
   * Exibe uma notificação personalizada ao usuário, respeitando o canal e permissões exigidas.
   *
   * Para Android 13+ (API 33), verifica dinamicamente a permissão [Manifest.permission.POST_NOTIFICATIONS].
   *
   * @param context Contexto usado para construção e exibição da notificação.
   * @param titulo Título visível da notificação.
   * @param mensagem Corpo da mensagem exibida na notificação.
   * @param id ID único da notificação (para atualização ou substituição posterior).
   * @param canalId ID do canal de notificação. Por padrão, utiliza o [CANAL_SISTEMA].
   */
  fun mostrarNotificacao(
    context: Context,
    titulo: String,
    mensagem: String,
    id: Int,
    canalId: String = CANAL_SISTEMA
  ) {
    // Verifica se há permissão para notificar (requerida no Android 13+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        return
      }
    }

    // Define ícone de acordo com o tipo de canal
    val icon = when (canalId) {
      CANAL_PRAZO -> R.drawable.ic_calendar
      else -> R.drawable.logo
    }

    val builder = NotificationCompat.Builder(context, canalId)
      .setSmallIcon(icon)
      .setContentTitle(titulo)
      .setContentText(mensagem)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)

    NotificationManagerCompat.from(context).notify(id, builder.build())
  }
}
