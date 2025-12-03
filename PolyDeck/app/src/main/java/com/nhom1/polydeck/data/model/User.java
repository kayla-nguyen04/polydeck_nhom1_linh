package com.nhom1.polydeck.data.model;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("_id")
    private String id;

    @SerializedName("ho_ten")
    private String hoTen;

    @SerializedName("email")
    private String email;

    @SerializedName("link_anh_dai_dien")
    private String linkAnhDaiDien;

    @SerializedName("cap_do")
    private int level;

    @SerializedName("diem_tich_luy")
    private int xp;

    @SerializedName("chuoi_ngay_hoc")
    private int chuoiNgayHoc;

    @SerializedName("ngay_tao")
    private String ngayThamGia;

    @SerializedName("trang_thai")
    private String trangThai;

    public User() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLinkAnhDaiDien() { return linkAnhDaiDien; }
    public void setLinkAnhDaiDien(String linkAnhDaiDien) { this.linkAnhDaiDien = linkAnhDaiDien; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }

    public int getChuoiNgayHoc() { return chuoiNgayHoc; }
    public void setChuoiNgayHoc(int chuoiNgayHoc) { this.chuoiNgayHoc = chuoiNgayHoc; }

    public String getNgayThamGia() { return ngayThamGia; }
    public void setNgayThamGia(String ngayThamGia) { this.ngayThamGia = ngayThamGia; }

    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }

    public String getInitials() {
        if (hoTen == null || hoTen.isEmpty()) return "?";
        String[] words = hoTen.trim().split("\\s+");
        if (words.length >= 2) {
            return (words[0].charAt(0) + "" + words[words.length - 1].charAt(0)).toUpperCase();
        }
        if(hoTen.length() > 0) return hoTen.substring(0, 1).toUpperCase();
        return "?";
    }
}