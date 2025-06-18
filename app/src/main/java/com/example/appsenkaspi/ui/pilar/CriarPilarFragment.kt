package com.example.appsenkaspi.ui.pilar

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appsenkaspi.data.local.enums.Cargo
import com.example.appsenkaspi.viewmodel.FuncionarioViewModel
import com.example.appsenkaspi.viewmodel.NotificacaoViewModel
import com.example.appsenkaspi.data.local.entity.PilarEntity
import com.example.appsenkaspi.viewmodel.PilarViewModel
import com.example.appsenkaspi.R
import com.example.appsenkaspi.data.local.enums.StatusPilar
import com.example.appsenkaspi.data.local.enums.StatusSubPilar
import com.example.appsenkaspi.ui.subpilares.SubpilarAdapter
import com.example.appsenkaspi.data.local.entity.SubpilarEntity
import com.example.appsenkaspi.viewmodel.SubpilarViewModel
import com.example.appsenkaspi.ui.pilar.TelaPilarFragment
import com.example.appsenkaspi.extensions.configurarBotaoVoltar
import com.example.appsenkaspi.extensions.configurarNotificacaoBadge
import com.example.appsenkaspi.databinding.FragmentCriarPilarBinding
import com.example.appsenkaspi.ui.dialog.AdicionarSubpilarDialogFragment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Fragmento responsável pela criação de um novo Pilar no sistema.
 *
 * Permite ao usuário inserir nome, descrição, prazo e subpilares opcionais.
 * A criação do Pilar pode ser feita diretamente (para Coordenadores) ou gerar uma requisição de aprovação (para Apoio).
 * Integra com os ViewModels de Pilar, Subpilar e Funcionário.
 */
class CriarPilarFragment : Fragment() {

  private var _binding: FragmentCriarPilarBinding? = null
  private val binding get() = _binding!!

  private val pilarViewModel: PilarViewModel by activityViewModels()
  private val subpilarViewModel: SubpilarViewModel by activityViewModels()
  private val funcionarioViewModel: FuncionarioViewModel by activityViewModels()
  private val notificacaoViewModel: NotificacaoViewModel by activityViewModels()

  private var dataPrazoSelecionada: Date? = null
  private val calendario = Calendar.getInstance()

