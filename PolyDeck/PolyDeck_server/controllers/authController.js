const NguoiDung = require('../models/NguoiDung');
const bcrypt = require('bcryptjs');
const crypto = require('crypto');
const { sendVerificationEmail, sendResetPasswordEmail } = require('../utils/emailService');

// Đăng ký người dùng mới
const register = async (req, res) => {
  try {
    const { ho_ten, email, mat_khau } = req.body;

    // Validate input
    if (!ho_ten || !email || !mat_khau) {
      return res.status(400).json({
        success: false,
        message: 'Vui lòng điền đầy đủ thông tin'
      });
    }

    // Validate email format
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      return res.status(400).json({
        success: false,
        message: 'Email không hợp lệ'
      });
    }

    // Validate password length
    if (mat_khau.length < 6) {
      return res.status(400).json({
        success: false,
        message: 'Mật khẩu phải có ít nhất 6 ký tự'
      });
    }

    // Kiểm tra email đã tồn tại chưa
    const existingUser = await NguoiDung.findOne({ email: email.toLowerCase() });
    if (existingUser) {
      return res.status(400).json({
        success: false,
        message: 'Email đã được sử dụng'
      });
    }

    // Hash password
    const salt = await bcrypt.genSalt(10);
    const mat_khau_hash = await bcrypt.hash(mat_khau, salt);

    // Tạo mã người dùng (dùng timestamp + random để đảm bảo unique)
    const ma_nguoi_dung = `USER_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;

    // Tạo token xác thực email
    const emailVerificationToken = crypto.randomBytes(32).toString('hex');
    const emailVerificationExpire = new Date();
    emailVerificationExpire.setHours(emailVerificationExpire.getHours() + 24); // Hết hạn sau 24 giờ

    // Tạo người dùng mới (chưa kích hoạt)
    const newUser = new NguoiDung({
      ma_nguoi_dung,
      ho_ten,
      email: email.toLowerCase(),
      mat_khau_hash,
      cap_do: 1,
      diem_tich_luy: 0,
      chuoi_ngay_hoc: 0,
      vai_tro: 'student',
      trang_thai: 'inactive', // Chưa kích hoạt
      email_verified: false,
      email_verification_token: emailVerificationToken,
      email_verification_expire: emailVerificationExpire
    });

    await newUser.save();

    // Gửi email kích hoạt
    const emailResult = await sendVerificationEmail(
      email.toLowerCase(),
      ho_ten,
      emailVerificationToken
    );

    if (!emailResult.success) {
      console.error('Không thể gửi email kích hoạt:', emailResult.error);
      // Vẫn trả về success nhưng cảnh báo
    }

    // Trả về thông tin người dùng (không trả về password)
    res.status(201).json({
      success: true,
      message: 'Đăng ký thành công. Vui lòng kiểm tra email để kích hoạt tài khoản.',
      data: {
        ma_nguoi_dung: newUser.ma_nguoi_dung,
        ho_ten: newUser.ho_ten,
        email: newUser.email,
        cap_do: newUser.cap_do,
        diem_tich_luy: newUser.diem_tich_luy,
        chuoi_ngay_hoc: newUser.chuoi_ngay_hoc,
        email_verified: newUser.email_verified
      }
    });
  } catch (error) {
    console.error('Register error:', error);
    res.status(500).json({
      success: false,
      message: 'Lỗi server khi đăng ký',
      error: error.message
    });
  }
};

// Đăng nhập
const login = async (req, res) => {
  try {
    const { email, mat_khau } = req.body;

    // Validate input
    if (!email || !mat_khau) {
      return res.status(400).json({
        success: false,
        message: 'Vui lòng nhập email và mật khẩu'
      });
    }

    // Tìm người dùng
    const user = await NguoiDung.findOne({ email: email.toLowerCase() });
    if (!user) {
      return res.status(401).json({
        success: false,
        message: 'Email hoặc mật khẩu không đúng'
      });
    }

    // Kiểm tra email đã được xác thực chưa
    if (!user.email_verified) {
      return res.status(403).json({
        success: false,
        message: 'Vui lòng kích hoạt tài khoản qua email trước khi đăng nhập'
      });
    }

    // Kiểm tra trạng thái tài khoản
    if (user.trang_thai !== 'active') {
      return res.status(403).json({
        success: false,
        message: 'Tài khoản đã bị khóa'
      });
    }

    // Kiểm tra user có password không (user đăng nhập Google không có password)
    if (!user.mat_khau_hash) {
      return res.status(401).json({
        success: false,
        message: 'Tài khoản này đăng nhập bằng Google. Vui lòng sử dụng Google để đăng nhập.'
      });
    }

    // Kiểm tra mật khẩu
    const isPasswordValid = await bcrypt.compare(mat_khau, user.mat_khau_hash);
    if (!isPasswordValid) {
      return res.status(401).json({
        success: false,
        message: 'Email hoặc mật khẩu không đúng'
      });
    }

    // Trả về thông tin người dùng (không trả về password)
    res.status(200).json({
      success: true,
      message: 'Đăng nhập thành công',
      data: {
        ma_nguoi_dung: user.ma_nguoi_dung,
        ho_ten: user.ho_ten,
        email: user.email,
        cap_do: user.cap_do,
        diem_tich_luy: user.diem_tich_luy,
        chuoi_ngay_hoc: user.chuoi_ngay_hoc,
        vai_tro: user.vai_tro,
        link_anh_dai_dien: user.link_anh_dai_dien
      }
    });
  } catch (error) {
    console.error('Login error:', error);
    res.status(500).json({
      success: false,
      message: 'Lỗi server khi đăng nhập',
      error: error.message
    });
  }
};

// Kích hoạt tài khoản qua email
const verifyEmail = async (req, res) => {
  try {
    const { token } = req.query;

    if (!token) {
      return res.status(400).json({
        success: false,
        message: 'Token không hợp lệ'
      });
    }

    // Tìm user theo token
    const user = await NguoiDung.findOne({
      email_verification_token: token
    });

    if (!user) {
      return res.status(400).json({
        success: false,
        message: 'Token không hợp lệ hoặc đã hết hạn'
      });
    }

    // Kiểm tra token đã hết hạn chưa
    if (user.email_verification_expire && new Date() > user.email_verification_expire) {
      return res.status(400).json({
        success: false,
        message: 'Token đã hết hạn. Vui lòng đăng ký lại hoặc yêu cầu gửi lại email kích hoạt.'
      });
    }

    // Kiểm tra email đã được xác thực chưa
    if (user.email_verified) {
      return res.status(200).json({
        success: true,
        message: 'Email đã được xác thực trước đó'
      });
    }

    // Kích hoạt tài khoản
    user.email_verified = true;
    user.trang_thai = 'active';
    user.email_verification_token = null;
    user.email_verification_expire = null;
    await user.save();

    res.status(200).json({
      success: true,
      message: 'Kích hoạt tài khoản thành công! Bạn có thể đăng nhập ngay bây giờ.'
    });
  } catch (error) {
    console.error('Verify email error:', error);
    res.status(500).json({
      success: false,
      message: 'Lỗi server khi kích hoạt tài khoản',
      error: error.message
    });
  }
};

// Gửi lại email kích hoạt
const resendVerificationEmail = async (req, res) => {
  try {
    const { email } = req.body;

    if (!email) {
      return res.status(400).json({
        success: false,
        message: 'Vui lòng nhập email'
      });
    }

    // Tìm user theo email
    const user = await NguoiDung.findOne({ email: email.toLowerCase() });

    if (!user) {
      // Không tiết lộ email có tồn tại hay không (bảo mật)
      return res.status(200).json({
        success: true,
        message: 'Nếu email tồn tại, chúng tôi đã gửi link kích hoạt'
      });
    }

    // Nếu email đã được xác thực rồi
    if (user.email_verified) {
      return res.status(400).json({
        success: false,
        message: 'Email đã được xác thực rồi'
      });
    }

    // Tạo token mới
    const emailVerificationToken = crypto.randomBytes(32).toString('hex');
    const emailVerificationExpire = new Date();
    emailVerificationExpire.setHours(emailVerificationExpire.getHours() + 24); // Hết hạn sau 24 giờ

    // Cập nhật token mới
    user.email_verification_token = emailVerificationToken;
    user.email_verification_expire = emailVerificationExpire;
    await user.save();

    // Gửi email kích hoạt
    const emailResult = await sendVerificationEmail(
      user.email,
      user.ho_ten,
      emailVerificationToken
    );

    if (!emailResult.success) {
      console.error('Không thể gửi email kích hoạt:', emailResult.error);
      return res.status(500).json({
        success: false,
        message: 'Không thể gửi email. Vui lòng thử lại sau.'
      });
    }

    res.status(200).json({
      success: true,
      message: 'Đã gửi lại email kích hoạt. Vui lòng kiểm tra hộp thư của bạn.'
    });
  } catch (error) {
    console.error('Resend verification email error:', error);
    res.status(500).json({
      success: false,
      message: 'Lỗi server khi gửi email',
      error: error.message
    });
  }
};

// Tạo mật khẩu ngẫu nhiên
const generateRandomPassword = (length = 12) => {
  const uppercase = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
  const lowercase = 'abcdefghijklmnopqrstuvwxyz';
  const numbers = '0123456789';
  const special = '!@#$%^&*';
  const allChars = uppercase + lowercase + numbers + special;
  
  let password = '';
  // Đảm bảo có ít nhất 1 ký tự từ mỗi loại
  password += uppercase[Math.floor(Math.random() * uppercase.length)];
  password += lowercase[Math.floor(Math.random() * lowercase.length)];
  password += numbers[Math.floor(Math.random() * numbers.length)];
  password += special[Math.floor(Math.random() * special.length)];
  
  // Thêm các ký tự ngẫu nhiên còn lại
  for (let i = password.length; i < length; i++) {
    password += allChars[Math.floor(Math.random() * allChars.length)];
  }
  
  // Xáo trộn mật khẩu
  return password.split('').sort(() => Math.random() - 0.5).join('');
};

// Quên mật khẩu - gửi mật khẩu mới qua email
const forgotPassword = async (req, res) => {
  try {
    const { email } = req.body;

    if (!email) {
      return res.status(400).json({
        success: false,
        message: 'Vui lòng nhập email'
      });
    }

    // Validate email format
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      return res.status(400).json({
        success: false,
        message: 'Email không hợp lệ'
      });
    }

    // Tìm user theo email
    const user = await NguoiDung.findOne({ email: email.toLowerCase() });

    console.log('Forgot password request for email:', email.toLowerCase());
    
    if (!user) {
      console.log('User not found for email:', email.toLowerCase());
      // Không tiết lộ email có tồn tại hay không (bảo mật)
      return res.status(200).json({
        success: true,
        message: 'Nếu email tồn tại, chúng tôi đã gửi mật khẩu mới đến email của bạn'
      });
    }

    console.log('User found:', user.ho_ten, 'Email verified:', user.email_verified);

    // Kiểm tra email đã được xác thực chưa
    if (!user.email_verified) {
      console.log('Email chưa được kích hoạt');
      return res.status(400).json({
        success: false,
        message: 'Vui lòng kích hoạt tài khoản trước. Kiểm tra email để kích hoạt.'
      });
    }

    // Tạo mật khẩu mới ngẫu nhiên
    const newPassword = generateRandomPassword(12);

    // Hash mật khẩu mới
    const salt = await bcrypt.genSalt(10);
    const hashedPassword = await bcrypt.hash(newPassword, salt);

    // Cập nhật mật khẩu trong database
    user.mat_khau_hash = hashedPassword;
    await user.save();

    // Gửi email chứa mật khẩu mới
    console.log('Đang gửi email reset password đến:', user.email);
    const emailResult = await sendResetPasswordEmail(
      user.email,
      user.ho_ten,
      newPassword
    );

    if (!emailResult.success) {
      console.error('Không thể gửi email reset password:', emailResult.error);
      return res.status(500).json({
        success: false,
        message: 'Không thể gửi email. Vui lòng thử lại sau.'
      });
    }

    console.log('Email reset password đã được gửi thành công. Message ID:', emailResult.messageId);
    res.status(200).json({
      success: true,
      message: 'Mật khẩu mới đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư.'
    });
  } catch (error) {
    console.error('Forgot password error:', error);
    res.status(500).json({
      success: false,
      message: 'Lỗi server khi xử lý yêu cầu quên mật khẩu',
      error: error.message
    });
  }
};

// Đăng nhập bằng Google
const googleLogin = async (req, res) => {
  try {
    const { id_token } = req.body; // Token từ Google Sign-In

    if (!id_token) {
      return res.status(400).json({
        success: false,
        message: 'Token không hợp lệ'
      });
    }

    // Verify token với Google
    const { OAuth2Client } = require('google-auth-library');
    const client = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);

    let ticket;
    try {
      ticket = await client.verifyIdToken({
        idToken: id_token,
        audience: process.env.GOOGLE_CLIENT_ID
      });
    } catch (error) {
      return res.status(401).json({
        success: false,
        message: 'Token Google không hợp lệ'
      });
    }

    const payload = ticket.getPayload();
    const { sub: google_id, email, name, picture } = payload;

    // Tìm user theo google_id hoặc email
    let user = await NguoiDung.findOne({
      $or: [
        { google_id: google_id },
        { email: email.toLowerCase() }
      ]
    });

    if (user) {
      // User đã tồn tại - cập nhật google_id và email_verified nếu chưa có
      if (!user.google_id) {
        user.google_id = google_id;
      }
      // Google đã xác thực email, nên tự động kích hoạt
      if (!user.email_verified) {
        user.email_verified = true;
        user.trang_thai = 'active';
      }
      await user.save();
    } else {
      // Tạo user mới - Google đã xác thực email nên tự động kích hoạt
      const ma_nguoi_dung = `USER_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
      user = new NguoiDung({
        ma_nguoi_dung,
        ho_ten: name,
        email: email.toLowerCase(),
        google_id: google_id,
        link_anh_dai_dien: picture,
        // Không set mat_khau_hash vì đăng nhập bằng Google không cần password
        cap_do: 1,
        diem_tich_luy: 0,
        chuoi_ngay_hoc: 0,
        vai_tro: 'student',
        trang_thai: 'active',
        email_verified: true // Google đã xác thực email
      });
      await user.save();
    }

    // Trả về thông tin user
    res.status(200).json({
      success: true,
      message: 'Đăng nhập Google thành công',
      data: {
        ma_nguoi_dung: user.ma_nguoi_dung,
        ho_ten: user.ho_ten,
        email: user.email,
        cap_do: user.cap_do,
        diem_tich_luy: user.diem_tich_luy,
        chuoi_ngay_hoc: user.chuoi_ngay_hoc,
        vai_tro: user.vai_tro,
        link_anh_dai_dien: user.link_anh_dai_dien
      }
    });
  } catch (error) {
    console.error('Google login error:', error);
    res.status(500).json({
      success: false,
      message: 'Lỗi server khi đăng nhập Google',
      error: error.message
    });
  }
};

