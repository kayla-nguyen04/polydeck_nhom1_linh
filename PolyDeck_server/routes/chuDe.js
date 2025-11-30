const express = require('express');
const router = express.Router();
const multer = require('multer');
const path = require('path');
const { Types } = require('mongoose');
const ChuDe = require('../models/ChuDe'); 
const TuVung = require('../models/TuVung'); 
const TienDoHocTap = require('../models/TienDoHocTap'); // dùng cho tiến độ học

const storage = multer.diskStorage({
    destination: function (req, file, cb) { cb(null, 'public/uploads/') },
    filename: function (req, file, cb) { cb(null, 'icon-' + Date.now() + path.extname(file.originalname)) }
});
const upload = multer({ storage: storage });

const resolveChuDeFilter = (identifier = '') => {
    return { _id: identifier };
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

// PUT: Cập nhật chủ đề (với file ảnh)
router.put('/:id', upload.single('file'), async (req, res) => {
    try {
        const updateData = { ...req.body };
        
        // Nếu có file ảnh mới, cập nhật link_anh_icon
        if (req.file) {
            const fileUrl = `${req.protocol}://${req.get('host')}/uploads/${req.file.filename}`;
            updateData.link_anh_icon = fileUrl;
        }
        
        const updatedChuDe = await ChuDe.findOneAndUpdate(resolveChuDeFilter(req.params.id), updateData, { new: true });
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

// POST: Thêm từ vựng mới vào một chủ đề
router.post('/:chuDeId/them-tu-vung', async (req, res) => {
    try {
        const chuDe = await ChuDe.findOne(resolveChuDeFilter(req.params.chuDeId));
        if (!chuDe) {
            return res.status(404).json({ message: 'Không tìm thấy chủ đề' });
        }

        const { tu_tieng_anh, nghia_tieng_viet, phien_am, cau_vi_du } = req.body;
        if (!tu_tieng_anh || !nghia_tieng_viet) {
            return res.status(400).json({ message: 'Từ tiếng Anh và nghĩa tiếng Việt là bắt buộc.' });
        }

        const tuVung = new TuVung({
            ma_chu_de: chuDe._id,
            tu_tieng_anh,
            phien_am,
            nghia_tieng_viet,
            cau_vi_du: cau_vi_du || null,
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
        const vocabList = await TuVung.find({ ma_chu_de: chuDe._id });
        res.json(vocabList);
    } catch (err) {
        console.error('Lỗi khi lấy danh sách từ vựng:', err);
        res.status(500).json({ message: 'Lỗi server' });
    }
});

// GET: Tiến độ học tập theo chủ đề cho một người dùng
// URL: /api/chude/:chuDeId/progress?userId=...
router.get('/:chuDeId/progress', async (req, res) => {
    try {
        const { chuDeId } = req.params;
        const { userId } = req.query;

        if (!userId) {
            return res.status(400).json({
                success: false,
                message: 'Thiếu userId',
                data: null
            });
        }

        const chuDe = await ChuDe.findOne(resolveChuDeFilter(chuDeId));
        if (!chuDe) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy chủ đề',
                data: null
            });
        }

        // Tổng số từ trong chủ đề (ưu tiên field so_luong_tu, fallback đếm thực tế)
        let totalWords = typeof chuDe.so_luong_tu === 'number' ? chuDe.so_luong_tu : 0;
        if (!totalWords || totalWords <= 0) {
            totalWords = await TuVung.countDocuments({ ma_chu_de: chuDe._id });
        }

        // Số từ đã học (trạng thái 'da_nho') cho user + chủ đề này
        const learnedWords = await TienDoHocTap.countDocuments({
            ma_nguoi_dung: userId,
            ma_chu_de: chuDe._id,
            trang_thai_hoc: 'da_nho'
        });

        return res.json({
            success: true,
            message: 'Lấy tiến độ học tập thành công',
            data: {
                totalWords,
                learnedWords
            }
        });
    } catch (err) {
        console.error('Lỗi khi lấy tiến độ học tập:', err);
        return res.status(500).json({
            success: false,
            message: 'Lỗi server',
            data: null
        });
    }
});

// POST: Import từ vựng hàng loạt từ Excel
router.post('/:chuDeId/import-vocab', async (req, res) => {
    try {
        const chuDe = await ChuDe.findOne(resolveChuDeFilter(req.params.chuDeId));
        if (!chuDe) {
            return res.status(404).json({ message: 'Không tìm thấy chủ đề' });
        }

        const vocabList = req.body; // Array of TuVung objects
        if (!Array.isArray(vocabList) || vocabList.length === 0) {
            return res.status(400).json({ message: 'Danh sách từ vựng không hợp lệ' });
        }

        let successCount = 0;
        let failCount = 0;

        for (const vocabData of vocabList) {
            try {
                const { tu_tieng_anh, nghia_tieng_viet, phien_am, cau_vi_du } = vocabData;
                
                if (!tu_tieng_anh || !nghia_tieng_viet) {
                    failCount++;
                    continue;
                }

                const tuVung = new TuVung({
                    ma_chu_de: chuDe._id,
                    tu_tieng_anh: tu_tieng_anh.trim(),
                    phien_am: phien_am ? phien_am.trim() : null,
                    nghia_tieng_viet: nghia_tieng_viet.trim(),
                    cau_vi_du: cau_vi_du ? cau_vi_du.trim() : null,
                });

                await tuVung.save();
                successCount++;
            } catch (err) {
                console.error('Error saving vocab:', err);
                failCount++;
            }
        }

        // Update so_luong_tu in ChuDe
        await ChuDe.updateOne({ _id: chuDe._id }, { $inc: { so_luong_tu: successCount } });

        res.status(200).json({ 
            message: `Đã import thành công ${successCount} từ vựng${failCount > 0 ? `, ${failCount} từ vựng thất bại` : ''}`,
            success: successCount,
            failed: failCount
        });
    } catch (err) {
        console.error('Error importing vocab:', err);
        res.status(500).json({ message: err.message });
    }
});

module.exports = router;