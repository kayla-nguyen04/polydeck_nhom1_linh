const express = require('express');
const cors = require('cors');
const connectDB = require('./config/database');
const bcrypt = require('bcryptjs');
const NguoiDung = require('./models/NguoiDung');
require('dotenv').config();
const { execSync } = require('child_process');

const app = express();

// Simple request logger to help debug requests coming from mobile
app.use((req, res, next) => {
  try {
    const ip = req.headers['x-forwarded-for'] || req.socket.remoteAddress || req.ip;
    console.log(`${new Date().toISOString()} - ${req.method} ${req.originalUrl} - from ${ip}`);
  } catch (e) {}
  next();
});

const createDefaultAdmin = async () => {
  try {
    const adminEmail = 'admin@polydeck.com';
    const adminPassword = 'admin123';

    const existingAdmin = await NguoiDung.findOne({ email: adminEmail });

    if (!existingAdmin) {
      console.log('Tài khoản Admin mặc định không tồn tại. Đang tạo...');
      const salt = await bcrypt.genSalt(10);
      const hashedPassword = await bcrypt.hash(adminPassword, salt);

      const adminUser = new NguoiDung({
        ma_nguoi_dung: `ADMIN_${Date.now()}`,
        ho_ten: 'PolyDeck Admin',
        email: adminEmail,
        mat_khau_hash: hashedPassword, 
        vai_tro: 'admin',
        trang_thai: 'active',
        email_verified: true,
        email_verification_token: null,
        email_verification_expire: null
      });

      await adminUser.save();
      console.log('✓ Tài khoản Admin mặc định đã được tạo thành công.');
      console.log(`  => Email: ${adminEmail}`);
      console.log(`  => Mật khẩu: ${adminPassword}`);
    } else {
      // Ensure default admin is active and verified
      let shouldSave = false;
      if (!existingAdmin.email_verified) {
        existingAdmin.email_verified = true;
        shouldSave = true;
      }
      if (existingAdmin.trang_thai !== 'active') {
        existingAdmin.trang_thai = 'active';
        shouldSave = true;
      }
      if (shouldSave) {
        existingAdmin.email_verification_token = null;
        existingAdmin.email_verification_expire = null;
        await existingAdmin.save();
        console.log('✓ Đã cập nhật Admin mặc định: kích hoạt và xác thực email.');
      }
    }
  } catch (error) {
    console.error('Lỗi khi tạo tài khoản Admin mặc định:', error.message);
  }
};

// Tạo tài khoản test: 1 user và 1 admin
const createTestAccounts = async () => {
  try {
    const accounts = [
      {
        email: 'user.test@polydeck.com',
        password: 'user123',
        ho_ten: 'PolyDeck Test User',
        vai_tro: 'student'
      },
      {
        email: 'admin.test@polydeck.com',
        password: 'admin123',
        ho_ten: 'PolyDeck Test Admin',
        vai_tro: 'admin'
      }
    ];

    for (const acc of accounts) {
      let user = await NguoiDung.findOne({ email: acc.email });
      const salt = await bcrypt.genSalt(10);
      const hashedPassword = await bcrypt.hash(acc.password, salt);

      if (!user) {
        user = new NguoiDung({
          ma_nguoi_dung: `${acc.vai_tro.toUpperCase()}_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`,
          ho_ten: acc.ho_ten,
          email: acc.email,
          mat_khau_hash: hashedPassword,
          vai_tro: acc.vai_tro,
          trang_thai: 'active',
          email_verified: true,
          email_verification_token: null,
          email_verification_expire: null
        });
        await user.save();
        console.log(`✓ Đã tạo tài khoản test ${acc.vai_tro}: ${acc.email} / ${acc.password}`);
      } else {
        // Cập nhật để đảm bảo có thể đăng nhập khi test
        user.ho_ten = acc.ho_ten;
        user.vai_tro = acc.vai_tro;
        user.trang_thai = 'active';
        user.email_verified = true;
        user.email_verification_token = null;
        user.email_verification_expire = null;
        user.mat_khau_hash = hashedPassword;
        await user.save();
        console.log(`✓ Đã cập nhật tài khoản test ${acc.vai_tro}: ${acc.email} / ${acc.password}`);
      }
    }
  } catch (error) {
    console.error('Lỗi khi tạo tài khoản test:', error.message);
  }
};

