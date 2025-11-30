package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class DifficultySelectActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_difficulty_select);

        Button easy = findViewById(R.id.btnEasy);
        Button normal = findViewById(R.id.btnNormal);
        Button hard = findViewById(R.id.btnHard);

        // 새로 추가
        SharedPreferences prefs = getSharedPreferences("sudoku_score", MODE_PRIVATE);

        if (prefs.getBoolean("game_saved", false)) {
            new AlertDialog.Builder(this)
                    .setTitle("이어하기")
                    .setMessage("이전 게임을 이어서 하시겠습니까?")
                    .setPositiveButton("예", (d, w) -> {
                        Intent i = new Intent(this, TimerActivity.class);
                        i.putExtra("continue", true);
                        startActivity(i);
                    })
                    .setNegativeButton("아니오", (d, w) -> {
                        prefs.edit().putBoolean("game_saved", false).apply(); // 저장된 게임 삭제
                    })
                    .show();
        }
        // 」여기까지 새로 추가

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
