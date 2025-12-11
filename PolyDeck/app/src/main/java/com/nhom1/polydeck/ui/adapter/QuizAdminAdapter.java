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
import com.nhom1.polydeck.utils.HiddenQuizManager;

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
    private OnQuizHiddenListener onQuizHiddenListener; // Callback for when quiz is hidden/unhidden
    private HiddenQuizManager hiddenQuizManager;
    private boolean isUnhideMode; // Mode to show hidden quizzes

    public interface OnQuizHiddenListener {
        void onQuizHidden();
    }

    public QuizAdminAdapter(Context context, List<BaiQuiz> quizList, Map<String, BoTu> deckMap) {
        this.context = context;
        this.quizList = quizList;
        this.deckMap = deckMap;
        this.apiService = RetrofitClient.getApiService();
        this.hiddenQuizManager = new HiddenQuizManager(context);
        this.isUnhideMode = false;
    }
    
    public void setOnQuizHiddenListener(OnQuizHiddenListener listener) {
        this.onQuizHiddenListener = listener;
    }
    
    public void setUnhideMode(boolean isUnhideMode) {
        this.isUnhideMode = isUnhideMode;
        notifyDataSetChanged();
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

        // Hide/Unhide button - Show confirmation dialog
        if (isUnhideMode) {
            holder.btnDeleteQuiz.setText("👁 Hiển thị lại");
            holder.btnDeleteQuiz.setBackground(context.getDrawable(R.drawable.bg_button_unhide));
            holder.btnDeleteQuiz.setTextColor(0xFFF97316); // orange_500
            holder.btnDeleteQuiz.setOnClickListener(v -> {
                showUnhideConfirmationDialog(quiz, position);
            });
        } else {
            holder.btnDeleteQuiz.setText("👁️‍🗨️ Ẩn");
            holder.btnDeleteQuiz.setBackground(context.getDrawable(R.drawable.bg_button_hide));
            holder.btnDeleteQuiz.setTextColor(0xFFFACC15); // yellow_bar
            holder.btnDeleteQuiz.setOnClickListener(v -> {
                showHideConfirmationDialog(quiz, position);
            });
        }
    }

    private void showHideConfirmationDialog(BaiQuiz quiz, int position) {
        BoTu deck = deckMap.get(quiz.getMaChuDe());
        String deckName = (deck != null) ? deck.getTenChuDe() : "Unknown Deck";
        
        new AlertDialog.Builder(context)
                .setTitle("Ẩn Quiz")
                .setMessage("Bạn có chắc chắn muốn ẩn quiz của bộ từ \"" + deckName + "\"?")
                .setPositiveButton("Ẩn", (dialog, which) -> {
                    hideQuiz(quiz.getId(), position);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showUnhideConfirmationDialog(BaiQuiz quiz, int position) {
        BoTu deck = deckMap.get(quiz.getMaChuDe());
        String deckName = (deck != null) ? deck.getTenChuDe() : "Unknown Deck";
        
        new AlertDialog.Builder(context)
                .setTitle("Hiển thị lại Quiz")
                .setMessage("Bạn có chắc chắn muốn hiển thị lại quiz của bộ từ \"" + deckName + "\"?")
                .setPositiveButton("Hiển thị lại", (dialog, which) -> {
                    unhideQuiz(quiz.getId(), position);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void hideQuiz(String quizId, int position) {
        if (quizId == null || quizId.isEmpty()) {
            Toast.makeText(context, "ID quiz không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        hiddenQuizManager.hideQuiz(quizId);
        quizList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, quizList.size());
        Toast.makeText(context, "Đã ẩn quiz thành công", Toast.LENGTH_SHORT).show();
        
        // Notify activity to refresh data
        if (onQuizHiddenListener != null) {
            onQuizHiddenListener.onQuizHidden();
        }
    }

    private void unhideQuiz(String quizId, int position) {
        if (quizId == null || quizId.isEmpty()) {
            Toast.makeText(context, "ID quiz không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        hiddenQuizManager.unhideQuiz(quizId);
        quizList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, quizList.size());
        Toast.makeText(context, "Đã hiển thị lại quiz thành công", Toast.LENGTH_SHORT).show();
        
        // Notify activity to refresh data
        if (onQuizHiddenListener != null) {
            onQuizHiddenListener.onQuizHidden();
        }
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
