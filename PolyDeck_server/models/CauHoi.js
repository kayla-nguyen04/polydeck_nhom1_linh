const mongoose = require('mongoose');

const luaChonSchema = new mongoose.Schema({
  ma_lua_chon: { type: String, required: true, trim: true },
  noi_dung: { type: String, required: true, trim: true }
}, { _id: false });

const cauHoiSchema = new mongoose.Schema({
  quiz_id: { 
    type: mongoose.Schema.Types.ObjectId, 
    ref: 'BaiQuiz', 
    required: true 
  },
  noi_dung_cau_hoi: { type: String, required: true, trim: true },
  dap_an_lua_chon: { type: [luaChonSchema], default: [] },
  dap_an_dung: { type: String, required: true, trim: true }
}, {
  timestamps: true,
  collection: 'cau_hoi'
});

cauHoiSchema.index({ quiz_id: 1 });

module.exports = mongoose.model('CauHoi', cauHoiSchema);

