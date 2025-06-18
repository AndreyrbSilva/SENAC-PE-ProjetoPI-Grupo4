package com.example.appsenkaspi.ui.relatorio

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.graphics.Color
import android.widget.TextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.example.appsenkaspi.R
import com.example.appsenkaspi.data.local.database.AppDatabase
import com.example.appsenkaspi.data.local.entity.PilarEntity
import com.example.appsenkaspi.databinding.FragmentRelatorioBinding
import com.example.appsenkaspi.extensions.configurarNotificacaoBadge
import com.example.appsenkaspi.helpers.getFuncionarioNomeUsuario
import com.example.appsenkaspi.viewmodel.FuncionarioViewModel
import com.example.appsenkaspi.viewmodel.NotificacaoViewModel
import com.example.appsenkaspi.viewmodel.PilarViewModel
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fragmento responsável pela geração, visualização e gerenciamento de relatórios do sistema.
 *
 * Funcionalidades:
 * - Geração de relatórios em PDF, Word (DOCX) ou Excel (XLSX), com base em dados dos Pilares, Ações e Atividades
 * - Suporte a relatórios gerais (todos os pilares) ou específicos por pilar
 * - Integração com a API externa para geração e download dos arquivos
 * - Armazenamento local do histórico de relatórios por usuário
 * - Exibição animada do histórico em lista, com divisão por tipo e data
 *
 * Integra os seguintes ViewModels:
 * - [PilarViewModel]: acesso aos dados dos pilares
 * - [FuncionarioViewModel]: identificação do usuário logado
 * - [NotificacaoViewModel]: exibição de badges de notificação
 *
 * Utiliza [RelatorioAdapter] para exibir o histórico salvo.
 */

class RelatorioFragment : Fragment() {

    private var _binding: FragmentRelatorioBinding? = null
    private val binding get() = _binding!!
    private val pilarViewModel: PilarViewModel by viewModels()
    private val apiService = RetrofitClient.apiService
    private val historicoRelatorios = mutableListOf<HistoricoRelatorio>()
    private lateinit var relatorioAdapter: RelatorioAdapter
    private val funcionarioViewModel: FuncionarioViewModel by activityViewModels()
    private val notificacaoViewModel: NotificacaoViewModel by activityViewModels()

    private var listaPilares: List<PilarEntity> = emptyList()
    private lateinit var pilarAdapter: ArrayAdapter<String>
    private var isGeral: Boolean = true
    private var nomeUsuarioLogado: String? = null