// Tạo dữ liệu mẫu: chủ đề + từ vựng cơ bản
const createSampleTopicsAndVocabulary = async () => {
  try {
    const ChuDe = require('./models/ChuDe');
    const TuVung = require('./models/TuVung');

    const count = await ChuDe.countDocuments();
    if (count > 0) return; // đã có dữ liệu

    const topics = [
      { ten_chu_de: 'Động vật' },
      { ten_chu_de: 'Thực phẩm' },
      { ten_chu_de: 'Thể thao' },
      { ten_chu_de: 'Gia đình' },
      { ten_chu_de: 'Quần áo' },
      { ten_chu_de: 'Công việc' },
    ];

    for (const t of topics) {
      const chuDe = new ChuDe({
        ma_chu_de: `CD_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
        ten_chu_de: t.ten_chu_de,
        link_anh_icon: null // dùng icon mặc định phía client
      });
      await chuDe.save();

      const maChuDe = chuDe.ma_chu_de;
      const sampleVocab = (() => {
        switch (t.ten_chu_de) {
          case 'Động vật':
            return [
              { en: 'Cat', pa: '/kæt/', vi: 'Con mèo' },
              { en: 'Dog', pa: '/dɒɡ/', vi: 'Con chó' },
              { en: 'Bird', pa: '/bɜːd/', vi: 'Con chim' },
            ];
          case 'Thực phẩm':
            return [
              { en: 'Apple', pa: '/ˈæp.əl/', vi: 'Quả táo' },
              { en: 'Bread', pa: '/brɛd/', vi: 'Bánh mì' },
              { en: 'Milk', pa: '/mɪlk/', vi: 'Sữa' },
            ];
          default:
            return [
              { en: 'Hello', pa: '/həˈləʊ/', vi: 'Xin chào' },
              { en: 'Goodbye', pa: '/ˌɡʊdˈbaɪ/', vi: 'Tạm biệt' },
            ];
        }
      })();

      for (const v of sampleVocab) {
        const tv = new TuVung({
          ma_tu_vung: `TV_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
          ma_chu_de: maChuDe,
          tu_tieng_anh: v.en,
          phien_am: v.pa,
          nghia_tieng_viet: v.vi,
          link_anh: null
        });
        await tv.save();
      }

      await ChuDe.updateOne({ _id: chuDe._id }, { $set: { so_luong_tu: sampleVocab.length } });
    }
    console.log('✓ Đã tạo dữ liệu mẫu: chủ đề và từ vựng');
  } catch (e) {
    console.error('Lỗi tạo dữ liệu mẫu chủ đề/từ vựng:', e.message);
  }
};

// Thông báo mẫu
const createSampleNotifications = async () => {
  try {
    const ThongBao = require('./models/ThongBao');
    const count = await ThongBao.countDocuments();
    if (count > 0) return;
    const now = Date.now();
    const items = [
      { ma_thong_bao: `TB_${now}_1`, tieu_de: 'Chào mừng đến PolyDeck!', noi_dung: 'Bắt đầu chinh phục từ vựng ngay hôm nay.' },
      { ma_thong_bao: `TB_${now}_2`, tieu_de: 'Chủ đề mới', noi_dung: 'Đã thêm chủ đề Thực phẩm.' },
      { ma_thong_bao: `TB_${now}_3`, tieu_de: 'Nhận thưởng', noi_dung: 'Bạn nhận +150 XP khi hoàn thành bài đầu tiên.' },
    ];
    for (const i of items) {
      await new (require('./models/ThongBao'))({
        ma_thong_bao: i.ma_thong_bao,
        ma_nguoi_dung: null,
        tieu_de: i.tieu_de,
        noi_dung: i.noi_dung
      }).save();
    }
    console.log('✓ Đã tạo thông báo mẫu');
  } catch (e) {
    console.error('Lỗi tạo thông báo mẫu:', e.message);
  }
};

