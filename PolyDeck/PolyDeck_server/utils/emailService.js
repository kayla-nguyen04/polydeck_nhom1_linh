const nodemailer = require('nodemailer');

// Tạo transporter để gửi email
const createTransporter = () => {
  // Kiểm tra cấu hình
  const smtpUser = process.env.SMTP_USER;
  const smtpPass = process.env.SMTP_PASS;
  
  if (!smtpUser || !smtpPass) {
    console.error('SMTP_USER hoặc SMTP_PASS chưa được cấu hình trong .env');
    throw new Error('Email configuration missing');
  }
  
  console.log('SMTP Config:', {
    host: process.env.SMTP_HOST || 'smtp.gmail.com',
    port: parseInt(process.env.SMTP_PORT || '587'),
    user: smtpUser,
    passLength: smtpPass ? smtpPass.length : 0
  });
  
  // Sử dụng SMTP server (Gmail hoặc email provider khác)
  return nodemailer.createTransport({
    host: process.env.SMTP_HOST || 'smtp.gmail.com',
    port: parseInt(process.env.SMTP_PORT || '587'),
    secure: process.env.SMTP_SECURE === 'true', // true cho 465, false cho các port khác
    auth: {
      user: smtpUser,
      pass: smtpPass
    },
    // Thêm tls để tránh lỗi certificate
    tls: {
      rejectUnauthorized: false
    }
  });
};

// Gửi email kích hoạt tài khoản
const sendVerificationEmail = async (email, hoTen, verificationToken) => {
  try {
    const transporter = createTransporter();
    
    // Tạo link kích hoạt (có thể là web URL hoặc deep link cho app)
    const baseUrl = process.env.BACKEND_URL || process.env.FRONTEND_URL || 'http://localhost:3000';
    const verificationUrl = `${baseUrl}/api/auth/verify-email?token=${verificationToken}`;
    
    const mailOptions = {
      from: process.env.EMAIL_FROM || process.env.SMTP_USER || 'noreply@polydeck.com',
      to: email,
      subject: 'Kích hoạt tài khoản PolyDeck',
      html: `
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="UTF-8">
          <style>
            body {
              font-family: Arial, sans-serif;
              line-height: 1.6;
              color: #333;
            }
            .container {
              max-width: 600px;
              margin: 0 auto;
              padding: 20px;
            }
            .header {
              background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
              color: white;
              padding: 30px;
              text-align: center;
              border-radius: 10px 10px 0 0;
            }
            .content {
              background: #f9f9f9;
              padding: 30px;
              border-radius: 0 0 10px 10px;
            }
            .button {
              display: inline-block;
              padding: 12px 30px;
              background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
              color: white;
              text-decoration: none;
              border-radius: 5px;
              margin: 20px 0;
            }
            .footer {
              text-align: center;
              margin-top: 20px;
              color: #666;
              font-size: 12px;
            }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <h1>Chào mừng đến với PolyDeck!</h1>
            </div>
            <div class="content">
              <p>Xin chào <strong>${hoTen}</strong>,</p>
              <p>Cảm ơn bạn đã đăng ký tài khoản tại PolyDeck. Để hoàn tất quá trình đăng ký, vui lòng kích hoạt tài khoản của bạn bằng cách nhấp vào nút bên dưới:</p>
              <div style="text-align: center;">
                <a href="${verificationUrl}" class="button">Kích hoạt tài khoản</a>
              </div>
              <p>Hoặc bạn có thể sao chép và dán link sau vào trình duyệt:</p>
              <p style="word-break: break-all; color: #667eea;">${verificationUrl}</p>
              <p><strong>Lưu ý:</strong> Link này sẽ hết hạn sau 24 giờ.</p>
              <p>Nếu bạn không đăng ký tài khoản này, vui lòng bỏ qua email này.</p>
            </div>
            <div class="footer">
              <p>© 2024 PolyDeck. Tất cả quyền được bảo lưu.</p>
            </div>
          </div>
        </body>
        </html>
      `,
      text: `
        Chào mừng đến với PolyDeck!
        
        Xin chào ${hoTen},
        
        Cảm ơn bạn đã đăng ký tài khoản tại PolyDeck. Để hoàn tất quá trình đăng ký, vui lòng kích hoạt tài khoản của bạn bằng cách truy cập link sau:
        
        ${verificationUrl}
        
        Lưu ý: Link này sẽ hết hạn sau 24 giờ.
        
        Nếu bạn không đăng ký tài khoản này, vui lòng bỏ qua email này.
        
        © 2024 PolyDeck
      `
    };

    const info = await transporter.sendMail(mailOptions);
    console.log('Email đã được gửi:', info.messageId);
    return { success: true, messageId: info.messageId };
  } catch (error) {
    console.error('Lỗi khi gửi email:', error);
    return { success: false, error: error.message };
  }
};