// Đổi mật khẩu
const changePassword = async (req, res) => {
  try {
    const { email, mat_khau_cu, mat_khau_moi } = req.body;

    // Validate input
    if (!email || !mat_khau_cu || !mat_khau_moi) {
      return res.status(400).json({
        success: false,
        message: 'Vui lòng điền đầy đủ thông tin'
      });
    }

    // Validate password length
    if (mat_khau_moi.length < 6) {
      return res.status(400).json({
        success: false,
        message: 'Mật khẩu mới phải có ít nhất 6 ký tự'
      });
    }

    // Tìm người dùng
    const user = await NguoiDung.findOne({ email: email.toLowerCase() });
    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'Email không tồn tại'
      });
    }

    // Kiểm tra user có password không (user đăng nhập Google không có password)
    if (!user.mat_khau_hash) {
      return res.status(400).json({
        success: false,
        message: 'Tài khoản này đăng nhập bằng Google. Không thể đổi mật khẩu.'
      });
    }

    // Kiểm tra mật khẩu cũ
    const isOldPasswordValid = await bcrypt.compare(mat_khau_cu, user.mat_khau_hash);
    if (!isOldPasswordValid) {
      return res.status(401).json({
        success: false,
        message: 'Mật khẩu cũ không đúng'
      });
    }

    // Kiểm tra mật khẩu mới có khác mật khẩu cũ không
    const isSamePassword = await bcrypt.compare(mat_khau_moi, user.mat_khau_hash);
    if (isSamePassword) {
      return res.status(400).json({
        success: false,
        message: 'Mật khẩu mới phải khác mật khẩu cũ'
      });
    }

    // Hash mật khẩu mới
    const salt = await bcrypt.genSalt(10);
    const hashedNewPassword = await bcrypt.hash(mat_khau_moi, salt);

    // Cập nhật mật khẩu
    user.mat_khau_hash = hashedNewPassword;
    await user.save();

    res.status(200).json({
      success: true,
      message: 'Đổi mật khẩu thành công'
    });
  } catch (error) {
    console.error('Change password error:', error);
    res.status(500).json({
      success: false,
      message: 'Lỗi server khi đổi mật khẩu',
      error: error.message
    });
  }
};

module.exports = {
  register,
  login,
  googleLogin,
  verifyEmail,
  resendVerificationEmail,
  forgotPassword,
  changePassword
};
