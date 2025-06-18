package com.example.appsenkaspi.extensions

import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.example.appsenkaspi.data.local.enums.Cargo
import com.example.appsenkaspi.ui.notificacao.NotificacaoFragment
import com.example.appsenkaspi.viewmodel.NotificacaoViewModel
import com.example.appsenkaspi.R

/**
 * Configura o comportamento do botão de voltar em um `Fragment`.
 *
 * Este método deve ser chamado no `onViewCreated()` de um fragmento,
 * e assume que existe um `ImageView` com o ID `IconVoltar` na hierarquia de `view`.
 *
 * @receiver Fragment onde o botão está inserido.
 * @param view A raiz da view inflada no fragmento.
 */
fun Fragment.configurarBotaoVoltar(view: View) {
  val botao = view.findViewById<ImageView>(R.id.IconVoltar)
  botao?.setOnClickListener {
    parentFragmentManager.popBackStack()
  }
}

/**
 * Configura o ícone de notificações e o badge numérico visível na top bar do aplicativo.
 *
 * O comportamento varia de acordo com o cargo:
 * - Coordenadores veem a soma de notificações de requisição pendente e de prazo (a vencer ou vencidas).
 * - Apoio e outros cargos veem apenas o número de notificações ainda não vistas.
 *
 * @param rootView A view raiz onde o ícone e o badge estão localizados.
 * @param lifecycleOwner Ciclo de vida associado ao fragmento ou atividade.
 * @param fragmentManager Usado para substituir o fragmento atual pelo `NotificacaoFragment` ao clicar no ícone.
 * @param funcionarioId ID do funcionário autenticado.
 * @param cargo Cargo do funcionário (COORDENADOR, APOIO, etc.).
 * @param viewModel ViewModel usado para acessar notificações pendentes e não vistas.
 */
fun configurarNotificacaoBadge(
  rootView: View,
  lifecycleOwner: LifecycleOwner,
  fragmentManager: FragmentManager,
  funcionarioId: Int,
  cargo: Cargo,
  viewModel: NotificacaoViewModel
) {
  val badgeView = rootView.findViewById<TextView>(R.id.notificationBadge)
  val notificationIcon = rootView.findViewById<ImageView>(R.id.notificationIcon)

  if (cargo == Cargo.COORDENADOR) {
    val pendentesLiveData = viewModel.getQuantidadePendentesParaCoordenador()
    val prazoLiveData = viewModel.getQuantidadeNotificacoesPrazoNaoVistas(funcionarioId)

    pendentesLiveData.observe(lifecycleOwner) { pendentes ->
      prazoLiveData.observe(lifecycleOwner) { prazo ->
        val total = pendentes + prazo
        Log.d("NOTIF_BADGE", "Pendentes: $pendentes | Prazo: $prazo | Total: $total")
        badgeView.visibility = if (total > 0) View.VISIBLE else View.GONE
        badgeView.text = if (total > 9) "9+" else total.toString()
      }
    }
  } else {
    viewModel.getQuantidadeNaoVistas(funcionarioId)
      .observe(lifecycleOwner) { quantidade ->
        badgeView.visibility = if (quantidade > 0) View.VISIBLE else View.GONE
        badgeView.text = if (quantidade > 9) "9+" else quantidade.toString()
      }
  }

  notificationIcon?.setOnClickListener {
    fragmentManager.beginTransaction()
      .setCustomAnimations(
        R.anim.pull_fade_in,
        R.anim.pull_fade_out,
        R.anim.pull_fade_in,
        R.anim.pull_fade_out
      )
      .replace(R.id.main_container, NotificacaoFragment())
      .addToBackStack(null)
      .commit()
  }
}