  private val listaSubpilares = mutableListOf<SubpilarTemp>()
  private lateinit var subpilarAdapter: SubpilarAdapter

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentCriarPilarBinding.inflate(inflater, container, false)
    return binding.root
  }

  /**
   * Configura a interface após a view ser criada:
   * - Observa o cargo do usuário e adapta os botões.
   * - Inicializa o RecyclerView de subpilares.
   * - Define os listeners dos botões de seleção de data e adição de subpilar.
   * - Configura listener para receber dados do DialogFragment de subpilar.
   */
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    // Badge de notificação no topo
    funcionarioViewModel.funcionarioLogado.observe(viewLifecycleOwner) { funcionario ->
      funcionario?.let {
        configurarNotificacaoBadge(
          rootView = view,
          lifecycleOwner = viewLifecycleOwner,
          fragmentManager = parentFragmentManager,
          funcionarioId = it.id,
          cargo = it.cargo,
          viewModel = notificacaoViewModel
        )
      }
    }

    configurarBotaoVoltar(view)

    // Exibe botão adequado conforme cargo
    funcionarioViewModel.funcionarioLogado.observe(viewLifecycleOwner) { funcionario ->
      when (funcionario?.cargo) {
        Cargo.APOIO -> {
          binding.confirmarButtonWrapper.visibility = View.GONE
          binding.pedirConfirmarButtonWrapper.visibility = View.VISIBLE
        }
        Cargo.COORDENADOR -> {
          binding.confirmarButtonWrapper.visibility = View.VISIBLE
          binding.pedirConfirmarButtonWrapper.visibility = View.GONE
        }
        else -> {
          binding.confirmarButtonWrapper.visibility = View.GONE
          binding.pedirConfirmarButtonWrapper.visibility = View.GONE
        }
      }
    }

    // Recycler de subpilares
    subpilarAdapter = SubpilarAdapter(listaSubpilares)
    binding.recyclerViewSubpilares.apply {
      layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
      adapter = subpilarAdapter
    }

    binding.buttonPickDate.setOnClickListener { abrirDatePicker() }
    binding.buttonAddSubpilar.setOnClickListener { abrirDialogAdicionarSubpilar() }
    binding.confirmarButtonWrapper.setOnClickListener { confirmarCriacaoPilar() }

    // Recebe novo subpilar adicionado via diálogo
    childFragmentManager.setFragmentResultListener("novoSubpilar", viewLifecycleOwner) { _, bundle ->
      val nome = bundle.getString("nomeSubpilar")
      val descricao = bundle.getString("descricaoSubpilar")
      val prazo = bundle.getSerializable("prazoSubpilar") as? Date

      if (nome != null && prazo != null) {
        listaSubpilares.add(SubpilarTemp(nome, descricao, prazo))
        subpilarAdapter.notifyItemInserted(listaSubpilares.size - 1)
      }
    }
  }

  /**
   * Abre um DatePickerDialog para seleção da data de prazo do Pilar.
   */
  private fun abrirDatePicker() {
    DatePickerDialog(
      requireContext(),
      { _, year, month, day ->
        calendario.set(year, month, day)
        dataPrazoSelecionada = calendario.time
        val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.buttonPickDate.text = fmt.format(dataPrazoSelecionada!!)
      },
      calendario.get(Calendar.YEAR),
      calendario.get(Calendar.MONTH),
      calendario.get(Calendar.DAY_OF_MONTH)
    ).show()
  }

  /**
   * Abre o diálogo para adicionar um novo subpilar ao Pilar atual.
   * O botão só funciona se a data de prazo principal estiver definida.
   */
  private fun abrirDialogAdicionarSubpilar() {
    dataPrazoSelecionada?.let { prazo ->
      AdicionarSubpilarDialogFragment.newInstance(-1, prazo)
        .show(childFragmentManager, "AdicionarSubpilarDialog")
    } ?: run {
      binding.buttonPickDate.error = "Escolha primeiro um prazo"
    }
  }

  /**
   * Valida os dados do formulário e insere o novo Pilar no banco de dados.
   * Caso existam subpilares, eles também são inseridos.
   * Navega para a tela correspondente ao tipo do Pilar (com ou sem subpilares).
   */
  private fun confirmarCriacaoPilar() {
    val nome = binding.inputNomePilar.text.toString().trim()
    val descricao = binding.inputDescricao.text.toString().trim()
    val prazo = dataPrazoSelecionada

    if (nome.isEmpty()) {
      binding.inputNomePilar.error = "Digite o nome do Pilar"
      return
    }
    if (prazo == null) {
      binding.buttonPickDate.error = "Escolha um prazo"
      return
    }

    val prefs = requireContext().getSharedPreferences("funcionario_prefs", Context.MODE_PRIVATE)
    val funcionarioId = prefs.getInt("funcionario_id", -1)
    if (funcionarioId == -1) {
      Toast.makeText(context, "Erro: usuário não autenticado", Toast.LENGTH_LONG).show()
      return
    }

    viewLifecycleOwner.lifecycleScope.launch {
      val idLong = pilarViewModel.inserirRetornandoId(
        PilarEntity(
          nome = nome,
          descricao = descricao,
          dataInicio = Date(),
          dataPrazo = prazo,
          status = StatusPilar.PLANEJADO,
          dataCriacao = Date(),
          criadoPor = funcionarioId,
          dataConclusao = null,
          dataExcluido = null,
        )
      )
      val novoId = idLong.toInt()

      listaSubpilares.forEach { sub ->
        subpilarViewModel.inserir(
          SubpilarEntity(
            nome = sub.nome,
            descricao = sub.descricao,
            dataInicio = Date(),
            dataPrazo = sub.prazo,
            pilarId = novoId,
            criadoPor = funcionarioId,
            dataCriacao = Date(),
            status = StatusSubPilar.PLANEJADO
          )
        )
      }

      val possuiSubpilares = listaSubpilares.isNotEmpty()

      val destino = if (possuiSubpilares) {
        TelaPilarComSubpilaresFragment.newInstance(novoId)
      } else {
        TelaPilarFragment().apply {
          arguments = Bundle().apply { putInt("pilarId", novoId) }
        }
      }

      parentFragmentManager.beginTransaction()
        .replace(R.id.main_container, destino)
        .addToBackStack(null)
        .commit()
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  /**
   * Representa um subpilar temporário adicionado pelo usuário antes da persistência.
   *
   * @property nome Nome do subpilar.
   * @property descricao Descrição opcional.
   * @property prazo Data de prazo.
   */
  data class SubpilarTemp(
    val nome: String,
    val descricao: String?,
    val prazo: Date
  )
}
