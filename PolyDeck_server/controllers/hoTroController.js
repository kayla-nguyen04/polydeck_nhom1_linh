// Controller xử lý logic Hỗ trợ
const YeuCauHoTro = require('../models/YeuCauHoTro');
const NguoiDung = require('../models/NguoiDung');

// Lấy tất cả yêu cầu hỗ trợ (admin)
const getAllSupportRequests = async (req, res) => {
    try {
        // Explicitly select fields and don't populate
        const requests = await YeuCauHoTro.find({})
            .select('_id ma_nguoi_dung noi_dung ten_nguoi_gui email_nguoi_gui ngay_gui')
            .sort({ ngay_gui: -1 })
            .lean(); // Use lean() for better performance - returns plain JS objects
        
        // Convert to plain objects with string IDs and dates
        const formattedRequests = requests.map(req => {
            // Handle ma_nguoi_dung - can be null, ObjectId, string, or populated object
            let maNguoiDungStr = null;
            if (req.ma_nguoi_dung) {
                if (typeof req.ma_nguoi_dung === 'string') {
                    // Already a string
                    maNguoiDungStr = req.ma_nguoi_dung;
                } else if (req.ma_nguoi_dung._id) {
                    // It's a populated object, get the _id
                    maNguoiDungStr = typeof req.ma_nguoi_dung._id === 'string' 
                        ? req.ma_nguoi_dung._id 
                        : req.ma_nguoi_dung._id.toString();
                } else if (req.ma_nguoi_dung.toString) {
                    // It's an ObjectId
                    maNguoiDungStr = req.ma_nguoi_dung.toString();
                }
            }
            
            // Handle ngay_gui - can be Date object or string
            let ngayGuiStr = new Date().toISOString();
            if (req.ngay_gui) {
                if (req.ngay_gui instanceof Date) {
                    ngayGuiStr = req.ngay_gui.toISOString();
                } else if (typeof req.ngay_gui === 'string') {
                    ngayGuiStr = req.ngay_gui;
                } else {
                    ngayGuiStr = new Date(req.ngay_gui).toISOString();
                }
            }
            
            return {
                _id: req._id ? (typeof req._id === 'string' ? req._id : req._id.toString()) : null,
                ma_nguoi_dung: maNguoiDungStr,
                noi_dung: req.noi_dung || '',
                ten_nguoi_gui: req.ten_nguoi_gui || '',
                email_nguoi_gui: req.email_nguoi_gui || '',
                ngay_gui: ngayGuiStr
            };
        });
        
        // Log first request for debugging
        if (formattedRequests.length > 0) {
            console.log('First formatted request:', JSON.stringify(formattedRequests[0], null, 2));
            console.log('ma_nguoi_dung type:', typeof formattedRequests[0].ma_nguoi_dung, 'value:', formattedRequests[0].ma_nguoi_dung);
        }
        res.status(200).json(formattedRequests);
    } catch (error) {
        console.error('Lỗi khi lấy danh sách yêu cầu hỗ trợ:', error);
        res.status(500).json({ message: 'Lỗi server' });
    }
};

// Tạo yêu cầu hỗ trợ mới (user)
const createSupportRequest = async (req, res) => {
    try {
        const { ma_nguoi_dung, noi_dung, ten_nguoi_gui, email_nguoi_gui } = req.body;

        if (!noi_dung || !ten_nguoi_gui || !email_nguoi_gui) {
            return res.status(400).json({ message: 'Thiếu thông tin yêu cầu hỗ trợ' });
        }

        const newRequest = new YeuCauHoTro({
            ma_nguoi_dung: ma_nguoi_dung || null,
            noi_dung,
            ten_nguoi_gui,
            email_nguoi_gui
        });

        await newRequest.save();
        res.status(201).json({ message: 'Gửi yêu cầu hỗ trợ thành công!', data: newRequest });
    } catch (error) {
        console.error('Lỗi khi tạo yêu cầu hỗ trợ:', error);
        res.status(500).json({ message: 'Lỗi server' });
    }
};

// Xóa yêu cầu hỗ trợ (admin)
const deleteSupportRequest = async (req, res) => {
    try {
        const { id } = req.params;
        await YeuCauHoTro.findByIdAndDelete(id);
        res.status(200).json({ message: 'Xóa yêu cầu hỗ trợ thành công!' });
    } catch (error) {
        console.error('Lỗi khi xóa yêu cầu hỗ trợ:', error);
        res.status(500).json({ message: 'Lỗi server' });
    }
};

module.exports = {
    getAllSupportRequests,
    createSupportRequest,
    deleteSupportRequest
};
