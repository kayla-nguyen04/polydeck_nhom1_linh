const mongoose = require('mongoose');
const Schema = mongoose.Schema;

// --- Định nghĩa cấu trúc cho một câu trả lời ---
const answerSchema = new Schema({
    answerText: {
        type: String,
        required: true
    },
    isCorrect: { // Trường này để xác định đáp án đúng
        type: Boolean,
        required: true,
        default: false
    }
}, { _id: false });

// --- Định nghĩa cấu trúc cho một câu hỏi ---
const questionSchema = new Schema({
    questionText: {
        type: String,
        required: true
    },
    answers: [answerSchema] // Một câu hỏi có một mảng các câu trả lời
}, { _id: false });

// --- Định nghĩa cấu- trúc chính cho Bài Quiz ---
const baiQuizSchema = new Schema({
  ma_chu_de: { 
    type: Schema.Types.ObjectId, 
    ref: 'ChuDe', // Tham chiếu đến model 'ChuDe'
    required: true 
  },
  questions: [questionSchema] // <<< FIX: Bổ sung mảng câu hỏi
}, {
  timestamps: true,
  collection: 'bai_quiz'
});

baiQuizSchema.index({ ma_chu_de: 1 });

module.exports = mongoose.model('BaiQuiz', baiQuizSchema);