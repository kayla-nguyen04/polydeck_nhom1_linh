const express = require('express');
const cors = require('cors');
const connectDB = require('./config/database');
const bcrypt = require('bcryptjs');
const NguoiDung = require('./models/NguoiDung');
require('dotenv').config();
const { execSync } = require('child_process');

const app = express();

app.use((req, res, next) => {
  try {
    const ip = req.headers['x-forwarded-for'] || req.socket.remoteAddress || req.ip;
    console.log(`${new Date().toISOString()} - ${req.method} ${req.originalUrl} - from ${ip}`);
  } catch (e) {}
  next();
});







app.use(cors()); 
app.use(express.json()); 
app.use(express.urlencoded({ extended: true }));

app.use(express.static('public'));

app.use('/api/auth', require('./routes/auth'));
app.use('/api/users', require('./routes/nguoiDung'));      
app.use('/api/chude', require('./routes/chuDe'));         
app.use('/api/admin', require('./routes/admin'));
app.use('/quiz', require('./routes/quiz'));



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
app.use('/api/hotro', require('./routes/hoTro'));         
app.use('/api/thongbao', require('./routes/thongBao'));   

const DEFAULT_PORT = parseInt(process.env.PORT, 10) || 3000;

const startServer = async (port = DEFAULT_PORT, maxAttempts = 10) => {
  try {
    await connectDB();

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
            if (p === DEFAULT_PORT) {
              try {
                console.warn(`Port ${p} is in use. Attempting to find and kill owning process...`);
                const cmd = `netstat -ano | findstr :${p}`;
                const out = execSync(cmd, { encoding: 'utf8' });
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