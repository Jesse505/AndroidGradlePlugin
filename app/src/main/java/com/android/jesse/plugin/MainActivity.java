package com.android.jesse.plugin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btnToast).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Java点击了按钮", Toast.LENGTH_SHORT).show();
                MainActivity.this.startActivity(new Intent(MainActivity.this, SecondActivity.class));
            }
        });
        //在编译时通过ASM修改字节码，把Html.fromHtml()静态方法改为HtmlUtil.fromHtml()方法，防止出现NPE
        ((TextView)findViewById(R.id.tvHtml)).setText(Html.fromHtml(null));
    }
}
