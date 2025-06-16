package com.example.appsenkaspi.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import com.example.appsenkaspi.ui.atividade.AtividadeRepository
import com.example.appsenkaspi.data.local.entity.RequisicaoEntity
import com.example.appsenkaspi.data.local.enums.StatusAtividade
import com.example.appsenkaspi.data.local.enums.StatusRequisicao
import com.example.appsenkaspi.data.local.enums.TipoRequisicao
import com.example.appsenkaspi.data.local.database.AppDatabase
import com.example.appsenkaspi.data.local.entity.AcaoEntity
import com.example.appsenkaspi.data.local.entity.AcaoFuncionarioEntity
import com.example.appsenkaspi.data.local.entity.AtividadeEntity
import com.example.appsenkaspi.data.local.entity.AtividadeFuncionarioEntity
import com.example.appsenkaspi.ui.acao.AcaoJson
import com.example.appsenkaspi.ui.atividade.AtividadeJson
import com.example.appsenkaspi.util.NotificationUtils
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * ViewModel responsável pelo gerenciamento das requisições e notificações internas no app.
 *
 * Esta classe processa requisições de criação, edição e conclusão de ações e atividades,
 * atualiza notificações, envia alertas para os usuários, e mantém contadores e estados reativos
 * para interface do coordenador e dos membros de apoio.
 */
class NotificacaoViewModel(application: Application) : AndroidViewModel(application) {

  private val db = AppDatabase.getDatabase(application)
  private val requisicaoDao = db.requisicaoDao()
  private val atividadeDao = db.atividadeDao()
  private val acaoDao = db.acaoDao()
  private val acaoFuncionarioDao = db.acaoFuncionarioDao()
  private val atividadeFuncionarioDao = db.atividadeFuncionarioDao()

  /**
   * Retorna todas as requisições pendentes aguardando decisão do coordenador.
   */
  fun getRequisicoesPendentes(): LiveData<List<RequisicaoEntity>> =
    requisicaoDao.getRequisicoesPendentes()

  /**
   * Busca uma requisição específica por ID (modo LiveData).
   */
  fun getRequisicaoPorId(id: Int): LiveData<RequisicaoEntity> {
    val result = MutableLiveData<RequisicaoEntity>()
    viewModelScope.launch {
      result.postValue(requisicaoDao.getRequisicaoById(id))
    }
    return result
  }

  /**
   * Processa a decisão do coordenador (aceitar ou recusar) para uma requisição.
   *
   * Executa ações correspondentes como salvar entidade no banco, atualizar status
   * e emitir notificações visuais.
   */
  fun responderRequisicao(context: Context, requisicao: RequisicaoEntity, aceitar: Boolean) {
    viewModelScope.launch {
      val status = if (aceitar) StatusRequisicao.ACEITA else StatusRequisicao.RECUSADA
      val novaRequisicao = requisicao.copy(
        status = status,
        dataResposta = Date(),
        coordenadorId = getFuncionarioLogadoId(context),
        mensagemResposta = if (aceitar) "Requisição aceita." else "Requisição recusada."
      )
      requisicaoDao.update(novaRequisicao)

      val tipoDescricao = when (requisicao.tipo) {
        TipoRequisicao.CRIAR_ATIVIDADE -> "criação de atividade"
        TipoRequisicao.EDITAR_ATIVIDADE -> "edição de atividade"
        TipoRequisicao.COMPLETAR_ATIVIDADE -> "conclusão de atividade"
        TipoRequisicao.CRIAR_ACAO -> "criação de ação"
        TipoRequisicao.EDITAR_ACAO -> "edição de ação"
        else -> "requisição"
      }

      NotificationUtils.mostrarNotificacao(
        context,
        "Requisição ${if (aceitar) "aceita" else "recusada"}",
        "Sua solicitação de $tipoDescricao foi ${if (aceitar) "aceita" else "recusada"}.",
        requisicao.id * 100
      )

      if (!aceitar) return@launch

      when (requisicao.tipo) {
        TipoRequisicao.CRIAR_ATIVIDADE -> salvarAtividade(context, requisicao.atividadeJson!!, isEdicao = false)
        TipoRequisicao.EDITAR_ATIVIDADE -> salvarAtividade(context, requisicao.atividadeJson!!, isEdicao = true)
        TipoRequisicao.COMPLETAR_ATIVIDADE -> completarAtividade(context, requisicao)
        TipoRequisicao.CRIAR_ACAO -> salvarAcao(requisicao.acaoJson!!, isEdicao = false)
        TipoRequisicao.EDITAR_ACAO -> salvarAcao(requisicao.acaoJson!!, isEdicao = true)
        else -> {}
      }
    }
  }

