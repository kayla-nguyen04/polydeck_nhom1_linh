const BaiQuiz = require('../models/BaiQuiz');
const CauHoi = require('../models/CauHoi');
const LichSuLamBai = require('../models/LichSuLamBai');

const mongoose = require('mongoose');
const ChuDe = require('../models/ChuDe');

// GET /api/quizzes/by-topic/:ma_chu_de
const getQuizByTopic = async (req, res) => {
  try {
    let { ma_chu_de } = req.params;
    // Accept both ObjectId and ma_chu_de
    if (mongoose.Types.ObjectId.isValid(ma_chu_de)) {
      const chuDe = await ChuDe.findById(ma_chu_de).lean();
      if (chuDe) ma_chu_de = chuDe.ma_chu_de;
    }
    const quiz = await BaiQuiz.findOne({ ma_chu_de }).sort({ createdAt: -1 }).lean();
    if (!quiz) return res.status(404).json({ success: false, message: 'Chưa có quiz cho chủ đề này' });

    const questions = await CauHoi.find({ ma_quiz: quiz.ma_quiz }).select('-__v -updatedAt').lean();
    res.json({ success: true, data: { quiz, questions } });
  } catch (e) {
    res.status(500).json({ success: false, message: e.message });
  }
};

// POST /api/quizzes/submit
// body: { ma_nguoi_dung, ma_quiz, ma_chu_de, answers: [{ ma_cau_hoi, ma_lua_chon }], thoi_gian_lam_bai }
const submitQuiz = async (req, res) => {
  try {
    const { ma_nguoi_dung, ma_quiz, ma_chu_de, answers = [], thoi_gian_lam_bai = 0 } = req.body || {};
    if (!ma_nguoi_dung || !ma_quiz || !ma_chu_de) {
      return res.status(400).json({ success: false, message: 'Thiếu dữ liệu bắt buộc' });
    }

    const allQuestions = await CauHoi.find({ ma_quiz }).lean();
    const answerMap = new Map(answers.map(a => [a.ma_cau_hoi, a.ma_lua_chon]));

    let correct = 0;
    for (const q of allQuestions) {
      const chosen = answerMap.get(q.ma_cau_hoi);
      if (chosen && chosen === q.dap_an_dung) correct++;
    }
    const total = allQuestions.length || 1;
    const scorePercent = Math.round((correct / total) * 100);

    const history = new LichSuLamBai({
      ma_lich_su: `HIS_${Date.now()}`,
      ma_nguoi_dung,
      ma_quiz,
      ma_chu_de,
      diem_so: scorePercent,
      diem_danh_duoc: correct,
      thoi_gian_lam_bai
    });
    await history.save();

    res.json({ success: true, data: { scorePercent, correct, total } });
  } catch (e) {
    res.status(500).json({ success: false, message: e.message });
  }
};

// GET /api/quizzes/history/:ma_nguoi_dung
const getHistoryByUser = async (req, res) => {
  try {
    const { ma_nguoi_dung } = req.params;
    const items = await LichSuLamBai.find({ ma_nguoi_dung }).sort({ createdAt: -1 }).lean();
    res.json({ success: true, data: items });
  } catch (e) {
    res.status(500).json({ success: false, message: e.message });
  }
};

module.exports = { getQuizByTopic, submitQuiz, getHistoryByUser };

