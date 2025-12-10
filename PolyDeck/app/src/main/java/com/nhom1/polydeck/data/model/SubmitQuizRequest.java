package com.nhom1.polydeck.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SubmitQuizRequest {
    public static class Answer {
        @SerializedName("ma_cau_hoi")
        public String ma_cau_hoi;
        
        @SerializedName("ma_lua_chon")
        public String ma_lua_chon;

        public Answer(String q, String a) {
            this.ma_cau_hoi = q;
            this.ma_lua_chon = a;
        }
    }

    @SerializedName("ma_nguoi_dung")
    public String ma_nguoi_dung; // _id của user
    
    @SerializedName("ma_quiz")
    public String ma_quiz; // _id của quiz
    
    @SerializedName("ma_chu_de")
    public String ma_chu_de; // _id của chủ đề (ObjectId ref)
    
    @SerializedName("thoi_gian_lam_bai")
    public int thoi_gian_lam_bai;
    
    @SerializedName("answers")
    public List<Answer> answers;
    
    // Thêm các field để backend lưu trực tiếp (tránh tính sai)
    @SerializedName("so_cau_dung")
    public int so_cau_dung; // Số câu đúng
    
    @SerializedName("tong_so_cau")
    public int tong_so_cau; // Tổng số câu
    
    @SerializedName("diem_so")
    public int diem_so; // Điểm số (phần trăm)
}




