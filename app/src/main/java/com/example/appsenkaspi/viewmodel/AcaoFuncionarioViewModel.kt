package com.example.appsenkaspi.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.appsenkaspi.data.local.database.AppDatabase
import com.example.appsenkaspi.data.local.entity.AcaoFuncionarioEntity
import kotlinx.coroutines.launch

/**
 * ViewModel responsável por gerenciar o relacionamento entre ações e funcionários.
 *
 * Esta classe fornece acesso aos dados relacionados à entidade de junção `AcaoFuncionarioEntity`,
 * permitindo inserir, remover e consultar associações entre ações e funcionários.
 *
 * Utiliza o `viewModelScope` para realizar operações assíncronas com segurança no ciclo de vida da UI.
 *
 * @param application A aplicação usada para obter o contexto e a instância do banco de dados.
 */
class AcaoFuncionarioViewModel(application: Application) : AndroidViewModel(application) {

  private val dao = AppDatabase.getDatabase(application).acaoFuncionarioDao()

  /**
   * Retorna os IDs dos funcionários associados a uma determinada ação.
   *
   * @param acaoId O ID da ação.
   * @return Uma [LiveData] contendo a lista de IDs dos funcionários vinculados à ação.
   */
  fun listarFuncionariosPorAcao(acaoId: Int): LiveData<List<Int>> {
    return dao.listarFuncionariosPorAcao(acaoId)
  }

  /**
   * Retorna os IDs das ações associadas a um determinado funcionário.
   *
   * @param funcionarioId O ID do funcionário.
   * @return Uma [LiveData] contendo a lista de IDs das ações vinculadas ao funcionário.
   */
  fun listarAcoesPorFuncionario(funcionarioId: Int): LiveData<List<Int>> {
    return dao.listarAcoesPorFuncionario(funcionarioId)
  }

  /**
   * Insere uma nova associação entre ação e funcionário no banco de dados.
   *
   * Executado em coroutine no escopo do ViewModel.
   *
   * @param acaoFuncionario A entidade representando o vínculo entre ação e funcionário.
   */
  fun inserir(acaoFuncionario: AcaoFuncionarioEntity) = viewModelScope.launch {
    dao.inserirAcaoFuncionario(acaoFuncionario)
  }

  /**
   * Remove uma associação entre ação e funcionário do banco de dados.
   *
   * Executado em coroutine no escopo do ViewModel.
   *
   * @param acaoFuncionario A entidade representando o vínculo a ser removido.
   */
  fun deletar(acaoFuncionario: AcaoFuncionarioEntity) = viewModelScope.launch {
    dao.deletarAcaoFuncionario(acaoFuncionario)
  }
}