// Tạo lịch sử làm bài mẫu cho user.test
const createSampleHistory = async () => {
  try {
    const NguoiDung = require('./models/NguoiDung');
    const LichSuLamBai = require('./models/LichSuLamBai');
    const BaiQuiz = require('./models/BaiQuiz');

    const user = await NguoiDung.findOne({ email: 'user.test@polydeck.com' });
    if (!user) return;

    const quiz = await BaiQuiz.findOne();
    if (!quiz) return;

    const existed = await LichSuLamBai.findOne({ ma_nguoi_dung: user.ma_nguoi_dung });
    if (existed) return;

    const records = [
      { score: 80, correct: 8, time: 210 },
      { score: 67, correct: 2, time: 272 },
    ];
    for (const r of records) {
      await new LichSuLamBai({
        ma_lich_su: `HIS_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
        ma_nguoi_dung: user.ma_nguoi_dung,
        ma_quiz: quiz.ma_quiz,
        ma_chu_de: quiz.ma_chu_de,
        diem_so: r.score,
        diem_danh_duoc: r.correct,
        thoi_gian_lam_bai: r.time
      }).save();
    }
    console.log('✓ Đã tạo lịch sử làm bài mẫu cho user.test');
  } catch (e) {
    console.error('Lỗi tạo lịch sử làm bài mẫu:', e.message);
  }
};

// Tạo quiz mẫu và câu hỏi nếu chưa có, cho chủ đề đầu tiên
const createSampleQuizIfMissing = async () => {
  try {
    const ChuDe = require('./models/ChuDe');
    const BaiQuiz = require('./models/BaiQuiz');
    const CauHoi = require('./models/CauHoi');

    const anyChuDe = await ChuDe.findOne();
    if (!anyChuDe) return;

    const maChuDe = anyChuDe.ma_chu_de || anyChuDe._id.toString();
    let quiz = await BaiQuiz.findOne({ ma_chu_de: maChuDe });
    if (!quiz) {
      quiz = new (require('./models/BaiQuiz'))({
        ma_quiz: `QZ_${Date.now()}`,
        ma_chu_de: maChuDe,
        tieu_de: `Quiz cho ${anyChuDe.ten_chu_de}`
      });
      await quiz.save();

      const questions = [
        {
          ma_cau_hoi: `Q_${Date.now()}_1`,
          noi_dung_cau_hoi: 'Cat nghĩa là gì?',
          options: [
            { ma_lua_chon: 'A', noi_dung: 'Con mèo' },
            { ma_lua_chon: 'B', noi_dung: 'Con chó' },
            { ma_lua_chon: 'C', noi_dung: 'Con chim' },
            { ma_lua_chon: 'D', noi_dung: 'Con cá' }
          ],
          correct: 'A'
        },
        {
          ma_cau_hoi: `Q_${Date.now()}_2`,
          noi_dung_cau_hoi: 'Dog nghĩa là gì?',
          options: [
            { ma_lua_chon: 'A', noi_dung: 'Con mèo' },
            { ma_lua_chon: 'B', noi_dung: 'Con chó' },
            { ma_lua_chon: 'C', noi_dung: 'Con chim' },
            { ma_lua_chon: 'D', noi_dung: 'Con cá' }
          ],
          correct: 'B'
        }
      ];

      for (const q of questions) {
        const c = new CauHoi({
          ma_cau_hoi: q.ma_cau_hoi,
          ma_quiz: quiz.ma_quiz,
          ma_chu_de: maChuDe,
          noi_dung_cau_hoi: q.noi_dung_cau_hoi,
          dap_an_lua_chon: q.options.map(o => ({ ma_lua_chon: o.ma_lua_chon, noi_dung: o.noi_dung })),
          dap_an_dung: q.correct
        });
        await c.save();
      }
      console.log('✓ Đã tạo quiz mẫu và câu hỏi cho chủ đề:', anyChuDe.ten_chu_de);
    }
  } catch (e) {
    console.error('Lỗi khi tạo quiz mẫu:', e.message);
  }
};

app.use(cors()); 
app.use(express.json()); 
app.use(express.urlencoded({ extended: true }));

app.use(express.static('public'));

app.use('/api/auth', require('./routes/auth'));
app.use('/api/users', require('./routes/nguoiDung'));      
app.use('/api/chude', require('./routes/chuDe'));         
app.use('/api/admin', require('./routes/admin'));

// Debug/info endpoint: returns machine LAN IPs to help mobile testing
app.get('/api/info', (req, res) => {
  try {
    const os = require('os');
    const ifaces = os.networkInterfaces();
    const ips = [];
    Object.keys(ifaces).forEach(ifname => {
      for (const iface of ifaces[ifname]) {
        if (iface.family === 'IPv4' && !iface.internal) {
          ips.push({ iface: ifname, address: iface.address });
        }
      }
    });
    res.json({ host: req.hostname, ips });
  } catch (e) {
    res.json({ error: e.message });
  }
});
// app.use('/api/tuvung', require('./routes/tuVung'));      
app.use('/api/quizzes', require('./routes/quiz'));        
// app.use('/api/hotro', require('./routes/hoTro'));         
app.use('/api/thongbao', require('./routes/thongBao'));   

const DEFAULT_PORT = parseInt(process.env.PORT, 10) || 3000;

// Try to start server on DEFAULT_PORT, if it's in use try next ports up to a limit
const startServer = async (port = DEFAULT_PORT, maxAttempts = 10) => {
  try {
    await connectDB();
    await createDefaultAdmin();
    await createTestAccounts();
    await createSampleTopicsAndVocabulary();
    await createSampleQuizIfMissing();
    await createSampleNotifications();
    await createSampleHistory();

    const attemptListen = (p, attemptsLeft) => {
      const server = app.listen(p, '0.0.0.0')
        .on('listening', () => {
          console.log('═══════════════════════════════════════');
          console.log(`✓ Server đang chạy trên port ${p}`);
          console.log(`✓ API: http://localhost:${p}`);
          try {
            const os = require('os');
            const ifaces = os.networkInterfaces();
            Object.keys(ifaces).forEach(ifname => {
              for (const iface of ifaces[ifname]) {
                if (iface.family === 'IPv4' && !iface.internal) {
                  console.log(`  => LAN: http://${iface.address}:${p}`);
                }
              }
            });
          } catch (e) {}
          console.log('═══════════════════════════════════════');
        })
        .on('error', (err) => {
          if (err.code === 'EADDRINUSE' && attemptsLeft > 0) {
            // If initial requested port (DEFAULT_PORT) is in use, try to free it on Windows by
            // finding the PID and killing it. This helps ensure the server runs on port 3000.
            if (p === DEFAULT_PORT) {
              try {
                console.warn(`Port ${p} is in use. Attempting to find and kill owning process...`);
                const cmd = `netstat -ano | findstr :${p}`;
                const out = execSync(cmd, { encoding: 'utf8' });
                // Parse PID from output (last column)
                const lines = out.split(/\r?\n/).map(l => l.trim()).filter(Boolean);
                for (const line of lines) {
                  const parts = line.split(/\s+/);
                  const pid = parts[parts.length - 1];
                  if (pid && !isNaN(pid)) {
                    try {
                      console.warn(`Killing PID ${pid} that is using port ${p}...`);
                      execSync(`taskkill /PID ${pid} /F`);
                      console.warn(`PID ${pid} killed.`);
                    } catch (killErr) {
                      console.error('Không thể kill PID:', pid, killErr.message);
                    }
                  }
                }
              } catch (findErr) {
                console.error('Không tìm thấy tiến trình dùng port hoặc lỗi khi chạy netstat:', findErr.message);
              }
            }

            console.warn(`Port ${p} is in use, trying port ${p + 1}...`);
            setTimeout(() => attemptListen(p + 1, attemptsLeft - 1), 200);
          } else {
            console.error('Lỗi khi lắng nghe cổng:', err);
            process.exit(1);
          }
        });
    };

    attemptListen(port, maxAttempts);

    // Graceful shutdown handlers
    const shutdown = () => {
      console.log('Shutting down server...');
      process.exit(0);
    };
    process.on('SIGINT', shutdown);
    process.on('SIGTERM', shutdown);
    process.on('unhandledRejection', (reason) => {
      console.error('Unhandled Rejection:', reason);
    });
    process.on('uncaughtException', (err) => {
      console.error('Uncaught Exception:', err);
    });

  } catch (error) {
    console.error('Lỗi không thể khởi động server:', error);
    process.exit(1);
  }
};

startServer();