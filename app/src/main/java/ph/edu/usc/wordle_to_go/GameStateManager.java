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

    public GameStateManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
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

    public void updateStreak() {
        String today = getTodayDate();
        String lastDate = prefs.getString(KEY_LAST_DATE, "");

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
                .putInt(KEY_STREAK, currentStreak)
                .putString(KEY_LAST_DATE, today)
                .apply();
    }

    public void resetStreakIfMissed() {
        String today = getTodayDate();
        String lastDate = prefs.getString(KEY_LAST_DATE, "");

        if (!today.equals(lastDate) && !getYesterdayDate().equals(lastDate) && !lastDate.isEmpty()) {
            prefs.edit().putInt(KEY_STREAK, 0).apply();
        }
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
