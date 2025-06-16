package com.example.appsenkaspi.ui.perfil

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.appsenkaspi.ui.main.MainActivity
import com.example.appsenkaspi.viewmodel.NotificacaoViewModel
import com.example.appsenkaspi.R
import com.example.appsenkaspi.data.local.dao.FuncionarioDao
import com.example.appsenkaspi.data.local.database.AppDatabase
import com.example.appsenkaspi.extensions.configurarNotificacaoBadge
import com.example.appsenkaspi.viewmodel.FuncionarioViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * Fragmento responsável por exibir o perfil do funcionário logado, incluindo:
 * - Foto de perfil e banner, com opção de edição e persistência local.
 * - Tabs com seções de "Detalhes" e "Meus Trabalhos".
 * - Nome do usuário.
 * - Integração com sistema de notificações.
 * - Botão de logout com confirmação.
 *
 * Funcionalidades:
 * - Imagens são redimensionadas e salvas internamente com nomes únicos baseados no ID do funcionário.
 * - As imagens e informações são persistidas em banco local (Room) e exibidas com Glide.
 * - Utiliza `SharedPreferences` para obter o ID do usuário logado.
 * - Lida com permissões e intents para seleção de imagens via `ActivityResultLauncher`.
 * - Realiza logout limpando as preferências e redirecionando para `MainActivity`.
 *
 * Dependências:
 * - ViewModel: [FuncionarioViewModel], [NotificacaoViewModel]
 * - DAO: [FuncionarioDao]
 * - Layout: `fragment_perfil.xml` com `TabLayout`, `ViewPager2`, `ImageView`, `TextView`, e botões de edição/logout.
 */
class PerfilFragment : Fragment() {

  // Imagens de perfil e banner
  private lateinit var imageViewPerfil: ImageView
  private lateinit var imageViewBanner: ImageView

  // Launchers para seleção de imagem via galeria
  private lateinit var perfilImageLauncher: ActivityResultLauncher<Intent>
  private lateinit var bannerImageLauncher: ActivityResultLauncher<Intent>

  // Identificador do funcionário e DAO
  private var idFuncionarioLogado: Int = -1
  private lateinit var funcionarioDao: FuncionarioDao

  // ViewModels compartilhados com a atividade
  private val funcionarioViewModel: FuncionarioViewModel by activityViewModels()
  private val notificacaoViewModel: NotificacaoViewModel by activityViewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Recupera ID do funcionário salvo em SharedPreferences
    val prefs = requireContext().getSharedPreferences("funcionario_prefs", Context.MODE_PRIVATE)
    idFuncionarioLogado = prefs.getInt("funcionario_id", -1)

    funcionarioDao = AppDatabase.getDatabase(requireContext()).funcionarioDao()

