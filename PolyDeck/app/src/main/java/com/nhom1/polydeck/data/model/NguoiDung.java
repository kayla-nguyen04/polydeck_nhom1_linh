package com.nhom1.polydeck.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class NguoiDung {

    @SerializedName("_id")
    private String id;


    @SerializedName("ho_ten")
    private String hoTen;

    @SerializedName("email")
    private String email;

    @SerializedName("link_anh_dai_dien")
    private String linkAnhDaiDien;

    @SerializedName("cap_do")
    private int capDo;

    @SerializedName("diem_tich_luy")
    private int diemTichLuy;

    @SerializedName("vai_tro")
    private String vaiTro;

    @SerializedName("trang_thai")
    private String trangThai;

    @SerializedName("ngay_tao")
    private Date ngayTao;

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLinkAnhDaiDien() {
        return linkAnhDaiDien;
    }

    public void setLinkAnhDaiDien(String linkAnhDaiDien) {
        this.linkAnhDaiDien = linkAnhDaiDien;
    }

    public int getCapDo() {
        return capDo;
    }

    public void setCapDo(int capDo) {
        this.capDo = capDo;
    }

    public int getDiemTichLuy() {
        return diemTichLuy;
    }

    public void setDiemTichLuy(int diemTichLuy) {
        this.diemTichLuy = diemTichLuy;
    }

    public String getVaiTro() {
        return vaiTro;
    }

    public void setVaiTro(String vaiTro) {
        this.vaiTro = vaiTro;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public Date getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(Date ngayTao) {
        this.ngayTao = ngayTao;
    }
}
