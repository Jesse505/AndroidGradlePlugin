package com.android.jesse.plugin

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_second.*

class SecondActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)
        btnToast.setOnClickListener {
            Toast.makeText(this, "Kotlin点击了按钮", Toast.LENGTH_SHORT).show()
        }
        btnToast2.setOnClickListener {
            Toast.makeText(this, "Kotlin2点击了按钮", Toast.LENGTH_SHORT).show()
        }
    }
}