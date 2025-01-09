package com.guanxun.toolbox

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.guanxun.util.GXWheel

class MainActivity : AppCompatActivity() {
    private var wheel: GXWheel? = null
    private var demoIntent: Intent? = null
    private var btnDemo: Button? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        wheel = findViewById(R.id.wheel)
        wheel?.apply {
            data = listOf("水平滚轮", "垂直滚轮", "时刻选择", "区间时刻选择")
            onSelectionChangedListener = { dataList, index ->
                demoIntent = prepareIntent(dataList[index].toString())
            }
        }
        btnDemo=findViewById(R.id.btnDemo)
        btnDemo?.setOnClickListener {
            if (demoIntent == null) {
                Toast.makeText(this, "请选择功能", Toast.LENGTH_SHORT).show()
            }else{
                startActivity(demoIntent)
            }
        }
    }

    private fun prepareIntent(choice: String): Intent? {
        return when (choice) {
            "水平滚轮" -> Intent(this, HorizontalWheelActivity::class.java)
            "垂直滚轮" -> Intent(this, VerticalWheelActivity::class.java)
            "区间时刻选择"->Intent(this, ClockInRangeActivity::class.java)
            "时刻选择"->Intent(this, TimePickerActivity::class.java)
            else -> null
        }
    }
}
