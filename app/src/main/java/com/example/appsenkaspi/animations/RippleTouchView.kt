package com.example.appsenkaspi.animations

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * Uma View personalizada que desenha animações circulares (efeitos de toque tipo ripple)
 * sempre que o usuário toca na tela. Cada toque inicia um novo círculo que se expande e desaparece.
 *
 * Utilizado para oferecer feedback visual suave e elegante ao toque.
 */
class RippleTouchView @JvmOverloads constructor(
  context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

  // Lista de efeitos de ripple ativos (um para cada toque)
  private val ripples = mutableListOf<Ripple>()

  // Estilo de pintura dos círculos, com borda branca semi-transparente
  private val paint = Paint().apply {
    color = 0x55FFFFFF.toInt() // branco com transparência
    isAntiAlias = true
    style = Paint.Style.STROKE
    strokeWidth = 6f
  }

  /**
   * Captura eventos de toque e inicia um novo efeito ripple no ponto de toque.
   */
  override fun onTouchEvent(event: MotionEvent): Boolean {
    if (event.action == MotionEvent.ACTION_DOWN) {
      startRipple(event.x, event.y)
    }
    return true
  }

  /**
   * Cria um novo objeto Ripple no ponto especificado e inicia sua animação.
   *
   * @param x coordenada X do toque
   * @param y coordenada Y do toque
   */
  private fun startRipple(x: Float, y: Float) {
    val ripple = Ripple(PointF(x, y))
    ripples.add(ripple)
    ripple.start()
  }

  /**
   * Desenha todos os efeitos ripple ativos no canvas.
   *
   * A lista é copiada para evitar problemas de concorrência com remoções
   * durante a iteração.
   */
  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    for (ripple in ripples.toList()) {
      ripple.draw(canvas)
    }
  }

  /**
   * Classe interna que representa um único efeito ripple animado.
   *
   * Cada instância cuida da animação de crescimento e desvanecimento
   * de um círculo a partir do ponto de toque.
   */
  private inner class Ripple(private val center: PointF) {
    private var radius = 0f
    private var alpha = 255

    // Anima o raio do círculo e reduz sua opacidade com o tempo
    private val animator = ValueAnimator.ofFloat(0f, 80f).apply {
      duration = 600L
      interpolator = LinearInterpolator()
      addUpdateListener {
        radius = it.animatedValue as Float
        alpha = (255 * (1f - it.animatedFraction)).toInt()
        invalidate()
      }
      addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
          ripples.remove(this@Ripple)
        }
      })
    }

    /**
     * Inicia a animação do efeito ripple.
     */
    fun start() {
      animator.start()
    }

    /**
     * Desenha o círculo atual com raio e opacidade baseados na animação.
     *
     * @param canvas a superfície onde o efeito será desenhado
     */
    fun draw(canvas: Canvas) {
      paint.alpha = alpha
      canvas.drawCircle(center.x, center.y, radius, paint)
    }
  }
}
