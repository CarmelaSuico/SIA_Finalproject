package ph.edu.usc.wordle_to_go;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class GameActivity extends AppCompatActivity {

    private View btnBack;
    private View btnLeaderboard;
    private LinearLayout statisticsPopup;
    private GridLayout wordGrid;
    private View keyboardContainer;

    private ArrayList<TextView> cells = new ArrayList<>();
    private int wordLength;
    private int currentRow = 0;
    private int currentCol = 0;
    private final int maxRows = 6;
    private String targetWord = "APPLE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        btnBack = findViewById(R.id.btnBack);
        btnLeaderboard = findViewById(R.id.btnLeaderboard);
        statisticsPopup = findViewById(R.id.statisticsPopup);
        wordGrid = findViewById(R.id.wordGrid);
        keyboardContainer = findViewById(R.id.keyboardContainer);

        wordLength = getIntent().getIntExtra("WORD_LENGTH", 5);
        targetWord = getTargetWord(wordLength);

        createGrid(wordLength);
        setupKeyboard(keyboardContainer);

        btnBack.setOnClickListener(v -> finish());

        btnLeaderboard.setOnClickListener(v -> {
            if (statisticsPopup.getVisibility() == View.VISIBLE) {
                statisticsPopup.setVisibility(View.GONE);
            } else {
                statisticsPopup.setVisibility(View.VISIBLE);
            }
        });

        statisticsPopup.setOnClickListener(v -> statisticsPopup.setVisibility(View.GONE));
    }

    private void createGrid(int wordLength) {
        wordGrid.removeAllViews();
        cells.clear();
        wordGrid.setColumnCount(wordLength);
        wordGrid.setRowCount(maxRows);

        int cellSize = getCellSize(wordLength);
        int margin = dpToPx(2);

        for (int i = 0; i < wordLength * maxRows; i++) {
            TextView cell = new TextView(this);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = cellSize;
            params.height = cellSize;
            params.setMargins(margin, margin, margin, margin);

            cell.setLayoutParams(params);
            cell.setBackgroundResource(R.drawable.grid_cell);
            cell.setGravity(Gravity.CENTER);
            cell.setTextColor(Color.WHITE);
            cell.setTextSize(22);
            cell.setTypeface(null, Typeface.BOLD);

            cells.add(cell);
            wordGrid.addView(cell);
        }
    }

    private void setupKeyboard(View view) {
        if (view instanceof TextView) {
            TextView key = (TextView) view;
            key.setOnClickListener(v -> handleKey(key.getText().toString()));
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++) {
                setupKeyboard(group.getChildAt(i));
            }
        }
    }

    private void handleKey(String key) {
        if (key.equals("ENTER")) {
            submitGuess();
        } else if (key.equals("⌫")) {
            deleteLetter();
        } else if (key.length() == 1 && currentCol < wordLength && currentRow < maxRows) {
            int index = currentRow * wordLength + currentCol;
            cells.get(index).setText(key);
            currentCol++;
        }
    }

    private void deleteLetter() {
        if (currentCol > 0) {
            currentCol--;
            int index = currentRow * wordLength + currentCol;
            cells.get(index).setText("");
        }
    }

    private void submitGuess() {
        if (currentCol < wordLength) {
            Toast.makeText(this, "Not enough letters", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder guess = new StringBuilder();

        for (int i = 0; i < wordLength; i++) {
            int index = currentRow * wordLength + i;
            guess.append(cells.get(index).getText().toString());
        }

        checkGuess(guess.toString());

        if (guess.toString().equals(targetWord)) {
            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show();
            return;
        }

        currentRow++;
        currentCol = 0;

        if (currentRow == maxRows) {
            Toast.makeText(this, "Game Over! Word was " + targetWord, Toast.LENGTH_LONG).show();
        }
    }

    private void checkGuess(String guess) {
        for (int i = 0; i < wordLength; i++) {
            int index = currentRow * wordLength + i;
            TextView cell = cells.get(index);
            String letter = String.valueOf(guess.charAt(i));

            if (letter.equals(String.valueOf(targetWord.charAt(i)))) {
                cell.setBackgroundColor(getResources().getColor(R.color.green));
            } else if (targetWord.contains(letter)) {
                cell.setBackgroundColor(getResources().getColor(R.color.yellow));
            } else {
                cell.setBackgroundColor(getResources().getColor(R.color.gray_box));
            }
        }
    }

    private String getTargetWord(int length) {
        if (length == 6) return "PLANET";
        if (length == 7) return "JOURNEY";
        if (length == 8) return "NOTEBOOK";
        if (length == 9) return "CHALLENGE";
        return "APPLE";
    }

    private int getCellSize(int wordLength) {
        if (wordLength <= 5) return dpToPx(48);
        if (wordLength == 6) return dpToPx(44);
        if (wordLength == 7) return dpToPx(39);
        if (wordLength == 8) return dpToPx(35);
        return dpToPx(31);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}