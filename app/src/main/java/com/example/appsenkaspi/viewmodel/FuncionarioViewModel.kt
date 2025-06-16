package com.example.appsenkaspi.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.appsenkaspi.data.local.database.AppDatabase
import com.example.appsenkaspi.data.local.entity.FuncionarioEntity
import kotlinx.coroutines.launch

/**
 * ViewModel responsável pela lógica de gerenciamento de funcionários no sistema.
 *
 * Permite listar, inserir, atualizar, remover e autenticar funcionários, utilizando o DAO correspondente
 * com suporte a LiveData para reatividade da interface.
 *
 * @param application Contexto da aplicação, necessário para obter instância do banco de dados Room.
 */
class FuncionarioViewModel(application: Application) : AndroidViewModel(application) {

  private val funcionarioDao = AppDatabase.getDatabase(application).funcionarioDao()

  /**
   * Lista completa de funcionários registrada no banco, observável via LiveData.
   */
  val listaFuncionarios: LiveData<List<FuncionarioEntity>> =
    funcionarioDao.listarTodosFuncionarios()

  /**
   * LiveData interna (privada) para controle de sessão do funcionário logado.
   */
  private val _funcionarioLogado = MutableLiveData<FuncionarioEntity?>()

  /**
   * LiveData pública para observação da sessão atual do funcionário logado.
   */
  val funcionarioLogado: LiveData<FuncionarioEntity?> get() = _funcionarioLogado

  /**
   * Realiza o login lógico de um funcionário, armazenando-o em memória.
   *
   * @param funcionario Funcionário que está efetuando login.
   */
  fun logarFuncionario(funcionario: FuncionarioEntity) {
    _funcionarioLogado.value = funcionario
  }

  /**
   * Realiza o logout do funcionário, limpando a sessão atual.
   */
  fun deslogarFuncionario() {
    _funcionarioLogado.value = null
  }

  /**
   * LiveData adicional para acesso à lista de funcionários (duplicada de `listaFuncionarios`).
   *
   * Pode ser utilizada separadamente caso precise de uma segunda referência.
   */
  val listasFuncionarios = funcionarioDao.listarTodosFuncionarios()

  /**
   * Insere um novo funcionário no banco de dados.
   *
   * @param funcionario Funcionário a ser inserido.
   */
  fun inserir(funcionario: FuncionarioEntity) = viewModelScope.launch {
    funcionarioDao.inserirFuncionario(funcionario)
  }

  /**
   * Atualiza os dados de um funcionário existente.
   *
   * @param funcionario Funcionário com dados modificados.
   */
  fun atualizar(funcionario: FuncionarioEntity) = viewModelScope.launch {
    funcionarioDao.atualizarFuncionario(funcionario)
  }

  /**
   * Remove um funcionário do banco de dados.
   *
   * @param funcionario Funcionário a ser removido.
   */
  fun deletar(funcionario: FuncionarioEntity) = viewModelScope.launch {
    funcionarioDao.deletarFuncionario(funcionario)
  }
}
