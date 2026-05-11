package ph.edu.usc.wordle_to_go;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    // Your specific Asia-Southeast URL
    private final String DB_URL = "https://wordletogo-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialize Firebase Context
        FirebaseApp.initializeApp(this);

        // 2. Establish connection to your specific regional database
        FirebaseDatabase database = FirebaseDatabase.getInstance(DB_URL);
        DatabaseReference myRef = database.getReference("connection_test");

        // 3. Simple Test: Write a value to Firebase
        myRef.setValue("Wordle To Go is Online!")
                .addOnSuccessListener(aVoid -> {
                    // If this shows up, your connection is 100% working
                    Toast.makeText(MainActivity.this, "Connected to Firebase!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // If this shows up, check your Internet or Firebase Rules
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}