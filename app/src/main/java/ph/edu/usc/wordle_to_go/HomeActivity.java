package ph.edu.usc.wordle_to_go;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {

    LinearLayout cardDaily, cardLeaderboard;
    Button btn6, btn7, btn8, btn9, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_home);

        cardDaily = findViewById(R.id.cardDaily);
        cardLeaderboard = findViewById(R.id.cardLeaderboard);
        btn6 = findViewById(R.id.btn6Letters);
        btn7 = findViewById(R.id.btn7Letters);
        btn8 = findViewById(R.id.btn8Letters);
        btn9 = findViewById(R.id.btn9Letters);
        btnLogout = findViewById(R.id.btnLogout);

        cardDaily.setOnClickListener(v -> openGame(5));
        cardLeaderboard.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, LeaderboardActivity.class));
        });

        btn6.setOnClickListener(v -> openGame(6));
        btn7.setOnClickListener(v -> openGame(7));
        btn8.setOnClickListener(v -> openGame(8));
        btn9.setOnClickListener(v -> openGame(9));

        // FIX 3: Logout logic
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void openGame(int length) {
        Intent intent = new Intent(HomeActivity.this, GameActivity.class);
        intent.putExtra("WORD_LENGTH", length);
        startActivity(intent);
    }
}