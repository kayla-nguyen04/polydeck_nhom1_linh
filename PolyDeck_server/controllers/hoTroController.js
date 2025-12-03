// Controller xử lý logic Hỗ trợ
const YeuCauHoTro = require('../models/YeuCauHoTro');
const NguoiDung = require('../models/NguoiDung');

// Lấy tất cả yêu cầu hỗ trợ (admin)
const getAllSupportRequests = async (req, res) => {
    try {
        const requests = await YeuCauHoTro.find({})
            .populate('ma_nguoi_dung', 'ho_ten email')
            .sort({ ngay_gui: -1 });
        
        res.status(200).json(requests);
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
