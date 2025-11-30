const mongoose = require('mongoose');

const tuVungSchema = new mongoose.Schema({
  ma_chu_de: { 
    type: mongoose.Schema.Types.ObjectId, 
    ref: 'ChuDe', 
    required: true 
  },
  tu_tieng_anh: { type: String, required: true, trim: true },
  phien_am: { type: String, default: null },
  cau_vi_du: { type: String, default: null },
  nghia_tieng_viet: { type: String, required: true, trim: true }
}, {
  timestamps: true,
  collection: 'tu_vung'
});

tuVungSchema.index({ ma_chu_de: 1 });
tuVungSchema.index({ tu_tieng_anh: 1 });

module.exports = mongoose.model('TuVung', tuVungSchema);