  /**
   * Salva ou atualiza uma atividade baseada no conteúdo serializado de uma requisição.
   *
   * Também cuida da reatribuição de responsáveis e atualização de notificações relacionadas.
   */
  private suspend fun salvarAtividade(context: Context, json: String, isEdicao: Boolean) {
    val atividadeJson = Gson().fromJson(json, AtividadeJson::class.java)
    val responsaveis = atividadeJson.responsaveis ?: emptyList()
    val funcionarioId = responsaveis.firstOrNull() ?: run {
      Log.e("Requisicao", "Erro: nenhum responsável encontrado para a atividade")
      return
    }

    val novaAtividade = AtividadeEntity(
      id = if (isEdicao) atividadeJson.id ?: throw IllegalStateException("ID ausente para edição") else null,
      nome = atividadeJson.nome,
      descricao = atividadeJson.descricao,
      dataInicio = atividadeJson.dataInicio,
      dataPrazo = atividadeJson.dataPrazo,
      acaoId = atividadeJson.acaoId,
      funcionarioId = funcionarioId,
      status = if (isEdicao) atividadeJson.status else StatusAtividade.PENDENTE,
      prioridade = atividadeJson.prioridade,
      criadoPor = atividadeJson.criadoPor,
      dataCriacao = atividadeJson.dataCriacao
    )

    if (!isEdicao) {
      val idAtividade = atividadeDao.insertComRetorno(novaAtividade).toInt()
      novaAtividade.id = idAtividade
    } else {
      val id = atividadeJson.id!!
      val antiga = atividadeDao.getAtividadePorIdDireto(id)
      val antigosResponsaveis = atividadeFuncionarioDao.getResponsaveisDaAtividade(id)
      val antigosIds = antigosResponsaveis.map { it.id }
      val novosIds = responsaveis

      atividadeDao.update(novaAtividade)
      atividadeDao.deletarRelacoesPorAtividade(id)

      responsaveis.forEach { idResp ->
        atividadeDao.inserirRelacaoFuncionario(
          AtividadeFuncionarioEntity(id, idResp)
        )
      }

      val atualizada = atividadeDao.getAtividadePorIdDireto(id)
      val repo = AtividadeRepository(context, atividadeDao, atividadeFuncionarioDao, requisicaoDao)
      repo.tratarAlteracaoPrazo(atualizada, antiga)

      val funcionarioDao = db.funcionarioDao()
      val adicionados = funcionarioDao.getByIds(novosIds - antigosIds)
      val removidos = funcionarioDao.getByIds(antigosIds - novosIds)
      repo.notificarMudancaResponsaveis(atualizada, adicionados, removidos)
    }
  }

