package com.nhom1.polydeck.data.model;

import com.google.gson.annotations.SerializedName;

public class AddXpResponse {
    @SerializedName("diem_tich_luy")
    private int diemTichLuy;

    public int getDiemTichLuy() {
        return diemTichLuy;
    }

    public void setDiemTichLuy(int diemTichLuy) {
        this.diemTichLuy = diemTichLuy;
    }
}





