package com.nhom1.polydeck.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class LichSuLamBai {
    @SerializedName("_id") public String id;
    @SerializedName("ma_lich_su") public String maLichSu;
    @SerializedName("ma_nguoi_dung") public String maNguoiDung;
    @SerializedName("ma_quiz") public String maQuiz;
    @SerializedName("ma_chu_de") public String maChuDe;
    @SerializedName("diem_so") public int diemSo;
    @SerializedName("diem_danh_duoc") public int diemDanhDuoc;
    @SerializedName("thoi_gian_lam_bai") public int thoiGianLamBai;
    @SerializedName("ngay_hoan_thanh") public Date ngayHoanThanh;
}



