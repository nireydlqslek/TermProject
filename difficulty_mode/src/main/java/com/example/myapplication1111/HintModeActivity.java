package com.example.myapplication1111;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.graphics.Color;
import android.util.TypedValue;

import java.util.*;

public class HintModeActivity extends AppCompatActivity {

    private static final int SIZE = 9;

    private GridLayout gridLayout;
    private LinearLayout numLayout, topLayout;

    private Button[][] cells = new Button[SIZE][SIZE];

    private int[][] solution = new int[SIZE][SIZE];
    private int[][] puzzle = new int[SIZE][SIZE];
    private int[][] user = new int[SIZE][SIZE];

    private int selectedRow = -1, selectedCol = -1;

    private int hintCount = 3;  // ⭐ 힌트 제한

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hint_mode);

        gridLayout = findViewById(R.id.gridLayout);
        numLayout = findViewById(R.id.numLayout);
        topLayout = findViewById(R.id.topLayout);

        createSudokuGrid();
        createNumberButtons();
        createHintButton();

        generatePuzzle();
        displayBoard(puzzle);
    }

    /** ▣ 힌트 버튼 */
    private void createHintButton() {
        Button hintBtn = new Button(this);
        hintBtn.setText("힌트 (3회)");
        hintBtn.setTextSize(16);

        hintBtn.setOnClickListener(v -> useHint(hintBtn));

        topLayout.addView(hintBtn);
    }

    private void useHint(Button hintBtn) {
        if (hintCount == 0) {
            Toast.makeText(this, "힌트 없음!", Toast.LENGTH_SHORT).show();
            return;
        }

        List<int[]> emptyCells = new ArrayList<>();

        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (puzzle[r][c] == 0)
                    emptyCells.add(new int[]{r, c});

        if (emptyCells.isEmpty()) {
            Toast.makeText(this, "빈칸 없음!", Toast.LENGTH_SHORT).show();
            return;
        }

        int[] pos = emptyCells.get(new Random().nextInt(emptyCells.size()));

        int rr = pos[0], cc = pos[1];
        puzzle[rr][cc] = solution[rr][cc];
        user[rr][cc] = solution[rr][cc];

        cells[rr][cc].setText(String.valueOf(puzzle[rr][cc]));
        cells[rr][cc].setTextColor(Color.BLUE);

        hintCount--;
        hintBtn.setText("힌트 (" + hintCount + "회)");
    }

    /** ▣ 스도쿠 판 UI (7주차 UI 그대로) */
    private void createSudokuGrid() {
        int thin = dp(1), medium = dp(2), thick = dp(4);
        int cell = dp(38);

        gridLayout.removeAllViews();
        gridLayout.setRowCount(SIZE);
        gridLayout.setColumnCount(SIZE);

        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++) {

                Button b = new Button(this);
                b.setText("");
                b.setTextSize(16);
                b.setBackgroundColor(Color.WHITE);

                int rr = r, cc = c;
                b.setOnClickListener(v -> selectCell(rr, cc));

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

    private void selectCell(int r, int c) {
        for (int i = 0; i < 9; i++)
            for (int j = 0; j < 9; j++)
                cells[i][j].setBackgroundColor(Color.WHITE);

        selectedRow = r;
        selectedCol = c;
        cells[r][c].setBackgroundColor(0xFFBBDEFB);
    }

    /** ▣ 숫자 입력 버튼 (네 UI 스타일 적용) */
    private void createNumberButtons() {
        for (int i = 1; i <= 9; i++) {
            Button b = new Button(this);
            b.setText(String.valueOf(i));
            b.setTextSize(18);

            LinearLayout.LayoutParams p =
                    new LinearLayout.LayoutParams(dp(60), dp(45));
            p.setMargins(dp(5), 0, dp(5), 0);
            b.setLayoutParams(p);

            int num = i;
            b.setOnClickListener(v -> inputNumber(num));

            numLayout.addView(b);
        }
    }

    private void inputNumber(int num) {
        if (selectedRow == -1) return;

        // 기본 문제칸은 수정 불가
        if (puzzle[selectedRow][selectedCol] != 0) return;

        user[selectedRow][selectedCol] = num;
        cells[selectedRow][selectedCol].setText(String.valueOf(num));

        if (isValidMove(selectedRow, selectedCol, num))
            cells[selectedRow][selectedCol].setTextColor(Color.BLUE);
        else
            cells[selectedRow][selectedCol].setTextColor(Color.RED);
    }

    /** ▣ 중복 검사 */
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

    /** ▣ 퍼즐 생성 */
    private void generatePuzzle() {
        boolean ok = false;
        int tries = 0;

        while (!ok && tries < 5) {
            clearBoard(solution);
            ok = fillBoard(solution, 0, 0);
            tries++;
        }

        copyBoard(solution, puzzle);

        int removed = 0;
        Random r = new Random();

        while (removed < 45) {
            int row = r.nextInt(9);
            int col = r.nextInt(9);

            if (puzzle[row][col] != 0) {
                puzzle[row][col] = 0;
                removed++;
            }
        }
    }

    private void displayBoard(int[][] b) {
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++) {

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

    /** ▣ 백트래킹 */
    private boolean fillBoard(int[][] b, int r, int c) {
        if (r == SIZE) return true;

        int nr = (c == 8) ? r + 1 : r;
        int nc = (c == 8) ? 0 : c + 1;

        List<Integer> list = new ArrayList<>();
        for (int i = 1; i <= 9; i++) list.add(i);
        Collections.shuffle(list);

        for (int n : list) {
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
            if (b[r][i] == n || b[i][c] == n)
                return false;

        int sr = (r / 3) * 3;
        int sc = (c / 3) * 3;

        for (int i = sr; i < sr + 3; i++)
            for (int j = sc; j < sc + 3; j++)
                if (b[i][j] == n)
                    return false;

        return true;
    }

    private void clearBoard(int[][] b) {
        for (int[] row : b)
            Arrays.fill(row, 0);
    }

    private void copyBoard(int[][] s, int[][] d) {
        for (int i = 0; i < 9; i++)
            System.arraycopy(s[i], 0, d[i], 0, 9);
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }
}
