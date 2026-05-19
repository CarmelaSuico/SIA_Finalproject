package ph.edu.usc.wordle_to_go;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class GameStateManager {
    private static final String PREF_NAME = "WordlePrefs_";
    private static final String KEY_STREAK = "streak_count";
    private static final String KEY_LAST_DATE = "last_played_date";
    private static final String KEY_DAILY_ANSWER = "daily_answer_";

    private final SharedPreferences prefs;
    private final String userId;

    // NEW: Runtime cloud sync memory anchor to safely transfer streak across device hardware switching
    private int cloudStreakFallback = 0;

    public GameStateManager(Context context, String userId) {
        this.userId = userId;
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

    // NEW Setter: Invoked by GameActivity when it pulls your history profile from the network
    public void setTemporaryCloudStreak(int streakCount) {
        this.cloudStreakFallback = streakCount;
    }

    public int getStreak() {
        // If our runtime database sync retrieved a valid historical number, use it! Otherwise fallback to local.
        if (cloudStreakFallback > 0) {
            return cloudStreakFallback;
        }
        return prefs.getInt(KEY_STREAK, 0);
    }

    public void updateStreak() {
        String today = getTodayDate();
        String yesterday = getYesterdayDate();
        String lastDate = prefs.getString(KEY_LAST_DATE, "");

        if (getDailyAnswer(today) == 1) {
            return;
        }

        setDailyAnswer(today, 1);

        // This will now dynamically read the downloaded cloud baseline integer!
        int currentStreak = getStreak();

        if (lastDate.isEmpty() && cloudStreakFallback == 0) {
            currentStreak = 1;
        } else if (lastDate.equals(yesterday) || cloudStreakFallback > 0) {
            if (getDailyAnswer(yesterday) == 0 && cloudStreakFallback == 0) {
                currentStreak = 1;
            } else {
                currentStreak++;
            }
        } else if (lastDate.equals(today)) {
            return;
        } else {
            currentStreak = 1;
        }

        // Save locally for offline usage protection, and update runtime tracking references
        this.cloudStreakFallback = currentStreak;
        prefs.edit()
                .putInt(KEY_STREAK, currentStreak)
                .putString(KEY_LAST_DATE, today)
                .apply();
    }

    public void resetStreakIfMissed() {
        String today = getTodayDate();
        String yesterday = getYesterdayDate();
        String lastDate = prefs.getString(KEY_LAST_DATE, "");

        if (lastDate.isEmpty() && cloudStreakFallback == 0) return;

        if (!lastDate.equals(today) && !lastDate.equals(yesterday) && cloudStreakFallback == 0) {
            prefs.edit().putInt(KEY_STREAK, 0).apply();
            this.cloudStreakFallback = 0;
            return;
        }

        if (lastDate.equals(yesterday) && getDailyAnswer(yesterday) == 0 && cloudStreakFallback == 0) {
            prefs.edit().putInt(KEY_STREAK, 0).apply();
            this.cloudStreakFallback = 0;
        }
    }

    public void setDailyAnswer(String date, int status) {
        prefs.edit().putInt(KEY_DAILY_ANSWER + date, status).apply();
    }

    public int getDailyAnswer(String date) {
        return prefs.getInt(KEY_DAILY_ANSWER + date, 0);
    }
}