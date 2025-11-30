package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Stack;

public class TimerActivity extends AppCompatActivity {

    private static final int SIZE = 9;

    private GridLayout gridLayout;
    private LinearLayout numLayout, topLayout;
    private TextView mistakeView, timerView;

    private TextView[][] cells = new TextView[SIZE][SIZE];

    private int[][] solution = new int[SIZE][SIZE];
    private int[][] puzzle   = new int[SIZE][SIZE];
    private int[][] user     = new int[SIZE][SIZE];

    //색상 저장용, 추가
    private int[][] colorBoard = new int[SIZE][SIZE];
    private int selectedRow = -1, selectedCol = -1;

    private int hintCount    = 3;
    private int undoCount    = 3;
    private int mistakeCount = 0;

    private boolean hintMode = false;

    private Stack<Move> undoStack = new Stack<>();

    // 난이도
    private String difficulty = "normal";
    private int removedCount  = 40;

    // 타이머 관련
    private long limitSeconds = 600; // 보통 10분
    private long elapsedSeconds = 0;
    private CountDownTimer timer;

    // 점수 저장
    private SharedPreferences prefs;

    private Button hintButton, undoButton;

    class Move {
        int r, c;
        int oldValue, newValue, oldColor;   //oldColor 추가
        Move(int r, int c, int oldV, int newV, int oldColor) {
            this.r = r;
            this.c = c;
            this.oldValue = oldV;
            this.newValue = newV;
            this.oldColor = oldColor;   //추가
        }
    }

    // 저장, 새로 추가
    private void saveGameState() {
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();

        editor.putBoolean("game_saved", true);

        // JSON으로 변환하여 저장
        editor.putString("saved_puzzle", gson.toJson(puzzle));
        editor.putString("saved_solution", gson.toJson(solution));
        editor.putString("saved_user", gson.toJson(user));
        editor.putString("saved_color", gson.toJson(colorBoard));

        editor.putLong("saved_timer", elapsedSeconds);
        editor.putInt("saved_mistake", mistakeCount);
        editor.putInt("saved_hint", hintCount);
        editor.putInt("saved_undo", undoCount);

        editor.putString("saved_diff", difficulty);

        editor.apply();
    }

    // 불러오기, 새로 추가
    private boolean loadSavedGame() {

        if (!prefs.getBoolean("game_saved", false)) return false;

        Gson gson = new Gson();

        puzzle = gson.fromJson(
                prefs.getString("saved_puzzle", ""),
                new TypeToken<int[][]>(){}.getType()
        );

        solution = gson.fromJson(
                prefs.getString("saved_solution", ""),
                new TypeToken<int[][]>(){}.getType()
        );

        user = gson.fromJson(
                prefs.getString("saved_user", ""),
                new TypeToken<int[][]>(){}.getType()
        );

        colorBoard = gson.fromJson(
                prefs.getString("saved_color", ""),
                new TypeToken<int[][]>(){}.getType()
        );

        elapsedSeconds = prefs.getLong("saved_timer", 0);
        mistakeCount = prefs.getInt("saved_mistake", 0);
        hintCount = prefs.getInt("saved_hint", 3);
        undoCount = prefs.getInt("saved_undo", 3);

        difficulty = prefs.getString("saved_diff", "normal");

        applyDifficultySettings();

        return true;
    }

    // 새로 추가
    @Override
    protected void onPause() {
        super.onPause();
        saveGameState();   // 앱 나갈 때 자동 저장
    }
    
    // 이어서 타이머 시작, 새로 추가
    private void restoreTimer() {
        long remain = limitSeconds - elapsedSeconds;

        timer = new CountDownTimer(remain * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long remain = millisUntilFinished / 1000;
                elapsedSeconds = limitSeconds - remain;

                long min = elapsedSeconds / 60;
                long sec = elapsedSeconds % 60;

                timerView.setText(String.format(Locale.getDefault(),
                        "시간: %02d:%02d", min, sec));
            }

            @Override
            public void onFinish() {
                onTimeOver();
            }
        };

