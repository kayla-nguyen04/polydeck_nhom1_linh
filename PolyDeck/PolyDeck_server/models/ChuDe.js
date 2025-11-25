const mongoose = require('mongoose');

const chuDeSchema = new mongoose.Schema({
  ma_chu_de: { type: String, required: true, unique: true, trim: true },
  ten_chu_de: { type: String, required: true, trim: true },
  link_anh_icon: { type: String, default: null },
  so_luong_tu: { type: Number, default: 0, min: 0 },
  so_luong_quiz: { type: Number, default: 0, min: 0 },
  ngay_tao: { type: Date, default: Date.now }
}, {
  timestamps: true,
  collection: 'chu_de'
});

chuDeSchema.index({ ten_chu_de: 1 });

module.exports = mongoose.model('ChuDe', chuDeSchema);

