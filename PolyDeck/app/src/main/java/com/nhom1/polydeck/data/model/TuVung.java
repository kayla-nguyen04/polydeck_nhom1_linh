package com.nhom1.polydeck.data.model;

import com.google.gson.annotations.SerializedName;

public class TuVung {
    @SerializedName("_id")
    private String id;

    @SerializedName("tu_tieng_anh")
    private String tuTiengAnh;

    @SerializedName("phien_am")
    private String phienAm;

    @SerializedName("nghia_tieng_viet")
    private String nghiaTiengViet;

    @SerializedName("cau_vi_du")
    private String cauViDu;

    @SerializedName("am_thanh")
    private String amThanh;

    @SerializedName("ma_chu_de")
    private String maChuDe;

    public TuVung() {}

    // Getters & Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTuTiengAnh() {
        return tuTiengAnh;
    }

    public void setTuTiengAnh(String tuTiengAnh) {
        this.tuTiengAnh = tuTiengAnh;
    }

    public String getPhienAm() {
        return phienAm;
    }

    public void setPhienAm(String phienAm) {
        this.phienAm = phienAm;
    }

    public String getNghiaTiengViet() {
        return nghiaTiengViet;
    }

    public void setNghiaTiengViet(String nghiaTiengViet) {
        this.nghiaTiengViet = nghiaTiengViet;
    }

    public String getCauViDu() {
        return cauViDu;
    }

    public void setCauViDu(String cauViDu) {
        this.cauViDu = cauViDu;
    }

    public String getAmThanh() {
        return amThanh;
    }

    public void setAmThanh(String amThanh) {
        this.amThanh = amThanh;
    }

    public String getMaChuDe() {
        return maChuDe;
    }

    public void setMaChuDe(String maChuDe) {
        this.maChuDe = maChuDe;
    }
}
