const express = require('express');
const mongoose = require('mongoose');
const router = express.Router();
const BaiQuiz = require('../models/BaiQuiz');
const LichSuLamBai = require('../models/LichSuLamBai');
const NguoiDung = require('../models/NguoiDung');
const ChuDe = require('../models/ChuDe');

const createApiResponse = (success, message, data = null) => {
    return {
        success,
        message,
        ...(data !== null && { data })
    };
};

const createQuiz = async (req, res) => {
    try {
        const { ma_chu_de, questions } = req.body;

        if (!ma_chu_de || !questions || !Array.isArray(questions) || questions.length === 0) {
            return res.status(400).json(createApiResponse(false, 'Dữ liệu quiz không hợp lệ.'));
        }

        // Validate questions structure
        for (const question of questions) {
            if (!question.questionText || !question.answers || !Array.isArray(question.answers) || question.answers.length === 0) {
                return res.status(400).json(createApiResponse(false, 'Cấu trúc câu hỏi không hợp lệ.'));
            }
            
            // Validate that at least one answer is correct
            const hasCorrectAnswer = question.answers.some(answer => answer.isCorrect === true);
            if (!hasCorrectAnswer) {
                return res.status(400).json(createApiResponse(false, 'Mỗi câu hỏi phải có ít nhất một đáp án đúng.'));
            }
        }

        // Xóa quiz cũ của chủ đề (nếu muốn overwrite)
        await BaiQuiz.deleteMany({ ma_chu_de });

        const newQuiz = new BaiQuiz({
            ma_chu_de,
            questions
        });

        await newQuiz.save();

        res.status(201).json(createApiResponse(true, 'Tạo quiz thành công!', newQuiz));
    } catch (error) {
        console.error('Lỗi khi tạo quiz:', error);
        res.status(500).json(createApiResponse(false, 'Lỗi server khi tạo quiz'));
    }
};

// ======= Lấy quiz theo chủ đề (dành cho học sinh) =======
const getQuizByTopic = async (req, res) => {
    try {
        const { ma_chu_de } = req.params;

        // Tìm quiz theo ma_chu_de
        let filter = {};
        if (mongoose.Types.ObjectId.isValid(ma_chu_de)) {
            filter.ma_chu_de = ma_chu_de;
        } else {
            filter.ma_chu_de = ma_chu_de;
        }

        const quiz = await BaiQuiz.findOne(filter);

        if (!quiz || !quiz.questions || quiz.questions.length === 0) {
            return res.status(404).json(createApiResponse(false, 'Không tìm thấy bài quiz cho chủ đề này.'));
        }

        // Clone quiz để xóa isCorrect
        const quizForStudent = quiz.toObject();
        quizForStudent.questions.forEach(question => {
            if (question.answers && question.answers.length > 0) {
                question.answers.forEach(answer => delete answer.isCorrect);
            }
        });

        res.status(200).json(createApiResponse(true, 'Lấy quiz thành công', quizForStudent));
    } catch (error) {
        console.error('Lỗi khi lấy bài quiz:', error);
        res.status(500).json(createApiResponse(false, 'Lỗi server'));
    }
};

const submitQuiz = async (req, res) => {
    try {
        const { ma_nguoi_dung, ma_quiz, userAnswers } = req.body;

        if (!ma_nguoi_dung || !mongoose.Types.ObjectId.isValid(ma_nguoi_dung)) {
            return res.status(400).json(createApiResponse(false, 'ma_nguoi_dung không hợp lệ'));
        }

        if (!ma_quiz || !mongoose.Types.ObjectId.isValid(ma_quiz)) {
            return res.status(400).json(createApiResponse(false, 'ma_quiz không hợp lệ'));
        }

        if (!userAnswers || !Array.isArray(userAnswers)) {
            return res.status(400).json(createApiResponse(false, 'userAnswers không hợp lệ'));
        }

        const quiz = await BaiQuiz.findById(ma_quiz);

        if (!quiz) return res.status(404).json(createApiResponse(false, 'Không tìm thấy bài quiz.'));

        let score = 0;

        quiz.questions.forEach((q, idx) => {
            const correctAnswer = q.answers.find(a => a.isCorrect);
            const userAnswer = userAnswers[idx];

            if (userAnswer && correctAnswer &&
                userAnswer.selectedAnswerText?.trim().toLowerCase() === correctAnswer.answerText.trim().toLowerCase()
            ) {
                score++;
            }
        });

        const finalScore = Math.round((score / quiz.questions.length) * 100);

        // Lưu lịch sử
        await LichSuLamBai.create({
            ma_nguoi_dung,
            ma_quiz,
            ma_chu_de: quiz.ma_chu_de,
            diem_so: finalScore,
            so_cau_dung: score,
            tong_so_cau: quiz.questions.length
        });

        // Cập nhật điểm tích lũy
        await NguoiDung.updateOne(
            { _id: ma_nguoi_dung },
            { $inc: { diem_tich_luy: finalScore } }
        );

        res.json(createApiResponse(true, 'Nộp bài thành công!', {
            scorePercent: finalScore,
            correct: score,
            total: quiz.questions.length
        }));
    } catch (error) {
        console.error('Lỗi submitQuiz:', error);
        res.status(500).json(createApiResponse(false, 'Lỗi server'));
    }
};

// ======= Lấy lịch sử làm bài theo người dùng =======
const getHistoryByUser = async (req, res) => {
    try {
        const { ma_nguoi_dung } = req.params;
        const history = await LichSuLamBai.find({ ma_nguoi_dung })
            .populate({
                path: 'ma_chu_de',
                select: 'ten_chu_de link_anh_icon'
            })
            .sort({ ngay_lam_bai: -1 });

        res.status(200).json(createApiResponse(true, 'Lấy lịch sử thành công', history));
    } catch (error) {
        console.error('Lỗi khi lấy lịch sử:', error);
        res.status(500).json(createApiResponse(false, 'Lỗi server'));
    }
};

// ======= Lấy tất cả quiz (admin) =======
router.get('/', async (req, res) => {
    try {
        const quizzes = await BaiQuiz.find({}).sort({ createdAt: -1 });
        res.json(quizzes);
    } catch (err) {
        res.status(500).json({ message: 'Lỗi server khi lấy danh sách quiz' });
    }
});

// ======= Xóa quiz theo ID (admin) =======
const deleteQuiz = async (req, res) => {
    try {
        const { id } = req.params;
        if (!mongoose.Types.ObjectId.isValid(id)) {
            return res.status(400).json(createApiResponse(false, 'ID quiz không hợp lệ.'));
        }

        const result = await BaiQuiz.findByIdAndDelete(id);
        if (!result) {
            return res.status(404).json(createApiResponse(false, 'Không tìm thấy quiz để xóa.'));
        }

        // Cũng nên xóa lịch sử làm bài liên quan (tùy chọn)
        await LichSuLamBai.deleteMany({ ma_quiz: id });

        res.status(200).json(createApiResponse(true, 'Xóa quiz thành công!'));
    } catch (error) {
        console.error('Lỗi khi xóa quiz:', error);
        res.status(500).json(createApiResponse(false, 'Lỗi server khi xóa quiz'));
    }
};

// ======= Routes =======
router.get('/by-topic/:ma_chu_de', getQuizByTopic);
router.post('/submit', submitQuiz);
router.get('/history/:ma_nguoi_dung', getHistoryByUser);
router.post('/create', createQuiz);
router.delete('/:id', deleteQuiz);

module.exports = router;

