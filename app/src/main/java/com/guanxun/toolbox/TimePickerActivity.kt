package com.guanxun.toolbox

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.guanxun.util.GXTimeWheel
import java.time.LocalTime

class TimePickerActivity : AppCompatActivity() {
    private var timePicker: GXTimeWheel?=null
    private var tvClock: TextView?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_time_picker)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        tvClock = findViewById(R.id.tvClock)
        timePicker=findViewById(R.id.timePicker)
        timePicker?.apply {
            this.currentTime= LocalTime.now()
            onSelectionChangedListener={
                tvClock?.text = currentTime.toString()
            }
        }

    }
}