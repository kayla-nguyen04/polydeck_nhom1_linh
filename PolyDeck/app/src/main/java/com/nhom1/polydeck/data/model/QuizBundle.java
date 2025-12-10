package com.nhom1.polydeck.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class QuizBundle {
    public static class QuizInfo {
        @SerializedName("ma_quiz") public String maQuiz;
        @SerializedName("ma_chu_de") public String maChuDe;
        @SerializedName("tieu_de") public String tieuDe;
    }
    public static class Option {
        @SerializedName("ma_lua_chon") public String maLuaChon;
        @SerializedName("noi_dung") public String noiDung;
    }
    public static class Question {
        @SerializedName("ma_cau_hoi") public String maCauHoi;
        @SerializedName("noi_dung_cau_hoi") public String noiDung;
        @SerializedName("dap_an_lua_chon") public List<Option> options;
        @SerializedName("dap_an_dung") public String dapAnDung; // Đáp án đúng (có thể là text hoặc ma_lua_chon)
    }

    @SerializedName("quiz") public QuizInfo quiz;
    @SerializedName("questions") public List<Question> questions;
}




