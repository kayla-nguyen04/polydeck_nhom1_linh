const mongoose = require('mongoose');

const lichSuLamBaiSchema = new mongoose.Schema({
  ma_lich_su: { type: String, required: true, unique: true, trim: true },
  ma_nguoi_dung: { type: String, required: true, trim: true },
  ma_quiz: { type: String, required: true, trim: true },
  ma_chu_de: { type: String, required: true, trim: true },
  diem_so: { type: Number, default: 0, min: 0 },
  diem_danh_duoc: { type: Number, default: 0, min: 0 },
  thoi_gian_lam_bai: { type: Number, default: 0 },
  ngay_hoan_thanh: { type: Date, default: Date.now }
}, {
  timestamps: true,
  collection: 'lich_su_lam_bai'
});

lichSuLamBaiSchema.index({ ma_nguoi_dung: 1 });
lichSuLamBaiSchema.index({ ma_quiz: 1 });

module.exports = mongoose.model('LichSuLamBai', lichSuLamBaiSchema);

