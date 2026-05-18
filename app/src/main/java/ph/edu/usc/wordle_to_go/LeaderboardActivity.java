package ph.edu.usc.wordle_to_go;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LeaderboardActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LeaderboardAdapter adapter;
    private List<LeaderboardEntry> leaderboardList = new ArrayList<>();
    private FirebaseFirestore db;
    private TextView txtNoData; // UI feedback helper

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        findViewById(R.id.btnBackLeaderboard).setOnClickListener(v -> finish());

        // Optional: Add a placeholder TextView to your layout XML if there are no entries
        txtNoData = findViewById(R.id.txtNoData);

        recyclerView = findViewById(R.id.recyclerLeaderboard);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LeaderboardAdapter(leaderboardList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        fetchLeaderboard();
    }

    private void fetchLeaderboard() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Multi-level query aggregation pattern
        db.collection("leaderboard")
                .whereEqualTo("date", today)
                .orderBy("attempts", Query.Direction.ASCENDING)   // Fewest attempts = Winner (#1 rank)
                .orderBy("timestamp", Query.Direction.ASCENDING)  // Tiebreaker: First to solve wins the higher rank
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    leaderboardList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        LeaderboardEntry entry = doc.toObject(LeaderboardEntry.class);
                        leaderboardList.add(entry);
                    }

                    // UI State Toggling
                    if (leaderboardList.isEmpty()) {
                        if (txtNoData != null) txtNoData.setVisibility(View.VISIBLE);
                    } else {
                        if (txtNoData != null) txtNoData.setVisibility(View.GONE);
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE_LEADERBOARD", "Error loading leaderboard", e);

                    // Critical IT Pro-Tip handler logic check for composite index crashes
                    if (e instanceof FirebaseFirestoreException &&
                            ((FirebaseFirestoreException) e).getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                        Toast.makeText(this, "Composite Index required! Check Logcat for the setup link.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Failed to load leaderboard scores.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public static class LeaderboardEntry {
        public String username;
        public int attempts;
        public String date;
        public long timestamp;

        public LeaderboardEntry() {} // Required explicitly for Firestore parsing loops

        public LeaderboardEntry(String username, int attempts, String date, long timestamp) {
            this.username = username;
            this.attempts = attempts;
            this.date = date;
            this.timestamp = timestamp;
        }
    }

    private static class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {
        private final List<LeaderboardEntry> list;

        public LeaderboardAdapter(List<LeaderboardEntry> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.leaderboard_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LeaderboardEntry entry = list.get(position);

            // Format styling variables
            int rank = position + 1;
            holder.txtRank.setText("#" + rank);
            holder.txtUsername.setText(entry.username);
            holder.txtAttempts.setText(entry.attempts + " " + (entry.attempts == 1 ? "guess" : "guesses"));

            // Visual Polish: Give top 3 places custom styling signatures
            if (rank == 1) {
                holder.txtRank.setTextColor(Color.parseColor("#FFD700")); // Gold color highlight
                holder.txtRank.setTypeface(null, Typeface.BOLD);
            } else if (rank == 2) {
                holder.txtRank.setTextColor(Color.parseColor("#C0C0C0")); // Silver color highlight
                holder.txtRank.setTypeface(null, Typeface.BOLD);
            } else if (rank == 3) {
                holder.txtRank.setTextColor(Color.parseColor("#CD7F32")); // Bronze color highlight
                holder.txtRank.setTypeface(null, Typeface.BOLD);
            } else {
                holder.txtRank.setTextColor(Color.parseColor("#FFFFFF")); // Default text wrap color
                holder.txtRank.setTypeface(null, Typeface.NORMAL);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtRank, txtUsername, txtAttempts;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                txtRank = itemView.findViewById(R.id.txtRank);
                txtUsername = itemView.findViewById(R.id.txtUsername);
                txtAttempts = itemView.findViewById(R.id.txtAttempts);
            }
        }
    }
}