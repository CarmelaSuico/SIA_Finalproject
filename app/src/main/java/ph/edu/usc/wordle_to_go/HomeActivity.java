package ph.edu.usc.wordle_to_go;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    LinearLayout cardDaily;
    Button btn6, btn7, btn8, btn9;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        cardDaily = findViewById(R.id.cardDaily);
        btn6 = findViewById(R.id.btn6Letters);
        btn7 = findViewById(R.id.btn7Letters);
        btn8 = findViewById(R.id.btn8Letters);
        btn9 = findViewById(R.id.btn9Letters);

        cardDaily.setOnClickListener(v -> openGame(5));

        btn6.setOnClickListener(v -> openGame(6));
        btn7.setOnClickListener(v -> openGame(7));
        btn8.setOnClickListener(v -> openGame(8));
        btn9.setOnClickListener(v -> openGame(9));
    }

    private void openGame(int length) {
        Intent intent = new Intent(HomeActivity.this, GameActivity.class);
        intent.putExtra("WORD_LENGTH", length);
        startActivity(intent);
    }
}