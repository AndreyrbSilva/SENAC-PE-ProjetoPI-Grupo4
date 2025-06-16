package com.example.appsenkaspi.ui.atividade

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.appsenkaspi.R
import com.example.appsenkaspi.data.local.database.AppDatabase
import com.example.appsenkaspi.data.local.entity.RequisicaoEntity
import com.example.appsenkaspi.data.local.enums.Cargo
import com.example.appsenkaspi.data.local.enums.PrioridadeAtividade
import com.example.appsenkaspi.data.local.enums.StatusAtividade
import com.example.appsenkaspi.data.local.enums.StatusRequisicao
import com.example.appsenkaspi.data.local.enums.TipoRequisicao
import com.example.appsenkaspi.databinding.FragmentTelaAtividadeBinding
import com.example.appsenkaspi.domain.model.AtividadeComFuncionarios
import com.example.appsenkaspi.extensions.configurarBotaoVoltar
import com.example.appsenkaspi.extensions.configurarNotificacaoBadge
import com.example.appsenkaspi.viewmodel.AcaoViewModel
import com.example.appsenkaspi.viewmodel.AtividadeViewModel
import com.example.appsenkaspi.viewmodel.ChecklistViewModel
import com.example.appsenkaspi.viewmodel.FuncionarioViewModel
import com.example.appsenkaspi.viewmodel.NotificacaoViewModel
import com.google.gson.Gson
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TelaAtividadeFragment : Fragment() {

  private var _binding: FragmentTelaAtividadeBinding? = null
  private val binding get() = _binding!!
  private val atividadeViewModel: AtividadeViewModel by activityViewModels()
  private val checklistViewModel: ChecklistViewModel by activityViewModels()
  private val acaoViewModel: AcaoViewModel by activityViewModels()
  private val funcionarioViewModel: FuncionarioViewModel by activityViewModels()
  private val requisicaoViewModel: NotificacaoViewModel by activityViewModels()
  private var atividadeId: Int = -1

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    _binding = FragmentTelaAtividadeBinding.inflate(inflater, container, false)
    atividadeId = arguments?.getInt("atividadeId") ?: -1
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    configurarBotaoVoltar(view)

    funcionarioViewModel.funcionarioLogado.observe(viewLifecycleOwner) { funcionario ->
      funcionario?.let {
          configurarNotificacaoBadge(
              rootView = view,
              lifecycleOwner = viewLifecycleOwner,
              fragmentManager = parentFragmentManager,
              funcionarioId = it.id,
              cargo = it.cargo,
              viewModel = requisicaoViewModel
          )
      }
    }

    funcionarioViewModel.funcionarioLogado.observe(viewLifecycleOwner) { funcionario ->
      when (funcionario?.cargo) {
        Cargo.APOIO -> {
          binding.cardConfirmarAtividade.visibility = View.GONE
          binding.cardPedirConfirmacao.visibility = View.VISIBLE
          binding.btnEditar.visibility = View.VISIBLE
          binding.btnDeletar.visibility = View.GONE
        }
        Cargo.COORDENADOR -> {
          binding.cardConfirmarAtividade.visibility = View.VISIBLE
          binding.cardPedirConfirmacao.visibility = View.GONE
          binding.btnDeletar.visibility = View.VISIBLE
          binding.btnEditar.visibility = View.VISIBLE
        }
        else -> {
          binding.cardConfirmarAtividade.visibility = View.GONE
          binding.cardPedirConfirmacao.visibility = View.GONE
          binding.btnEditar.visibility = View.GONE
          binding.btnDeletar.visibility = View.GONE
        }
      }
    }

    if (atividadeId == -1) {
      Toast.makeText(requireContext(), "Erro: atividade inválida.", Toast.LENGTH_SHORT).show()
      parentFragmentManager.popBackStack()
      return
    }

    atividadeViewModel.getAtividadeComFuncionariosById(atividadeId).observe(viewLifecycleOwner) { atividadeComFuncionarios ->
      if (atividadeComFuncionarios != null) {
        preencherCampos(atividadeComFuncionarios)
        if (atividadeComFuncionarios.atividade.status == StatusAtividade.CONCLUIDA) {
          binding.cardPedirConfirmacao.visibility = View.GONE
        }
      } else {
        Toast.makeText(requireContext(), "Atividade não encontrada!", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
      }
    }

    binding.btnEditar.setOnClickListener {
      val args = Bundle().apply { putInt("atividadeId", atividadeId) }
      parentFragmentManager.beginTransaction()
        .setCustomAnimations(
          R.anim.slide_fade_in_right,
          R.anim.slide_fade_out_left,
          R.anim.slide_fade_in_left,
          R.anim.slide_fade_out_right
        )
        .replace(R.id.main_container, EditarAtividadeFragment::class.java, args)
        .addToBackStack(null)
        .commit()
    }

    binding.btnDeletar.setOnClickListener {
      mostrarDialogoConfirmacao()
    }

    binding.cardConfirmarAtividade.setOnClickListener {
      atividadeViewModel.getAtividadeById(atividadeId).observe(viewLifecycleOwner) { atividade ->
        if (atividade != null) {
          if (atividade.status == StatusAtividade.CONCLUIDA) {
            Toast.makeText(requireContext(), "A atividade já está concluída.", Toast.LENGTH_SHORT).show()
            return@observe
          }

          if (atividade.status == StatusAtividade.VENCIDA) {
            Toast.makeText(requireContext(), "A atividade está vencida. Edite o prazo antes de concluir.", Toast.LENGTH_LONG).show()
            return@observe
          }

          val atividadeAtualizada = atividade.copy(status = StatusAtividade.CONCLUIDA)
          Log.d("ATIVIDADE_OBSERVADA", "Status da atividade: ${atividade.status}")

          atividadeViewModel.concluirAtividade(atividadeAtualizada)
          acaoViewModel.atualizarStatusAcaoAutomaticamente(atividade.acaoId)

          Toast.makeText(requireContext(), "Atividade marcada como concluída!", Toast.LENGTH_SHORT).show()
          parentFragmentManager.popBackStack()
        }
      }
    }

    binding.cardPedirConfirmacao.setOnClickListener {
      atividadeViewModel.getAtividadeComFuncionariosById(atividadeId).observe(viewLifecycleOwner) { atividade ->

        val hojeTruncado = Calendar.getInstance().apply {
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }.time

        val dataPrazoTruncado = Calendar.getInstance().apply {
          time = atividade.atividade.dataPrazo
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }.time

        val vencida = dataPrazoTruncado.before(hojeTruncado) && atividade.atividade.status != StatusAtividade.CONCLUIDA

        if (atividade.atividade.status == StatusAtividade.CONCLUIDA) {
          Toast.makeText(requireContext(), "A atividade já está concluída.", Toast.LENGTH_SHORT).show()
          return@observe
        }

        if (vencida) {
          Toast.makeText(requireContext(), "Esta atividade está vencida. Não é possível pedir confirmação, atualize o prazo.", Toast.LENGTH_LONG).show()
          return@observe
        }

        val funcionario = funcionarioViewModel.funcionarioLogado.value ?: return@observe

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
          val acao = AppDatabase.Companion.getDatabase(requireContext()).acaoDao().getAcaoPorIdDireto(atividade.atividade.acaoId)
          val nomePilar = acao?.pilarId?.let {
            AppDatabase.Companion.getDatabase(requireContext()).pilarDao().getNomePilarPorId(it)
          } ?: "Pilar não identificado"

          val atividadeJson = AtividadeJson(
            id = atividade.atividade.id,
            nome = atividade.atividade.nome,
            descricao = atividade.atividade.descricao,
            dataInicio = atividade.atividade.dataInicio,
            dataPrazo = atividade.atividade.dataPrazo,
            status = StatusAtividade.CONCLUIDA,
            prioridade = atividade.atividade.prioridade,
            criadoPor = atividade.atividade.criadoPor,
            dataCriacao = atividade.atividade.dataCriacao,
            acaoId = atividade.atividade.acaoId,
            nomePilar = nomePilar,
            responsaveis = atividade.funcionarios.map { it.id }
          )

          val json = Gson().toJson(atividadeJson)

          val requisicao = RequisicaoEntity(
              tipo = TipoRequisicao.COMPLETAR_ATIVIDADE,
              atividadeJson = json,
              status = StatusRequisicao.PENDENTE,
              solicitanteId = funcionario.id,
              dataSolicitacao = Date()
          )

          requisicaoViewModel.inserirRequisicao(requisicao)

          launch(Dispatchers.Main) {
            Toast.makeText(requireContext(), "Requisição de conclusão enviada para aprovação!", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
          }
        }
      }
    }
  }

  private fun preencherCampos(atividade: AtividadeComFuncionarios) {
    binding.tituloAtividade.text = atividade.atividade.nome
    binding.descricaoAtividade.text = atividade.atividade.descricao

    val prioridade = atividade.atividade.prioridade
    binding.textPrioridade.text = when (prioridade) {
      PrioridadeAtividade.ALTA -> "Prioridade Alta"
      PrioridadeAtividade.MEDIA -> "Prioridade Média"
      PrioridadeAtividade.BAIXA -> "Prioridade Baixa"
    }

    val corPrioridade = when (prioridade) {
      PrioridadeAtividade.ALTA -> Color.parseColor("#E74C3C")
      PrioridadeAtividade.MEDIA -> Color.parseColor("#F1C40F")
      PrioridadeAtividade.BAIXA -> Color.parseColor("#2ECC40")
    }

    binding.layoutPrioridade.backgroundTintList = ColorStateList.valueOf(corPrioridade)

    val sdf = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("pt", "BR"))
    binding.textDataInicio.text = "Data de início da atividade ${sdf.format(atividade.atividade.dataInicio)}"
    binding.textDataPrazo.text = "Essa atividade deve ser finalizada até ${sdf.format(atividade.atividade.dataPrazo)}"

    binding.containerResponsaveis.removeAllViews()
    val dimensao = resources.getDimensionPixelSize(R.dimen.tamanho_foto_responsavel)
    atividade.funcionarios.forEach { funcionario ->
      val foto = CircleImageView(requireContext()).apply {
        layoutParams = ViewGroup.MarginLayoutParams(dimensao, dimensao).apply { marginEnd = 12 }
        borderWidth = 2
        borderColor = Color.WHITE
      }
      Glide.with(this).load(funcionario.fotoPerfil).placeholder(R.drawable.ic_person).into(foto)
      binding.containerResponsaveis.addView(foto)
    }
  }

  private fun mostrarDialogoConfirmacao() {
    val dialog = AlertDialog.Builder(requireContext())
      .setTitle("Confirmação")
      .setMessage("Você tem certeza de que deseja deletar a atividade \"${binding.tituloAtividade.text}\"?")
      .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
      .setPositiveButton("Confirmar") { _, _ ->
        atividadeViewModel.getAtividadeById(atividadeId).observe(viewLifecycleOwner) { atividade ->
          if (atividade != null) {
            atividadeViewModel.deletarAtividadePorId(atividadeId)
            acaoViewModel.atualizarStatusAcaoAutomaticamente(atividade.acaoId)
          }
        }
        Toast.makeText(requireContext(), "Atividade deletada", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
      }
      .create()

    dialog.show()

    // Aplica background customizado
    dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

    // Ajusta largura
    val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
    dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

    // Alinha texto à esquerda
    (dialog.findViewById<TextView>(android.R.id.message))?.textAlignment = View.TEXT_ALIGNMENT_VIEW_START

    // Personaliza botões
    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.RED)
    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.WHITE)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
