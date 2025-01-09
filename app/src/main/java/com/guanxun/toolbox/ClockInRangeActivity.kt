package com.guanxun.toolbox

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.guanxun.util.GXTimeWheel
import java.time.LocalTime

class ClockInRangeActivity : AppCompatActivity() {
    private var tvRange: TextView? = null
    private var tvSelection: TextView? = null
    private var timePicker: GXTimeWheel? = null
    private val rangeStart: LocalTime = LocalTime.of(9, 5)
    private val rangeEnd: LocalTime = LocalTime.of(15, 30)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_clock_in_range)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        tvRange=findViewById(R.id.tvRange)
        tvSelection = findViewById(R.id.tvSelection)
        timePicker = findViewById(R.id.timePicker)
        tvRange?.text = "可选时间范围: $rangeStart - $rangeEnd"
        timePicker?.apply {
            minuteList = (0..59 step 5).toList()
            setValidTimeRange(rangeEnd.hour, rangeEnd.minute, rangeStart.hour, rangeStart.minute)
            this.onSelectionChangedListener = { t ->
                "当前选择: $t".also { tvSelection?.text = it }
            }
        }

    }
}