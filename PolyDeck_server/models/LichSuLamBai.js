const mongoose = require('mongoose');
const Schema = mongoose.Schema;

const lichSuLamBaiSchema = new mongoose.Schema({
  ma_nguoi_dung: {
      type: Schema.Types.ObjectId, 
      ref: 'NguoiDung', 
      required: true 
  },
  ma_quiz: { 
      type: Schema.Types.ObjectId, 
      ref: 'BaiQuiz', 
      required: true 
  },
  ma_chu_de: { 
      type: Schema.Types.ObjectId, 
      ref: 'ChuDe', 
      required: true 
  },
  diem_so: { type: Number, required: true, min: 0, max: 100 },
  so_cau_dung: { type: Number, required: true, min: 0 }, // <<< FIX: Thêm trường này
  tong_so_cau: { type: Number, required: true, min: 0 }, // <<< FIX: Thêm trường này
  ngay_lam_bai: { type: Date, default: Date.now } // <<< FIX: Đổi tên cho nhất quán
}, {
  timestamps: true,
  collection: 'lich_su_lam_bai'
});

lichSuLamBaiSchema.index({ ma_nguoi_dung: 1 });
lichSuLamBaiSchema.index({ ma_quiz: 1 });

module.exports = mongoose.model('LichSuLamBai', lichSuLamBaiSchema);