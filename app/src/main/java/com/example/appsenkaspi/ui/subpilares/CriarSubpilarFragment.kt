package com.example.appsenkaspi.ui.subpilares

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
import com.example.appsenkaspi.data.local.enums.Cargo
import com.example.appsenkaspi.viewmodel.FuncionarioViewModel
import com.example.appsenkaspi.viewmodel.PilarViewModel
import com.example.appsenkaspi.R
import com.example.appsenkaspi.data.local.enums.StatusSubPilar
import com.example.appsenkaspi.data.local.entity.SubpilarEntity
import com.example.appsenkaspi.viewmodel.SubpilarViewModel
import com.example.appsenkaspi.ui.subpilares.TelaSubpilarComAcoesFragment
import com.example.appsenkaspi.extensions.configurarBotaoVoltar
import com.example.appsenkaspi.databinding.FragmentCriarSubpilaresBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Fragmento responsável por criar um novo Subpilar vinculado a um Pilar existente.
 *
 * Exibe campos para nome, descrição e seleção de prazo final.
 * Valida a data escolhida para garantir que não ultrapasse o prazo do pilar pai.
 * Apenas coordenadores podem confirmar a criação do subpilar.
 */
class CriarSubpilarFragment : Fragment() {

  private var _binding: FragmentCriarSubpilaresBinding? = null
  private val binding get() = _binding!!

  /** ViewModel responsável pela lógica de criação e persistência do subpilar. */
  private val subpilarViewModel: SubpilarViewModel by activityViewModels()

  /** ViewModel que fornece informações sobre o funcionário logado (para controle de permissões). */
  private val funcionarioViewModel: FuncionarioViewModel by activityViewModels()

  /** ViewModel usado para consultar o prazo máximo do pilar pai. */
  private val pilarViewModel: PilarViewModel by activityViewModels()

  /** Data selecionada como prazo final do subpilar. */
  private var dataPrazoSelecionada: Date? = null

  /** Calendário auxiliar para manipulação da data no DatePicker. */
  private val calendario = Calendar.getInstance()

  /** ID do pilar ao qual este subpilar será vinculado. */
  private var pilarId: Int = -1

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentCriarSubpilaresBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    configurarBotaoVoltar(view)

    // Recupera ID do pilar passado como argumento
    pilarId = arguments?.getInt("pilarId") ?: -1
    if (pilarId == -1) {
      Toast.makeText(requireContext(), "Pilar inválido!", Toast.LENGTH_SHORT).show()
      parentFragmentManager.popBackStack()
      return
    }

    // Permissão de criação visível apenas para coordenadores
    funcionarioViewModel.funcionarioLogado.observe(viewLifecycleOwner) { funcionario ->
      binding.confirmarButtonWrapper.visibility =
        if (funcionario?.cargo == Cargo.COORDENADOR) View.VISIBLE else View.GONE
    }

    binding.buttonPickDate.setOnClickListener { abrirDatePicker() }

    binding.confirmarButtonWrapper.setOnClickListener {
      lifecycleScope.launch {
        if (validarPrazoComPilar()) {
          confirmarCriacaoSubpilar()
        }
      }
    }
  }

  /**
   * Exibe o seletor de data para o usuário e atualiza a interface com a data escolhida.
   */
  private fun abrirDatePicker() {
    DatePickerDialog(
      requireContext(),
      { _, year, month, day ->
        calendario.set(year, month, day)
        dataPrazoSelecionada = calendario.time
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.buttonPickDate.text = formato.format(dataPrazoSelecionada!!)
      },
      calendario.get(Calendar.YEAR),
      calendario.get(Calendar.MONTH),
      calendario.get(Calendar.DAY_OF_MONTH)
    ).show()
  }

  /**
   * Verifica se a data selecionada para o subpilar está dentro do limite permitido pelo pilar.
   *
   * @return true se a data for válida, false caso contrário (com mensagens exibidas).
   */
  private suspend fun validarPrazoComPilar(): Boolean {
    if (dataPrazoSelecionada == null) {
      binding.buttonPickDate.error = "Escolha um prazo"
      return false
    }

    val dataLimite = pilarViewModel.getDataPrazoDoPilar(pilarId)
    if (dataLimite == null) {
      withContext(Dispatchers.Main) {
        Toast.makeText(requireContext(), "Erro ao buscar data do pilar.", Toast.LENGTH_SHORT).show()
      }
      return false
    }

    val selecionadaTruncada = truncarData(dataPrazoSelecionada!!)
    val limiteTruncado = truncarData(dataLimite)

    if (selecionadaTruncada.after(limiteTruncado)) {
      val dataFormatada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(limiteTruncado)
      withContext(Dispatchers.Main) {
        Toast.makeText(
          requireContext(),
          "A data do subpilar não pode ultrapassar o prazo do pilar ($dataFormatada).",
          Toast.LENGTH_LONG
        ).show()
      }
      return false
    }

    return true
  }

  /**
   * Remove informações de hora de uma data para realizar comparações apenas com base na data.
   *
   * @param data Instância de [Date] a ser truncada.
   * @return Uma nova [Date] com horas, minutos e segundos zerados.
   */
  private fun truncarData(data: Date): Date {
    return Calendar.getInstance().apply {
      time = data
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.time
  }

  /**
   * Valida os campos do formulário e, se válidos, cria um novo subpilar no banco de dados
   * por meio do ViewModel. Redireciona para a tela do novo subpilar após a inserção.
   */
  private fun confirmarCriacaoSubpilar() {
    val nome = binding.inputNomeSubpilar.text.toString().trim()
    val descricao = binding.inputDescricao.text.toString().trim()
    val prazo = dataPrazoSelecionada

    if (nome.isEmpty()) {
      binding.inputNomeSubpilar.error = "Digite o nome do Subpilar"
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

    lifecycleScope.launch {
      val novoId = subpilarViewModel.inserirRetornandoId(
        SubpilarEntity(
          nome = nome,
          descricao = descricao,
          dataInicio = Date(),
          dataPrazo = prazo,
          pilarId = pilarId,
          criadoPor = funcionarioId,
          dataCriacao = Date(),
          status = StatusSubPilar.PLANEJADO
        )
      )

      parentFragmentManager.beginTransaction()
        .replace(
          R.id.main_container,
          TelaSubpilarComAcoesFragment.newInstance(novoId.toInt())
        )
        .addToBackStack(null)
        .commit()
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  companion object {
    /**
     * Cria uma nova instância do fragmento, vinculada a um pilar específico.
     *
     * @param pilarId ID do pilar ao qual o subpilar será vinculado.
     * @return Fragmento pronto para uso.
     */
    fun newInstance(pilarId: Int): CriarSubpilarFragment {
      return CriarSubpilarFragment().apply {
        arguments = Bundle().apply {
          putInt("pilarId", pilarId)
        }
      }
    }
  }
}
