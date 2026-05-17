package ph.edu.usc.wordle_to_go;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        findViewById(R.id.btnBackLeaderboard).setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerLeaderboard);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LeaderboardAdapter(leaderboardList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        fetchLeaderboard();
    }

    private void fetchLeaderboard() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        db.collection("leaderboard")
                .whereEqualTo("date", today)
                .orderBy("attempts", Query.Direction.ASCENDING)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    leaderboardList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        LeaderboardEntry entry = doc.toObject(LeaderboardEntry.class);
                        leaderboardList.add(entry);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    public static class LeaderboardEntry {
        public String username;
        public int attempts;
        public String date;
        public long timestamp;

        public LeaderboardEntry() {} // Required for Firestore

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
            holder.txtRank.setText("#" + (position + 1));
            holder.txtUsername.setText(entry.username);
            holder.txtAttempts.setText(String.valueOf(entry.attempts));
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
