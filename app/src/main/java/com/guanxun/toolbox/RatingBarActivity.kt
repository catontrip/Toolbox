package com.guanxun.toolbox

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.guanxun.util.GXRatingBar

class RatingBarActivity : AppCompatActivity() {
    var ratingBar: GXRatingBar? = null
    var tvLabel: TextView? = null
    var tvValue: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_rating_bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        tvLabel=findViewById<TextView?>(R.id.tvLabel)?.apply {
            text = "请您评价"
        }
        tvValue=findViewById<TextView?>(R.id.tvValue)?.apply {

        }
        ratingBar=findViewById<GXRatingBar>(R.id.ratingBar)?.apply {
            rating=0f
            tvValue?.text="得分: ${rating}"
            onRatingChangedListener={  rating ->
                tvValue?.text="得分: ${rating}"
            }

        }
    }
}