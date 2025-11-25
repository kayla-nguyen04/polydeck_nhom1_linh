const express = require('express');
const router = express.Router();
const multer = require('multer');
const path = require('path');
const { Types } = require('mongoose');
const ChuDe = require('../models/ChuDe'); 
const TuVung = require('../models/TuVung'); 

const storage = multer.diskStorage({
    destination: function (req, file, cb) { cb(null, 'public/uploads/') },
    filename: function (req, file, cb) { cb(null, 'icon-' + Date.now() + path.extname(file.originalname)) }
});
const upload = multer({ storage: storage });

const resolveChuDeFilter = (identifier = '') => {
    if (Types.ObjectId.isValid(identifier)) {
        return { _id: identifier };
    }
    return { ma_chu_de: identifier };
};


// GET: Lấy danh sách tất cả chủ đề
router.get('/', async (req, res) => {
    console.log('GET /api/chude - query from', req.headers['x-forwarded-for'] || req.ip || req.socket.remoteAddress);
    try {
        const chuDeList = await ChuDe.find();
        res.json(chuDeList);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// GET: Tìm kiếm chủ đề theo tên
router.get('/search', async (req, res) => {
    try {
        const query = req.query.q;
        if (!query) {
            return res.status(400).json({ message: "Vui lòng cung cấp từ khóa tìm kiếm." });
        }
        const chuDeList = await ChuDe.find({
            ten_chu_de: { $regex: query, $options: 'i' }
        });
        res.json(chuDeList);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// GET: Lấy chi tiết một chủ đề theo ID
router.get('/:id', async (req, res) => {
    console.log(`GET /api/chude/${req.params.id} - from`, req.headers['x-forwarded-for'] || req.ip || req.socket.remoteAddress);
    try {
        const chuDe = await ChuDe.findOne(resolveChuDeFilter(req.params.id));
        if (!chuDe) return res.status(404).json({ message: 'Không tìm thấy chủ đề' });
        res.json(chuDe);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

router.post('/chude_with_image', upload.single('file'), async (req, res) => {
    console.log('POST /api/chude/chude_with_image - headers:', { host: req.get('host'), ip: req.headers['x-forwarded-for'] || req.ip || req.socket.remoteAddress });
    try {
        if (!req.file) return res.status(400).json({ message: 'Vui lòng cung cấp tệp ảnh.' });
        
        const { ten_chu_de } = req.body;
        if (!ten_chu_de) return res.status(400).json({ message: 'Tên chủ đề là bắt buộc.' });
        
        const fileUrl = `${req.protocol}://${req.get('host')}/uploads/${req.file.filename}`;
        
        const newChuDe = new ChuDe({
            ma_chu_de: `CD_${Date.now()}`,
            ten_chu_de: ten_chu_de,
            link_anh_icon: fileUrl,
        });

        const savedChuDe = await newChuDe.save();
        res.status(201).json(savedChuDe);
    } catch (err) {
        console.error('Error creating ChuDe:', err);
        res.status(500).json({ message: 'Lỗi server khi tạo chủ đề.' });
    }
});

// PUT: Cập nhật chủ đề
router.put('/:id', async (req, res) => {
    try {
        const updatedChuDe = await ChuDe.findOneAndUpdate(resolveChuDeFilter(req.params.id), req.body, { new: true });
        if (!updatedChuDe) return res.status(404).json({ message: 'Không tìm thấy chủ đề' });
        res.json(updatedChuDe);
    } catch (err) {
        res.status(400).json({ message: err.message });
    }
});

// DELETE: Xóa chủ đề
router.delete('/:id', async (req, res) => {
    try {
        const deletedChuDe = await ChuDe.findOneAndDelete(resolveChuDeFilter(req.params.id));
        if (!deletedChuDe) return res.status(404).json({ message: 'Không tìm thấy chủ đề' });
        res.json({ message: 'Đã xóa chủ đề' });
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// POST: Thêm từ vựng mới vào một chủ đề (hỗ trợ upload ảnh/audio tùy chọn)
router.post('/:chuDeId/them-tu-vung', upload.single('file'), async (req, res) => {
    try {
        const chuDe = await ChuDe.findOne(resolveChuDeFilter(req.params.chuDeId));
        if (!chuDe) {
            return res.status(404).json({ message: 'Không tìm thấy chủ đề' });
        }

        const { tu_tieng_anh, nghia_tieng_viet, phien_am } = req.body;
        if (!tu_tieng_anh || !nghia_tieng_viet) {
            return res.status(400).json({ message: 'Từ tiếng Anh và nghĩa tiếng Việt là bắt buộc.' });
        }

        let fileUrl = null;
        if (req.file) {
            fileUrl = `${req.protocol}://${req.get('host')}/uploads/${req.file.filename}`;
        }

        const maChuDe = chuDe.ma_chu_de || chuDe._id.toString();

        const tuVung = new TuVung({
            ma_tu_vung: `TV_${Date.now()}`,
            ma_chu_de: maChuDe,
            tu_tieng_anh,
            phien_am,
            nghia_tieng_viet,
            link_anh: fileUrl,
        });

        const newTuVung = await tuVung.save();
        await ChuDe.updateOne({ _id: chuDe._id }, { $inc: { so_luong_tu: 1 } });

        res.status(201).json(newTuVung);
    } catch (err) {
        console.error('Error adding TuVung:', err);
        res.status(500).json({ message: err.message });
    }
});

router.get('/:chuDeId/tuvung', async (req, res) => {
    try {
        // Chấp nhận cả ObjectId lẫn ma_chu_de
        const chuDe = await ChuDe.findOne(resolveChuDeFilter(req.params.chuDeId));
        if (!chuDe) {
            return res.status(404).json({ message: 'Không tìm thấy chủ đề' });
        }
        const maChuDe = chuDe.ma_chu_de || req.params.chuDeId;
        const vocabList = await TuVung.find({ ma_chu_de: maChuDe });
        res.json(vocabList);
    } catch (err) {
        console.error('Lỗi khi lấy danh sách từ vựng:', err);
        res.status(500).json({ message: 'Lỗi server' });
    }
});

module.exports = router;