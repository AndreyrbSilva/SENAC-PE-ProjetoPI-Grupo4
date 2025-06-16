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
import android.graphics.Color
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

class PerfilFragment : Fragment() {

  private lateinit var imageViewPerfil: ImageView
  private lateinit var imageViewBanner: ImageView

  private lateinit var perfilImageLauncher: ActivityResultLauncher<Intent>
  private lateinit var bannerImageLauncher: ActivityResultLauncher<Intent>

  private var idFuncionarioLogado: Int = -1
  private lateinit var funcionarioDao: FuncionarioDao

  private val funcionarioViewModel: FuncionarioViewModel by activityViewModels()
  private val notificacaoViewModel: NotificacaoViewModel by activityViewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)


    val prefs = requireContext().getSharedPreferences("funcionario_prefs", Context.MODE_PRIVATE)
    idFuncionarioLogado = prefs.getInt("funcionario_id", -1)

    Log.d("PerfilFragment", "ID do funcionário logado: $idFuncionarioLogado")

    funcionarioDao = AppDatabase.Companion.getDatabase(requireContext()).funcionarioDao()

    perfilImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        val uriOriginal = result.data?.data
        Log.d("PerfilFragment", "URI original da imagem de perfil: $uriOriginal")
        uriOriginal?.let {
          val nomeArquivo = "perfil_usuario_$idFuncionarioLogado.jpg"
          val uriInterna = redimensionarESalvarImagem(it, nomeArquivo, 76, 76)

          uriInterna?.let { finalUri ->
            Log.d("PerfilFragment", "Imagem de perfil salva em: ${finalUri.path}")
            Glide.with(requireContext())
              .load(File(finalUri.path!!))
              .circleCrop()
              .placeholder(R.drawable.ic_icone_perfil)
              .into(imageViewPerfil)

            lifecycleScope.launch {
              funcionarioDao.getFuncionarioById(idFuncionarioLogado)?.let { funcionario ->
                val atualizado = funcionario.copy(fotoPerfil = finalUri.toString())
                funcionarioDao.atualizarFuncionario(atualizado)
                Log.d("PerfilFragment", "fotoPerfil atualizada no banco: ${finalUri}" )
              }
            }
          }
        }
      }
    }

    bannerImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        val uriOriginal = result.data?.data
        Log.d("PerfilFragment", "URI original da imagem do banner: $uriOriginal")
        uriOriginal?.let {
          val nomeArquivo = "banner_usuario_$idFuncionarioLogado.jpg"
          val uriInterna = redimensionarESalvarImagem(it, nomeArquivo, 1080, 600)

          uriInterna?.let { finalUri ->
            imageViewBanner.setImageURI(finalUri)
            Log.d("PerfilFragment", "Imagem de banner salva em: ${finalUri.path}")

            lifecycleScope.launch {
              funcionarioDao.getFuncionarioById(idFuncionarioLogado)?.let { funcionario ->
                val atualizado = funcionario.copy(fotoBanner = finalUri.toString())
                funcionarioDao.atualizarFuncionario(atualizado)
                Log.d("PerfilFragment", "fotoBanner atualizada no banco: ${finalUri}" )
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

    view.findViewById<ImageButton>(R.id.edit_profile_pic_button).setOnClickListener {
      val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
      perfilImageLauncher.launch(intent)
    }

    view.findViewById<ImageButton>(R.id.edit_banner_button).setOnClickListener {
      val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
      bannerImageLauncher.launch(intent)
    }

    // NOVO: Configurar botão logout com confirmação
    val logoutButton = view.findViewById<Button>(R.id.logoutButton)
    logoutButton.setOnClickListener {
      mostrarConfirmacaoLogout()
    }

    lifecycleScope.launch {
      val funcionario = funcionarioDao.getFuncionarioById(idFuncionarioLogado)
      Log.d("PerfilFragment", "Funcionario carregado: $funcionario")

      nomeUsuarioTextView.text = funcionario?.nomeCompleto ?: "Usuário"

      val uriPerfil = funcionario?.fotoPerfil
      if (!uriPerfil.isNullOrBlank() && uriPerfil.startsWith("file://")) {
        Log.d("PerfilFragment", "Carregando imagem de perfil de: $uriPerfil")
        Glide.with(requireContext())
          .load(File(Uri.parse(uriPerfil).path!!))
          .circleCrop()
          .placeholder(R.drawable.ic_icone_perfil)
          .into(imageViewPerfil)
      } else {
        Glide.with(requireContext())
          .load(R.drawable.ic_icone_perfil)
          .circleCrop()
          .into(imageViewPerfil)
      }

      val uriBanner = funcionario?.fotoBanner
      if (!uriBanner.isNullOrBlank() && uriBanner.startsWith("file://")) {
        Log.d("PerfilFragment", "Carregando imagem de banner de: $uriBanner")
        imageViewBanner.setImageURI(Uri.parse(uriBanner))
      } else {
        imageViewBanner.setImageResource(R.drawable.ic_imagemfundo)
      }
    }

    return view
  }

  private fun mostrarConfirmacaoLogout() {
    val dialog = AlertDialog.Builder(requireContext())
      .setTitle("Confirmação")
      .setMessage("Deseja realmente sair?")
      .setPositiveButton("Sim") { dialogInterface, _ ->
        dialogInterface.dismiss()
        realizarLogout()
      }
      .setNegativeButton("Não") { dialogInterface, _ ->
        dialogInterface.dismiss()
      }
      .create()

    dialog.show()

    dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

    val width = (requireContext().resources.displayMetrics.widthPixels * 0.85).toInt()
    dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

    dialog.findViewById<TextView>(android.R.id.message)?.textAlignment = View.TEXT_ALIGNMENT_VIEW_START

    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.RED)
    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.WHITE)
  }

  private fun realizarLogout() {
    val prefs = requireContext().getSharedPreferences("funcionario_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().apply()

    val intent = Intent(requireContext(), MainActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

    val options = ActivityOptionsCompat.makeCustomAnimation(
      requireContext(),
      R.anim.scale_fade_in,
      R.anim.scale_fade_out
    )

    startActivity(intent, options.toBundle())
    requireActivity().finish()
  }

  private fun redimensionarESalvarImagem(uriOriginal: Uri, nomeArquivo: String, largura: Int, altura: Int): Uri? {
    return try {
      val source = ImageDecoder.createSource(requireContext().contentResolver, uriOriginal)
      val bitmapOriginal = ImageDecoder.decodeBitmap(source)
      val bitmapRedimensionado = Bitmap.createScaledBitmap(bitmapOriginal, largura, altura, true)

      val arquivo = File(requireContext().filesDir, nomeArquivo)
      val outputStream = FileOutputStream(arquivo)
      bitmapRedimensionado.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
      outputStream.close()

      Log.d("PerfilFragment", "Imagem salva em: ${arquivo.absolutePath}")
      Uri.fromFile(arquivo)
    } catch (e: Exception) {
      Log.e("PerfilFragment", "Erro ao salvar imagem redimensionada", e)
      null
    }
  }
}
