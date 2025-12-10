const express = require('express');
const router = express.Router();
const { listForUser, markAsRead } = require('../controllers/thongBaoController');

// Đánh dấu đã đọc - phải đặt trước route /:userId để tránh conflict
router.post('/:id/read', markAsRead);

// Lấy danh sách thông báo cho user (bao gồm thông báo chung)
// Support both query param and path param
router.get('/', listForUser);
router.get('/:userId', listForUser);

module.exports = router;

