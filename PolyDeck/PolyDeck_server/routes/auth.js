const express = require('express');
const router = express.Router();
const authController = require('../controllers/authController');

// Đăng ký
router.post('/register', authController.register);

// Đăng nhập
router.post('/login', authController.login);

// Đăng nhập bằng Google
router.post('/google', authController.googleLogin);

// Kích hoạt tài khoản qua email
router.get('/verify-email', authController.verifyEmail);

// Gửi lại email kích hoạt
router.post('/resend-verification-email', authController.resendVerificationEmail);

// Quên mật khẩu - gửi mật khẩu mới qua email
router.post('/forgot-password', authController.forgotPassword);

module.exports = router;
