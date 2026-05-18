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
    private GridLayout wordGrid;
    private ViewGroup keyboardContainer;

    private ArrayList<TextView> cells = new ArrayList<>();
    private HashMap<String, Integer> keyState = new HashMap<>();

    // High-performance cache lookup memory structure for instantaneous guess validation
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

        // Security Checkpoint: Route back to Login if the user instance token is empty
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_game);

        btnBack = findViewById(R.id.btnBack);
        btnLeaderboard = findViewById(R.id.btnLeaderboard);
        wordGrid = findViewById(R.id.wordGrid);
        keyboardContainer = findViewById(R.id.keyboardContainer);
        txtStreakDisplay = findViewById(R.id.txtStreakDisplay);

        // Scope the State Manager to the active user's UID to isolate streak histories
        gameStateManager = new GameStateManager(this, user.getUid());
        gameStateManager.resetStreakIfMissed();
        updateStreakUI();

        // Configures structural dimension specs dynamically (5, 6, 7, 8, or 9 letters)
        wordLength = getIntent().getIntExtra("WORD_LENGTH", 5);

        createGrid(wordLength);
        setupKeyboard(keyboardContainer);

        // Asynchronously populate vocabulary lookup indices and calculate today's puzzle word
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
                    currentRow = maxRows; // Lock board access input metrics
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

                    // Stream parsing: Populates lookup index and puzzle arrays simultaneously
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

                    // Assign the temp map to our active lookup cache pointer
                    validWordsCache = tempCache;

                    // Deterministic seed math generation based on phone clock metadata calendar variables
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

        // Localized membership verification validation layer check running at O(1) efficiency
        if (validWordsCache.contains(guess)) {
            applyColoringAndMove(guess);
        } else {
            Toast.makeText(this, "Not a real word!", Toast.LENGTH_SHORT).show();
        }
    }

    private void applyColoringAndMove(String guess) {
        int[] status = new int[wordLength];
        boolean[] targetUsed = new boolean[wordLength];

        // First pass parsing check: identify precise matching Green locations
        for (int i = 0; i < wordLength; i++) {
            if (guess.charAt(i) == targetWord.charAt(i)) {
                status[i] = 2;
                targetUsed[i] = true;
            }
        }

        // Second pass parsing check: identify shifting Yellow boundaries
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

        // Apply background drawable assignments across row targets
        for (int i = 0; i < wordLength; i++) {
            TextView cell = cells.get(currentRow * wordLength + i);
            String letter = String.valueOf(guess.charAt(i));
            applyCellColor(cell, letter, status[i]);
        }

        // Win Verification Checkpoint Rule evaluation
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

        // Loss Verification Checkpoint Rule evaluation
        if (currentRow == maxRows) {
            isGameOver = true;
            Toast.makeText(this, "The word was: " + targetWord, Toast.LENGTH_LONG).show();
            gameStateManager.setDailyAnswer(gameStateManager.getTodayDate(), 0); // Mark today as failed (0)
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
        if (uid == null) {
            Log.e("WORDLE_SYNC", "User UID is null! Cannot submit score.");
            return;
        }

        String regionalDbUrl = "https://wordletogo-default-rtdb.asia-southeast1.firebasedatabase.app/";
        FirebaseDatabase dbInstance = FirebaseDatabase.getInstance(regionalDbUrl);

        // 1. Fetch the user's registered username from the profile node path directory
        dbInstance.getReference("Users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String username = snapshot.child("username").getValue(String.class);
                        if (username == null || username.trim().isEmpty()) {
                            username = "User_" + uid.substring(0, 5);
                        }

                        String today = gameStateManager.getTodayDate();
                        int activeStreak = gameStateManager.getStreak();

                        // 2. Package all data properties directly into a standard Map payload allocation
                        java.util.HashMap<String, Object> entry = new java.util.HashMap<>();
                        entry.put("uid", uid);
                        entry.put("username", username);
                        entry.put("attempts", attempts);
                        entry.put("date", today);
                        entry.put("streak", activeStreak);
                        entry.put("timestamp", System.currentTimeMillis());

                        Log.d("WORDLE_SYNC", "Writing score to RTDB path: leaderboard/" + today + "/" + uid);

                        // 3. Save directly to your active Realtime Database tree paths node directory
                        dbInstance.getReference("leaderboard").child(today).child(uid)
                                .setValue(entry)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("WORDLE_SYNC", "RTDB Leaderboard table entry updated successfully!");
                                    Toast.makeText(GameActivity.this, "Score and Streak synced to Cloud!", Toast.LENGTH_SHORT).show();

                                    // Route cleanly over to the scoreboard dashboard presentation activity layout wrapper
                                    Intent intent = new Intent(GameActivity.this, LeaderboardActivity.class);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("WORDLE_SYNC", "RTDB write operation dropped out", e);
                                    Toast.makeText(GameActivity.this, "Cloud Sync Failed!", Toast.LENGTH_SHORT).show();
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("WORDLE_SYNC", "Database path lookup cancelled: " + error.getMessage());
                    }
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