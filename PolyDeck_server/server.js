const express = require('express');
const cors = require('cors');
const connectDB = require('./config/database');
require('dotenv').config();

const app = express();

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Connect to MongoDB
connectDB();

// Routes (sẽ thêm sau)
app.get('/', (req, res) => {
  res.json({ message: 'PolyDeck API Server is running!' });
});

// Import models để đảm bảo chúng được load
require('./models/ChuDe');
require('./models/TuVung');
require('./models/NguoiDung');
require('./models/CauHoi');
require('./models/LichSuLamBai');
require('./models/ChiTietLamBai');
require('./models/ThongBao');
require('./models/ThongBaoDaDoc');

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});

