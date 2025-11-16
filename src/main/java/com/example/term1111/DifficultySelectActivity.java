package com.example.term1111;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

public class DifficultySelectActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_difficulty_select);

        Button easy = findViewById(R.id.btnEasy);
        Button normal = findViewById(R.id.btnNormal);
        Button hard = findViewById(R.id.btnHard);

        easy.setOnClickListener(v -> openGame("easy"));
        normal.setOnClickListener(v -> openGame("normal"));
        hard.setOnClickListener(v -> openGame("hard"));
    }

    private void openGame(String level) {
        Intent intent = new Intent(this, SudokuGameActivity.class);
        intent.putExtra("level", level);
        startActivity(intent);
    }
}
