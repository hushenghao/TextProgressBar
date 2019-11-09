package com.dede.textprogressbar_demo

import android.animation.ValueAnimator
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pb.setProgressText("初始化文字")
    }

    var animator: ValueAnimator? = null

    fun random(v: View) {
        val random = Random()
        var p = random.nextInt(101)
        pb.setProgress(p, true)
        pb.setProgressText("$p %")
        p = random.nextInt(101)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            pb_normal.setProgress(p, true)
        } else {
            pb_normal.progress = p
        }
    }

    fun anim(view: View) {
        if (animator == null || !animator!!.isRunning) {
            pb.setProgressText("开始了")
            val animator = ValueAnimator.ofInt(0, 101)
            animator.interpolator = LinearInterpolator()
            animator.repeatMode = ValueAnimator.REVERSE
            animator.repeatCount = ValueAnimator.INFINITE
            animator.duration = 8000
            animator.addUpdateListener {
                val p = it.animatedValue as Int
                pb.setProgress(p)
                pb.setProgressText("$p % 了")

                pb_normal.progress = p
            }
            animator.start()
            this.animator = animator
        } else {
            pb.setProgressText("取消了")
            animator!!.cancel()
        }
    }


}
