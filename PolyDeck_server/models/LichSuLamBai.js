const mongoose = require('mongoose');

const lichSuLamBaiSchema = new mongoose.Schema({
  ma_lich_su: {
    type: String,
    required: true,
    unique: true
  },
  ma_nguoi_dung: {
    type: String,
    required: true,
    ref: 'NguoiDung'
  },
  ma_chu_de: {
    type: String,
    required: true,
    ref: 'ChuDe'
  },
  diem_so: {
    type: Number,
    required: true,
    min: 0
  },
  ngay_hoan_thanh: {
    type: Date,
    default: Date.now
  }
}, {
  timestamps: true,
  collection: 'lichsulambai'
});

// Index để tìm kiếm nhanh
lichSuLamBaiSchema.index({ ma_lich_su: 1 });
lichSuLamBaiSchema.index({ ma_nguoi_dung: 1 });
lichSuLamBaiSchema.index({ ma_chu_de: 1 });
lichSuLamBaiSchema.index({ ngay_hoan_thanh: -1 });

module.exports = mongoose.model('LichSuLamBai', lichSuLamBaiSchema);

