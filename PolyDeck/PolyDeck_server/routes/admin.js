const express = require('express');
const router = express.Router();
const ThongBao = require('../models/ThongBao'); 
const NguoiDung = require('../models/NguoiDung');
const ChuDe = require('../models/ChuDe');
const TuVung = require('../models/TuVung');

//lay so lieu db
router.get('/stats', async (req, res) => {
    try {
        const tongNguoiDung = await NguoiDung.countDocuments();
        const tongBoTu = await ChuDe.countDocuments();
        const tongTuVung = await TuVung.countDocuments();

        const oneDayAgo = new Date(Date.now() - 24 * 60 * 60 * 1000);
        const nguoiDungMoi = await NguoiDung.countDocuments({ ngay_tao: { $gte: oneDayAgo } });

        res.json({
            tongNguoiDung,
            tongBoTu,
            nguoiHoatDong: nguoiDungMoi,
            tongTuVung
        });
    } catch (err) {
        console.error('Lỗi khi lấy thống kê:', err);
        res.status(500).json({ message: 'Lỗi server khi lấy thống kê' });
    }
});

router.post('/thong-bao', async (req, res) => {
    const { tieu_de, noi_dung } = req.body;

    if (!tieu_de || !noi_dung) {
        return res.status(400).json({ message: 'Tiêu đề và nội dung là bắt buộc.' });
    }

    const thongBao = new ThongBao({
        ma_thong_bao: `TB_${Date.now()}`, 
        tieu_de: tieu_de,
        noi_dung: noi_dung,
    });

    try {
        const newThongBao = await thongBao.save();
        res.status(201).json(newThongBao);
    } catch (err) {
        console.error("Lỗi khi lưu thông báo:", err);
        res.status(400).json({ message: err.message });
    }
});

module.exports = router;