package com.nhom1.polydeck.data.model;

import com.google.gson.annotations.SerializedName;

public class UpdateProgressRequest {
    @SerializedName("userId")
    private String userId;

    @SerializedName("tuVungId")
    private String tuVungId;

    @SerializedName("trangThaiHoc")
    private String trangThaiHoc; // "chua_hoc", "dang_hoc", "da_nho"

    public UpdateProgressRequest(String userId, String tuVungId, String trangThaiHoc) {
        this.userId = userId;
        this.tuVungId = tuVungId;
        this.trangThaiHoc = trangThaiHoc;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTuVungId() {
        return tuVungId;
    }

    public void setTuVungId(String tuVungId) {
        this.tuVungId = tuVungId;
    }

    public String getTrangThaiHoc() {
        return trangThaiHoc;
    }

    public void setTrangThaiHoc(String trangThaiHoc) {
        this.trangThaiHoc = trangThaiHoc;
    }
}