        timer.start();
    }
    //」여기까지 새로 추가

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        gridLayout   = findViewById(R.id.gridLayout);
        numLayout    = findViewById(R.id.numLayout);
        topLayout    = findViewById(R.id.topLayout);
        mistakeView  = findViewById(R.id.mistakeView);

        prefs = getSharedPreferences("sudoku_score", MODE_PRIVATE);

        createTimerText();
        createHintButton();
        createUndoButton();
        createEraseButton();
        createSudokuGrid();
        createNumberButtons();
        
        //이어하기 여부 체크
        boolean isContinue = getIntent().getBooleanExtra("continue", false);

        if (isContinue && loadSavedGame()) {
            displayBoard(user);
            restoreColors();
            restoreButtons();
            restoreTimer();
            return;
        }

        difficulty = getIntent().getStringExtra("difficulty");
        if (difficulty == null) difficulty = "normal";

        applyDifficultySettings();

        generatePuzzle();
        displayBoard(puzzle);
        startTimer();
    }


    // 난이도 설정(빈칸 수 + 제한시간)
    private void applyDifficultySettings() {

        switch (difficulty) {
            case "easy":
                removedCount = 30;      // 쉬움 - 빈칸 30칸
                limitSeconds = 300;     // 5분
                break;

            case "hard":
                removedCount = 50;      // 어려움 - 빈칸 50칸
                limitSeconds = 900;     // 15분
                break;

            default:  // normal
                removedCount = 40;      // 보통 - 빈칸 40칸
                limitSeconds = 600;     // 10분
                break;
        }
    }

    // Timer UI 생성
    private void createTimerText() {
        timerView = new TextView(this);
        timerView.setText("시간: 00:00");
        timerView.setTextSize(18);
        timerView.setPadding(10,10,10,10);
        topLayout.addView(timerView);
    }

    // Timer 0초부터 증가
    private void startTimer() {
        if (timer != null) timer.cancel();

        elapsedSeconds = 0;

        timer = new CountDownTimer(limitSeconds * 1000, 1000) {
            @Override
            public void onTick(long m) {
                long remain = m / 1000;

                elapsedSeconds = limitSeconds - remain;

                long min = elapsedSeconds / 60;
                long sec = elapsedSeconds % 60;

                timerView.setText(String.format(Locale.getDefault(),
                        "시간: %02d:%02d", min, sec));
            }

            @Override
            public void onFinish() {
                onTimeOver();
            }
        };
        timer.start();
    }

    private void stopTimer() {
        if (timer != null) timer.cancel();
    }

    // 시간 종료 → 게임 오버 → 난이도 다시 선택 Dialog
    private void onTimeOver() {
        // 타이머 중지
        stopTimer();

        new AlertDialog.Builder(this)
                .setTitle("게임 오버")
                .setMessage("제한 시간을 초과했습니다.\n난이도를 다시 선택하세요.")
                .setCancelable(false)
                .setPositiveButton("메인 화면", (dialog, which) -> goToMain())  // 중요
                .show();
    }

    // 퍼즐 완성 검사
    private void checkPuzzleCompletion() {

        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (user[r][c] != solution[r][c]) return;

        stopTimer();

        long score = 10000 - elapsedSeconds * 5;
        if (score < 0)  score = 0;

        String key = "best_" + difficulty;
        long best = prefs.getLong(key, 0);

        boolean newRecord = score > best;
        if (newRecord) prefs.edit().putLong(key, score).apply();

        String msg = "현재 점수: " + score + "\n최고 점수: " + Math.max(score, best);
        if (newRecord) msg += "\n\n🎉 최고 기록 갱신!";

        new AlertDialog.Builder(this)
                .setTitle("퍼즐 완료!")
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("새 게임", (d,w)-> showNewGameDialog())
                .setNegativeButton("메인 화면", (d,w)-> goToMain())
                .show();
    }

    // 새 게임 (난이도 다시 고르게 함)
    private void showNewGameDialog() {
        String[] labels = {"쉬움", "보통", "어려움"};
        String[] values = {"easy", "normal", "hard"};

        new AlertDialog.Builder(this)
                .setTitle("난이도 선택")
                .setItems(labels, (dialog, which) -> {
                    difficulty = values[which];
                    applyDifficultySettings();
                    resetGame();
                })
                .show();
    }

    // 리셋 전체
    private void resetGame() {
        hintCount    = 3;
        undoCount    = 3;
        mistakeCount = 0;
        mistakeView.setText("실수: 0/3");
        undoStack.clear();
        selectedRow = selectedCol = -1;

        for (int r=0;r<9;r++)
            for (int c=0;c<9;c++) {
                cells[r][c].setText("");
                cells[r][c].setTextColor(Color.BLACK);
                cells[r][c].setBackgroundColor(Color.WHITE);
                colorBoard[r][c] = 0;   //추가
            }

        generatePuzzle();
        displayBoard(puzzle);
        startTimer();
    }

    // 메인 화면(난이도 선택 화면) 이동 함수
    private void goToMain() {
        Intent intent = new Intent(this, DifficultySelectActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // Flat Button
    private void applyFlat(Button b) {
        b.setBackgroundColor(Color.parseColor("#EDEDED"));
        b.setElevation(0);
        b.setStateListAnimator(null);
    }

    // 힌트 버튼
    private void createHintButton() {
        hintButton = new Button(this);
        hintButton.setText("힌트 (3)");
        hintButton.setTextSize(16);
        applyFlat(hintButton);

        hintButton.setOnClickListener(v -> {
            if (hintCount == 0) {
                Toast.makeText(this,"힌트 없음",Toast.LENGTH_SHORT).show();
                return;
            }
            hintMode = true;
            Toast.makeText(this,"힌트 넣을 칸 선택",Toast.LENGTH_SHORT).show();
        });

        topLayout.addView(hintButton);
    }

    // 되돌리기 버튼
    private void createUndoButton() {
        undoButton = new Button(this);
        undoButton.setText("되돌리기 (3)");
        undoButton.setTextSize(16);
        applyFlat(undoButton);

        undoButton.setOnClickListener(v -> {
            if (undoCount == 0) {
                Toast.makeText(this,"없음!",Toast.LENGTH_SHORT).show();
                return;
            }
            if (undoStack.isEmpty()) {
                Toast.makeText(this,"기록 없음!",Toast.LENGTH_SHORT).show();
                return;
            }

            Move mv = undoStack.pop();

            // 되돌리기 실수 카운트 복구
            if (mv.newValue != 0 && mv.newValue != solution[mv.r][mv.c]) {
                if (mistakeCount > 0) {
                    mistakeCount--;
                    mistakeView.setText("실수: " + mistakeCount + "/3");
                }
            }

            user[mv.r][mv.c] = mv.oldValue;
            puzzle[mv.r][mv.c] = mv.oldValue;

            cells[mv.r][mv.c].setText(mv.oldValue == 0 ? "" : String.valueOf(mv.oldValue));
            cells[mv.r][mv.c].setTextColor(mv.oldColor);

            colorBoard[mv.r][mv.c] = mv.oldColor;

            undoCount--;
            undoButton.setText("되돌리기 (" + undoCount + ")");
        });

        topLayout.addView(undoButton);
    }

    // 지우기 버튼
    private void createEraseButton() {
        Button erase = new Button(this);
        erase.setText("지우기");
        erase.setTextSize(16);
        applyFlat(erase);

        erase.setOnClickListener(v -> {
            if (selectedRow == -1) return;

            cells[selectedRow][selectedCol].setText("");
            user[selectedRow][selectedCol] = 0;
            colorBoard[selectedRow][selectedCol] = 0;   //추가
        });

        topLayout.addView(erase);
    }

    private void restoreButtons() {
        hintButton.setText("힌트 (" + hintCount + ")");
        undoButton.setText("되돌리기 (" + undoCount + ")");
        mistakeView.setText("실수: " + mistakeCount + "/3");
    }

    // 셀 선택
    private void selectCell(int r, int c) {

        if (hintMode) {
            if (puzzle[r][c] != 0) {
                Toast.makeText(this,"기본칸은 힌트불가",Toast.LENGTH_SHORT).show();
                return;
            }

            int oldV = user[r][c];
            int oldColor = colorBoard[r][c];    // 추가
            int newV = solution[r][c];

            undoStack.push(new Move(r,c,oldV,newV,oldColor));

            user[r][c] = newV;
            puzzle[r][c] = newV;

            cells[r][c].setText(String.valueOf(newV));
            cells[r][c].setTextColor(Color.BLUE);
            colorBoard[r][c] = Color.BLUE;  //추가

            hintCount--;
            hintButton.setText("힌트 (" + hintCount + ")");

            hintMode = false;
            checkPuzzleCompletion();
            return;
        }

        for (int i=0;i<9;i++)
            for (int j=0;j<9;j++)
                cells[i][j].setBackgroundColor(Color.WHITE);

        selectedRow = r;
        selectedCol = c;
        cells[r][c].setBackgroundColor(Color.parseColor("#BBDEFB"));
    }

    // 숫자 입력
    private void inputNumber(int num) {
        if (selectedRow == -1) return;

        TextView cell = cells[selectedRow][selectedCol];
        int oldValue = user[selectedRow][selectedCol];
        int oldColor = colorBoard[selectedRow][selectedCol];    //추가

        undoStack.push(new Move(selectedRow,selectedCol,oldValue,num,oldColor));

        user[selectedRow][selectedCol] = num;

        if (num == solution[selectedRow][selectedCol]) {
            cell.setTextColor(Color.BLUE);
            colorBoard[selectedRow][selectedCol] = Color.BLUE;  //추가
        }
        else {
            cell.setTextColor(Color.RED);
            colorBoard[selectedRow][selectedCol] = Color.RED;   //추가
            mistakeCount++;
            mistakeView.setText("실수: " + mistakeCount + "/3");
        }

        cell.setText(String.valueOf(num));

        checkPuzzleCompletion();
    }

    // 색상 저장, 추가
    private void restoreColors() {
        for (int r=0;r<9;r++)
            for (int c=0;c<9;c++)
                if (colorBoard[r][c] != 0)
                    cells[r][c].setTextColor(colorBoard[r][c]);
    }

    // 숫자 버튼
    private void createNumberButtons() {
        for (int n = 1; n <= 9; n++) {
            Button b = new Button(this);
            b.setText(String.valueOf(n));
            b.setTextSize(18);
            applyFlat(b);

            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(dp(60), dp(45));
            lp.setMargins(dp(5), 0, dp(5), 0);
            b.setLayoutParams(lp);

            int num = n;
            b.setOnClickListener(v -> inputNumber(num));

            numLayout.addView(b);
        }
    }

    // Grid 생성
    private void createSudokuGrid() {

        int thin = dp(1), medium = dp(2), thick = dp(4);
        int cellSize = dp(38);

        gridLayout.removeAllViews();
        gridLayout.setRowCount(9);
        gridLayout.setColumnCount(9);

        for (int r=0;r<9;r++) {
            for (int c=0;c<9;c++) {
                TextView tv = new TextView(this);
                tv.setText("");
                tv.setTextSize(18);
                tv.setGravity(Gravity.CENTER);
                tv.setBackgroundColor(Color.WHITE);

                tv.setClickable(true);
                tv.setStateListAnimator(null);
                tv.setElevation(0);

                int rr = r, cc = c;
                tv.setOnClickListener(v -> selectCell(rr,cc));

                GridLayout.LayoutParams p = new GridLayout.LayoutParams();
                p.width  = cellSize;
                p.height = cellSize;

                int top = thin, left = thin, right = thin, bottom = thin;
                if (r==0) top = thick;
                else if (r % 3 == 0) top = medium;

                if (c==0) left = thick;
                else if (c % 3 == 0) left = medium;

                if (r==8) bottom = thick;
                if (c==8) right  = thick;

                p.setMargins(left,top,right,bottom);

                gridLayout.addView(tv,p);
                cells[r][c] = tv;
            }
        }
    }

    // 퍼즐 생성
    private void generatePuzzle() {

        boolean ok = false;
        int tries = 0;

        while (!ok && tries<5) {
            clearBoard(solution);
            ok = fillBoard(solution, 0, 0);
            tries++;
        }

        copyBoard(solution,puzzle);

        Random r = new Random();
        int removed = 0;

        while (removed < removedCount) {
            int rr = r.nextInt(9);
            int cc = r.nextInt(9);

            if (puzzle[rr][cc] != 0) {
                puzzle[rr][cc] = 0;
                removed++;
            }
        }
    }

    // 보드 출력
    private void displayBoard(int[][] b) {
        for (int r=0;r<9;r++)
            for (int c=0;c<9;c++) {
                user[r][c] = b[r][c];

                if (b[r][c] == 0) {
                    cells[r][c].setText("");
                } else {
                    cells[r][c].setText(String.valueOf(b[r][c]));
                    cells[r][c].setTextColor(Color.BLACK);
                }
            }
    }

    // 백트래킹
    private boolean fillBoard(int[][] b, int r, int c) {
        if (r==9) return true;

        int nr = (c==8)? r+1 : r;
        int nc = (c==8)? 0 : c+1;

        List<Integer> nums = new ArrayList<>();
        for (int i=1;i<=9;i++) nums.add(i);
        Collections.shuffle(nums);

        for (int n: nums) {
            if (isValidPlacement(b,r,c,n)) {
                b[r][c] = n;
                if (fillBoard(b,nr,nc)) return true;
                b[r][c] = 0;
            }
        }
        return false;
    }

    private boolean isValidPlacement(int[][] b,int r,int c,int n) {
        for (int i=0;i<9;i++)
            if (b[r][i]==n || b[i][c]==n) return false;

        int sr = (r / 3) * 3;
        int sc = (c / 3) * 3;

        for (int i = sr; i < sr + 3; i++)
            for (int j = sc; j < sc + 3; j++)
                if (b[i][j] == n) return false;

        return true;
    }

    private void clearBoard(int[][] b) {
        for (int[] row : b) Arrays.fill(row,0);
    }

    private void copyBoard(int[][] s,int[][] d) {
        for (int i = 0; i < 9; i++)
            System.arraycopy(s[i],0,d[i],0,9);
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                v,
                getResources().getDisplayMetrics()
        );
    }
}
