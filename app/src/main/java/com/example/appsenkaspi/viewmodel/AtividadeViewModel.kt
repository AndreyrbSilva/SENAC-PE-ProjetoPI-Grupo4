package com.example.appsenkaspi.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.appsenkaspi.ui.atividade.AtividadeRepository
import com.example.appsenkaspi.data.local.entity.RequisicaoEntity
import com.example.appsenkaspi.data.local.enums.StatusAtividade
import com.example.appsenkaspi.data.local.database.AppDatabase
import com.example.appsenkaspi.data.local.entity.AtividadeEntity
import com.example.appsenkaspi.data.local.entity.AtividadeFuncionarioEntity
import com.example.appsenkaspi.domain.model.AtividadeComFuncionarios
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * ViewModel responsável pela lógica de negócio relacionada à entidade [AtividadeEntity].
 *
 * Gerencia operações como criação, edição, exclusão e conclusão de atividades,
 * além de controlar notificações e vínculos com funcionários.
 *
 * Utiliza corrotinas para operações assíncronas e delega parte da lógica ao [AtividadeRepository].
 *
 * @param application Contexto de aplicação necessário para acesso ao banco de dados.
 */
class AtividadeViewModel(application: Application) : AndroidViewModel(application) {

  val context = getApplication<Application>().applicationContext

  private val atividadeDao = AppDatabase.getDatabase(application).atividadeDao()
  private val atividadeFuncionarioDao = AppDatabase.getDatabase(application).atividadeFuncionarioDao()
  private val requisicaoDao = AppDatabase.getDatabase(application).requisicaoDao()

  /**
   * Repositório que centraliza a lógica de negócios relacionada às atividades.
   */
  val repository = AtividadeRepository(
    context,
    atividadeDao,
    atividadeFuncionarioDao,
    requisicaoDao
  )

  /**
   * Deleta uma atividade diretamente pelo seu ID.
   *
   * @param id Identificador da atividade.
   */
  fun deletarAtividadePorId(id: Int) = viewModelScope.launch {
    atividadeDao.deletarPorId(id)
  }

  /**
   * Insere uma nova atividade no banco e retorna o ID gerado via callback.
   *
   * @param atividade A nova atividade a ser salva.
   * @param onComplete Callback com o ID da atividade inserida.
   */
  fun inserir(atividade: AtividadeEntity, onComplete: (Int) -> Unit = {}) = viewModelScope.launch {
    val id = atividadeDao.inserirComRetorno(atividade).toInt()
    onComplete(id)
  }

  /**
   * Lista todas as atividades com seus respectivos funcionários para uma determinada ação.
   *
   * @param acaoId ID da ação.
   * @return [LiveData] com lista de [AtividadeComFuncionarios].
   */
  fun listarAtividadesComFuncionariosPorAcao(acaoId: Int): LiveData<List<AtividadeComFuncionarios>> =
    atividadeDao.listarAtividadesComFuncionariosPorAcao(acaoId)

  /**
   * Retorna uma atividade com funcionários associados por ID.
   *
   * @param id ID da atividade.
   * @return [LiveData] com [AtividadeComFuncionarios].
   */
  fun getAtividadeComFuncionariosById(id: Int): LiveData<AtividadeComFuncionarios> =
    atividadeDao.getAtividadeComFuncionariosPorId(id)

  /**
   * Retorna uma atividade diretamente por seu ID.
   *
   * @param id ID da atividade.
   * @return [LiveData] com a entidade [AtividadeEntity].
   */
  fun getAtividadeById(id: Int): LiveData<AtividadeEntity> =
    atividadeDao.getAtividadeById(id)

  /**
   * Cria uma relação entre atividade e funcionário.
   *
   * @param relacao Entidade de vínculo entre a atividade e o funcionário.
   */
  fun inserirRelacaoFuncionario(relacao: AtividadeFuncionarioEntity) = viewModelScope.launch {
    atividadeFuncionarioDao.inserirAtividadeFuncionario(relacao)
  }

  /**
   * Atualiza os dados de uma atividade existente.
   *
   * @param atividade Entidade com dados atualizados.
   */
  fun atualizar(atividade: AtividadeEntity) = viewModelScope.launch {
    atividadeDao.atualizarAtividade(atividade)
  }

