// Routes cho Hỗ trợ (Gửi yêu cầu hỗ trợ, xem danh sách, trả lời)
const express = require('express');
const router = express.Router();
const {
    getAllSupportRequests,
    createSupportRequest,
    deleteSupportRequest
} = require('../controllers/hoTroController');

// Lấy tất cả yêu cầu hỗ trợ (admin)
router.get('/', getAllSupportRequests);

// Tạo yêu cầu hỗ trợ mới (user)
router.post('/', createSupportRequest);

// Xóa yêu cầu hỗ trợ (admin)
router.delete('/:id', deleteSupportRequest);

module.exports = router;
