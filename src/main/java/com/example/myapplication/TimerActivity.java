package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.*;
import android.os.CountDownTimer;
import android.content.SharedPreferences;

import java.util.*;

public class TimerActivity extends AppCompatActivity {

    private static final int SIZE = 9;

    private GridLayout gridLayout;
    private LinearLayout numLayout, topLayout;
    private TextView mistakeView;
    private TextView timerView;     // ⬅ 타이머 표시

    private TextView[][] cells = new TextView[SIZE][SIZE];

    private int[][] solution = new int[SIZE][SIZE];
    private int[][] puzzle   = new int[SIZE][SIZE];
    private int[][] user     = new int[SIZE][SIZE];

    private int selectedRow = -1, selectedCol = -1;

    private int hintCount    = 3;
    private int undoCount    = 3;
    private int mistakeCount = 0;

    private boolean hintMode = false;

    private Stack<Move> undoStack = new Stack<>();

    private long startTime = 0;     // ⬅ 타이머 시작 시간
    private CountDownTimer timer;   // ⬅ 타이머 객체
    private long elapsedSeconds = 0;

    private SharedPreferences prefs;    // ⬅ 최고 기록 저장용

    class Move {
        int r, c;
        int oldValue, newValue;
        Move(int r, int c, int oldV, int newV) {
            this.r = r;
            this.c = c;
            this.oldValue = oldV;
            this.newValue = newV;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        gridLayout   = findViewById(R.id.gridLayout);
        numLayout    = findViewById(R.id.numLayout);
        topLayout    = findViewById(R.id.topLayout);
        mistakeView  = findViewById(R.id.mistakeView);

        prefs = getSharedPreferences("sudoku_score", MODE_PRIVATE);

        createTimerText();   // ⬅ 타이머 UI 생성
        createHintButton();
        createUndoButton();
        createEraseButton();

        createSudokuGrid();
        createNumberButtons();

        generatePuzzle();
        displayBoard(puzzle);

        startTimer();        // ⬅ 타이머 시작
    }



    // ----------------------------- 타이머 UI -----------------------------
    private void createTimerText() {
        timerView = new TextView(this);
        timerView.setText("시간: 00:00");
        timerView.setTextSize(18);
        timerView.setPadding(10, 10, 10, 10);

        topLayout.addView(timerView);
    }

    // ----------------------------- 타이머 시작 -----------------------------
    private void startTimer() {
        startTime = System.currentTimeMillis();

        timer = new CountDownTimer(Long.MAX_VALUE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

                long now = System.currentTimeMillis();
                elapsedSeconds = (now - startTime) / 1000;

                long min = elapsedSeconds / 60;
                long sec = elapsedSeconds % 60;

                timerView.setText(String.format("시간: %02d:%02d", min, sec));
            }

            @Override public void onFinish() {}
        };

        timer.start();
    }

    private void stopTimer() {
        if (timer != null) timer.cancel();
    }

    // ----------------------------- 점수 계산 -----------------------------
    private void checkPuzzleCompletion() {

        for (int r=0;r<9;r++)
            for (int c=0;c<9;c++)
                if (user[r][c] != solution[r][c])
                    return;

        // 여기 도달 → 퍼즐 완성!
        stopTimer();

        long score = 10000 - elapsedSeconds * 5;
        if (score < 0) score = 0;

        long bestScore = prefs.getLong("best", 0);

        SharedPreferences.Editor editor = prefs.edit();

        boolean newRecord = false;
        if (score > bestScore) {
            newRecord = true;
            editor.putLong("best", score);
            editor.apply();
        }

        String msg = "점수: " + score;
        if (newRecord) msg += "\n최고 기록 갱신! 🎉";

        new android.app.AlertDialog.Builder(this)
                .setTitle("게임 클리어!")
                .setMessage(msg)
                .setPositiveButton("확인", null)
                .show();
    }



    // ----------------------------- Flat Button -----------------------------
    private void applyFlatButton(Button b) {
        b.setBackgroundColor(Color.parseColor("#EDEDED"));
        b.setElevation(0);
        b.setStateListAnimator(null);
    }


