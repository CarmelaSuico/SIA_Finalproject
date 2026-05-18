package ph.edu.usc.wordle_to_go;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class GameStateManager {
    private static final String PREF_NAME = "WordlePrefs";
    private static final String KEY_STREAK = "streak_count";
    private static final String KEY_LAST_DATE = "last_played_date";
    private static final String KEY_GRID_DATA = "grid_data_";
    private static final String KEY_GAME_FINISHED = "game_finished_";

    private final SharedPreferences prefs;
    private final String uid; // UID for scoping local data

    public GameStateManager(Context context, String uid) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.uid = uid; // Initialize with current user's UID
    }

    /**
     * FIX 1: Helper to prefix SharedPreferences keys with the user's UID.
     * This ensures different users on the same device have separate game states.
     */
    private String getScopedKey(String key) {
        return uid + "_" + key;
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
        // UID-scoping change
        return prefs.getInt(getScopedKey(KEY_STREAK), 0);
    }

    public void updateStreak() {
        String today = getTodayDate();
        // UID-scoping change
        String lastDate = prefs.getString(getScopedKey(KEY_LAST_DATE), "");

        if (today.equals(lastDate)) {
            // Already played today, don't increment streak again
            return;
        }

        int currentStreak = getStreak();
        if (getYesterdayDate().equals(lastDate)) {
            currentStreak++;
        } else {
            currentStreak = 1;
        }

        prefs.edit()
                // UID-scoping changes
                .putInt(getScopedKey(KEY_STREAK), currentStreak)
                .putString(getScopedKey(KEY_LAST_DATE), today)
                .apply();
    }

    public void resetStreakIfMissed() {
        String today = getTodayDate();
        // UID-scoping change
        String lastDate = prefs.getString(getScopedKey(KEY_LAST_DATE), "");

        if (!today.equals(lastDate) && !getYesterdayDate().equals(lastDate) && !lastDate.isEmpty()) {
            // UID-scoping change
            prefs.edit().putInt(getScopedKey(KEY_STREAK), 0).apply();
        }
    }

    public void saveGridState(int wordLength, String gridJson, boolean finished) {
        String today = getTodayDate();
        prefs.edit()
                // UID-scoping changes
                .putString(getScopedKey(KEY_GRID_DATA + today + "_" + wordLength), gridJson)
                .putBoolean(getScopedKey(KEY_GAME_FINISHED + today + "_" + wordLength), finished)
                .apply();
    }

    public String getSavedGridData(int wordLength) {
        // UID-scoping change
        return prefs.getString(getScopedKey(KEY_GRID_DATA + getTodayDate() + "_" + wordLength), null);
    }

    public boolean isGameFinishedToday(int wordLength) {
        // UID-scoping change
        return prefs.getBoolean(getScopedKey(KEY_GAME_FINISHED + getTodayDate() + "_" + wordLength), false);
    }
}
