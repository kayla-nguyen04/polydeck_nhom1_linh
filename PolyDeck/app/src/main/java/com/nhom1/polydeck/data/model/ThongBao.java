package com.nhom1.polydeck.data.model;

import com.google.gson.annotations.SerializedName;

public class ThongBao {

    @SerializedName("_id")
    private String id;

    @SerializedName("ma_thong_bao")
    private String maThongBao;

    @SerializedName("ma_nguoi_dung")
    private String maNguoiDung;

    @SerializedName("tieu_de")
    private String tieuDe;

    @SerializedName("noi_dung")
    private String noiDung;

    @SerializedName("ngay_gui")
    private String ngayGui;

    public ThongBao(String tieuDe, String noiDung) {
        this.tieuDe = tieuDe;
        this.noiDung = noiDung;
    }

    public String getId() { return id; }
    public String getMaThongBao() { return maThongBao; }
    public String getMaNguoiDung() { return maNguoiDung; }
    public String getTieuDe() { return tieuDe; }
    public String getNoiDung() { return noiDung; }
    public String getNgayGui() { return ngayGui; }

    public void setTieuDe(String tieuDe) { this.tieuDe = tieuDe; }
    public void setNoiDung(String noiDung) { this.noiDung = noiDung; }
}
