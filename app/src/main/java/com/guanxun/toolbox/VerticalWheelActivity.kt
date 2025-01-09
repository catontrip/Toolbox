package com.guanxun.toolbox

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.guanxun.util.GXWheel
import java.util.Locale

class VerticalWheelActivity : AppCompatActivity() {
    private var wheel: GXWheel? = null
    private var tvSelection: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_vertical_wheel)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        tvSelection = findViewById(R.id.tvSelection)
        wheel = findViewById(R.id.wheel)
        wheel?.apply {
            data = (0..9).toList()
            customizedFormat = {
                String.format(Locale.getDefault(), "%03d", it)
            }
            onSelectionChangedListener = { dataList, index ->
                tvSelection?.text = "当前选择: ${dataList[index]}"
            }
        }
    }
}