const express = require('express');
const router = express.Router();
const multer = require('multer');
const path = require('path');
const mongoose = require('mongoose');
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
        const chuDeId = req.params.id;
        
        // Tìm chủ đề trước để đảm bảo nó tồn tại
        const chuDe = await ChuDe.findOne(resolveChuDeFilter(chuDeId));
        if (!chuDe) {
            return res.status(404).json({ message: 'Không tìm thấy chủ đề' });
        }
        
        // Xóa tất cả từ vựng liên quan trước
        const deleteVocabResult = await TuVung.deleteMany({ ma_chu_de: chuDe._id });
        console.log(`Đã xóa ${deleteVocabResult.deletedCount} từ vựng của chủ đề ${chuDeId}`);
        
        // Xóa tiến độ học tập liên quan (nếu có)
        try {
            await TienDoHocTap.deleteMany({ ma_chu_de: chuDe._id });
            console.log(`Đã xóa tiến độ học tập của chủ đề ${chuDeId}`);
        } catch (tienDoErr) {
            console.warn('Lỗi khi xóa tiến độ học tập (có thể không tồn tại):', tienDoErr.message);
        }
        
        // Sau đó xóa chủ đề
        const deletedChuDe = await ChuDe.findOneAndDelete(resolveChuDeFilter(chuDeId));
        
        res.json({ 
            message: 'Đã xóa chủ đề và tất cả từ vựng',
            deletedVocabCount: deleteVocabResult.deletedCount
        });
    } catch (err) {
        console.error('Lỗi khi xóa chủ đề:', err);
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

        // Validate userId
        if (!mongoose.Types.ObjectId.isValid(userId)) {
            return res.status(400).json({
                success: false,
                message: 'userId không hợp lệ',
                data: null
            });
        }

        // Tổng số từ trong chủ đề - luôn đếm thực tế từ TuVung để đảm bảo chính xác
        // (không dùng so_luong_tu vì có thể bị lệch khi import/xóa từ)
        const totalWords = await TuVung.countDocuments({ ma_chu_de: chuDe._id });

        // Số từ đã học (trạng thái 'da_nho') cho user + chủ đề này
        // Convert userId sang ObjectId để match với dữ liệu đã lưu
        const learnedWords = await TienDoHocTap.countDocuments({
            ma_nguoi_dung: new mongoose.Types.ObjectId(userId),
            ma_chu_de: chuDe._id,
            trang_thai_hoc: 'da_nho'
        });

        console.log(`[GET /progress] chuDeId: ${chuDeId}, userId: ${userId}, totalWords: ${totalWords}, learnedWords: ${learnedWords}`);

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

// POST: Cập nhật tiến độ học tập cho một từ vựng
// URL: /api/chude/:chuDeId/progress
router.post('/:chuDeId/progress', async (req, res) => {
    try {
        const { chuDeId } = req.params;
        const { userId, tuVungId, trangThaiHoc } = req.body; // trangThaiHoc: 'chua_hoc', 'dang_hoc', 'da_nho'

        console.log(`[POST /progress] Request body:`, { chuDeId, userId, tuVungId, trangThaiHoc });

        if (!userId || !tuVungId) {
            return res.status(400).json({
                success: false,
                message: 'Thiếu userId hoặc tuVungId',
                data: null
            });
        }

        if (!mongoose.Types.ObjectId.isValid(userId) || !mongoose.Types.ObjectId.isValid(tuVungId) || !mongoose.Types.ObjectId.isValid(chuDeId)) {
            console.error(`[POST /progress] Invalid ObjectId - userId: ${userId}, tuVungId: ${tuVungId}, chuDeId: ${chuDeId}`);
            return res.status(400).json({
                success: false,
                message: 'ID không hợp lệ',
                data: null
            });
        }

        // Kiểm tra ChuDe có tồn tại không
        const chuDe = await ChuDe.findById(chuDeId);
        if (!chuDe) {
            console.error(`[POST /progress] ChuDe not found: ${chuDeId}`);
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy chủ đề',
                data: null
            });
        }

        // Kiểm tra TuVung có tồn tại không
        const tuVung = await TuVung.findById(tuVungId);
        if (!tuVung) {
            console.error(`[POST /progress] TuVung not found: ${tuVungId}`);
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy từ vựng',
                data: null
            });
        }

        // Kiểm tra TuVung có thuộc ChuDe này không
        if (tuVung.ma_chu_de.toString() !== chuDe._id.toString()) {
            console.error(`[POST /progress] TuVung ${tuVungId} không thuộc ChuDe ${chuDeId}. TuVung.ma_chu_de: ${tuVung.ma_chu_de}, ChuDe._id: ${chuDe._id}`);
            return res.status(400).json({
                success: false,
                message: 'Từ vựng không thuộc chủ đề này',
                data: null
            });
        }

        const validStatus = ['chua_hoc', 'dang_hoc', 'da_nho'];
        const status = validStatus.includes(trangThaiHoc) ? trangThaiHoc : 'da_nho'; // Default: da_nho

        // Tìm hoặc tạo tiến độ học tập
        // Sử dụng ObjectId để đảm bảo type đúng
        const tienDo = await TienDoHocTap.findOneAndUpdate(
            {
                ma_nguoi_dung: new mongoose.Types.ObjectId(userId),
                ma_tu_vung: new mongoose.Types.ObjectId(tuVungId),
                ma_chu_de: new mongoose.Types.ObjectId(chuDeId)
            },
            {
                ma_nguoi_dung: new mongoose.Types.ObjectId(userId),
                ma_tu_vung: new mongoose.Types.ObjectId(tuVungId),
                ma_chu_de: new mongoose.Types.ObjectId(chuDeId),
                trang_thai_hoc: status,
                lan_cuoi_hoc: new Date()
            },
            {
                upsert: true,
                new: true,
                runValidators: true
            }
        );

        console.log(`[POST /progress] ✅ Updated progress - chuDeId: ${chuDeId}, userId: ${userId}, tuVungId: ${tuVungId}, status: ${status}`);
        console.log(`[POST /progress] TienDo saved with ID:`, tienDo ? tienDo._id : 'null');

        return res.json({
            success: true,
            message: 'Cập nhật tiến độ học tập thành công',
            data: null
        });
    } catch (err) {
        console.error('❌ Lỗi khi cập nhật tiến độ học tập:', err);
        console.error('Error details:', {
            name: err.name,
            message: err.message,
            stack: err.stack
        });
        return res.status(500).json({
            success: false,
            message: err.message || 'Lỗi server',
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