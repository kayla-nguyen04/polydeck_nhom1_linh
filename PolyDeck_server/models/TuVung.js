const mongoose = require('mongoose');

const tuVungSchema = new mongoose.Schema({
  ma_tu_vung: {
    type: String,
    required: true,
    unique: true
  },
  ma_chu_de: {
    type: String,
    required: true,
    ref: 'ChuDe'
  },
  tu_tieng_anh: {
    type: String,
    required: true
  },
  nghia_tieng_viet: {
    type: String,
    required: true
  },
  phien_am: {
    type: String,
    default: null
  },
  am_thanh: {
    type: String,
    default: null
  },
  cau_vi_du: {
    type: String,
    default: null
  },
  nghia_cau_vi_du: {
    type: String,
    default: null
  }
}, {
  timestamps: true,
  collection: 'tuvung'
});

// Index để tìm kiếm nhanh
tuVungSchema.index({ ma_tu_vung: 1 });
tuVungSchema.index({ ma_chu_de: 1 });
tuVungSchema.index({ tu_tieng_anh: 1 });

module.exports = mongoose.model('TuVung', tuVungSchema);

