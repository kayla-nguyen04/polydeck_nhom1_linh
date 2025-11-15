const mongoose = require('mongoose');

const chiTietLamBaiSchema = new mongoose.Schema({
  ma_chi_tiet: {
    type: String,
    required: true,
    unique: true
  },
  ma_lich_su: {
    type: String,
    required: true,
    ref: 'LichSuLamBai'
  },
  ma_cau_hoi: {
    type: String,
    required: true,
    ref: 'CauHoi'
  },
  dap_an_chon: {
    type: String,
    required: true,
    enum: ['A', 'B', 'C', 'D', '']
  },
  ket_qua: {
    type: String,
    required: true,
    enum: ['correct', 'incorrect']
  }
}, {
  timestamps: true,
  collection: 'chitietlambai'
});

// Index để tìm kiếm nhanh
chiTietLamBaiSchema.index({ ma_chi_tiet: 1 });
chiTietLamBaiSchema.index({ ma_lich_su: 1 });
chiTietLamBaiSchema.index({ ma_cau_hoi: 1 });

module.exports = mongoose.model('ChiTietLamBai', chiTietLamBaiSchema);

