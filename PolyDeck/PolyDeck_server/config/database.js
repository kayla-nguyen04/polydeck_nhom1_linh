const mongoose = require('mongoose');
require('dotenv').config();

const connectDB = async () => {
  try {
    const mongoURI = process.env.MONGODB_URI || 'mongodb://127.0.0.1:27017/polydeck';
    
    console.log('Đang kết nối đến MongoDB...');
    console.log(`URI: ${mongoURI}`);
    
    const conn = await mongoose.connect(mongoURI, {
      // Các options mới cho Mongoose 6+
      // useNewUrlParser và useUnifiedTopology không cần thiết nữa
    });

    // Kiểm tra trạng thái kết nối
    if (mongoose.connection.readyState === 1) {
      console.log('═══════════════════════════════════════');
      console.log('✓ MongoDB đã kết nối thành công!');
      console.log(`✓ Host: ${conn.connection.host}`);
      console.log(`✓ Database: ${conn.connection.name}`);
      console.log(`✓ Port: ${conn.connection.port}`);
      console.log('═══════════════════════════════════════');
    }
    
    // Tự động tạo database và collections bằng cách insert document vào mỗi collection
    await initDatabase(conn.connection.db);
    
    // Lắng nghe sự kiện kết nối
    mongoose.connection.on('connected', () => {
      console.log('✓ MongoDB đã kết nối lại');
    });
    
    mongoose.connection.on('error', (err) => {
      console.error('✗ MongoDB connection error:', err);
    });
    
    mongoose.connection.on('disconnected', () => {
      console.warn('⚠ MongoDB đã ngắt kết nối');
    });
    
  } catch (error) {
    console.error('═══════════════════════════════════════');
    console.error('✗ Lỗi kết nối MongoDB:');
    console.error(`✗ ${error.message}`);
    console.error('═══════════════════════════════════════');
    process.exit(1);
  }
};

// Hàm khởi tạo database và collections
const initDatabase = async (db) => {
  try {
    const collections = [
      'chu_de',
      'tu_vung',
      'nguoi_dung',
      'tien_do_hoc_tap',
      'bai_quiz',
      'cau_hoi',
      'lich_su_lam_bai',
      'thong_bao',
      'yeu_cau_ho_tro'
    ];
    
    const existingCollections = await db.listCollections().toArray();
    const existingCollectionNames = existingCollections.map(col => col.name);
    
    // Tạo collections nếu chưa tồn tại bằng cách insert một document rỗng và xóa ngay
    for (const collectionName of collections) {
      if (!existingCollectionNames.includes(collectionName)) {
        await db.collection(collectionName).insertOne({ _init: true });
        await db.collection(collectionName).deleteOne({ _init: true });
        console.log(`✓ Collection '${collectionName}' đã được tạo`);
      }
    }
    
    console.log('✓ Database và collections đã sẵn sàng!');
    console.log('');
  } catch (error) {
    console.error('Error initializing database:', error.message);
  }
};

module.exports = connectDB;

