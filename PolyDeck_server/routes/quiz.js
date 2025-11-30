const BaiQuiz = require('../models/BaiQuiz');
const LichSuLamBai = require('../models/LichSuLamBai');
const NguoiDung = require('../models/NguoiDung');
const ChuDe = require('../models/ChuDe');
const express = require('express');
const router = express.Router();
// --- 1. LẤY BÀI QUIZ THEO CHỦ ĐỀ ---
// Chức năng: Tìm một bài quiz dựa trên mã chủ đề và gửi về cho client.
// Quan trọng: Nó sẽ loại bỏ thông tin về "đáp án đúng" để người dùng không thể gian lận.
const getQuizByTopic = async (req, res) => {
    try {
        const { ma_chu_de } = req.params;

        // Tìm một bài quiz trong database có mã chủ đề tương ứng
        const quiz = await BaiQuiz.findOne({ ma_chu_de: ma_chu_de }); // ma_chu_de từ params là ObjectId string

        if (!quiz || quiz.questions.length === 0) {
            return res.status(404).json({ message: 'Không tìm thấy bài quiz cho chủ đề này.' });
        }

        // Tạo một bản sao của bài quiz để sửa đổi trước khi gửi đi
        const quizForStudent = JSON.parse(JSON.stringify(quiz));

        // Lặp qua từng câu hỏi và từng câu trả lời để xóa trường 'isCorrect'
        quizForStudent.questions.forEach(question => {
            question.answers.forEach(answer => {
                delete answer.isCorrect; // Xóa thông tin đáp án đúng
            });
        });

        res.status(200).json(quizForStudent);

    } catch (error) {
        console.error('Lỗi khi lấy bài quiz:', error);
        res.status(500).json({ message: 'Lỗi server' });
    }
};


// --- 2. NỘP BÀI VÀ CHẤM ĐIỂM ---
// Chức năng: Nhận bài làm của người dùng, so sánh với đáp án đúng, tính điểm,
//            lưu vào lịch sử và cập nhật điểm cho người dùng.
const submitQuiz = async (req, res) => {
    try {
        const { ma_nguoi_dung, ma_quiz, userAnswers } = req.body;

        if (!ma_nguoi_dung || !ma_quiz || !userAnswers) {
            return res.status(400).json({ message: 'Thiếu thông tin bài làm.' });
        }

        // Lấy bài quiz gốc từ DB để có đáp án đúng
        const correctQuiz = await BaiQuiz.findById(ma_quiz);
        if (!correctQuiz) {
            return res.status(404).json({ message: 'Không tìm thấy bài quiz.' });
        }

        let score = 0;
        const totalQuestions = correctQuiz.questions.length;

        // Lặp qua từng câu hỏi trong bài quiz gốc
        correctQuiz.questions.forEach((correctQuestion, index) => {
            // Tìm câu trả lời đúng của câu hỏi này
            const correctAnswer = correctQuestion.answers.find(answer => answer.isCorrect);
            
            // Lấy câu trả lời của người dùng cho câu hỏi tương ứng
            const userAnswer = userAnswers[index];

            // So sánh và tính điểm
            if (userAnswer && correctAnswer && userAnswer.selectedAnswerText === correctAnswer.answerText) {
                score++;
            }
        });

        // Tính điểm cuối cùng (ví dụ: mỗi câu 10 điểm)
        const finalScore = (score / totalQuestions) * 100;

        // Tạo một bản ghi lịch sử mới
        const newHistory = new LichSuLamBai({
            ma_nguoi_dung: ma_nguoi_dung,
            ma_quiz: ma_quiz,
            ma_chu_de: correctQuiz.ma_chu_de, // ma_chu_de là ObjectId ref
            diem_so: Math.round(finalScore),
            so_cau_dung: score,
            tong_so_cau: totalQuestions,
        });
        await newHistory.save();

        // Cập nhật điểm tích lũy cho người dùng
        // Ví dụ: cộng thêm số điểm đạt được vào điểm tích lũy
        await NguoiDung.updateOne({ _id: ma_nguoi_dung }, { $inc: { diem_tich_luy: Math.round(finalScore) } });

        // Trả về kết quả cho người dùng
        res.status(200).json({
            message: 'Nộp bài thành công!',
            diem_so: Math.round(finalScore),
            so_cau_dung: score,
            tong_so_cau: totalQuestions,
        });

    } catch (error) {
        console.error('Lỗi khi nộp bài:', error);
        res.status(500).json({ message: 'Lỗi server' });
    }
};


// --- 3. LẤY LỊCH SỬ LÀM BÀI CỦA NGƯỜI DÙNG ---
const getHistoryByUser = async (req, res) => {
    try {
        const { ma_nguoi_dung } = req.params;

        // Tìm tất cả lịch sử làm bài của người dùng và sắp xếp theo ngày mới nhất
        const history = await LichSuLamBai.find({ ma_nguoi_dung: ma_nguoi_dung })
            .populate({
                path: 'ma_chu_de', // Lấy thông tin từ bảng ChuDe
                select: 'ten_chu_de link_anh_icon' // Chỉ lấy tên và ảnh của chủ đề
            })
            .sort({ ngay_lam_bai: -1 }); // Sắp xếp giảm dần theo ngày

        res.status(200).json(history);

    } catch (error) {
        console.error('Lỗi khi lấy lịch sử:', error);
        res.status(500).json({ message: 'Lỗi server' });
    }
};

// Register routes
router.get('/by-topic/:ma_chu_de', getQuizByTopic);
router.post('/submit', submitQuiz);
router.get('/history/:ma_nguoi_dung', getHistoryByUser);

module.exports = router;
