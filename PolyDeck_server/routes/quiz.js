const express = require('express');
const mongoose = require('mongoose');
const router = express.Router();
const BaiQuiz = require('../models/BaiQuiz');
const CauHoi = require('../models/CauHoi');
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

/**
 * Tính và cập nhật streak cho người dùng
 * Logic:
 * - Nếu hôm nay chưa học: streak = 1, ngay_hoc_cuoi = hôm nay
 * - Nếu hôm qua đã học: streak += 1, ngay_hoc_cuoi = hôm nay
 * - Nếu cách hôm nay > 1 ngày: streak = 1, ngay_hoc_cuoi = hôm nay
 */
const updateStreak = async (userId) => {
    try {
        const user = await NguoiDung.findById(userId);
        if (!user) return;

        const today = new Date();
        today.setHours(0, 0, 0, 0);
        
        const yesterday = new Date(today);
        yesterday.setDate(yesterday.getDate() - 1);

        let newStreak = 1;
        let lastStudyDate = today;

        if (user.ngay_hoc_cuoi) {
            const lastStudy = new Date(user.ngay_hoc_cuoi);
            lastStudy.setHours(0, 0, 0, 0);
            
            const daysDiff = Math.floor((today - lastStudy) / (1000 * 60 * 60 * 24));
            
            if (daysDiff === 0) {
                // Đã học hôm nay rồi, giữ nguyên streak
                newStreak = user.chuoi_ngay_hoc || 0;
                lastStudyDate = user.ngay_hoc_cuoi;
            } else if (daysDiff === 1) {
                // Hôm qua đã học, tăng streak
                newStreak = (user.chuoi_ngay_hoc || 0) + 1;
                lastStudyDate = today;
            } else {
                // Cách > 1 ngày, reset streak về 1
                newStreak = 1;
                lastStudyDate = today;
            }
        } else {
            // Chưa có ngày học cuối, bắt đầu streak = 1
            newStreak = 1;
            lastStudyDate = today;
        }

        await NguoiDung.updateOne(
            { _id: userId },
            { 
                $set: { 
                    chuoi_ngay_hoc: newStreak,
                    ngay_hoc_cuoi: lastStudyDate
                }
            }
        );

        console.log(`✅ Updated streak for user ${userId}: ${newStreak} days`);
    } catch (error) {
        console.error('❌ Error updating streak:', error);
    }
};