    // Configura a seleção e salvamento da imagem de perfil
    perfilImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        val uri = result.data?.data
        uri?.let {
          val nomeArquivo = "perfil_usuario_$idFuncionarioLogado.jpg"
          val uriInterna = redimensionarESalvarImagem(it, nomeArquivo, 76, 76)
          uriInterna?.let { finalUri ->
            Glide.with(requireContext())
              .load(File(finalUri.path!!)).circleCrop()
              .placeholder(R.drawable.ic_icone_perfil)
              .into(imageViewPerfil)

            lifecycleScope.launch {
              funcionarioDao.getFuncionarioById(idFuncionarioLogado)?.let { funcionario ->
                funcionarioDao.atualizarFuncionario(funcionario.copy(fotoPerfil = finalUri.toString()))
              }
            }
          }
        }
      }
    }

    // Configura a seleção e salvamento da imagem de banner
    bannerImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        val uri = result.data?.data
        uri?.let {
          val nomeArquivo = "banner_usuario_$idFuncionarioLogado.jpg"
          val uriInterna = redimensionarESalvarImagem(it, nomeArquivo, 1080, 600)
          uriInterna?.let { finalUri ->
            imageViewBanner.setImageURI(finalUri)
            lifecycleScope.launch {
              funcionarioDao.getFuncionarioById(idFuncionarioLogado)?.let { funcionario ->
                funcionarioDao.atualizarFuncionario(funcionario.copy(fotoBanner = finalUri.toString()))
              }
            }
          }
        }
      }
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    val view = inflater.inflate(R.layout.fragment_perfil, container, false)
    val nomeUsuarioTextView = view.findViewById<TextView>(R.id.user_name)

    // Configura badge de notificação
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

    // Configura tabs e ViewPager
    val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
    val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)
    viewPager.adapter = PerfilPagerAdapter(this)

    TabLayoutMediator(tabLayout, viewPager) { tab, position ->
      tab.text = when (position) {
        0 -> "Detalhes"
        1 -> "Meus Trabalhos"
        else -> ""
      }
    }.attach()

    imageViewPerfil = view.findViewById(R.id.profile_pic)
    imageViewBanner = view.findViewById(R.id.banner)

    // Botões para editar imagens
    view.findViewById<ImageButton>(R.id.edit_profile_pic_button).setOnClickListener {
      val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
      perfilImageLauncher.launch(intent)
    }

    view.findViewById<ImageButton>(R.id.edit_banner_button).setOnClickListener {
      val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
      bannerImageLauncher.launch(intent)
    }

    // Botão de logout
    view.findViewById<Button>(R.id.logoutButton).setOnClickListener {
      mostrarConfirmacaoLogout()
    }

    // Carrega informações do usuário e imagens
    lifecycleScope.launch {
      val funcionario = funcionarioDao.getFuncionarioById(idFuncionarioLogado)
      nomeUsuarioTextView.text = funcionario?.nomeCompleto ?: "Usuário"

      funcionario?.fotoPerfil?.let { uri ->
        Glide.with(requireContext())
          .load(if (uri.startsWith("file://")) File(Uri.parse(uri).path!!) else R.drawable.ic_icone_perfil)
          .circleCrop()
          .into(imageViewPerfil)
      }

      funcionario?.fotoBanner?.let { uri ->
        if (uri.startsWith("file://")) {
          imageViewBanner.setImageURI(Uri.parse(uri))
        } else {
          imageViewBanner.setImageResource(R.drawable.ic_imagemfundo)
        }
      }
    }

    return view
  }

  /**
   * Exibe um diálogo de confirmação para logout.
   */
  private fun mostrarConfirmacaoLogout() {
    AlertDialog.Builder(requireContext())
      .setTitle("Confirmação")
      .setMessage("Deseja realmente sair?")
      .setPositiveButton("Sim") { dialog, _ ->
        dialog.dismiss()
        realizarLogout()
      }
      .setNegativeButton("Não") { dialog, _ ->
        dialog.dismiss()
      }
      .create()
      .show()
  }

  /**
   * Limpa preferências do usuário logado e redireciona para a tela principal.
   */
  private fun realizarLogout() {
    val prefs = requireContext().getSharedPreferences("funcionario_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().apply()

    val intent = Intent(requireContext(), MainActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

    val options = ActivityOptionsCompat.makeCustomAnimation(
      requireContext(),
      android.R.anim.fade_in,
      android.R.anim.fade_out
    )

    startActivity(intent, options.toBundle())
    requireActivity().finish()
  }

  /**
   * Redimensiona uma imagem e salva no diretório interno do app.
   *
   * @param uriOriginal URI da imagem original selecionada
   * @param nomeArquivo Nome do arquivo salvo localmente
   * @param largura Largura em pixels
   * @param altura Altura em pixels
   * @return URI da imagem salva localmente ou null em caso de erro
   */
  private fun redimensionarESalvarImagem(uriOriginal: Uri, nomeArquivo: String, largura: Int, altura: Int): Uri? {
    return try {
      val source = ImageDecoder.createSource(requireContext().contentResolver, uriOriginal)
      val bitmapOriginal = ImageDecoder.decodeBitmap(source)
      val bitmapRedimensionado = Bitmap.createScaledBitmap(bitmapOriginal, largura, altura, true)

      val arquivo = File(requireContext().filesDir, nomeArquivo)
      FileOutputStream(arquivo).use { out ->
        bitmapRedimensionado.compress(Bitmap.CompressFormat.JPEG, 90, out)
      }

      Uri.fromFile(arquivo)
    } catch (e: Exception) {
      Log.e("PerfilFragment", "Erro ao salvar imagem redimensionada", e)
      null
    }
  }
}
