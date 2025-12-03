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

        // Tính người dùng hoạt động: các tài khoản không bị khóa (trạng thái 'active')
        const nguoiHoatDong = await NguoiDung.countDocuments({ trang_thai: 'active' });

        // Tính tỷ lệ tăng trưởng (so với tháng trước)
        const oneMonthAgo = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000);
        const twoMonthsAgo = new Date(Date.now() - 60 * 24 * 60 * 60 * 1000);
        
        const usersLastMonth = await NguoiDung.countDocuments({ createdAt: { $gte: oneMonthAgo, $lt: new Date() } });
        const usersPreviousMonth = await NguoiDung.countDocuments({ createdAt: { $gte: twoMonthsAgo, $lt: oneMonthAgo } });
        const tyLeNguoiDung = usersPreviousMonth > 0 
            ? `+${((usersLastMonth - usersPreviousMonth) / usersPreviousMonth * 100).toFixed(1)}%`
            : usersLastMonth > 0 ? '+100%' : '0%';

        const decksLastMonth = await ChuDe.countDocuments({ createdAt: { $gte: oneMonthAgo, $lt: new Date() } });
        const decksPreviousMonth = await ChuDe.countDocuments({ createdAt: { $gte: twoMonthsAgo, $lt: oneMonthAgo } });
        const tyLeBoTu = decksPreviousMonth > 0
            ? `+${((decksLastMonth - decksPreviousMonth) / decksPreviousMonth * 100).toFixed(1)}%`
            : decksLastMonth > 0 ? '+100%' : '0%';

        // Tính tỷ lệ hoạt động: so sánh số người active tháng này với tháng trước
        const activeLastMonth = await NguoiDung.countDocuments({ 
            trang_thai: 'active',
            createdAt: { $gte: oneMonthAgo, $lt: new Date() }
        });
        const activePreviousMonth = await NguoiDung.countDocuments({ 
            trang_thai: 'active',
            createdAt: { $gte: twoMonthsAgo, $lt: oneMonthAgo }
        });
        const tyLeHoatDong = activePreviousMonth > 0
            ? `+${((activeLastMonth - activePreviousMonth) / activePreviousMonth * 100).toFixed(1)}%`
            : activeLastMonth > 0 ? '+100%' : '0%';

        const wordsLastMonth = await TuVung.countDocuments({ createdAt: { $gte: oneMonthAgo, $lt: new Date() } });
        const wordsPreviousMonth = await TuVung.countDocuments({ createdAt: { $gte: twoMonthsAgo, $lt: oneMonthAgo } });
        const tyLeTuVung = wordsPreviousMonth > 0
            ? `+${((wordsLastMonth - wordsPreviousMonth) / wordsPreviousMonth * 100).toFixed(1)}%`
            : wordsLastMonth > 0 ? '+100%' : '0%';

        res.json({
            tongNguoiDung,
            tongBoTu,
            nguoiHoatDong,
            tongTuVung,
            tyLeNguoiDung,
            tyLeBoTu,
            tyLeHoatDong,
            tyLeTuVung
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