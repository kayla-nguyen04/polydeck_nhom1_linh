const express = require('express');
const router = express.Router();
const multer = require('multer');
const path = require('path');
const { Types } = require('mongoose');
const NguoiDung = require('../models/NguoiDung');
const TuVung = require('../models/TuVung'); // dùng cho tính năng từ vựng yêu thích

const resolveUserFilter = (identifier = '') => {
    if (Types.ObjectId.isValid(identifier)) {
        return { _id: identifier };
    }
    return { ma_nguoi_dung: identifier };
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
        const user = await NguoiDung.findOne(resolveUserFilter(req.params.id));
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy người dùng',
                data: null
            });
        }

        const { tu_vung_id } = req.body;
        if (!tu_vung_id) {
            return res.status(400).json({
                success: false,
                message: 'Thiếu tu_vung_id',
                data: null
            });
        }

        // Kiểm tra từ vựng có tồn tại không (tránh lưu id rỗng)
        // Android đang gửi MongoDB _id, nên ưu tiên tìm theo _id
        let vocab = null;
        try {
            vocab = await TuVung.findById(tu_vung_id);
        } catch (e) {
            vocab = null;
        }

        // Fallback: nếu không phải ObjectId hợp lệ, thử tìm theo ma_tu_vung
        if (!vocab) {
            vocab = await TuVung.findOne({ ma_tu_vung: tu_vung_id });
        }

        if (!vocab) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy từ vựng',
                data: null
            });
        }

        if (!Array.isArray(user.tu_vung_yeu_thich)) {
            user.tu_vung_yeu_thich = [];
        }

        const vocabIdStr = vocab._id.toString();

        // Nếu đã tồn tại thì không thêm trùng, nhưng vẫn trả về success
        if (!user.tu_vung_yeu_thich.map(id => id.toString()).includes(vocabIdStr)) {
            user.tu_vung_yeu_thich.push(vocabIdStr);
            await user.save();
        }

        return res.json({
            success: true,
            message: 'Đã thêm vào danh sách yêu thích',
            data: null
        });
    } catch (err) {
        console.error('Lỗi khi thêm từ vựng yêu thích:', err);
        return res.status(500).json({
            success: false,
            message: 'Lỗi server',
            data: null
        });
    }
});

// DELETE: Xoá một từ vựng khỏi danh sách yêu thích
// URL: /api/users/:id/favorites/:fav  (fav = ma_tu_vung)
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

module.exports = router;