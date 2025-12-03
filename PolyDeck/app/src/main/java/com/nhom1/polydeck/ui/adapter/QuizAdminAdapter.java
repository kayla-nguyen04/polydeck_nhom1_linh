package com.nhom1.polydeck.ui.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.BaiQuiz;
import com.nhom1.polydeck.data.model.BoTu;
import com.nhom1.polydeck.ui.activity.CreateQuizActivity;
import com.nhom1.polydeck.ui.activity.ViewQuizActivity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuizAdminAdapter extends RecyclerView.Adapter<QuizAdminAdapter.QuizAdminViewHolder> {

    private List<BaiQuiz> quizList;
    private Map<String, BoTu> deckMap; // To show deck names
    private Context context;
    private APIService apiService;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private OnQuizDeletedListener onQuizDeletedListener; // Callback for when quiz is deleted

    public interface OnQuizDeletedListener {
        void onQuizDeleted();
    }

    public QuizAdminAdapter(Context context, List<BaiQuiz> quizList, Map<String, BoTu> deckMap) {
        this.context = context;
        this.quizList = quizList;
        this.deckMap = deckMap;
        this.apiService = RetrofitClient.getApiService();
    }
    
    public void setOnQuizDeletedListener(OnQuizDeletedListener listener) {
        this.onQuizDeletedListener = listener;
    }

    @NonNull
    @Override
    public QuizAdminViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quiz_admin, parent, false);
        return new QuizAdminViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuizAdminViewHolder holder, int position) {
        BaiQuiz quiz = quizList.get(position);
        if (quiz == null) return;

        BoTu deck = deckMap.get(quiz.getMaChuDe());
        String deckName = (deck != null) ? deck.getTenChuDe() : "Unknown Deck";
        
        // Format: "Quiz: Tên bộ từ"
        holder.tvQuizDeckName.setText("Quiz: " + deckName);

        // Format: "15 câu • Thuộc Bộ: CNTT • 01/10/2024"
        String info = String.format(Locale.getDefault(), "%d câu • Thuộc Bộ: %s • %s",
                quiz.getQuestions() != null ? quiz.getQuestions().size() : 0,
                deckName,
                quiz.getCreatedAt() != null ? sdf.format(quiz.getCreatedAt()) : "N/A");
        holder.tvQuizInfo.setText(info);

        // View button - Navigate to ViewQuizActivity to see questions
        holder.btnViewQuiz.setOnClickListener(v -> {
            Intent intent = new Intent(context, ViewQuizActivity.class);
            intent.putExtra("QUIZ_ID", quiz.getId());
            context.startActivity(intent);
        });

        // Edit button - Navigate to CreateQuizActivity with quiz data
        holder.btnEditQuiz.setOnClickListener(v -> {
            Intent intent = new Intent(context, CreateQuizActivity.class);
            intent.putExtra("QUIZ_ID", quiz.getId());
            intent.putExtra("DECK_ID", quiz.getMaChuDe());
            context.startActivity(intent);
        });

        // Delete button - Show confirmation dialog
        holder.btnDeleteQuiz.setOnClickListener(v -> {
            showDeleteConfirmationDialog(quiz, position);
        });
    }

    private void showDeleteConfirmationDialog(BaiQuiz quiz, int position) {
        BoTu deck = deckMap.get(quiz.getMaChuDe());
        String deckName = (deck != null) ? deck.getTenChuDe() : "Unknown Deck";
        
        new AlertDialog.Builder(context)
                .setTitle("Xóa Quiz")
                .setMessage("Bạn có chắc chắn muốn xóa quiz của bộ từ \"" + deckName + "\"?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    deleteQuiz(quiz.getId(), position);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteQuiz(String quizId, int position) {
        if (quizId == null || quizId.isEmpty()) {
            Toast.makeText(context, "ID quiz không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.deleteQuiz(quizId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    quizList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, quizList.size());
                    Toast.makeText(context, "Đã xóa quiz thành công", Toast.LENGTH_SHORT).show();
                    
                    // Notify activity to refresh data (update stats and fullQuizList)
                    if (onQuizDeletedListener != null) {
                        onQuizDeletedListener.onQuizDeleted();
                    }
                } else {
                    Toast.makeText(context, "Không thể xóa quiz", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(context, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return quizList != null ? quizList.size() : 0;
    }

    public void updateData(List<BaiQuiz> newQuizList, Map<String, BoTu> newDeckMap) {
        if (this.quizList == null) {
            this.quizList = newQuizList;
        } else {
            this.quizList.clear();
            this.quizList.addAll(newQuizList);
        }
        if (this.deckMap == null) {
            this.deckMap = newDeckMap;
        } else {
            this.deckMap.clear();
            this.deckMap.putAll(newDeckMap);
        }
        notifyDataSetChanged();
    }

    static class QuizAdminViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuizDeckName, tvQuizInfo;
        TextView btnViewQuiz, btnEditQuiz, btnDeleteQuiz;

        public QuizAdminViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuizDeckName = itemView.findViewById(R.id.tvQuizDeckName);
            tvQuizInfo = itemView.findViewById(R.id.tvQuizInfo);
            btnViewQuiz = itemView.findViewById(R.id.btnViewQuiz);
            btnEditQuiz = itemView.findViewById(R.id.btnEditQuiz);
            btnDeleteQuiz = itemView.findViewById(R.id.btnDeleteQuiz);
        }
    }
}
