# PolyDeck Server

Backend server cho ứng dụng PolyDeck - Học từ vựng và làm bài quiz.

## Cấu trúc Database (MongoDB)

Dự án sử dụng MongoDB với các collections sau:

### 1. ChuDe (Chủ đề)
- `ma_chu_de`: ID chủ đề (unique)
- `ten_chu_de`: Tên chủ đề
- `anh_bia`: Đường dẫn ảnh bìa

### 2. TuVung (Từ vựng)
- `ma_tu_vung`: ID từ vựng (unique)
- `ma_chu_de`: ID chủ đề (reference)
- `tu_tieng_anh`: Từ tiếng Anh
- `nghia_tieng_viet`: Nghĩa tiếng Việt
- `phien_am`: Phiên âm
- `am_thanh`: Đường dẫn file âm thanh
- `cau_vi_du`: Câu ví dụ
- `nghia_cau_vi_du`: Nghĩa câu ví dụ

### 3. NguoiDung (Người dùng)
- `ma_nguoi_dung`: ID người dùng (unique)
- `email`: Email (unique)
- `ho_ten`: Họ tên
- `anh_dai_dien`: Đường dẫn ảnh đại diện
- `vai_tro`: Vai trò (student/admin)
- `diem_tich_luy`: Điểm tích lũy
- `ngay_tham_gia`: Ngày tham gia

### 4. CauHoi (Câu hỏi)
- `ma_cau_hoi`: ID câu hỏi (unique)
- `ma_chu_de`: ID chủ đề (reference)
- `noi_dung_cau_hoi`: Nội dung câu hỏi
- `dap_an_a`, `dap_an_b`, `dap_an_c`, `dap_an_d`: Các đáp án
- `dap_an_dung`: Đáp án đúng (A/B/C/D)
- `loai_cau_hoi`: Loại câu hỏi
- `am_thanh`: Đường dẫn file âm thanh

### 5. LichSuLamBai (Lịch sử làm bài)
- `ma_lich_su`: ID lịch sử (unique)
- `ma_nguoi_dung`: ID người dùng (reference)
- `ma_chu_de`: ID chủ đề (reference)
- `diem_so`: Điểm số
- `ngay_hoan_thanh`: Ngày hoàn thành

### 6. ChiTietLamBai (Chi tiết làm bài)
- `ma_chi_tiet`: ID chi tiết (unique)
- `ma_lich_su`: ID lịch sử (reference)
- `ma_cau_hoi`: ID câu hỏi (reference)
- `dap_an_chon`: Đáp án đã chọn
- `ket_qua`: Kết quả (correct/incorrect)

### 7. ThongBao (Thông báo)
- `ma_thong_bao`: ID thông báo (unique)
- `tieu_de`: Tiêu đề
- `noi_dung`: Nội dung
- `ngay_gui`: Ngày gửi

### 8. ThongBaoDaDoc (Thông báo đã đọc)
- `ma_thong_bao_da_doc`: ID (unique)
- `ma_nguoi_dung`: ID người dùng (reference)
- `ma_thong_bao`: ID thông báo (reference)

## Cài đặt

1. Cài đặt dependencies:
```bash
npm install
```

2. Tạo file `.env`:
```
MONGODB_URI=mongodb://localhost:27017/polydeck
PORT=3000
```

3. Chạy server:
```bash
npm start
```

Hoặc chạy với nodemon (development):
```bash
npm run dev
```

## Yêu cầu

- Node.js (v14 trở lên)
- MongoDB (local hoặc MongoDB Atlas)

