const mongoose = require('mongoose');

const thongBaoSchema = new mongoose.Schema({
  ma_nguoi_dung: { 
    type: mongoose.Schema.Types.ObjectId, 
    ref: 'NguoiDung', 
    default: null 
  }, // null = thông báo chung, có giá trị = thông báo cá nhân
  tieu_de: { type: String, required: true, trim: true },
  noi_dung: { type: String, required: true },
  ngay_gui: { type: Date, default: Date.now },
  // Lưu danh sách user đã đọc thông báo này
  da_doc_cho: { type: [String], default: [] }
}, {
  timestamps: true,
  collection: 'thong_bao'
});

thongBaoSchema.index({ ngay_gui: -1 });

module.exports = mongoose.model('ThongBao', thongBaoSchema);

