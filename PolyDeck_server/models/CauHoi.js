const mongoose = require('mongoose');

const cauHoiSchema = new mongoose.Schema({
  ma_cau_hoi: {
    type: String,
    required: true,
    unique: true
  },
  ma_chu_de: {
    type: String,
    required: true,
    ref: 'ChuDe'
  },
  noi_dung_cau_hoi: {
    type: String,
    required: true
  },
  dap_an_a: {
    type: String,
    required: true
  },
  dap_an_b: {
    type: String,
    required: true
  },
  dap_an_c: {
    type: String,
    required: true
  },
  dap_an_d: {
    type: String,
    required: true
  },
  dap_an_dung: {
    type: String,
    required: true,
    enum: ['A', 'B', 'C', 'D']
  },
  loai_cau_hoi: {
    type: String,
    enum: ['multiple_choice', 'fill_in_blank', 'matching'],
    default: 'multiple_choice'
  },
  am_thanh: {
    type: String,
    default: null
  }
}, {
  timestamps: true,
  collection: 'cauhoi'
});

// Index để tìm kiếm nhanh
cauHoiSchema.index({ ma_cau_hoi: 1 });
cauHoiSchema.index({ ma_chu_de: 1 });

module.exports = mongoose.model('CauHoi', cauHoiSchema);

