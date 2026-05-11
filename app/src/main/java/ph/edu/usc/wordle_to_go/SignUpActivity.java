package ph.edu.usc.wordle_to_go;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity {

    TextView txtGoLogin;
    // Declare the views from your XML
    EditText edtUsername, edtEmail, edtPassword;
    Button btnSignup;

    // Firebase variables
    private FirebaseAuth mAuth;
    private final String DB_URL = "https://wordletogo-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // 1. Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 2. Bind the UI components
        edtUsername = findViewById(R.id.edtUsername);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnSignup = findViewById(R.id.btnSignup);
        txtGoLogin = findViewById(R.id.txtGoLogin);

        txtGoLogin.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
        });


        // 3. Set the Click Listener
        btnSignup.setOnClickListener(v -> {
            registerUser();
        });
    }

    private void registerUser() {
        String username = edtUsername.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        // Basic Validation: Ensure fields are not empty
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create the user in Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Account created in Auth, now save details to Database
                            saveUserToDatabase(user.getUid(), username, email, password);
                        }
                    } else {
                        // Show error if Auth fails (e.g., email already exists)
                        Toast.makeText(SignUpActivity.this, "Sign Up Failed: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToDatabase(String uid, String username, String email, String password) {
        FirebaseDatabase database = FirebaseDatabase.getInstance(DB_URL);
        DatabaseReference usersRef = database.getReference("Users");

        // Prepare the data map
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("username", username);
        userMap.put("email", email);
        userMap.put("password", password);

        // Save to Users -> [Unique ID]
        usersRef.child(uid).setValue(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SignUpActivity.this, "Account Saved!", Toast.LENGTH_SHORT).show();

                    // 4. Finally, move to HomeActivity
                    Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish(); // Close SignUpActivity so user can't "back" into it
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SignUpActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}