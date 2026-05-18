package ph.edu.usc.wordle_to_go;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class GameStateManager {
    private static final String PREF_NAME = "WordlePrefs_"; // Base preference name template
    private static final String KEY_STREAK = "streak_count";
    private static final String KEY_LAST_DATE = "last_played_date";
    private static final String KEY_DAILY_ANSWER = "daily_answer_";
    private static final String KEY_GRID_DATA = "grid_data_";
    private static final String KEY_GAME_FINISHED = "game_finished_";

    private final SharedPreferences prefs;
    private final String userId; // Scopes local caching storage files to the active user session

    // FIXED: Constructor now accepts both Context and UID string parameters
    public GameStateManager(Context context, String userId) {
        this.userId = userId;
        // Scopes the SharedPreferences file name explicitly to the logged-in user: WordlePrefs_YOURUID
        this.prefs = context.getSharedPreferences(PREF_NAME + userId, Context.MODE_PRIVATE);
    }

    public String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    public String getYesterdayDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
    }

    public int getStreak() {
        return prefs.getInt(KEY_STREAK, 0);
    }

    /**
     * Increments the streak upon a successful win.
     * Marks the current day's answer status as 1.
     */
    public void updateStreak() {
        String today = getTodayDate();
        String yesterday = getYesterdayDate();
        String lastDate = prefs.getString(KEY_LAST_DATE, "");

        // Already solved today, safe-guard against multiple updates
        if (getDailyAnswer(today) == 1) {
            return;
        }

        setDailyAnswer(today, 1);
        int currentStreak = getStreak();

        if (lastDate.isEmpty()) {
            currentStreak = 1;
        } else if (lastDate.equals(yesterday)) {
            // Check if they played yesterday but failed (0 status). If so, reset to 1 instead.
            if (getDailyAnswer(yesterday) == 0) {
                currentStreak = 1;
            } else {
                currentStreak++;
            }
        } else if (lastDate.equals(today)) {
            return;
        } else {
            // Gap detected (skipped days)
            currentStreak = 1;
        }

        prefs.edit()
                .putInt(KEY_STREAK, currentStreak)
                .putString(KEY_LAST_DATE, today)
                .apply();
    }

    /**
     * Checks temporal integrity. If a whole day was skipped, or they explicitly
     * failed yesterday (dailyAnswer = 0), the current streak drops to zero.
     */
    public void resetStreakIfMissed() {
        String today = getTodayDate();
        String yesterday = getYesterdayDate();
        String lastDate = prefs.getString(KEY_LAST_DATE, "");

        if (lastDate.isEmpty()) return;

        // Condition 1: Skipped a day entirely
        if (!lastDate.equals(today) && !lastDate.equals(yesterday)) {
            prefs.edit().putInt(KEY_STREAK, 0).apply();
            return;
        }

        // Condition 2: Played yesterday but failed to find the target word (0)
        if (lastDate.equals(yesterday) && getDailyAnswer(yesterday) == 0) {
            prefs.edit().putInt(KEY_STREAK, 0).apply();
        }
    }

    public void setDailyAnswer(String date, int status) {
        prefs.edit().putInt(KEY_DAILY_ANSWER + date, status).apply();
    }

    public int getDailyAnswer(String date) {
        return prefs.getInt(KEY_DAILY_ANSWER + date, 0); // Defaults to 0 (Not answered / failed)
    }

    public void saveGridState(int wordLength, String gridJson, boolean finished) {
        String today = getTodayDate();
        prefs.edit()
                .putString(KEY_GRID_DATA + today + "_" + wordLength, gridJson)
                .putBoolean(KEY_GAME_FINISHED + today + "_" + wordLength, finished)
                .apply();
    }

    public String getSavedGridData(int wordLength) {
        return prefs.getString(KEY_GRID_DATA + getTodayDate() + "_" + wordLength, null);
    }

    public boolean isGameFinishedToday(int wordLength) {
        return prefs.getBoolean(KEY_GAME_FINISHED + getTodayDate() + "_" + wordLength, false);
    }
}