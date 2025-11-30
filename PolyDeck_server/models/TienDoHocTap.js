const mongoose = require('mongoose');

const tienDoHocTapSchema = new mongoose.Schema({
  ma_nguoi_dung: { 
    type: mongoose.Schema.Types.ObjectId, 
    ref: 'NguoiDung', 
    required: true 
  },
  ma_tu_vung: { 
    type: mongoose.Schema.Types.ObjectId, 
    ref: 'TuVung', 
    required: true 
  },
  ma_chu_de: { 
    type: mongoose.Schema.Types.ObjectId, 
    ref: 'ChuDe', 
    required: true 
  },
  trang_thai_hoc: { type: String, enum: ['chua_hoc', 'dang_hoc', 'da_nho'], default: 'chua_hoc' },
  lan_cuoi_hoc: { type: Date, default: Date.now }
}, {
  timestamps: true,
  collection: 'tien_do_hoc_tap'
});

tienDoHocTapSchema.index({ ma_nguoi_dung: 1, ma_tu_vung: 1 }, { unique: true });

module.exports = mongoose.model('TienDoHocTap', tienDoHocTapSchema);