    // --------------------------- 힌트 버튼 ---------------------------
    private void createHintButton() {
        Button btn = new Button(this);
        btn.setText("힌트 (3)");
        applyFlatButton(btn);

        btn.setOnClickListener(v -> {
            if (hintCount == 0) {
                Toast.makeText(this, "힌트 없음!", Toast.LENGTH_SHORT).show();
                return;
            }

            hintMode = true;
            Toast.makeText(this, "힌트 넣을 칸 선택!", Toast.LENGTH_SHORT).show();
        });

        topLayout.addView(btn);
    }


    // --------------------------- 되돌리기 버튼 ---------------------------
    private void createUndoButton() {
        Button btn = new Button(this);
        btn.setText("되돌리기 (3)");
        applyFlatButton(btn);

        btn.setOnClickListener(v -> {
            if (undoCount == 0) {
                Toast.makeText(this, "되돌리기 없음!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (undoStack.isEmpty()) {
                Toast.makeText(this, "기록 없음!", Toast.LENGTH_SHORT).show();
                return;
            }

            Move mv = undoStack.pop();

            // ➤ 실수 복구
            if (mv.newValue != solution[mv.r][mv.c] && mistakeCount > 0) {
                mistakeCount--;
                mistakeView.setText("실수: " + mistakeCount + "/3");
            }

            user[mv.r][mv.c]   = mv.oldValue;
            puzzle[mv.r][mv.c] = mv.oldValue;

            if (mv.oldValue == 0) {
                cells[mv.r][mv.c].setText("");
            } else {
                cells[mv.r][mv.c].setText(String.valueOf(mv.oldValue));
                cells[mv.r][mv.c].setTextColor(Color.BLACK);
            }

            undoCount--;
            btn.setText("되돌리기 (" + undoCount + ")");
        });

        topLayout.addView(btn);
    }


    // --------------------------- 지우기 버튼 ---------------------------
    private void createEraseButton() {
        Button btn = new Button(this);
        btn.setText("지우기");
        applyFlatButton(btn);

        btn.setOnClickListener(v -> {
            if (selectedRow == -1) return;
            if (puzzle[selectedRow][selectedCol] != 0) return;

            cells[selectedRow][selectedCol].setText("");
            user[selectedRow][selectedCol] = 0;
        });

        topLayout.addView(btn);
    }


    // --------------------------- 셀 선택 ---------------------------
    private void selectCell(int r, int c) {

        if (hintMode) {
            if (puzzle[r][c] != 0) {
                Toast.makeText(this, "기본칸은 힌트 불가", Toast.LENGTH_SHORT).show();
                return;
            }

            int oldV = user[r][c];
            int newV = solution[r][c];

            undoStack.push(new Move(r, c, oldV, newV));

            user[r][c]   = newV;
            puzzle[r][c] = newV;

            cells[r][c].setText(String.valueOf(newV));
            cells[r][c].setTextColor(Color.BLUE);

            hintCount--;
            ((Button) topLayout.getChildAt(1)).setText("힌트 (" + hintCount + ")");

            hintMode = false;

            checkPuzzleCompletion();   // ⬅ 클리어 체크
            return;
        }

        // 일반 선택
        for (int i=0;i<9;i++)
            for (int j=0;j<9;j++)
                cells[i][j].setBackgroundColor(Color.WHITE);

        selectedRow = r;
        selectedCol = c;

        cells[r][c].setBackgroundColor(Color.parseColor("#BBDEFB"));
    }


    // --------------------------- 숫자 입력 ---------------------------
    private void inputNumber(int num) {
        if (selectedRow == -1) return;
        if (puzzle[selectedRow][selectedCol] != 0) return;

        TextView cell = cells[selectedRow][selectedCol];

        int old = user[selectedRow][selectedCol];
        undoStack.push(new Move(selectedRow, selectedCol, old, num));

        user[selectedRow][selectedCol] = num;

        if (num == solution[selectedRow][selectedCol]) {
            cell.setTextColor(Color.BLUE);
        } else {
            cell.setTextColor(Color.RED);
            mistakeCount++;
            mistakeView.setText("실수: " + mistakeCount + "/3");
        }

        cell.setText(String.valueOf(num));

        checkPuzzleCompletion();   // ⬅ 클리어 체크
    }


    // --------------------------- 숫자 버튼 ---------------------------
    private void createNumberButtons() {
        for (int n = 1; n <= 9; n++) {

            Button b = new Button(this);
            b.setText(String.valueOf(n));
            b.setTextSize(18);
            applyFlatButton(b);

            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(dp(60), dp(45));
            lp.setMargins(dp(5), 0, dp(5), 0);
            b.setLayoutParams(lp);

            int num = n;
            b.setOnClickListener(v -> inputNumber(num));

            numLayout.addView(b);
        }
    }


    // --------------------------- 스도쿠 보드 ---------------------------
    private void createSudokuGrid() {

        int thin = dp(1), medium = dp(2), thick = dp(4);
        int cellSize = dp(38);

        gridLayout.removeAllViews();
        gridLayout.setRowCount(9);
        gridLayout.setColumnCount(9);

        for (int r=0; r<9; r++) {
            for (int c=0; c<9; c++) {

                TextView tv = new TextView(this);
                tv.setText("");
                tv.setTextSize(18);
                tv.setGravity(Gravity.CENTER);

                tv.setClickable(true);
                tv.setFocusable(false);
                tv.setBackground(null);
                tv.setElevation(0);
                tv.setStateListAnimator(null);

                tv.setBackgroundColor(Color.WHITE);

                int rr=r, cc=c;
                tv.setOnClickListener(v -> selectCell(rr,cc));

                GridLayout.LayoutParams p = new GridLayout.LayoutParams();
                p.width  = cellSize;
                p.height = cellSize;

                int top = thin, left = thin, right = thin, bottom = thin;

                if (r == 0) top = thick;
                else if (r % 3 == 0) top = medium;

                if (c == 0) left = thick;
                else if (c % 3 == 0) left = medium;

                if (r == 8) bottom = thick;
                if (c == 8) right  = thick;

                p.setMargins(left, top, right, bottom);

                gridLayout.addView(tv, p);
                cells[r][c] = tv;
            }
        }
    }


    // --------------------------- 퍼즐 생성 ---------------------------
    private void generatePuzzle() {

        boolean ok = false;
        int tries = 0;

        while (!ok && tries < 5) {
            clearBoard(solution);
            ok = fillBoard(solution, 0, 0);
            tries++;
        }

        copyBoard(solution, puzzle);

        Random r = new Random();
        int removed = 0;

        while (removed < 45) {
            int rr = r.nextInt(9);
            int cc = r.nextInt(9);

            if (puzzle[rr][cc] != 0) {
                puzzle[rr][cc] = 0;
                removed++;
            }
        }
    }


    // --------------------------- 화면 표시 ---------------------------
    private void displayBoard(int[][] b) {
        for (int r=0;r<9;r++)
            for (int c=0;c<9;c++) {
                if (b[r][c] == 0) {
                    cells[r][c].setText("");
                    user[r][c] = 0;
                } else {
                    cells[r][c].setText(String.valueOf(b[r][c]));
                    cells[r][c].setTextColor(Color.BLACK);
                    user[r][c] = b[r][c];
                }
            }
    }


    // --------------------------- 정답판 생성 ---------------------------
    private boolean fillBoard(int[][] b, int r, int c) {
        if (r == 9) return true;

        int nr = (c == 8) ? r+1 : r;
        int nc = (c == 8) ? 0   : c+1;

        List<Integer> nums = new ArrayList<>();
        for (int i=1;i<=9;i++) nums.add(i);
        Collections.shuffle(nums);

        for (int n : nums) {
            if (isValidPlacement(b,r,c,n)) {
                b[r][c] = n;
                if (fillBoard(b,nr,nc)) return true;
                b[r][c] = 0;
            }
        }
        return false;
    }

    private boolean isValidPlacement(int[][] b, int r, int c, int n) {

        for (int i=0;i<9;i++)
            if (b[r][i]==n || b[i][c]==n)
                return false;

        int sr=(r/3)*3, sc=(c/3)*3;
        for (int i=sr;i<sr+3;i++)
            for (int j=sc;j<sc+3;j++)
                if (b[i][j]==n) return false;

        return true;
    }


    private void clearBoard(int[][] b) {
        for (int[] row : b) Arrays.fill(row, 0);
    }

    private void copyBoard(int[][] s, int[][] d) {
        for (int i=0;i<9;i++)
            System.arraycopy(s[i], 0, d[i], 0, 9);
    }


    private int dp(int v) {
        return (int)TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                v,
                getResources().getDisplayMetrics()
        );
    }
}
