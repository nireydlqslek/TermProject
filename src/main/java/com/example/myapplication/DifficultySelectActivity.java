package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class DifficultySelectActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_difficulty_select);

        Button easy = findViewById(R.id.btnEasy);
        Button normal = findViewById(R.id.btnNormal);
        Button hard = findViewById(R.id.btnHard);

        easy.setOnClickListener(v -> {
            Intent intent = new Intent(this, TimerActivity.class);
            intent.putExtra("difficulty", "easy");
            startActivity(intent);
        });

        normal.setOnClickListener(v -> {
            Intent intent = new Intent(this, TimerActivity.class);
            intent.putExtra("difficulty", "normal");
            startActivity(intent);
        });

        hard.setOnClickListener(v -> {
            Intent intent = new Intent(this, TimerActivity.class);
            intent.putExtra("difficulty", "hard");
            startActivity(intent);
        });

    }

    private void openGame(String level) {
        Intent intent = new Intent(this, TimerActivity.class);
        intent.putExtra("level", level);
        startActivity(intent);
    }
}
