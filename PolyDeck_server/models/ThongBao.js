const mongoose = require('mongoose');

const thongBaoSchema = new mongoose.Schema({
  ma_thong_bao: {
    type: String,
    required: true,
    unique: true
  },
  tieu_de: {
    type: String,
    required: true
  },
  noi_dung: {
    type: String,
    required: true
  },
  ngay_gui: {
    type: Date,
    default: Date.now
  }
}, {
  timestamps: true,
  collection: 'thongbao'
});

// Index để tìm kiếm nhanh
thongBaoSchema.index({ ma_thong_bao: 1 });
thongBaoSchema.index({ ngay_gui: -1 });

module.exports = mongoose.model('ThongBao', thongBaoSchema);

