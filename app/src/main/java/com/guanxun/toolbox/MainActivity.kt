package com.guanxun.toolbox

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.guanxun.util.GXTimeWheel
import com.guanxun.util.GXWheel
import java.time.LocalTime

class MainActivity : AppCompatActivity() {
    var hour: GXWheel? = null
    var p: GXTimeWheel? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        p=findViewById(R.id.p)
        p?.apply {
            //minuteList=(0..59 step 5).toList()
            setValidTimeRange(19,0,5,45)
            onSelectionChangedListener={ t ->
                Toast.makeText(context, "$t", Toast.LENGTH_SHORT).show()
            }
            currentTime= LocalTime.of(5,45)
            use12Hour=true
        }

//        hour?.data=(0..6).toList()
//        minute=findViewById(R.id.minute)
//        minute?.data = (8..16).toList()
    }
}