package ph.edu.usc.wordle_to_go;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class GameActivity extends AppCompatActivity {

    private View btnBack, btnLeaderboard;
    private LinearLayout statisticsPopup;
    private GridLayout wordGrid;
    private ViewGroup keyboardContainer;

    private ArrayList<TextView> cells = new ArrayList<>();
    private HashMap<String, Integer> keyState = new HashMap<>();

    private int wordLength;
    private int currentRow = 0;
    private int currentCol = 0;
    private final int maxRows = 6;
    private String targetWord = "";

    // Public list of 5-letter words on GitHub
    private final String WORD_LIST_URL = "https://raw.githubusercontent.com/charlesreid1/five-letter-words/master/sgb-words.txt";

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

        createGrid(wordLength);
        setupKeyboard(keyboardContainer);

        // Fetch the list and pick today's word
        generateDailyWord();

        btnBack.setOnClickListener(v -> finish());
    }

    private void generateDailyWord() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(WORD_LIST_URL).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> targetWord = "BOOKS"); // Backup
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String content = response.body().string();
                    String[] allWords = content.split("\n");

                    // Get a "Seed" based on the Date
                    Calendar cal = Calendar.getInstance();
                    int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
                    int year = cal.get(Calendar.YEAR);

                    // Simple Math to pick the same word for everyone today
                    int index = (dayOfYear + year) % allWords.length;

                    targetWord = allWords[index].trim().toUpperCase();
                    Log.d("WORDLE_LOGIC", "Today's Word: " + targetWord);
                }
            }
        });
    }

    private void submitGuess() {
        if (targetWord.isEmpty()) return;

        if (currentCol < wordLength) {
            Toast.makeText(this, "Not enough letters", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder guessBuilder = new StringBuilder();
        for (int i = 0; i < wordLength; i++) {
            guessBuilder.append(cells.get(currentRow * wordLength + i).getText());
        }
        String guess = guessBuilder.toString().toUpperCase();

        // Standard Dictionary Check (Using the Free API we used before)
        validateWithFreeDictionary(guess);
    }

    private void validateWithFreeDictionary(String guess) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.dictionaryapi.dev/api/v2/entries/en/" + guess.toLowerCase())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(GameActivity.this, "Network Error", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final boolean isValid = response.isSuccessful();
                runOnUiThread(() -> {
                    if (isValid) {
                        applyColoringAndMove(guess);
                    } else {
                        Toast.makeText(GameActivity.this, "Not a real word!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void applyColoringAndMove(String guess) {
        int[] status = new int[wordLength];
        boolean[] targetUsed = new boolean[wordLength];

        // 1. Greens
        for (int i = 0; i < wordLength; i++) {
            if (guess.charAt(i) == targetWord.charAt(i)) {
                status[i] = 2;
                targetUsed[i] = true;
            }
        }

        // 2. Yellows
        for (int i = 0; i < wordLength; i++) {
            if (status[i] == 2) continue;
            for (int j = 0; j < wordLength; j++) {
                if (!targetUsed[j] && guess.charAt(i) == targetWord.charAt(j)) {
                    status[i] = 1;
                    targetUsed[j] = true;
                    break;
                }
            }
        }

        // 3. UI Update
        for (int i = 0; i < wordLength; i++) {
            TextView cell = cells.get(currentRow * wordLength + i);
            String letter = String.valueOf(guess.charAt(i));
            int color;

            if (status[i] == 2) {
                cell.setBackgroundResource(R.drawable.rounded_green);
                color = Color.parseColor("#6AAA64");
            } else if (status[i] == 1) {
                cell.setBackgroundResource(R.drawable.rounded_yellow);
                color = Color.parseColor("#C9B458");
            } else {
                cell.setBackgroundResource(R.drawable.rounded_gray);
                color = Color.parseColor("#3A3A3C");
            }
            updateKeyboardColor(letter, status[i], color);
        }

        if (guess.equalsIgnoreCase(targetWord)) {
            Toast.makeText(this, "Splendid!", Toast.LENGTH_LONG).show();
            return;
        }

        currentRow++;
        currentCol = 0;
        if (currentRow == maxRows) Toast.makeText(this, "The word was: " + targetWord, Toast.LENGTH_LONG).show();
    }

    private void updateKeyboardColor(String letter, int status, int color) {
        Integer currentBest = keyState.get(letter);
        if (currentBest == null || status > currentBest) {
            keyState.put(letter, status);
            View keyView = findKeyView(keyboardContainer, letter);
            if (keyView != null) {
                keyView.getBackground().setTint(color);
            }
        }
    }

    private View findKeyView(ViewGroup root, String letter) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child instanceof TextView && ((TextView) child).getText().toString().equalsIgnoreCase(letter)) {
                return child;
            } else if (child instanceof ViewGroup) {
                View found = findKeyView((ViewGroup) child, letter);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void handleKey(String key) {
        if (key.equals("ENTER")) submitGuess();
        else if (key.equals("⌫")) {
            if (currentCol > 0) {
                currentCol--;
                cells.get(currentRow * wordLength + currentCol).setText("");
            }
        } else if (key.length() == 1 && currentCol < wordLength && currentRow < maxRows) {
            cells.get(currentRow * wordLength + currentCol).setText(key);
            currentCol++;
        }
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
            params.width = cellSize; params.height = cellSize;
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
            view.setOnClickListener(v -> handleKey(((TextView) v).getText().toString()));
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) setupKeyboard(group.getChildAt(i));
        }
    }

    private int getCellSize(int wordLength) {
        return (wordLength <= 5) ? dpToPx(50) : dpToPx(35);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}