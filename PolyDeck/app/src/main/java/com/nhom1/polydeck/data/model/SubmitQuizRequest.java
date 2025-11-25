package com.nhom1.polydeck.data.model;

import java.util.List;

public class SubmitQuizRequest {
    public static class Answer {
        public String ma_cau_hoi;
        public String ma_lua_chon;

        public Answer(String q, String a) {
            this.ma_cau_hoi = q;
            this.ma_lua_chon = a;
        }
    }

    public String ma_nguoi_dung;
    public String ma_quiz;
    public String ma_chu_de;
    public int thoi_gian_lam_bai;
    public List<Answer> answers;
}



