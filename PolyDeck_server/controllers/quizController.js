const BaiQuiz = require('../models/BaiQuiz');
const LichSuLamBai = require('../models/LichSuLamBai');
const NguoiDung = require('../models/NguoiDung');
const ChuDe = require('../models/ChuDe');

const createQuiz = async (req, res) => {
    try {
        const { ma_chu_de, questions } = req.body;

        if (!ma_chu_de || !questions || !Array.isArray(questions) || questions.length === 0) {
            return res.status(400).json({ message: 'Dữ liệu quiz không hợp lệ.' });
        }

        await BaiQuiz.deleteMany({ ma_chu_de: ma_chu_de });

        // Tạo một bài quiz mới dựa trên model BaiQuiz
        const newQuiz = new BaiQuiz({
            ma_chu_de: ma_chu_de,
            questions: questions // Dữ liệu câu hỏi và câu trả lời từ client
        });

        await newQuiz.save();

        res.status(201).json({ message: 'Tạo quiz thành công!', data: newQuiz });

    } catch (error) {
        console.error('Lỗi khi tạo quiz:', error);
        res.status(500).json({ message: 'Lỗi server khi tạo quiz' });
    }
};
const getQuizByTopic = async (req, res) => {
    try {
        const { ma_chu_de } = req.params;
        const quiz = await BaiQuiz.findOne({ ma_chu_de: ma_chu_de }); // ma_chu_de từ params là ObjectId string

        if (!quiz || quiz.questions.length === 0) {
            return res.status(404).json({ message: 'Không tìm thấy bài quiz cho chủ đề này.' });
        }
        const quizForStudent = JSON.parse(JSON.stringify(quiz));
        quizForStudent.questions.forEach(question => {
            question.answers.forEach(answer => {
                delete answer.isCorrect;
            });
        });
        res.status(200).json(quizForStudent);
    } catch (error) {
        console.error('Lỗi khi lấy bài quiz:', error);
        res.status(500).json({ message: 'Lỗi server' });
    }
};

// --- "Đầu bếp" 2: NỘP BÀI VÀ CHẤM ĐIỂM ---
const submitQuiz = async (req, res) => {
    try {
        const { ma_nguoi_dung, ma_quiz, userAnswers } = req.body;
        if (!ma_nguoi_dung || !ma_quiz || !userAnswers) {
            return res.status(400).json({ message: 'Thiếu thông tin bài làm.' });
        }
        const correctQuiz = await BaiQuiz.findById(ma_quiz);
        if (!correctQuiz) {
            return res.status(404).json({ message: 'Không tìm thấy bài quiz.' });
        }
        let score = 0;
        const totalQuestions = correctQuiz.questions.length;
        correctQuiz.questions.forEach((correctQuestion, index) => {
            const correctAnswer = correctQuestion.answers.find(answer => answer.isCorrect);
            const userAnswer = userAnswers[index];
            if (userAnswer && correctAnswer && userAnswer.selectedAnswerText === correctAnswer.answerText) {
                score++;
            }
        });
        const finalScore = (score / totalQuestions) * 100;
        const newHistory = new LichSuLamBai({
            ma_nguoi_dung: ma_nguoi_dung,
            ma_quiz: ma_quiz,
            ma_chu_de: correctQuiz.ma_chu_de, // ma_chu_de là ObjectId ref
            diem_so: Math.round(finalScore),
            so_cau_dung: score,
            tong_so_cau: totalQuestions,
        });
        await newHistory.save();
        await NguoiDung.updateOne({ _id: ma_nguoi_dung }, { $inc: { diem_tich_luy: Math.round(finalScore) } });
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

// --- "Đầu bếp" 3: LẤY LỊCH SỬ LÀM BÀI CỦA NGƯỜI DÙNG ---
const getHistoryByUser = async (req, res) => {
    try {
        const { ma_nguoi_dung } = req.params;
        const history = await LichSuLamBai.find({ ma_nguoi_dung: ma_nguoi_dung })
            .populate({
                path: 'ma_chu_de',
                select: 'ten_chu_de link_anh_icon'
            })
            .sort({ ngay_lam_bai: -1 });
        res.status(200).json(history);
    } catch (error) {
        console.error('Lỗi khi lấy lịch sử:', error);
        res.status(500).json({ message: 'Lỗi server' });
    }
};
module.exports = {
    createQuiz,     
    getQuizByTopic,
    submitQuiz,
    getHistoryByUser
};
