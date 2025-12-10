const mongoose = require('mongoose');

const nguoiDungSchema = new mongoose.Schema({
  ho_ten: { type: String, required: true, trim: true },
  email: { type: String, required: true, unique: true, lowercase: true, trim: true },
  mat_khau_hash: { type: String, default: null }, // null nếu đăng nhập bằng Google
  link_anh_dai_dien: { type: String, default: null },
  google_id: { type: String, default: null },
  cap_do: { type: Number, default: 1, min: 1 },
  diem_tich_luy: { type: Number, default: 0, min: 0 },
  chuoi_ngay_hoc: { type: Number, default: 0, min: 0 },
  ngay_hoc_cuoi: { type: Date, default: null }, // Ngày học cuối cùng để tính streak
  vai_tro: { type: String, enum: ['student', 'admin'], default: 'student' },
  trang_thai: { type: String, enum: ['active', 'inactive', 'banned'], default: 'active' },
  tu_vung_yeu_thich: [{ type: String }],
  ngay_tao: { type: Date, default: Date.now },
  email_verified: { type: Boolean, default: false },
  email_verification_token: { type: String, default: null },
  email_verification_expire: { type: Date, default: null }
}, {
  timestamps: true,
  collection: 'nguoi_dung'
});

nguoiDungSchema.index({ email: 1 });

module.exports = mongoose.model('NguoiDung', nguoiDungSchema);