    companion object {
        const val BASE_URL_DOWNLOAD = "https://matheusmoura19.pythonanywhere.com/relatorio/download/"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRelatorioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        nomeUsuarioLogado = getFuncionarioNomeUsuario(requireContext())

        configurarSpinners()
        configurarBotoes()
        carregarPilares()

        binding.recyclerHistorico.layoutManager = LinearLayoutManager(requireContext())
        relatorioAdapter = RelatorioAdapter(
            requireContext(),
            nomeUsuarioLogado!!,
            historicoRelatorios,
            viewLifecycleOwner
        )
        binding.recyclerHistorico.adapter = relatorioAdapter

        val divider = DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
        ContextCompat.getDrawable(requireContext(), R.drawable.my_divider)?.let { drawable ->
            divider.setDrawable(drawable)
            binding.recyclerHistorico.addItemDecoration(divider)
        }

        carregarHistoricoSalvo()

        binding.textSelecionarPilar.measure(
            View.MeasureSpec.makeMeasureSpec(binding.baseLayout.width, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        binding.layoutSpinnerPilares.measure(
            View.MeasureSpec.makeMeasureSpec(binding.baseLayout.width, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        binding.textSelecionarPilar.alpha = 0f
        binding.layoutSpinnerPilares.alpha = 0f
    }
  /**
   * Inicializa o spinner de seleção de tipo de arquivo e oculta temporariamente o spinner de pilares.
   */

    private fun configurarSpinners() {
        val tipoArquivoAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.tipos_arquivo,
            R.layout.spinner_dropdown_item
        )
        binding.spinnerTipoArquivo.setAdapter(tipoArquivoAdapter)

        binding.textSelecionarPilar.visibility = View.GONE
        binding.layoutSpinnerPilares.visibility = View.GONE
    }
  /**
   * Define os comportamentos dos botões de seleção (geral vs. por pilar), geração de relatório e limpeza de histórico.
   * Inclui animações de seleção e alternância de visibilidade.
   */


  private fun configurarBotoes() {
        updateButtonSelection(binding.btnRelatorioGeral, binding.btnRelatorioPorPilar)

        binding.btnRelatorioGeral.setOnClickListener {
            isGeral = true
            updateButtonSelection(binding.btnRelatorioGeral, binding.btnRelatorioPorPilar)
            animateLayoutChange(binding.baseLayout)
            binding.textSelecionarPilar.visibility = View.GONE
            binding.layoutSpinnerPilares.visibility = View.GONE
        }

        binding.btnRelatorioPorPilar.setOnClickListener {
            isGeral = false
            updateButtonSelection(binding.btnRelatorioPorPilar, binding.btnRelatorioGeral)
            binding.textSelecionarPilar.alpha = 0f
            binding.layoutSpinnerPilares.alpha = 0f
            binding.textSelecionarPilar.visibility = View.VISIBLE
            binding.layoutSpinnerPilares.visibility = View.VISIBLE
            animateLayoutChange(binding.baseLayout)
            binding.textSelecionarPilar.animate().alpha(1f).setDuration(300).start()
            binding.layoutSpinnerPilares.animate().alpha(1f).setDuration(300).setStartDelay(50).start()
        }

        binding.btnGerarRelatorio.setOnClickListener {
            animateButtonClick(binding.btnGerarRelatorio) {
                val tipoSelecionado = when (binding.spinnerTipoArquivo.text.toString()) {
                    "PDF" -> "pdf"
                    "Excel" -> "xlsx"
                    "Word" -> "docx"
                    else -> ""
                }

                if (tipoSelecionado.isNotEmpty()) {
                    gerarRelatorio(tipoSelecionado)
                } else {
                    Toast.makeText(requireContext(), "Selecione um tipo de arquivo", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnLimparHistorico.setOnClickListener {
            animateButtonClick(binding.btnLimparHistorico) {
                mostrarDialogConfirmacaoLimpeza()
            }
        }
    }
  /**
   * Aplica uma transição suave ao layout raiz, usada para alternar visibilidade de elementos.
   *
   * @param root O ViewGroup onde a transição deve ser aplicada.
   */

    private fun animateLayoutChange(root: ViewGroup) {
        val transition = AutoTransition()
        transition.duration = 300
        TransitionManager.beginDelayedTransition(root, transition)
    }
  /**
   * Atualiza a seleção visual entre dois botões (MaterialCardView), alterando cores com animação.
   *
   * @param selected O botão que foi selecionado.
   * @param deselected O botão que foi desmarcado.
   */

    private fun updateButtonSelection(selected: View, deselected: View) {
        selected.isSelected = true
        deselected.isSelected = false

        val selectedColor = ContextCompat.getColor(requireContext(), R.color.azulSelecionado)
        val defaultColor = ContextCompat.getColor(requireContext(), R.color.cardNormal)

        val fromColorSelected = (selected as? MaterialCardView)
            ?.cardBackgroundColor?.defaultColor ?: defaultColor

        val fromColorDeselected = (deselected as? MaterialCardView)
            ?.cardBackgroundColor?.defaultColor ?: selectedColor

        animateBackgroundColor(selected, fromColorSelected, selectedColor)
        animateBackgroundColor(deselected, fromColorDeselected, defaultColor)
    }

  /**
   * Anima a transição de cor de fundo de um [MaterialCardView] com interpolação suave.
   *
   * @param view O card a ser animado.
   * @param fromColor Cor inicial.
   * @param toColor Cor final.
   */

    private fun animateBackgroundColor(view: View, fromColor: Int, toColor: Int) {
        val animator = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
        animator.duration = 300
        animator.addUpdateListener { animation ->
            val color = animation.animatedValue as Int
            (view as? MaterialCardView)?.setCardBackgroundColor(color)
        }
        animator.start()
    }
  /**
   * Aplica uma leve animação de "clique" no botão com escala, e executa uma ação ao final.
   *
   * @param view O botão clicado.
   * @param onEnd Ação a ser executada após a animação de retorno.
   */
    private fun animateButtonClick(view: View, onEnd: (() -> Unit)? = null) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(80)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(80)
                    .withEndAction { onEnd?.invoke() }
                    .start()
            }
            .start()
    }
    /**
   * Carrega todos os pilares do banco local e os insere no spinner de seleção.
   */
    private fun carregarPilares() {
        lifecycleScope.launch {
            listaPilares = withContext(Dispatchers.IO) {
                pilarViewModel.getTodosPilares()
            }

            val nomes = listaPilares.map { it.nome }

            pilarAdapter = ArrayAdapter(
                requireContext(),
                R.layout.spinner_dropdown_item,
                nomes
            )

            binding.spinnerPilares.setAdapter(pilarAdapter)
        }
    }
  /**
   * Gera o relatório no formato especificado, com base na seleção do usuário (geral ou por pilar).
   *
   * @param tipo Formato de arquivo desejado: "pdf", "docx" ou "xlsx"
   */
    private fun gerarRelatorio(tipo: String) {
        lifecycleScope.launch {
            if (listaPilares.isEmpty()) {
                Toast.makeText(requireContext(), "Impossível fazer relatório sem pilares cadastrados", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                return@launch
            }

            binding.progressBar.visibility = View.VISIBLE
            try {
                val pilaresSelecionados = if (isGeral) {
                    withContext(Dispatchers.IO) { pilarViewModel.getTodosPilares() }
                } else {
                    val nomeSelecionado = binding.spinnerPilares.text.toString()
                    val pilarSelecionado = listaPilares.find { it.nome == nomeSelecionado }
                    if (pilarSelecionado != null) listOf(pilarSelecionado) else emptyList()
                }

                val db = AppDatabase.Companion.getDatabase(requireContext())

                val listaDTO = pilaresSelecionados.map { pilar ->
                    val acoesEntity = withContext(Dispatchers.IO) {
                        db.acaoDao().getAcoesPorPilarDireto(pilar.id)
                    }

                    val acoes = acoesEntity.map { acao ->
                        val atividadesEntity = withContext(Dispatchers.IO) {
                            db.atividadeDao().getAtividadesPorAcaoDireto(acao.id!!)
                        }

                        val atividades = atividadesEntity.map { atividade ->
                            AtividadeDTO(
                                nome = atividade.nome,
                                descricao = atividade.descricao,
                                status = atividade.status.name,
                                responsavel = "Funcionário ID: ${atividade.funcionarioId}"
                            )
                        }

                        AcaoDTO(
                            nome = acao.nome,
                            descricao = acao.descricao,
                            status = acao.status.name,
                            atividades = atividades
                        )
                    }

                    PilarDTO(
                        nome = pilar.nome,
                        descricao = pilar.descricao,
                        dataInicio = pilar.dataInicio.toString(),
                        dataPrazo = pilar.dataPrazo.toString(),
                        status = pilar.status.name,
                        criadoPor = "Usuário ID: ${pilar.criadoPor}",
                        acoes = acoes
                    )
                }

                val tipoRelatorio = if (isGeral) "geral" else "pilar"
                val pilarId = if (!isGeral && pilaresSelecionados.isNotEmpty()) pilaresSelecionados.first().id else null

                val request = RelatorioRequest(
                    tipoRelatorio = tipoRelatorio,
                    pilarId = pilarId,
                    pilares = listaDTO
                )

                val response: Response<ResponseBody>? = when (tipo) {
                    "pdf" -> apiService.gerarPdf(request)
                    "docx" -> apiService.gerarWord(request)
                    "xlsx" -> apiService.gerarExcel(request)
                    else -> null
                }

                response?.let {
                    if (it.isSuccessful) {
                        // Captura o nome do arquivo do cabeçalho Content-Disposition (se disponível)
                        val contentDisposition = it.headers()["Content-Disposition"]
                        val nomeArquivoServidor = contentDisposition?.let { header ->
                            Regex("""filename="?([^\";]+)"?""").find(header)?.groupValues?.get(1)
                        } ?: "relatorio.$tipo"

// Salva o arquivo com o nome real
                        val caminhoDoArquivoSalvo = salvarArquivo(it.body(), nomeArquivoServidor)
                        if (caminhoDoArquivoSalvo == null) {
                            Toast.makeText(requireContext(), "Falha ao salvar o arquivo", Toast.LENGTH_SHORT).show()
                            binding.progressBar.visibility = View.GONE
                            return@launch
                        }

                        val acoesCount = listaDTO.sumOf { pilar -> pilar.acoes.size }
                        val atividadesCount = listaDTO.sumOf { pilar -> pilar.acoes.sumOf { acao -> acao.atividades.size } }

                        val statusGeral = if (listaDTO.all { pilar -> pilar.acoes.all { acao -> acao.status == "CONCLUIDO" } }) {
                            "Concluído"
                        } else {
                            "Em andamento"
                        }

                        val responsaveisSet = listaDTO.flatMap { pilar ->
                            pilar.acoes.flatMap { acao ->
                                acao.atividades.map { it.responsavel }
                            }
                        }.toSet()

                        val responsaveis = responsaveisSet.joinToString(", ")

                        val urlArquivo = "${BASE_URL_DOWNLOAD}${nomeArquivoServidor}"
                        println("DEBUG: URL para download: $urlArquivo")

                        val titulo = if (isGeral) "Relatório Geral" else "Relatório por Pilar"
                        val dataAtual = SimpleDateFormat(
                            "dd/MM/yyyy 'às' HH:mm",
                            Locale.getDefault()
                        ).format(Date())
                        val nomePilar = if (!isGeral) listaDTO.firstOrNull()?.nome else null

                        val novoHistorico = HistoricoRelatorio(
                          titulo = titulo,
                          data = dataAtual,
                          tipoArquivo = tipo,
                          pilarNome = nomePilar,
                          caminhoArquivo = caminhoDoArquivoSalvo,
                          urlArquivo = urlArquivo
                        )

                        historicoRelatorios.add(0, novoHistorico)
                        relatorioAdapter.notifyItemInserted(0)

                        if (nomeUsuarioLogado != null) {
                            HistoricoStorage.salvar(requireContext(), historicoRelatorios, nomeUsuarioLogado!!)
                        }
                        binding.recyclerHistorico.visibility = View.VISIBLE

                    } else {
                        Toast.makeText(requireContext(), "Erro ao gerar relatório", Toast.LENGTH_SHORT).show()
                    }
                } ?: run {
                    Toast.makeText(requireContext(), "Resposta inválida do servidor", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro inesperado: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
  /**
   * Salva o [ResponseBody] do relatório no diretório de Downloads do dispositivo,
   * com suporte a versões do Android ≥ Q.
   *
   * @param body Conteúdo binário do relatório retornado pela API
   * @param nomeArquivo Nome final do arquivo a ser salvo
   * @return Caminho como URI em string, ou null se falhar
   */

    private fun salvarArquivo(body: ResponseBody?, nomeArquivo: String): String? {
        if (body == null) return null

        val resolver = requireContext().contentResolver
        val mimeType = when {
            nomeArquivo.endsWith(".pdf") -> "application/pdf"
            nomeArquivo.endsWith(".docx") -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            nomeArquivo.endsWith(".xlsx") -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            else -> "application/octet-stream"
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, nomeArquivo)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val itemUri = resolver.insert(collection, contentValues)

        return if (itemUri != null) {
            try {
                resolver.openOutputStream(itemUri).use { outputStream ->
                    body.byteStream().use { inputStream ->
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream?.write(buffer, 0, bytesRead)
                        }
                        outputStream?.flush()
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(itemUri, contentValues, null, null)
                }

                Toast.makeText(requireContext(), "Arquivo salvo em Downloads", Toast.LENGTH_LONG).show()
                itemUri.toString()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Erro ao salvar o arquivo", Toast.LENGTH_SHORT).show()
                null
            }
        } else {
            Toast.makeText(requireContext(), "Erro ao acessar a pasta Downloads", Toast.LENGTH_SHORT).show()
            null
        }
    }
  /**
   * Carrega o histórico de relatórios salvo para o usuário atual e exibe no RecyclerView.
   */

    private fun carregarHistoricoSalvo() {
        if (nomeUsuarioLogado == null) return
        historicoRelatorios.clear()
        historicoRelatorios.addAll(HistoricoStorage.carregar(requireContext(), nomeUsuarioLogado!!))
        relatorioAdapter.notifyDataSetChanged()
        if (historicoRelatorios.isNotEmpty()) {
            binding.recyclerHistorico.visibility = View.VISIBLE
        }
    }
  /**
   * Exclui todos os relatórios do histórico local e atualiza a interface.
   */

    private fun limparHistorico() {
        historicoRelatorios.clear()
        relatorioAdapter.notifyDataSetChanged()
        binding.recyclerHistorico.visibility = View.GONE
        if (nomeUsuarioLogado != null) {
            HistoricoStorage.salvar(requireContext(), historicoRelatorios, nomeUsuarioLogado!!)
        }
        Toast.makeText(requireContext(), "Histórico limpo com sucesso", Toast.LENGTH_SHORT).show()
    }
  /**
   * Mostra um diálogo de confirmação antes de limpar o histórico de relatórios.
   */

    private fun mostrarDialogConfirmacaoLimpeza() {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Confirmar ação")
            .setMessage("Tem certeza que deseja limpar o histórico de relatórios?")
            .setPositiveButton("Sim") { _, _ -> limparHistorico() }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

        (dialog.findViewById<TextView>(android.R.id.message))?.textAlignment = View.TEXT_ALIGNMENT_VIEW_START

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.RED)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.WHITE)
    }
    /**
   * Desfaz o binding para evitar memory leaks.
   */
    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
