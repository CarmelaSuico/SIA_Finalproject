package ph.edu.usc.wordle_to_go;

import android.content.Intent;
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

import com.google.firebase.auth.FirebaseAuth;
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
    private TextView txtNoData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_leaderboard);
        findViewById(R.id.btnBackLeaderboard).setOnClickListener(v -> finish());
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
        String regionalDbUrl = "https://wordletogo-default-rtdb.asia-southeast1.firebasedatabase.app/";

        com.google.firebase.database.FirebaseDatabase.getInstance(regionalDbUrl)
                .getReference("leaderboard").child(today)
                .orderByChild("attempts")
                .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot dataSnapshot) {
                        leaderboardList.clear();

                        for (com.google.firebase.database.DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            LeaderboardEntry entry = snapshot.getValue(LeaderboardEntry.class);
                            if (entry != null) {
                                leaderboardList.add(entry);
                            }
                        }

                        if (leaderboardList.isEmpty()) {
                            if (txtNoData != null) txtNoData.setVisibility(View.VISIBLE);
                        } else {
                            if (txtNoData != null) txtNoData.setVisibility(View.GONE);
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError databaseError) {
                        Log.e("RTDB_LEADERBOARD", "Error reading records snapshot structure", databaseError.toException());
                    }
                });
    }

    public static class LeaderboardEntry {
        public String uid;
        public String username;
        public int attempts;
        public String date;
        public long timestamp;
        public int streak;

        public LeaderboardEntry() {}

        public LeaderboardEntry(String uid, String username, int attempts, String date, long timestamp) {
            this.uid = uid;
            this.username = username;
            this.attempts = attempts;
            this.date = date;
            this.timestamp = timestamp;
            this.streak = 0;
        }

        public void setStreak(int streak) {
            this.streak = streak;
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

            int rank = position + 1;
            holder.txtRank.setText("#" + rank);
            holder.txtUsername.setText(entry.username);

            holder.txtAttempts.setText(entry.attempts + (entry.attempts == 1 ? " guess " : " guesses ") + "🔥" + entry.streak);

            if (rank == 1) {
                holder.txtRank.setTextColor(Color.parseColor("#FFD700"));
                holder.txtRank.setTypeface(null, Typeface.BOLD);
            } else if (rank == 2) {
                holder.txtRank.setTextColor(Color.parseColor("#C0C0C0"));
                holder.txtRank.setTypeface(null, Typeface.BOLD);
            } else if (rank == 3) {
                holder.txtRank.setTextColor(Color.parseColor("#CD7F32"));
                holder.txtRank.setTypeface(null, Typeface.BOLD);
            } else {
                holder.txtRank.setTextColor(Color.parseColor("#FFFFFF"));
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