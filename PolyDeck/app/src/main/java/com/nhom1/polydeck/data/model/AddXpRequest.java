package com.nhom1.polydeck.data.model;

import com.google.gson.annotations.SerializedName;

public class AddXpRequest {
    @SerializedName("xpAmount")
    private int xpAmount;

    public AddXpRequest(int xpAmount) {
        this.xpAmount = xpAmount;
    }

    public int getXpAmount() {
        return xpAmount;
    }

    public void setXpAmount(int xpAmount) {
        this.xpAmount = xpAmount;
    }
}





