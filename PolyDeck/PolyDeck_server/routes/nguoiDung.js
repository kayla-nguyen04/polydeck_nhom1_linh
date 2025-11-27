const express = require('express');
const router = express.Router();
const NguoiDung = require('../models/NguoiDung'); 
const { Types } = require('mongoose');
const TuVung = require('../models/TuVung');

const resolveUserFilter = (identifier = '') => {
    if (Types.ObjectId.isValid(identifier)) {
        return { _id: identifier };
    }
    return { ma_nguoi_dung: identifier };
};
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

// Thêm từ vựng yêu thích
router.post('/:id/favorites', async (req, res) => {
    try {
        const { tu_vung_id, ma_tu_vung } = req.body || {};
        const fav = tu_vung_id || ma_tu_vung;
        if (!fav) return res.status(400).json({ message: 'Thiếu tu_vung_id hoặc ma_tu_vung' });
        const user = await NguoiDung.findOneAndUpdate(
            resolveUserFilter(req.params.id),
            { $addToSet: { tu_vung_yeu_thich: String(fav) } },
            { new: true }
        );
        if (!user) return res.status(404).json({ message: 'Không tìm thấy người dùng' });
        res.json({ success: true, tu_vung_yeu_thich: user.tu_vung_yeu_thich });
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// Xóa khỏi yêu thích
router.delete('/:id/favorites/:fav', async (req, res) => {
    try {
        const fav = req.params.fav;
        const user = await NguoiDung.findOneAndUpdate(
            resolveUserFilter(req.params.id),
            { $pull: { tu_vung_yeu_thich: String(fav) } },
            { new: true }
        );
        if (!user) return res.status(404).json({ message: 'Không tìm thấy người dùng' });
        res.json({ success: true, tu_vung_yeu_thich: user.tu_vung_yeu_thich });
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// Lấy danh sách từ vựng yêu thích (trả về chi tiết từ)
router.get('/:id/favorites', async (req, res) => {
    try {
        const user = await NguoiDung.findOne(resolveUserFilter(req.params.id));
        if (!user) return res.status(404).json({ success: false, message: 'Không tìm thấy người dùng' });
        const ids = (user.tu_vung_yeu_thich || []).map(String);
        const objIds = ids
            .filter(id => Types.ObjectId.isValid(id))
            .map(id => new Types.ObjectId(id));
        const list = await TuVung.find({ _id: { $in: objIds } });
        res.json({ success: true, data: list });
    } catch (err) {
        res.status(500).json({ success: false, message: err.message });
    }
});
module.exports = router;