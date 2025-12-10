// Routes cho Từ vựng (CRUD từ vựng, lấy theo chủ đề, tìm kiếm)
const express = require('express');
const router = express.Router();
const TuVung = require('../models/TuVung');
const ChuDe = require('../models/ChuDe');
const mongoose = require('mongoose');

// GET: Lấy tất cả từ vựng
router.get('/', async (req, res) => {
    try {
        const vocabList = await TuVung.find().populate('ma_chu_de', 'ten_chu_de');
        res.json(vocabList);
    } catch (err) {
        console.error('Lỗi khi lấy danh sách từ vựng:', err);
        res.status(500).json({ message: 'Lỗi server' });
    }
});

// GET: Tìm kiếm từ vựng (phải đặt trước route /:id để tránh conflict)
router.get('/search/:keyword', async (req, res) => {
    try {
        const keyword = req.params.keyword;
        if (!keyword) {
            return res.status(400).json({ message: 'Vui lòng cung cấp từ khóa tìm kiếm.' });
        }

        const vocabList = await TuVung.find({
            $or: [
                { tu_tieng_anh: { $regex: keyword, $options: 'i' } },
                { nghia_tieng_viet: { $regex: keyword, $options: 'i' } }
            ]
        }).populate('ma_chu_de', 'ten_chu_de');

        res.json(vocabList);
    } catch (err) {
        console.error('Lỗi khi tìm kiếm từ vựng:', err);
        res.status(500).json({ message: err.message });
    }
});

// GET: Lấy từ vựng theo chủ đề (phải đặt trước route /:id)
router.get('/chude/:chuDeId', async (req, res) => {
    try {
        const chuDe = await ChuDe.findById(req.params.chuDeId);
        if (!chuDe) {
            return res.status(404).json({ message: 'Không tìm thấy chủ đề' });
        }
        const vocabList = await TuVung.find({ ma_chu_de: chuDe._id });
        res.json(vocabList);
    } catch (err) {
        console.error('Lỗi khi lấy danh sách từ vựng theo chủ đề:', err);
        res.status(500).json({ message: 'Lỗi server' });
    }
});

// GET: Lấy từ vựng theo ID (phải đặt cuối cùng)
router.get('/:id', async (req, res) => {
    try {
        const tuVung = await TuVung.findById(req.params.id).populate('ma_chu_de', 'ten_chu_de');
        if (!tuVung) {
            return res.status(404).json({ message: 'Không tìm thấy từ vựng' });
        }
        res.json(tuVung);
    } catch (err) {
        console.error('Lỗi khi lấy từ vựng:', err);
        res.status(500).json({ message: 'Lỗi server' });
    }
});

// POST: Tạo từ vựng mới
router.post('/', async (req, res) => {
    try {
        const { ma_chu_de, tu_tieng_anh, nghia_tieng_viet, phien_am, cau_vi_du } = req.body;
        
        if (!ma_chu_de || !tu_tieng_anh || !nghia_tieng_viet) {
            return res.status(400).json({ message: 'Thiếu thông tin bắt buộc (ma_chu_de, tu_tieng_anh, nghia_tieng_viet)' });
        }

        // Kiểm tra chủ đề có tồn tại không
        const chuDe = await ChuDe.findById(ma_chu_de);
        if (!chuDe) {
            return res.status(404).json({ message: 'Không tìm thấy chủ đề' });
        }

        // Kiểm tra từ vựng đã tồn tại trong chủ đề này chưa
        const existingVocab = await TuVung.findOne({
            ma_chu_de: chuDe._id,
            tu_tieng_anh: tu_tieng_anh.trim()
        });
        if (existingVocab) {
            return res.status(409).json({ message: 'Từ vựng này đã tồn tại trong chủ đề' });
        }

        const tuVung = new TuVung({
            ma_chu_de: chuDe._id,
            tu_tieng_anh: tu_tieng_anh.trim(),
            phien_am: phien_am ? phien_am.trim() : null,
            nghia_tieng_viet: nghia_tieng_viet.trim(),
            cau_vi_du: cau_vi_du ? cau_vi_du.trim() : null,
        });

        const newTuVung = await tuVung.save();
        
        // Cập nhật số lượng từ trong chủ đề
        await ChuDe.updateOne({ _id: chuDe._id }, { $inc: { so_luong_tu: 1 } });

        res.status(201).json(newTuVung);
    } catch (err) {
        console.error('Lỗi khi tạo từ vựng:', err);
        res.status(500).json({ message: err.message });
    }
});

// PUT: Cập nhật từ vựng
router.put('/:id', async (req, res) => {
    try {
        const { tu_tieng_anh, nghia_tieng_viet, phien_am, cau_vi_du } = req.body;
        
        if (!tu_tieng_anh || !nghia_tieng_viet) {
            return res.status(400).json({ message: 'Từ tiếng Anh và nghĩa tiếng Việt là bắt buộc.' });
        }

        const tuVung = await TuVung.findById(req.params.id);
        if (!tuVung) {
            return res.status(404).json({ message: 'Không tìm thấy từ vựng' });
        }

        // Kiểm tra trùng lặp nếu thay đổi từ tiếng Anh
        if (tu_tieng_anh !== tuVung.tu_tieng_anh) {
            const existingVocab = await TuVung.findOne({
                ma_chu_de: tuVung.ma_chu_de,
                tu_tieng_anh: tu_tieng_anh.trim(),
                _id: { $ne: req.params.id }
            });
            if (existingVocab) {
                return res.status(409).json({ message: 'Từ vựng này đã tồn tại trong chủ đề' });
            }
        }

        // Cập nhật từ vựng
        tuVung.tu_tieng_anh = tu_tieng_anh.trim();
        tuVung.nghia_tieng_viet = nghia_tieng_viet.trim();
        tuVung.phien_am = phien_am ? phien_am.trim() : null;
        tuVung.cau_vi_du = cau_vi_du ? cau_vi_du.trim() : null;

        const updatedTuVung = await tuVung.save();
        res.json(updatedTuVung);
    } catch (err) {
        console.error('Lỗi khi cập nhật từ vựng:', err);
        res.status(500).json({ message: err.message });
    }
});

// DELETE: Xóa từ vựng
router.delete('/:id', async (req, res) => {
    try {
        const tuVung = await TuVung.findById(req.params.id);
        if (!tuVung) {
            return res.status(404).json({ message: 'Không tìm thấy từ vựng' });
        }

        const chuDeId = tuVung.ma_chu_de;
        await TuVung.findByIdAndDelete(req.params.id);
        
        // Cập nhật số lượng từ trong chủ đề
        await ChuDe.updateOne({ _id: chuDeId }, { $inc: { so_luong_tu: -1 } });

        res.json({ message: 'Đã xóa từ vựng thành công' });
    } catch (err) {
        console.error('Lỗi khi xóa từ vựng:', err);
        res.status(500).json({ message: err.message });
    }
});

module.exports = router;
