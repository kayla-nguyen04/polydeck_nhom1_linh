const mongoose = require('mongoose');

const nguoiDungSchema = new mongoose.Schema({
  ma_nguoi_dung: { type: String, required: true, unique: true, trim: true },
  ho_ten: { type: String, required: true, trim: true },
  email: { type: String, required: true, unique: true, lowercase: true, trim: true },
  mat_khau_hash: { type: String, required: false }, // Không bắt buộc vì có thể đăng nhập bằng Google
  link_anh_dai_dien: { type: String, default: null },
  google_id: { type: String, default: null },
  cap_do: { type: Number, default: 1, min: 1 },
  diem_tich_luy: { type: Number, default: 0, min: 0 },
  chuoi_ngay_hoc: { type: Number, default: 0, min: 0 },
  vai_tro: { type: String, enum: ['student', 'admin'], default: 'student' },
  trang_thai: { type: String, enum: ['active', 'inactive', 'banned'], default: 'inactive' },
  email_verified: { type: Boolean, default: false },
  email_verification_token: { type: String, default: null },
  email_verification_expire: { type: Date, default: null },
  tu_vung_yeu_thich: [{ type: String }],
  ngay_tao: { type: Date, default: Date.now }
}, {
  timestamps: true,
  collection: 'nguoi_dung'
});

// `unique: true` already creates indexes for `ma_nguoi_dung` and `email`.
// Keep explicit index only for queries on other fields if needed.

module.exports = mongoose.model('NguoiDung', nguoiDungSchema);

