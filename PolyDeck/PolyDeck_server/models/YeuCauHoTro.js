const mongoose = require('mongoose');

const yeuCauHoTroSchema = new mongoose.Schema({
  ma_ho_tro: { type: String, required: true, unique: true, trim: true },
  ma_nguoi_dung: { type: String, required: true, trim: true },
  noi_dung: { type: String, required: true },
  ten_nguoi_gui: { type: String, required: true, trim: true },
  email_nguoi_gui: { type: String, required: true, trim: true },
  ngay_gui: { type: Date, default: Date.now }
}, {
  timestamps: true,
  collection: 'yeu_cau_ho_tro'
});

yeuCauHoTroSchema.index({ ma_nguoi_dung: 1, ngay_gui: -1 });

module.exports = mongoose.model('YeuCauHoTro', yeuCauHoTroSchema);

