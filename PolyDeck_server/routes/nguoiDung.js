const express = require('express');
const router = express.Router();
const multer = require('multer');
const path = require('path');
const mongoose = require('mongoose');
const NguoiDung = require('../models/NguoiDung');
const TuVung = require('../models/TuVung'); // dùng cho tính năng từ vựng yêu thích

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
        return newStreak;
    } catch (error) {
        console.error('❌ Error updating streak:', error);
        throw error;
    }
};

const resolveUserFilter = (identifier = '') => {
    return { _id: identifier };
};

const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        cb(null, 'public/uploads/');
    },
    filename: function (req, file, cb) {
        cb(null, 'avatar-' + Date.now() + path.extname(file.originalname));
    }
});
const upload = multer({ storage: storage }); 
// GET: Lấy danh sách tất cả người dùng
router.get('/', async (req, res) => {
    try {
        const users = await NguoiDung.find();
        res.json(users);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// GET: Tìm kiếm người dùng theo tên hoặc email
router.get('/search', async (req, res) => {
    try {
        const query = req.query.q;
        if (!query) {
            return res.status(400).json({ message: "Vui lòng cung cấp từ khóa tìm kiếm." });
        }
        const users = await NguoiDung.find({
            $or: [
                { ho_ten: { $regex: query, $options: 'i' } }, // 'i' để không phân biệt chữ hoa/thường
                { email: { $regex: query, $options: 'i' } }
            ]
        });
        res.json(users);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// GET: Lọc người dùng theo khoảng thời gian (theo ngày tham gia)
router.get('/filter-by-date', async (req, res) => {
    try {
        const { startDate, endDate } = req.query;
        
        if (!startDate || !endDate) {
            return res.status(400).json({ message: 'Thiếu startDate hoặc endDate' });
        }
        
        const start = new Date(startDate);
        const end = new Date(endDate);
        end.setHours(23, 59, 59, 999); // Set to end of day
        
        // Find users created in date range
        const users = await NguoiDung.find({
            createdAt: { $gte: start, $lte: end }
        }).sort({ createdAt: -1 });
        
        res.json(users);
    } catch (err) {
        console.error('Lỗi khi lọc người dùng theo ngày:', err);
        res.status(500).json({ message: err.message });
    }
});
// GET: Lấy chi tiết một người dùng theo ID
router.get('/:id', async (req, res) => {
    try {
        const user = await NguoiDung.findOne(resolveUserFilter(req.params.id), '-mat_khau'); 
        if (!user) {
            return res.status(404).json({ message: 'Không tìm thấy người dùng' });
        }
        res.json(user);
    } catch (err) {
        console.error('Lỗi khi lấy chi tiết người dùng:', err);
        res.status(500).json({ message: 'Lỗi máy chủ' });
    }
});
//cap nhat
router.put('/:id', async (req, res) => {
    try {
        const { ho_ten, email, cap_do, diem_tich_luy } = req.body;
        const updatedData = { ho_ten, email, cap_do, diem_tich_luy };
        const updatedUser = await NguoiDung.findOneAndUpdate(resolveUserFilter(req.params.id), updatedData, { new: true });
        if (!updatedUser) return res.status(404).json({ message: 'Không tìm thấy người dùng' });
        res.json(updatedUser);
    } catch (err) {
        res.status(400).json({ message: err.message });
    }
});
//khoa - mo khoa
router.put('/:id/block', async (req, res) => {
    try {
        const user = await NguoiDung.findOne(resolveUserFilter(req.params.id));
        if (!user) return res.status(404).json({ message: 'Không tìm thấy người dùng' });

        user.trang_thai = user.trang_thai === 'active' ? 'banned' : 'active';
        await user.save();
        res.json({ message: `Đã cập nhật trạng thái thành ${user.trang_thai}` });
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// Upload avatar
router.post('/:id/upload-avatar', upload.single('file'), async (req, res) => {
    try {
        const user = await NguoiDung.findOne(resolveUserFilter(req.params.id));
        if (!user) {
            return res.status(404).json({ message: 'Không tìm thấy người dùng' });
        }

        if (!req.file) {
            return res.status(400).json({ message: 'Không có file được upload' });
        }

        const fileUrl = `${req.protocol}://${req.get('host')}/uploads/${req.file.filename}`;
        user.link_anh_dai_dien = fileUrl;
        await user.save();

        res.json(user);
    } catch (err) {
        console.error('Error uploading avatar:', err);
        res.status(500).json({ message: err.message });
    }
});

// ================== FAVORITES (TỪ VỰNG YÊU THÍCH) ==================

// GET: Lấy danh sách từ vựng yêu thích của một người dùng
// Trả về đúng format ApiResponse<T> mà Android đang dùng
router.get('/:id/favorites', async (req, res) => {
    try {
        const user = await NguoiDung.findOne(resolveUserFilter(req.params.id));
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy người dùng',
                data: null
            });
        }

        const favoriteIds = (user.tu_vung_yeu_thich || []).map(id => id.toString());
        if (!favoriteIds.length) {
            return res.json({
                success: true,
                message: 'Người dùng chưa có từ vựng yêu thích',
                data: []
            });
        }

        // Vì Android đang dùng field "_id" làm id, ta lưu và truy vấn theo _id
        const vocabList = await TuVung.find({ _id: { $in: favoriteIds } });
        return res.json({
            success: true,
            message: 'Lấy danh sách từ vựng yêu thích thành công',
            data: vocabList
        });
    } catch (err) {
        console.error('Lỗi khi lấy danh sách từ vựng yêu thích:', err);
        return res.status(500).json({
            success: false,
            message: 'Lỗi server',
            data: null
        });
    }
});

// POST: Thêm một từ vựng vào danh sách yêu thích của người dùng
// Body từ Android: { "tu_vung_id": "TV_..." }
router.post('/:id/favorites', async (req, res) => {
    try {
        const userId = req.params.id;
        console.log(`[POST /favorites] userId: ${userId}, body:`, req.body);
        
        if (!mongoose.Types.ObjectId.isValid(userId)) {
            return res.status(400).json({
                success: false,
                message: 'ID người dùng không hợp lệ',
                data: null
            });
        }

        const user = await NguoiDung.findById(userId);
        if (!user) {
            console.log(`[POST /favorites] User not found: ${userId}`);
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy người dùng',
                data: null
            });
        }

        const { tu_vung_id } = req.body;
        if (!tu_vung_id) {
            console.log(`[POST /favorites] Missing tu_vung_id`);
            return res.status(400).json({
                success: false,
                message: 'Thiếu tu_vung_id',
                data: null
            });
        }

        // Kiểm tra tu_vung_id có phải ObjectId hợp lệ không
        if (!mongoose.Types.ObjectId.isValid(tu_vung_id)) {
            console.log(`[POST /favorites] Invalid tu_vung_id: ${tu_vung_id}`);
            return res.status(400).json({
                success: false,
                message: 'ID từ vựng không hợp lệ',
                data: null
            });
        }

        // Kiểm tra từ vựng có tồn tại không
        let vocab = null;
        try {
            vocab = await TuVung.findById(tu_vung_id);
        } catch (e) {
            console.error(`[POST /favorites] Error finding vocab:`, e);
            vocab = null;
        }

        if (!vocab) {
            console.log(`[POST /favorites] Vocab not found: ${tu_vung_id}`);
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy từ vựng',
                data: null
            });
        }

        // Khởi tạo array nếu chưa có
        if (!Array.isArray(user.tu_vung_yeu_thich)) {
            user.tu_vung_yeu_thich = [];
        }

        // Đảm bảo cap_do hợp lệ (min: 1)
        if (!user.cap_do || user.cap_do < 1) {
            user.cap_do = 1;
        }

        const vocabIdStr = vocab._id.toString();

        // Kiểm tra xem đã có trong danh sách chưa
        const existingIndex = user.tu_vung_yeu_thich.findIndex(id => id.toString() === vocabIdStr);
        
        if (existingIndex === -1) {
            // Chưa có, thêm vào
            user.tu_vung_yeu_thich.push(vocabIdStr);
            try {
                await user.save();
                console.log(`[POST /favorites] ✅ Added favorite - userId: ${userId}, vocabId: ${vocabIdStr}`);
            } catch (saveError) {
                console.error(`[POST /favorites] ❌ Error saving user:`, saveError);
                return res.status(500).json({
                    success: false,
                    message: 'Lỗi khi lưu dữ liệu: ' + saveError.message,
                    data: null
                });
            }
        } else {
            console.log(`[POST /favorites] Already in favorites - userId: ${userId}, vocabId: ${vocabIdStr}`);
        }

        return res.json({
            success: true,
            message: 'Đã thêm vào danh sách yêu thích',
            data: null
        });
    } catch (err) {
        console.error('❌ [POST /favorites] Lỗi khi thêm từ vựng yêu thích:', err);
        console.error('Stack trace:', err.stack);
        return res.status(500).json({
            success: false,
            message: 'Lỗi server: ' + err.message,
            data: null
        });
    }
});

// DELETE: Xoá một từ vựng khỏi danh sách yêu thích
// URL: /api/users/:id/favorites/:fav  (fav = _id của từ vựng)
router.delete('/:id/favorites/:fav', async (req, res) => {
    try {
        const user = await NguoiDung.findOne(resolveUserFilter(req.params.id));
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy người dùng',
                data: null
            });
        }

        const favId = req.params.fav;
        if (!favId) {
            return res.status(400).json({
                success: false,
                message: 'Thiếu mã từ vựng yêu thích',
                data: null
            });
        }

        if (!Array.isArray(user.tu_vung_yeu_thich)) {
            user.tu_vung_yeu_thich = [];
        }

        const beforeLength = user.tu_vung_yeu_thich.length;
        user.tu_vung_yeu_thich = user.tu_vung_yeu_thich
            .map(id => id.toString())
            .filter(id => id !== favId);

        if (user.tu_vung_yeu_thich.length === beforeLength) {
            // Không tìm thấy để xoá nhưng vẫn coi là success (idempotent)
            return res.json({
                success: true,
                message: 'Từ vựng không còn trong danh sách yêu thích',
                data: null
            });
        }

        await user.save();
        return res.json({
            success: true,
            message: 'Đã xoá khỏi danh sách yêu thích',
            data: null
        });
    } catch (err) {
        console.error('Lỗi khi xoá từ vựng yêu thích:', err);
        return res.status(500).json({
            success: false,
            message: 'Lỗi server',
            data: null
        });
    }
});

