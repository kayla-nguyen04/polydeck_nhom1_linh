package com.nhom1.polydeck.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.model.Answer;
import com.nhom1.polydeck.data.model.Question;

import java.util.List;

public class ViewQuestionAdapter extends RecyclerView.Adapter<ViewQuestionAdapter.ViewQuestionViewHolder> {

    private List<Question> questionList;

    public ViewQuestionAdapter(List<Question> questionList) {
        this.questionList = questionList;
    }

    @NonNull
    @Override
    public ViewQuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view_question, parent, false);
        return new ViewQuestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewQuestionViewHolder holder, int position) {
        Question question = questionList.get(position);
        if (question == null) return;

        // Set question number and content
        holder.tvQuestionNumber.setText("Câu " + (position + 1));
        holder.tvQuestionContent.setText(question.getQuestionText());

        // Set answers
        List<Answer> answers = question.getAnswers();
        if (answers != null && answers.size() >= 4) {
            holder.tvAnswer1.setText("A. " + answers.get(0).getAnswerText());
            holder.tvAnswer2.setText("B. " + answers.get(1).getAnswerText());
            holder.tvAnswer3.setText("C. " + answers.get(2).getAnswerText());
            holder.tvAnswer4.setText("D. " + answers.get(3).getAnswerText());

            // Find correct answer
            String correctAnswerLabel = "";
            for (int i = 0; i < answers.size(); i++) {
                if (answers.get(i).isCorrect()) {
                    char label = (char) ('A' + i);
                    correctAnswerLabel = String.valueOf(label);
                    break;
                }
            }

            if (!correctAnswerLabel.isEmpty()) {
                holder.tvCorrectAnswer.setText("Đáp án đúng: " + correctAnswerLabel);
                holder.tvCorrectAnswer.setVisibility(View.VISIBLE);
            } else {
                holder.tvCorrectAnswer.setVisibility(View.GONE);
            }
        } else {
            holder.tvAnswer1.setText("A. -");
            holder.tvAnswer2.setText("B. -");
            holder.tvAnswer3.setText("C. -");
            holder.tvAnswer4.setText("D. -");
            holder.tvCorrectAnswer.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return questionList != null ? questionList.size() : 0;
    }

    public void updateData(List<Question> newQuestionList) {
        if (this.questionList == null) {
            this.questionList = newQuestionList;
        } else {
            this.questionList.clear();
            this.questionList.addAll(newQuestionList);
        }
        notifyDataSetChanged();
    }

    static class ViewQuestionViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestionNumber, tvQuestionContent;
        TextView tvAnswer1, tvAnswer2, tvAnswer3, tvAnswer4;
        TextView tvCorrectAnswer;

        public ViewQuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuestionNumber = itemView.findViewById(R.id.tvQuestionNumber);
            tvQuestionContent = itemView.findViewById(R.id.tvQuestionContent);
            tvAnswer1 = itemView.findViewById(R.id.tvAnswer1);
            tvAnswer2 = itemView.findViewById(R.id.tvAnswer2);
            tvAnswer3 = itemView.findViewById(R.id.tvAnswer3);
            tvAnswer4 = itemView.findViewById(R.id.tvAnswer4);
            tvCorrectAnswer = itemView.findViewById(R.id.tvCorrectAnswer);
        }
    }
}






