  /**
   * Deleta uma atividade do banco.
   *
   * @param atividade A atividade a ser removida.
   */
  fun deletar(atividade: AtividadeEntity) = viewModelScope.launch {
    atividadeDao.deletarAtividade(atividade)
  }

  /**
   * Deleta todos os vínculos de uma atividade com funcionários.
   *
   * @param atividadeId ID da atividade.
   */
  suspend fun deletarRelacoesPorAtividade(atividadeId: Int) {
    atividadeFuncionarioDao.deletarPorAtividade(atividadeId)
  }

  /**
   * Salva alterações na atividade e trata mudanças no prazo.
   *
   * @param atividadeEditada Nova versão da atividade.
   * @param atividadeAntiga Versão anterior da atividade.
   */
  fun salvarEdicaoAtividade(
    atividadeEditada: AtividadeEntity,
    atividadeAntiga: AtividadeEntity
  ) = viewModelScope.launch {
    atividadeDao.update(atividadeEditada)
    repository.tratarAlteracaoPrazo(atividadeEditada, atividadeAntiga)
    repository.verificarAtividadesVencidas()
  }

  /**
   * Executa verificação de notificações relacionadas a prazos de atividades.
   */
  fun checarPrazos() = viewModelScope.launch {
    repository.verificarNotificacoesDePrazo()
  }

  /**
   * Marca uma atividade como concluída, respeitando regras de vencimento.
   *
   * - Impede conclusão se a atividade estiver vencida ou no dia do prazo.
   *
   * @param atividade Atividade a ser concluída.
   */
  fun concluirAtividade(atividade: AtividadeEntity) = viewModelScope.launch {
    val atual = atividadeDao.getAtividadePorIdDireto(atividade.id ?: return@launch)

    if (atual.status == StatusAtividade.CONCLUIDA) return@launch

    val prazo = atual.dataPrazo
    if (prazo != null && isPrazoVencidoOuHoje(prazo)) {
      Log.w("ATIVIDADE", "Tentativa de concluir atividade vencida ou no dia do prazo: ${atual.nome}")
      return@launch
    }

    val atualizado = atual.copy(status = StatusAtividade.CONCLUIDA)
    repository.tratarConclusaoAtividade(atualizado)
  }

  /**
   * Verifica todas as atividades vencidas e emite notificações se necessário.
   */
  fun checarAtividadesVencidas() = viewModelScope.launch {
    repository.verificarAtividadesVencidas()
    repository.verificarNotificacoesDeAtividadesVencidasJaMarcadas()
  }

  /**
   * LiveData contendo todas as notificações/requisições pendentes do sistema.
   */
  val notificacoes: LiveData<List<RequisicaoEntity>> = requisicaoDao.getTodasNotificacoes()

  /**
   * Verifica vencimentos de atividades ao iniciar o app ou atualizar dados.
   */
  fun verificarVencimentos() {
    viewModelScope.launch {
      val repo = AtividadeRepository(
        context,
        AppDatabase.getDatabase(getApplication()).atividadeDao(),
        AppDatabase.getDatabase(getApplication()).atividadeFuncionarioDao(),
        AppDatabase.getDatabase(getApplication()).requisicaoDao()
      )
      repo.verificarAtividadesVencidas()
    }
  }

  /**
   * Lista atividades com funcionários associadas a um determinado responsável.
   *
   * @param idFuncionario ID do funcionário.
   * @return [LiveData] contendo lista de [AtividadeComFuncionarios].
   */
  fun listarAtividadesComFuncionariosPorFuncionario(idFuncionario: Int): LiveData<List<AtividadeComFuncionarios>> {
    return atividadeDao.listarAtividadesComResponsaveis(idFuncionario)
  }

  /**
   * Verifica se uma data de prazo é hoje ou já passou.
   *
   * @param prazo Data de prazo da atividade.
   * @return `true` se a data for igual ou anterior a hoje.
   */
  fun isPrazoVencidoOuHoje(prazo: Date): Boolean {
    val formato = java.text.SimpleDateFormat("yyyy-MM-dd")
    val hojeStr = formato.format(Date())
    val prazoStr = formato.format(prazo)
    return prazoStr <= hojeStr
  }
}
