const mongoose = require('mongoose');

const nguoiDungSchema = new mongoose.Schema({
  ma_nguoi_dung: {
    type: String,
    required: true,
    unique: true
  },
  email: {
    type: String,
    required: true,
    unique: true,
    lowercase: true,
    trim: true
  },
  ho_ten: {
    type: String,
    required: true
  },
  anh_dai_dien: {
    type: String,
    default: null
  },
  vai_tro: {
    type: String,
    enum: ['student', 'admin'],
    default: 'student'
  },
  diem_tich_luy: {
    type: Number,
    default: 0,
    min: 0
  },
  ngay_tham_gia: {
    type: Date,
    default: Date.now
  }
}, {
  timestamps: true,
  collection: 'nguoidung'
});

// Index để tìm kiếm nhanh
nguoiDungSchema.index({ ma_nguoi_dung: 1 });
nguoiDungSchema.index({ email: 1 });

module.exports = mongoose.model('NguoiDung', nguoiDungSchema);