// Gửi email reset password với mật khẩu mới
const sendResetPasswordEmail = async (email, hoTen, newPassword) => {
  try {
    const transporter = createTransporter();
    
    const mailOptions = {
      from: process.env.EMAIL_FROM || process.env.SMTP_USER || 'noreply@polydeck.com',
      to: email,
      subject: 'Mật khẩu mới - PolyDeck',
      html: `
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="UTF-8">
          <style>
            body {
              font-family: Arial, sans-serif;
              line-height: 1.6;
              color: #333;
            }
            .container {
              max-width: 600px;
              margin: 0 auto;
              padding: 20px;
            }
            .header {
              background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
              color: white;
              padding: 30px;
              text-align: center;
              border-radius: 10px 10px 0 0;
            }
            .content {
              background: #f9f9f9;
              padding: 30px;
              border-radius: 0 0 10px 10px;
            }
            .password-box {
              background: #fff;
              border: 2px solid #667eea;
              border-radius: 8px;
              padding: 20px;
              margin: 20px 0;
              text-align: center;
              font-size: 24px;
              font-weight: bold;
              color: #667eea;
              letter-spacing: 2px;
            }
            .warning {
              background: #fff3cd;
              border-left: 4px solid #ffc107;
              padding: 15px;
              margin: 20px 0;
              border-radius: 4px;
            }
            .footer {
              text-align: center;
              margin-top: 20px;
              color: #666;
              font-size: 12px;
            }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <h1>Mật khẩu mới của bạn</h1>
            </div>
            <div class="content">
              <p>Xin chào <strong>${hoTen}</strong>,</p>
              <p>Chúng tôi đã nhận được yêu cầu đặt lại mật khẩu cho tài khoản PolyDeck của bạn.</p>
              <p>Mật khẩu mới của bạn là:</p>
              <div class="password-box">
                ${newPassword}
              </div>
              <div class="warning">
                <strong>⚠️ Lưu ý quan trọng:</strong>
                <ul style="margin: 10px 0; padding-left: 20px;">
                  <li>Vui lòng đăng nhập ngay và đổi mật khẩu này thành mật khẩu dễ nhớ của bạn</li>
                  <li>Không chia sẻ mật khẩu này với bất kỳ ai</li>
                  <li>Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng liên hệ hỗ trợ ngay</li>
                </ul>
              </div>
              <p>Bạn có thể đăng nhập ngay bây giờ với mật khẩu mới này.</p>
            </div>
            <div class="footer">
              <p>© 2024 PolyDeck. Tất cả quyền được bảo lưu.</p>
            </div>
          </div>
        </body>
        </html>
      `,
      text: `
        Mật khẩu mới - PolyDeck
        
        Xin chào ${hoTen},
        
        Chúng tôi đã nhận được yêu cầu đặt lại mật khẩu cho tài khoản PolyDeck của bạn.
        
        Mật khẩu mới của bạn là: ${newPassword}
        
        ⚠️ Lưu ý quan trọng:
        - Vui lòng đăng nhập ngay và đổi mật khẩu này thành mật khẩu dễ nhớ của bạn
        - Không chia sẻ mật khẩu này với bất kỳ ai
        - Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng liên hệ hỗ trợ ngay
        
        Bạn có thể đăng nhập ngay bây giờ với mật khẩu mới này.
        
        © 2024 PolyDeck
      `
    };

    const info = await transporter.sendMail(mailOptions);
    console.log('Email reset password đã được gửi:', info.messageId);
    return { success: true, messageId: info.messageId };
  } catch (error) {
    console.error('Lỗi khi gửi email reset password:', error);
    return { success: false, error: error.message };
  }
};

module.exports = {
  sendVerificationEmail,
  sendResetPasswordEmail
};