  /**
   * Marca uma atividade como concluída, se estiver dentro do prazo e com responsáveis válidos.
   */
  private suspend fun completarAtividade(context: Context, requisicao: RequisicaoEntity) {
    val hoje = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.time

    val atividadeJson = Gson().fromJson(requisicao.atividadeJson, AtividadeJson::class.java)
    val responsaveis = atividadeJson.responsaveis ?: emptyList()
    val funcionarioId = responsaveis.firstOrNull()

    if (atividadeJson.id == null || atividadeJson.dataPrazo.before(hoje) || funcionarioId == null) {
      requisicaoDao.update(
        requisicao.copy(
          status = StatusRequisicao.RECUSADA,
          dataResposta = Date(),
          mensagemResposta = "Atividade vencida ou sem responsável.",
          coordenadorId = getFuncionarioLogadoId(context)
        )
      )
      return
    }

    val concluida = AtividadeEntity(
      id = atividadeJson.id,
      nome = atividadeJson.nome,
      descricao = atividadeJson.descricao,
      dataInicio = atividadeJson.dataInicio,
      dataPrazo = atividadeJson.dataPrazo,
      acaoId = atividadeJson.acaoId,
      funcionarioId = funcionarioId,
      status = StatusAtividade.CONCLUIDA,
      prioridade = atividadeJson.prioridade,
      criadoPor = atividadeJson.criadoPor,
      dataCriacao = atividadeJson.dataCriacao
    )

    val repo = AtividadeRepository(context, atividadeDao, atividadeFuncionarioDao, requisicaoDao)
    repo.tratarConclusaoAtividade(concluida)
  }

  /**
   * Cria ou edita uma ação com seus respectivos responsáveis.
   */
  private suspend fun salvarAcao(json: String, isEdicao: Boolean) {
    val acaoJson = Gson().fromJson(json, AcaoJson::class.java)

    val acao = AcaoEntity(
      id = if (isEdicao) acaoJson.id ?: throw IllegalStateException("ID ausente") else null,
      nome = acaoJson.nome,
      descricao = acaoJson.descricao,
      dataInicio = acaoJson.dataInicio,
      dataPrazo = acaoJson.dataPrazo,
      status = acaoJson.status,
      criadoPor = acaoJson.criadoPor,
      dataCriacao = acaoJson.dataCriacao,
      pilarId = if (acaoJson.subpilarId == null) acaoJson.pilarId else null,
      subpilarId = acaoJson.subpilarId
    )

    val idAcao = if (isEdicao) {
      acaoDao.update(acao)
      acaoJson.id!!
    } else {
      acaoDao.inserirComRetorno(acao).toInt()
    }

    acaoFuncionarioDao.deletarResponsaveisPorAcao(idAcao)
    acaoJson.responsaveis.forEach { idResp ->
      acaoFuncionarioDao.inserirAcaoFuncionario(
        AcaoFuncionarioEntity(idAcao.toLong(), idResp)
      )
    }
  }

  /**
   * Obtém o ID do funcionário logado via SharedPreferences.
   */
  fun getFuncionarioLogadoId(context: Context): Int {
    val prefs = context.getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
    return prefs.getInt("funcionarioId", -1)
  }

  /**
   * Insere uma nova requisição no banco de dados.
   */
  fun inserirRequisicao(requisicao: RequisicaoEntity) {
    viewModelScope.launch {
      requisicaoDao.inserir(requisicao)
    }
  }

  fun getNotificacoesDoApoio(usuarioId: Int): LiveData<List<RequisicaoEntity>> =
    requisicaoDao.getNotificacoesDoApoio(usuarioId)

  fun marcarTodasComoVistas(usuarioId: Int) {
    viewModelScope.launch {
      requisicaoDao.marcarComoVista(usuarioId)
    }
  }

  fun getQuantidadeNaoVistas(usuarioId: Int): LiveData<Int> =
    requisicaoDao.getQuantidadeNaoVistas(usuarioId)

  fun getQuantidadePendentesParaCoordenador(): LiveData<Int> =
    requisicaoDao.getQuantidadePendentesParaCoordenador()

  fun getQuantidadeNotificacoesPrazoNaoVistas(usuarioId: Int): LiveData<Int> =
    requisicaoDao.getQuantidadeNotificacoesPrazoNaoVistas(usuarioId)

  fun marcarNotificacoesDePrazoComoVistas(usuarioId: Int) {
    viewModelScope.launch {
      requisicaoDao.marcarNotificacoesDePrazoComoVistas(usuarioId)
    }
  }

  /**
   * Exclui logicamente um conjunto de requisições, marcando como removidas.
   */
  fun excluirRequisicoes(lista: List<RequisicaoEntity>) {
    viewModelScope.launch {
      val ids = lista.map { it.id }
      requisicaoDao.marcarComoExcluidas(ids)
    }
  }
}
