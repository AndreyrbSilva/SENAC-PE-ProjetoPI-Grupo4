package com.example.appsenkaspi.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.appsenkaspi.data.local.database.AppDatabase
import com.example.appsenkaspi.data.local.entity.AtividadeFuncionarioEntity
import kotlinx.coroutines.launch

/**
 * ViewModel responsável por gerenciar o vínculo entre atividades e funcionários.
 *
 * Esta classe oferece métodos para listar associações, inserir novos vínculos e remover vínculos
 * entre a entidade `AtividadeEntity` e `FuncionarioEntity`, utilizando o DAO correspondente.
 *
 * Opera com LiveData para atualização reativa na interface e utiliza corrotinas no escopo do ViewModel
 * para operações de banco assíncronas seguras.
 *
 * @param application Contexto necessário para acessar o banco de dados via [AppDatabase].
 */
class AtividadeFuncionarioViewModel(application: Application) : AndroidViewModel(application) {

  private val dao = AppDatabase.getDatabase(application).atividadeFuncionarioDao()

  /**
   * Lista os IDs de funcionários associados a uma atividade específica.
   *
   * @param atividadeId ID da atividade.
   * @return [LiveData] contendo a lista de IDs dos funcionários vinculados à atividade.
   */
  fun listarFuncionariosPorAtividade(atividadeId: Int): LiveData<List<Int>> {
    return dao.listarFuncionariosPorAtividade(atividadeId)
  }

  /**
   * Lista os IDs de atividades associadas a um funcionário.
   *
   * @param funcionarioId ID do funcionário.
   * @return [LiveData] contendo a lista de IDs das atividades atribuídas ao funcionário.
   */
  fun listarAtividadesPorFuncionario(funcionarioId: Int): LiveData<List<Int>> {
    return dao.listarAtividadesPorFuncionario(funcionarioId)
  }

  /**
   * Insere um vínculo entre uma atividade e um funcionário no banco de dados.
   *
   * @param atividadeFuncionario Entidade de junção representando o relacionamento.
   */
  fun inserir(atividadeFuncionario: AtividadeFuncionarioEntity) = viewModelScope.launch {
    dao.inserirAtividadeFuncionario(atividadeFuncionario)
  }

  /**
   * Remove um vínculo entre uma atividade e um funcionário.
   *
   * @param atividadeFuncionario Entidade de junção a ser removida.
   */
  fun deletar(atividadeFuncionario: AtividadeFuncionarioEntity) = viewModelScope.launch {
    dao.deletarAtividadeFuncionario(atividadeFuncionario)
  }
}
