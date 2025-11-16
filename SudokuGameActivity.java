package com.example.term1111;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.*;
import android.util.TypedValue;

import java.util.*;

public class SudokuGameActivity extends AppCompatActivity {

    private static final int SIZE = 9;

    private GridLayout gridLayout;
    private LinearLayout numberLayout;

    private Button[][] cells = new Button[SIZE][SIZE];

    private int[][] solution = new int[SIZE][SIZE];
    private int[][] puzzle = new int[SIZE][SIZE];
    private int[][] user = new int[SIZE][SIZE];

    private int selectedRow = -1;
    private int selectedCol = -1;

    private int emptyCount; // 난이도→ 비울 칸 수

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sudoku_game);

        gridLayout = findViewById(R.id.gridLayout);
        numberLayout = findViewById(R.id.numberLayout);

        String level = getIntent().getStringExtra("level");
        if (level == null) level = "normal";

        switch (level) {
            case "easy": emptyCount = 30; break;
            case "normal": emptyCount = 45; break;
            default: emptyCount = 60; break;
        }

        createSudokuGrid();
        createNumberButtons();

        generatePuzzle();
        displayBoard(puzzle);

        Toast.makeText(this, "난이도: " + level, Toast.LENGTH_SHORT).show();
    }

    /** -------------------------------------------------------------------
     *   1) 스도쿠 보드 UI 생성
     * ------------------------------------------------------------------- */
    private void createSudokuGrid() {
        gridLayout.removeAllViews();
        gridLayout.setRowCount(SIZE);
        gridLayout.setColumnCount(SIZE);

        int thin = dp(1), medium = dp(2), thick = dp(4);
        int cell = dp(38);

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {

                Button b = new Button(this);
                b.setText("");
                b.setTextSize(18);
                b.setBackgroundColor(Color.WHITE);

                int rr = r, cc = c;

                // 📌 셀 클릭 시 하늘색 강조 적용 (네 UI 반영)
                b.setOnClickListener(v -> {
                    selectedRow = rr;
                    selectedCol = cc;
                    highlightSelection();
                });

                GridLayout.LayoutParams p = new GridLayout.LayoutParams();
                p.width = cell;
                p.height = cell;

                int top = thin, left = thin, right = thin, bottom = thin;

                if (r == 0) top = thick;
                else if (r % 3 == 0) top = medium;

                if (c == 0) left = thick;
                else if (c % 3 == 0) left = medium;

                if (r == SIZE - 1) bottom = thick;
                if (c == SIZE - 1) right = thick;

                p.setMargins(left, top, right, bottom);

                gridLayout.addView(b, p);
                cells[r][c] = b;
            }
        }
    }

    /** -------------------------------------------------------------------
     *   1-1) 셀 선택 → 하늘색 강조 (MainActivity에서 가져온 기능)
     * ------------------------------------------------------------------- */
    private void highlightSelection() {
        // 전체 배경 초기화
        for (int i = 0; i < SIZE; i++)
            for (int j = 0; j < SIZE; j++)
                cells[i][j].setBackgroundColor(Color.WHITE);

        // 선택된 곳만 하늘색 강조
        if (selectedRow != -1)
            cells[selectedRow][selectedCol].setBackgroundColor(0xFFBBDEFB);
    }

    /** -------------------------------------------------------------------
     *   2) 숫자 버튼 생성 (MainActivity 스타일 적용)
     * ------------------------------------------------------------------- */
    private void createNumberButtons() {
        for (int n = 1; n <= 9; n++) {

            Button btn = new Button(this);
            btn.setText(String.valueOf(n));
            btn.setTextSize(20);
            btn.setBackgroundColor(Color.LTGRAY);

            // 📌 네 UI의 숫자 버튼 스타일(정사각형) 적용
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(dp(60), dp(45));
            params.setMargins(dp(5), 0, dp(5), 0);
            btn.setLayoutParams(params);

            int num = n;
            btn.setOnClickListener(v -> inputNumber(num));

            numberLayout.addView(btn);
        }
    }

    /** 숫자 입력 + 중복 표시 */
    private void inputNumber(int num) {
        if (selectedRow == -1) return;

        // 문제판 기본 숫자는 수정 불가
        if (puzzle[selectedRow][selectedCol] != 0) return;

        user[selectedRow][selectedCol] = num;
        cells[selectedRow][selectedCol].setText(String.valueOf(num));

        if (isValidMove(selectedRow, selectedCol, num))
            cells[selectedRow][selectedCol].setTextColor(Color.BLUE);
        else
            cells[selectedRow][selectedCol].setTextColor(Color.RED);
    }

    /** -------------------------------------------------------------------
     *   중복 검사 (9주차)
     * ------------------------------------------------------------------- */
    private boolean isValidMove(int r, int c, int n) {

        for (int i = 0; i < SIZE; i++)
            if (i != c && user[r][i] == n) return false;

        for (int i = 0; i < SIZE; i++)
            if (i != r && user[i][c] == n) return false;

        int sr = (r / 3) * 3;
        int sc = (c / 3) * 3;

        for (int i = sr; i < sr + 3; i++)
            for (int j = sc; j < sc + 3; j++)
                if (!(i == r && j == c) && user[i][j] == n) return false;

        return true;
    }

    /** -------------------------------------------------------------------
     *   3) 정답판 생성 + 퍼즐 생성
     * ------------------------------------------------------------------- */
    private void generatePuzzle() {
        boolean ok = false;
        int tries = 0;

        while (!ok && tries < 5) {
            clearBoard(solution);
            ok = fillBoard(solution, 0, 0);
            tries++;
        }

        copyBoard(solution, puzzle);

        Random random = new Random();
        int removed = 0;

        while (removed < emptyCount) {
            int r = random.nextInt(9);
            int c = random.nextInt(9);

            if (puzzle[r][c] != 0) {
                puzzle[r][c] = 0;
                removed++;
            }
        }
    }

    private void displayBoard(int[][] board) {
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == 0) {
                    cells[r][c].setText("");
                    user[r][c] = 0;
                } else {
                    cells[r][c].setText(String.valueOf(board[r][c]));
                    cells[r][c].setTextColor(Color.BLACK);
                    user[r][c] = board[r][c];
                }
            }
    }

    /** -------------------------------------------------------------------
     *   백트래킹
     * ------------------------------------------------------------------- */
    private boolean fillBoard(int[][] b, int r, int c) {
        if (r == SIZE) return true;

        int nr = (c == 8) ? r + 1 : r;
        int nc = (c == 8) ? 0 : c + 1;

        List<Integer> nums = new ArrayList<>();
        for (int i = 1; i <= 9; i++) nums.add(i);
        Collections.shuffle(nums);

        for (int n : nums) {
            if (isValidPlacement(b, r, c, n)) {
                b[r][c] = n;
                if (fillBoard(b, nr, nc)) return true;
                b[r][c] = 0;
            }
        }
        return false;
    }

    private boolean isValidPlacement(int[][] b, int r, int c, int n) {

        for (int i = 0; i < 9; i++)
            if (b[r][i] == n || b[i][c] == n) return false;

        int sr = (r / 3) * 3;
        int sc = (c / 3) * 3;

        for (int i = sr; i < sr + 3; i++)
            for (int j = sc; j < sc + 3; j++)
                if (b[i][j] == n) return false;

        return true;
    }

    private void clearBoard(int[][] b) {
        for (int i = 0; i < 9; i++)
            Arrays.fill(b[i], 0);
    }

    private void copyBoard(int[][] s, int[][] d) {
        for (int i = 0; i < 9; i++)
            System.arraycopy(s[i], 0, d[i], 0, 9);
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
