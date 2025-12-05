package com.nhom1.polydeck.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;
import java.util.Map;

public class LichSuLamBai {

    @SerializedName("_id")
    private String id;

    @SerializedName("ma_nguoi_dung")
    private String maNguoiDung;

    // Server may return either a String id or an embedded object { _id, ten_chu_de, link_anh_icon }
    @SerializedName("ma_chu_de")
    private Object maChuDe;

    // FIX: Changed field name and added getter/setter to match server response and fragment usage
    @SerializedName("diem_so")
    private int diemSo;

    @SerializedName("so_cau_dung")
    private int soCauDung;

    @SerializedName("tong_so_cau")
    private int tongSoCau;

    @SerializedName("ngay_lam_bai")
    private Date ngayLamBai;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMaNguoiDung() {
        return maNguoiDung;
    }

    public void setMaNguoiDung(String maNguoiDung) {
        this.maNguoiDung = maNguoiDung;
    }

    public String getMaChuDe() {
        if (maChuDe == null) return null;
        if (maChuDe instanceof String) return (String) maChuDe;
        if (maChuDe instanceof Map) {
            Object id = ((Map<?, ?>) maChuDe).get("_id");
            return id != null ? String.valueOf(id) : null;
        }
        return null;
    }

    public String getTenChuDe() {
        if (maChuDe instanceof Map) {
            Object ten = ((Map<?, ?>) maChuDe).get("ten_chu_de");
            return ten != null ? String.valueOf(ten) : null;
        }
        return null;
    }

    public int getDiemSo() {
        return diemSo;
    }

    public void setDiemSo(int diemSo) {
        this.diemSo = diemSo;
    }

    public int getSoCauDung() {
        return soCauDung;
    }

    public void setSoCauDung(int soCauDung) {
        this.soCauDung = soCauDung;
    }

    public int getTongSoCau() {
        return tongSoCau;
    }

    public void setTongSoCau(int tongSoCau) {
        this.tongSoCau = tongSoCau;
    }

    public Date getNgayLamBai() {
        return ngayLamBai;
    }

    public void setNgayLamBai(Date ngayLamBai) {
        this.ngayLamBai = ngayLamBai;
    }
}

