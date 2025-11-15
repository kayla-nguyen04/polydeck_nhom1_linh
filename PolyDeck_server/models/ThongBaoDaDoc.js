const mongoose = require('mongoose');

const thongBaoDaDocSchema = new mongoose.Schema({
  ma_thong_bao_da_doc: {
    type: String,
    required: true,
    unique: true
  },
  ma_nguoi_dung: {
    type: String,
    required: true,
    ref: 'NguoiDung'
  },
  ma_thong_bao: {
    type: String,
    required: true,
    ref: 'ThongBao'
  }
}, {
  timestamps: true,
  collection: 'thongbaodadoc'
});

// Index để tìm kiếm nhanh và đảm bảo unique combination
thongBaoDaDocSchema.index({ ma_thong_bao_da_doc: 1 });
thongBaoDaDocSchema.index({ ma_nguoi_dung: 1, ma_thong_bao: 1 }, { unique: true });

module.exports = mongoose.model('ThongBaoDaDoc', thongBaoDaDocSchema);

