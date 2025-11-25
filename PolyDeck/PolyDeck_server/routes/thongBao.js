const express = require('express');
const router = express.Router();
const { listForUser, markAsRead } = require('../controllers/thongBaoController');

// Lấy danh sách thông báo cho user (bao gồm thông báo chung)
router.get('/', listForUser);

// Đánh dấu đã đọc
router.post('/:id/read', markAsRead);

module.exports = router;

