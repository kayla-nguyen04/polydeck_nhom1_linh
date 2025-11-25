const mongoose = require('mongoose');

const baiQuizSchema = new mongoose.Schema({
  ma_quiz: { type: String, required: true, unique: true, trim: true },
  ma_chu_de: { type: String, required: true, trim: true },
  tieu_de: { type: String, required: true, trim: true },
  ngay_tao: { type: Date, default: Date.now }
}, {
  timestamps: true,
  collection: 'bai_quiz'
});

baiQuizSchema.index({ ma_chu_de: 1 });

module.exports = mongoose.model('BaiQuiz', baiQuizSchema);