// POST: Cập nhật streak khi học flashcard
// URL: /api/users/:id/update-streak
router.post('/:id/update-streak', async (req, res) => {
    try {
        const userId = req.params.id;
        if (!userId || !mongoose.Types.ObjectId.isValid(userId)) {
            return res.status(400).json({
                success: false,
                message: 'ID người dùng không hợp lệ',
                data: null
            });
        }

        const newStreak = await updateStreak(userId);
        const user = await NguoiDung.findById(userId);
        
        return res.json({
            success: true,
            message: 'Cập nhật streak thành công',
            data: {
                chuoi_ngay_hoc: user.chuoi_ngay_hoc,
                ngay_hoc_cuoi: user.ngay_hoc_cuoi
            }
        });
    } catch (err) {
        console.error('Lỗi khi cập nhật streak:', err);
        return res.status(500).json({
            success: false,
            message: 'Lỗi server',
            data: null
        });
    }
});

// POST: Cộng XP khi học flashcard
// URL: /api/users/:id/add-xp
router.post('/:id/add-xp', async (req, res) => {
    try {
        const userId = req.params.id;
        const { xpAmount } = req.body; // Số XP muốn cộng

        if (!mongoose.Types.ObjectId.isValid(userId)) {
            return res.status(400).json({
                success: false,
                message: 'ID người dùng không hợp lệ',
                data: null
            });
        }

        if (!xpAmount || typeof xpAmount !== 'number' || xpAmount <= 0) {
            return res.status(400).json({
                success: false,
                message: 'Số XP không hợp lệ',
                data: null
            });
        }

        // Cộng XP vào diem_tich_luy
        const updateResult = await NguoiDung.updateOne(
            { _id: userId },
            { $inc: { diem_tich_luy: xpAmount } }
        );

        if (updateResult.matchedCount === 0) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy người dùng',
                data: null
            });
        }

        // Lấy user mới để trả về diem_tich_luy mới
        const updatedUser = await NguoiDung.findById(userId);
        
        console.log(`✅ Added ${xpAmount} XP to user ${userId}. New total: ${updatedUser.diem_tich_luy}`);

        return res.json({
            success: true,
            message: 'Cộng XP thành công',
            data: {
                diem_tich_luy: updatedUser.diem_tich_luy
            }
        });
    } catch (err) {
        console.error('Lỗi khi cộng XP:', err);
        return res.status(500).json({
            success: false,
            message: 'Lỗi server',
            data: null
        });
    }
});

module.exports = router;