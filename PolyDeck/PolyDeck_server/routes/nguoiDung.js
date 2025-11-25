const express = require('express');
const router = express.Router();
const NguoiDung = require('../models/NguoiDung'); 
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
        const user = await NguoiDung.findById(req.params.id, '-mat_khau'); 
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
        const updatedUser = await NguoiDung.findByIdAndUpdate(req.params.id, updatedData, { new: true });
        if (!updatedUser) return res.status(404).json({ message: 'Không tìm thấy người dùng' });
        res.json(updatedUser);
    } catch (err) {
        res.status(400).json({ message: err.message });
    }
});
//khoa - mo khoa
router.put('/:id/block', async (req, res) => {
    try {
        const user = await NguoiDung.findById(req.params.id);
        if (!user) return res.status(404).json({ message: 'Không tìm thấy người dùng' });

        user.trang_thai = user.trang_thai === 'active' ? 'banned' : 'active';
        await user.save();
        res.json({ message: `Đã cập nhật trạng thái thành ${user.trang_thai}` });
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});
module.exports = router;