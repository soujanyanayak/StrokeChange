package com.example.strokechange;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View input = findViewById(R.id.captureImageFab);
        View output = findViewById(R.id.tvPlaceholder);

        input.setOnClickListener(new View.OnClickListener(){
            @Override
            public  void onClick(View v){
                image =
                float regression = runRegression()
            }
        });
    }

}