const createQuiz = async (req, res) => {
    try {
        const { ma_chu_de, questions } = req.body;

        if (!ma_chu_de || !questions || !Array.isArray(questions) || questions.length === 0) {
            return res.status(400).json(createApiResponse(false, 'Dữ liệu quiz không hợp lệ.'));
        }

        // Validate và convert ma_chu_de sang ObjectId nếu cần
        let chuDeId = ma_chu_de;
        if (!mongoose.Types.ObjectId.isValid(ma_chu_de)) {
            return res.status(400).json(createApiResponse(false, 'ma_chu_de không hợp lệ.'));
        }
        if (typeof ma_chu_de === 'string') {
            chuDeId = new mongoose.Types.ObjectId(ma_chu_de);
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

        // Xóa quiz cũ và câu hỏi cũ
        const oldQuiz = await BaiQuiz.findOne({ ma_chu_de: chuDeId });
        if (oldQuiz) {
            // Xóa tất cả câu hỏi cũ liên quan đến quiz này
            await CauHoi.deleteMany({ quiz_id: oldQuiz._id });
        }
        await BaiQuiz.deleteMany({ ma_chu_de: chuDeId });

        // Tạo một bài quiz mới
        const newQuiz = new BaiQuiz({
            ma_chu_de: chuDeId,
            questions
        });

        await newQuiz.save();

        // Lưu từng câu hỏi vào collection cau_hoi
        const savedQuestions = [];
        for (let i = 0; i < questions.length; i++) {
            const question = questions[i];
            
            try {
                // Tìm đáp án đúng
                const correctAnswer = question.answers.find(answer => answer.isCorrect === true);
                if (!correctAnswer) {
                    console.warn(`Câu hỏi ${i + 1} không có đáp án đúng, bỏ qua.`);
                    continue;
                }

                // Chuyển đổi answers sang format dap_an_lua_chon
                // Format: a_{qIndex}_{aIndex} để nhất quán với API response
                const dapAnLuaChon = question.answers.map((answer, idx) => ({
                    ma_lua_chon: `a_${i}_${idx}`,
                    noi_dung: answer.answerText
                }));

                // Tạo câu hỏi mới trong collection cau_hoi
                const newCauHoi = new CauHoi({
                    quiz_id: newQuiz._id,
                    noi_dung_cau_hoi: question.questionText,
                    dap_an_lua_chon: dapAnLuaChon,
                    dap_an_dung: correctAnswer.answerText
                });

                await newCauHoi.save();
                savedQuestions.push(newCauHoi);
            } catch (questionError) {
                console.error(`Lỗi khi lưu câu hỏi ${i + 1}:`, questionError);
                // Tiếp tục với câu hỏi tiếp theo thay vì dừng lại
            }
        }

        console.log(`✅ Đã lưu ${savedQuestions.length}/${questions.length} câu hỏi vào collection cau_hoi`);
        console.log(`✅ Quiz ID: ${newQuiz._id}, Chủ đề ID: ${chuDeId}`);

        if (savedQuestions.length === 0) {
            console.warn('⚠️ Cảnh báo: Không có câu hỏi nào được lưu vào collection cau_hoi!');
        }

        res.status(201).json(createApiResponse(true, 'Tạo quiz thành công!', {
            ...newQuiz.toObject(),
            questionsSaved: savedQuestions.length
        }));
    } catch (error) {
        console.error('Lỗi khi tạo quiz:', error);
        res.status(500).json(createApiResponse(false, 'Lỗi server khi tạo quiz'));
    }
};

// ======= Lấy quiz theo chủ đề (dành cho học sinh) =======
const getQuizByTopic = async (req, res) => {
    try {
        const { ma_chu_de } = req.params;

        // Validate ma_chu_de
        if (!ma_chu_de || !mongoose.Types.ObjectId.isValid(ma_chu_de)) {
            return res.status(400).json(createApiResponse(false, 'ID chủ đề không hợp lệ.'));
        }

        // Convert ma_chu_de sang ObjectId để đảm bảo so sánh chính xác
        const chuDeObjectId = new mongoose.Types.ObjectId(ma_chu_de);

        // Tìm quiz theo ma_chu_de CHÍNH XÁC (không lấy quiz từ chủ đề khác)
        const quiz = await BaiQuiz.findOne({ ma_chu_de: chuDeObjectId })
            .populate('ma_chu_de', 'ten_chu_de');

        if (!quiz) {
            return res.status(404).json(createApiResponse(false, 'Không tìm thấy bài quiz cho chủ đề này.'));
        }

        // Đảm bảo quiz tìm được thuộc đúng chủ đề (double check)
        const quizChuDeId = quiz.ma_chu_de._id ? quiz.ma_chu_de._id.toString() : quiz.ma_chu_de.toString();
        if (quizChuDeId !== ma_chu_de) {
            console.error(`⚠️ CẢNH BÁO: Quiz ID ${quiz._id} không thuộc chủ đề ${ma_chu_de}, mà thuộc ${quizChuDeId}`);
            return res.status(404).json(createApiResponse(false, 'Không tìm thấy bài quiz cho chủ đề này.'));
        }

        // Lấy thông tin chủ đề
        let chuDeId = quiz.ma_chu_de._id ? quiz.ma_chu_de._id.toString() : quiz.ma_chu_de.toString();
        let chuDeName = 'Quiz';
        
        if (quiz.ma_chu_de && typeof quiz.ma_chu_de === 'object' && quiz.ma_chu_de.ten_chu_de) {
            chuDeName = quiz.ma_chu_de.ten_chu_de;
        } else {
            // Nếu chưa populate, lấy từ database
            const chuDe = await ChuDe.findById(quiz.ma_chu_de);
            if (chuDe) {
                chuDeName = chuDe.ten_chu_de;
            }
        }

        // Lấy câu hỏi từ collection CauHoi (có dap_an_dung)
        const cauHoiList = await CauHoi.find({ quiz_id: quiz._id }).sort({ createdAt: 1 });

        if (!cauHoiList || cauHoiList.length === 0) {
            return res.status(404).json(createApiResponse(false, 'Không tìm thấy câu hỏi cho quiz này.'));
        }

        // Transform từ CauHoi sang QuizBundle format
        const quizBundle = {
            quiz: {
                ma_quiz: quiz._id.toString(),
                ma_chu_de: chuDeId,
                tieu_de: chuDeName
            },
            questions: cauHoiList.map((cauHoi, qIndex) => ({
                ma_cau_hoi: `q_${qIndex}`, // Format nhất quán với API response hiện tại
                noi_dung_cau_hoi: cauHoi.noi_dung_cau_hoi,
                dap_an_lua_chon: cauHoi.dap_an_lua_chon.map((luaChon, aIndex) => ({
                    ma_lua_chon: luaChon.ma_lua_chon || `a_${qIndex}_${aIndex}`, // Dùng ma_lua_chon từ DB, fallback nếu không có
                    noi_dung: luaChon.noi_dung
                })),
                dap_an_dung: cauHoi.dap_an_dung // QUAN TRỌNG: Trả về dap_an_dung từ database
            }))
        };

        res.status(200).json(createApiResponse(true, 'Lấy quiz thành công', quizBundle));
    } catch (error) {
        console.error('Lỗi khi lấy bài quiz:', error);
        res.status(500).json(createApiResponse(false, 'Lỗi server'));
    }
};

const submitQuiz = async (req, res) => {
    try {
        const { 
            ma_nguoi_dung, 
            ma_quiz, 
            ma_chu_de, 
            answers,
            // Ưu tiên sử dụng dữ liệu từ client (đã tính sẵn)
            so_cau_dung,
            tong_so_cau,
            diem_so
        } = req.body;

        console.log('[submitQuiz] Request body:', JSON.stringify(req.body, null, 2));

        if (!ma_nguoi_dung || !mongoose.Types.ObjectId.isValid(ma_nguoi_dung)) {
            return res.status(400).json(createApiResponse(false, 'ma_nguoi_dung không hợp lệ'));
        }

        if (!ma_quiz || !mongoose.Types.ObjectId.isValid(ma_quiz)) {
            return res.status(400).json(createApiResponse(false, 'ma_quiz không hợp lệ'));
        }

        const quiz = await BaiQuiz.findById(ma_quiz);
        if (!quiz) return res.status(404).json(createApiResponse(false, 'Không tìm thấy bài quiz.'));

        let finalScore, correctCount, totalQuestions;

        // Ưu tiên sử dụng dữ liệu từ client (đã tính sẵn)
        if (typeof so_cau_dung === 'number' && typeof tong_so_cau === 'number' && typeof diem_so === 'number') {
            correctCount = so_cau_dung;
            totalQuestions = tong_so_cau;
            finalScore = diem_so;
            console.log(`[submitQuiz] ✅ Sử dụng dữ liệu từ client: ${correctCount}/${totalQuestions} = ${finalScore}%`);
        } else {
            // Fallback: Tính từ answers nếu client không gửi
            console.log('[submitQuiz] ⚠️ Client không gửi so_cau_dung/tong_so_cau/diem_so, tính từ answers...');
            
            if (!answers || !Array.isArray(answers)) {
                return res.status(400).json(createApiResponse(false, 'answers không hợp lệ'));
            }

            let score = 0;
            const answerMap = new Map();
            answers.forEach(answer => {
                if (answer.ma_cau_hoi && answer.ma_lua_chon) {
                    answerMap.set(answer.ma_cau_hoi, answer.ma_lua_chon);
                }
            });

            // So sánh với đáp án đúng
            quiz.questions.forEach((q, idx) => {
                const questionId = `q_${idx}`;
                const userAnswerId = answerMap.get(questionId);
                
                if (userAnswerId) {
                    const match = userAnswerId.match(/^a_(\d+)_(\d+)$/);
                    if (match) {
                        const answerIndex = parseInt(match[2]);
                        const userAnswer = q.answers[answerIndex];
                        if (userAnswer && userAnswer.isCorrect) {
                            score++;
                        }
                    }
                }
            });

            correctCount = score;
            totalQuestions = quiz.questions.length;
            finalScore = Math.round((score / quiz.questions.length) * 100);
            console.log(`[submitQuiz] Tính từ answers: ${correctCount}/${totalQuestions} = ${finalScore}%`);
        }

        console.log(`[submitQuiz] Final data - so_cau_dung: ${correctCount}, tong_so_cau: ${totalQuestions}, diem_so: ${finalScore}%`);
        console.log(`[submitQuiz] ma_nguoi_dung: ${ma_nguoi_dung}, ma_chu_de: ${ma_chu_de || quiz.ma_chu_de}`);

        // Lưu lịch sử với dữ liệu chính xác
        const historyData = {
            ma_nguoi_dung,
            ma_quiz,
            ma_chu_de: ma_chu_de || quiz.ma_chu_de,
            diem_so: finalScore,
            so_cau_dung: correctCount,
            tong_so_cau: totalQuestions
        };
        
        console.log('[submitQuiz] Saving history:', JSON.stringify(historyData, null, 2));
        const savedHistory = await LichSuLamBai.create(historyData);
        console.log(`[submitQuiz] ✅ Lịch sử đã lưu: _id=${savedHistory._id}`);

        // Cập nhật điểm tích lũy
        const updateResult = await NguoiDung.updateOne(
            { _id: ma_nguoi_dung },
            { $inc: { diem_tich_luy: finalScore } }
        );
        
        console.log(`[submitQuiz] Cập nhật XP: matched=${updateResult.matchedCount}, modified=${updateResult.modifiedCount}, diem_tich_luy += ${finalScore}`);
        
        // Kiểm tra xem có cập nhật thành công không
        if (updateResult.matchedCount === 0) {
            console.error(`[submitQuiz] ⚠️ Không tìm thấy user với _id: ${ma_nguoi_dung}`);
        } else if (updateResult.modifiedCount === 0) {
            console.warn(`[submitQuiz] ⚠️ User được tìm thấy nhưng không được cập nhật (có thể giá trị không đổi)`);
        } else {
            // Lấy user mới để xác nhận
            const updatedUser = await NguoiDung.findById(ma_nguoi_dung);
            console.log(`[submitQuiz] ✅ XP mới của user: ${updatedUser.diem_tich_luy}`);
        }

        // Cập nhật streak (mỗi ngày học/làm quiz sẽ tăng streak)
        await updateStreak(ma_nguoi_dung);

        res.json(createApiResponse(true, 'Nộp bài thành công!', {
            scorePercent: finalScore,
            correct: correctCount,
            total: totalQuestions
        }));
    } catch (error) {
        console.error('Lỗi submitQuiz:', error);
        res.status(500).json(createApiResponse(false, 'Lỗi server: ' + error.message));
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

// ======= Lấy quiz theo ID (admin) =======
const getQuizById = async (req, res) => {
    try {
        const { id } = req.params;
        if (!mongoose.Types.ObjectId.isValid(id)) {
            return res.status(400).json(createApiResponse(false, 'ID quiz không hợp lệ.'));
        }

        const quiz = await BaiQuiz.findById(id);
        if (!quiz) {
            return res.status(404).json(createApiResponse(false, 'Không tìm thấy quiz.'));
        }

        res.status(200).json(quiz);
    } catch (error) {
        console.error('Lỗi khi lấy quiz:', error);
        res.status(500).json(createApiResponse(false, 'Lỗi server khi lấy quiz'));
    }
};

// ======= Routes =======
// Routes cụ thể phải đặt trước routes generic
router.get('/by-topic/:ma_chu_de', getQuizByTopic);
router.post('/submit', submitQuiz);
router.get('/history/:ma_nguoi_dung', getHistoryByUser);
router.post('/', createQuiz); // POST /api/quizzes
router.post('/create', createQuiz); // POST /api/quizzes/create (backward compatibility)
router.get('/:id', getQuizById); // GET /api/quizzes/:id - Phải đặt sau các route cụ thể
router.delete('/:id', deleteQuiz);

module.exports = router;

