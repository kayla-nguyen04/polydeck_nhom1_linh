package com.nhom1.polydeck.data.model;

import com.google.gson.annotations.SerializedName;

public class QuizResult {
    @SerializedName("scorePercent") public int scorePercent;
    @SerializedName("correct") public int correct;
    @SerializedName("total") public int total;
}



