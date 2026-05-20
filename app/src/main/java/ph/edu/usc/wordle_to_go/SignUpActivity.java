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
    EditText edtUsername, edtEmail, edtPassword;
    Button btnSignup;

    private FirebaseAuth mAuth;
    private final String DB_URL = "https://wordletogo-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();

        edtUsername = findViewById(R.id.edtUsername);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnSignup = findViewById(R.id.btnSignup);
        txtGoLogin = findViewById(R.id.txtGoLogin);

        txtGoLogin.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
        });


        btnSignup.setOnClickListener(v -> {
            registerUser();
        });
    }

    private void registerUser() {
        String username = edtUsername.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToDatabase(user.getUid(), username, email, password);
                        }
                    } else {
                        Toast.makeText(SignUpActivity.this, "Sign Up Failed: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToDatabase(String uid, String username, String email, String password) {
        FirebaseDatabase database = FirebaseDatabase.getInstance(DB_URL);
        DatabaseReference usersRef = database.getReference("Users");

        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("username", username);
        userMap.put("email", email);
        userMap.put("password", password);

        usersRef.child(uid).setValue(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SignUpActivity.this, "Account Saved!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SignUpActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}