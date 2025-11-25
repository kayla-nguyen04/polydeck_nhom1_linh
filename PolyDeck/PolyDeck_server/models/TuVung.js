const mongoose = require('mongoose');

const tuVungSchema = new mongoose.Schema({
  ma_tu_vung: { type: String, required: true, unique: true, trim: true },
  ma_chu_de: { type: String, required: true, trim: true },
  tu_tieng_anh: { type: String, required: true, trim: true },
  phien_am: { type: String, default: null },
  link_anh: { type: String, default: null },
  cau_vi_du: { type: String, default: null },
  am_thanh: { type: String, default: null },
  nghia_tieng_viet: { type: String, required: true, trim: true }
}, {
  timestamps: true,
  collection: 'tu_vung'
});

tuVungSchema.index({ ma_chu_de: 1 });
tuVungSchema.index({ tu_tieng_anh: 1 });

module.exports = mongoose.model('TuVung', tuVungSchema);

