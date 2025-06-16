package com.example.appsenkaspi.ui.perfil

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appsenkaspi.R
import com.example.appsenkaspi.databinding.FragmentTrabalhosBinding
import com.example.appsenkaspi.ui.atividade.TelaAtividadeFragment
import com.example.appsenkaspi.viewmodel.AtividadeViewModel

/**
 * Fragmento responsável por exibir a aba "Meus Trabalhos" no perfil do usuário.
 *
 * Mostra uma lista de atividades em que o funcionário está envolvido. As atividades são obtidas
 * via ViewModel e associadas ao ID salvo em `SharedPreferences`. Caso não existam atividades,
 * um estado vazio (EmptyState) é exibido.
 */
class TrabalhosFragment : Fragment() {

  private var _binding: FragmentTrabalhosBinding? = null
  private val binding get() = _binding!!

  private val atividadeViewModel: AtividadeViewModel by activityViewModels()

  private lateinit var adapter: AtividadePerfilAdapter

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentTrabalhosBinding.inflate(inflater, container, false)
    return binding.root
  }

  /**
   * Inicializa a visualização com a lista de atividades do funcionário logado.
   *
   * - Recupera o ID do funcionário via SharedPreferences.
   * - Observa as atividades do ViewModel.
   * - Atualiza a RecyclerView com a lista ou exibe uma view de estado vazio.
   */
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val prefs = requireContext().getSharedPreferences("funcionario_prefs", Context.MODE_PRIVATE)
    val funcionarioId = prefs.getInt("funcionario_id", -1)

    if (funcionarioId == -1) {
      Toast.makeText(requireContext(), "Funcionário não identificado!", Toast.LENGTH_SHORT).show()
      return
    }

    configurarRecycler()

    atividadeViewModel
      .listarAtividadesComFuncionariosPorFuncionario(funcionarioId)
      .observe(viewLifecycleOwner) { atividades ->
        if (atividades.isNullOrEmpty()) {
          binding.recyclerAtividades.visibility = View.GONE
          binding.emptyStateView.visibility = View.VISIBLE
        } else {
          binding.recyclerAtividades.visibility = View.VISIBLE
          binding.emptyStateView.visibility = View.GONE
          adapter.submitList(atividades)
        }
      }
  }

  /**
   * Configura o RecyclerView com o adapter de atividades e define o comportamento ao clicar em uma atividade.
   *
   * Ao clicar, navega para [TelaAtividadeFragment] passando o ID da atividade via `arguments`.
   */
  private fun configurarRecycler() {
    adapter = AtividadePerfilAdapter { atividadeComFuncionarios ->
      val fragment = TelaAtividadeFragment().apply {
        arguments = Bundle().apply {
          putInt("atividadeId", atividadeComFuncionarios.atividade.id!!)
        }
      }
      requireActivity().supportFragmentManager.beginTransaction()
        .replace(R.id.main_container, fragment)
        .addToBackStack(null)
        .commit()
    }

    binding.recyclerAtividades.layoutManager = LinearLayoutManager(requireContext())
    binding.recyclerAtividades.adapter = adapter
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
