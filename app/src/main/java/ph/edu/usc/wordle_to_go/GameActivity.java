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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;

public class GameActivity extends AppCompatActivity {

    private View btnBack, btnLeaderboard;
    private LinearLayout statisticsPopup;
    private GridLayout wordGrid;
    private ViewGroup keyboardContainer;

    private ArrayList<TextView> cells = new ArrayList<>();
    private HashMap<String, Integer> keyState = new HashMap<>();

    // Cache memory data structure for O(1) instantaneous guess validation
    private HashSet<String> validWordsCache = new HashSet<>();

    private int wordLength;
    private int currentRow = 0;
    private int currentCol = 0;
    private final int maxRows = 6;
    private String targetWord = "";
    private boolean isGameOver = false;
    private GameStateManager gameStateManager;
    private TextView txtStreakDisplay;

    private final String MASTER_WORD_LIST_URL = "https://raw.githubusercontent.com/dwyl/english-words/master/words_alpha.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // FIX 3: Redirect to login if user is not authenticated
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_game);

        btnBack = findViewById(R.id.btnBack);
        btnLeaderboard = findViewById(R.id.btnLeaderboard);
        statisticsPopup = findViewById(R.id.statisticsPopup);
        wordGrid = findViewById(R.id.wordGrid);
        keyboardContainer = findViewById(R.id.keyboardContainer);
        txtStreakDisplay = findViewById(R.id.txtStreakDisplay);

        // FIX 1: Pass user UID to GameStateManager to scope SharedPreferences keys
        gameStateManager = new GameStateManager(this, user.getUid());
        gameStateManager.resetStreakIfMissed();
        updateStreakUI();

        wordLength = getIntent().getIntExtra("WORD_LENGTH", 5);

        createGrid(wordLength);
        setupKeyboard(keyboardContainer);

        // Fetch the list, parse it into the HashSet cache, and pick today's target word
        generateDailyWord();

        btnBack.setOnClickListener(v -> finish());
        btnLeaderboard.setOnClickListener(v -> {
            startActivity(new Intent(this, LeaderboardActivity.class));
        });
    }

    private void updateStreakUI() {
        txtStreakDisplay.setText("🔥 " + gameStateManager.getStreak());
    }

    private void restoreSavedState() {
        String savedData = gameStateManager.getSavedGridData(wordLength);
        if (savedData != null) {
            try {
                JSONArray array = new JSONArray(savedData);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    String letter = obj.getString("l").trim();
                    int status = obj.getInt("s");
                    TextView cell = cells.get(i);
                    cell.setText(letter);
                    applyCellColor(cell, letter, status);
                }

                isGameOver = gameStateManager.isGameFinishedToday(wordLength);
                if (isGameOver) {
                    currentRow = maxRows;
                } else {
                    for (int r = 0; r < maxRows; r++) {
                        boolean rowEmpty = true;
                        for (int c = 0; c < wordLength; c++) {
                            if (!cells.get(r * wordLength + c).getText().toString().isEmpty()) {
                                rowEmpty = false;
                                break;
                            }
                        }
                        if (rowEmpty) {
                            currentRow = r;
                            break;
                        }
                        if (r == maxRows - 1) currentRow = maxRows;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void applyCellColor(TextView cell, String letter, int status) {
        int color;
        if (status == 2) {
            cell.setBackgroundResource(R.drawable.rounded_green);
            color = Color.parseColor("#6AAA64");
        } else if (status == 1) {
            cell.setBackgroundResource(R.drawable.rounded_yellow);
            color = Color.parseColor("#C9B458");
        } else if (status == 0) {
            cell.setBackgroundResource(R.drawable.rounded_gray);
            color = Color.parseColor("#3A3A3C");
        } else {
            return;
        }
        cell.setTag(status);
        updateKeyboardColor(letter, status, color);
    }

    private String getFallbackWord(int length) {
        switch (length) {
            case 6: return "PLANET";
            case 7: return "JOURNEY";
            case 8: return "NOTEBOOK";
            case 9: return "CHALLENGE";
            default: return "BOOKS";
        }
    }

    private void generateDailyWord() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(MASTER_WORD_LIST_URL).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> targetWord = getFallbackWord(wordLength));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String content = response.body().string();
                    String[] allWords = content.split("\r?\n");

                    ArrayList<String> filteredWords = new ArrayList<>();
                    HashSet<String> tempCache = new HashSet<>();

                    // Single-pass optimization: populate our data cache and selection array simultaneously
                    for (String w : allWords) {
                        String clean = w.trim().toUpperCase();
                        if (clean.length() == wordLength) {
                            filteredWords.add(clean);
                            tempCache.add(clean);
                        }
                    }

                    if (filteredWords.isEmpty()) {
                        runOnUiThread(() -> targetWord = getFallbackWord(wordLength));
                        return;
                    }

                    // Save the temporary set into our global lookup cache memory
                    validWordsCache = tempCache;

                    Calendar cal = Calendar.getInstance();
                    int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
                    int year = cal.get(Calendar.YEAR);

                    int index = (dayOfYear + year) % filteredWords.size();
                    targetWord = filteredWords.get(index);
                    Log.d("WORDLE_LOGIC", "Today's " + wordLength + "-letter Word: " + targetWord);

                    runOnUiThread(GameActivity.this::restoreSavedState);
                } else {
                    runOnUiThread(() -> targetWord = getFallbackWord(wordLength));
                }
            }
        });
    }

    private void submitGuess() {
        if (isGameOver) return;
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

        // OFFLINE VALIDATION LOGIC: Check the locally hosted GitHub dataset memory cache
        if (validWordsCache.contains(guess)) {
            applyColoringAndMove(guess);
        } else {
            Toast.makeText(this, "Not a real word!", Toast.LENGTH_SHORT).show();
        }
    }

    private void applyColoringAndMove(String guess) {
        int[] status = new int[wordLength];
        boolean[] targetUsed = new boolean[wordLength];

        for (int i = 0; i < wordLength; i++) {
            if (guess.charAt(i) == targetWord.charAt(i)) {
                status[i] = 2;
                targetUsed[i] = true;
            }
        }

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

        for (int i = 0; i < wordLength; i++) {
            TextView cell = cells.get(currentRow * wordLength + i);
            String letter = String.valueOf(guess.charAt(i));
            applyCellColor(cell, letter, status[i]);
        }

        if (guess.equalsIgnoreCase(targetWord)) {
            isGameOver = true;
            Toast.makeText(this, "Splendid!", Toast.LENGTH_LONG).show();
            gameStateManager.updateStreak();
            updateStreakUI();
            saveGridState(true);
            submitScore(currentRow + 1);
            return;
        }

        currentRow++;
        currentCol = 0;
        if (currentRow == maxRows) {
            isGameOver = true;
            Toast.makeText(this, "The word was: " + targetWord, Toast.LENGTH_LONG).show();
            saveGridState(true);
        } else {
            saveGridState(false);
        }
    }

    private void saveGridState(boolean finished) {
        JSONArray array = new JSONArray();
        for (TextView cell : cells) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("l", cell.getText().toString());
                Object tag = cell.getTag();
                obj.put("s", (tag instanceof Integer) ? (Integer) tag : -1);
                array.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        gameStateManager.saveGridState(wordLength, array.toString(), finished);
    }

    private void submitScore(int attempts) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        String dbUrl = "https://wordletogo-default-rtdb.asia-southeast1.firebasedatabase.app/";
        FirebaseDatabase.getInstance(dbUrl).getReference("Users").child(uid).child("username")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String username = snapshot.getValue(String.class);
                        if (username != null) {
                            String today = gameStateManager.getTodayDate();
                            LeaderboardActivity.LeaderboardEntry entry = new LeaderboardActivity.LeaderboardEntry(
                                    uid,
                                    username,
                                    attempts,
                                    today,
                                    System.currentTimeMillis()
                            );

                            FirebaseFirestore.getInstance().collection("leaderboard")
                                    .document(uid + "_" + today)
                                    .set(entry)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(GameActivity.this, "Score submitted!", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(GameActivity.this, LeaderboardActivity.class));
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void updateKeyboardColor(String letter, int status, int color) {
        if (letter == null || letter.trim().isEmpty()) return;
        String cleanLetter = letter.trim().toUpperCase();

        Integer currentBest = keyState.get(cleanLetter);
        if (currentBest == null || status > currentBest) {
            keyState.put(cleanLetter, status);
            View keyView = findKeyView(keyboardContainer, cleanLetter);
            if (keyView != null && keyView.getBackground() != null) {
                keyView.getBackground().mutate().setTint(color);
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
        if (isGameOver) return;
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
            cell.setTextSize(wordLength > 7 ? 16 : 22);
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
        if (wordLength <= 5) return dpToPx(50);
        if (wordLength == 6) return dpToPx(44);
        if (wordLength == 7) return dpToPx(38);
        if (wordLength == 8) return dpToPx(33);
        return dpToPx(29);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}