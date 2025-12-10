package com.nhom1.polydeck.data.model;

import com.google.gson.annotations.SerializedName;

public class DeckProgress {

    @SerializedName("totalWords")
    private int totalWords;

    @SerializedName("learnedWords")
    private int learnedWords;

    public int getTotalWords() {
        return totalWords;
    }

    public void setTotalWords(int totalWords) {
        this.totalWords = totalWords;
    }

    public int getLearnedWords() {
        return learnedWords;
    }

    public void setLearnedWords(int learnedWords) {
        this.learnedWords = learnedWords;
    }
}





























