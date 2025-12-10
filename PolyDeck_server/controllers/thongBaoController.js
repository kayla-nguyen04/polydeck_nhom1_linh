const ThongBao = require('../models/ThongBao');

// GET /api/thongbao?ma_nguoi_dung=USER_ID hoặc /api/thongbao/:userId
// Trả về thông báo chung (ma_nguoi_dung=null) + thông báo cá nhân của user
const listForUser = async (req, res) => {
  try {
    // Support both query param and path param
    let userId = req.params.userId || req.query.ma_nguoi_dung || '';
    if (userId) userId = userId.trim();
    
    const mongoose = require('mongoose');
    let filter;
    
    if (userId && mongoose.Types.ObjectId.isValid(userId)) {
      // User has ID, get both general and personal notifications
      filter = { 
        $or: [
          { ma_nguoi_dung: null }, // General notifications
          { ma_nguoi_dung: new mongoose.Types.ObjectId(userId) } // Personal notifications
        ] 
      };
    } else {
      // No userId, only general notifications
      filter = { ma_nguoi_dung: null };
    }

    const items = await ThongBao.find(filter).sort({ ngay_gui: -1 }).lean();
    
    // Format response - convert ObjectId to string and Date to ISO string
    const formattedItems = items.map(item => ({
      _id: item._id ? item._id.toString() : null,
      ma_nguoi_dung: item.ma_nguoi_dung ? item.ma_nguoi_dung.toString() : null,
      tieu_de: item.tieu_de || '',
      noi_dung: item.noi_dung || '',
      ngay_gui: item.ngay_gui ? item.ngay_gui.toISOString() : new Date().toISOString(),
      da_doc_cho: item.da_doc_cho || []
    }));

    res.json({
      success: true,
      data: formattedItems
    });
  } catch (e) {
    console.error('Error in listForUser:', e);
    res.status(500).json({ success: false, message: e.message });
  }
};

// POST /api/thongbao/:id/read { ma_nguoi_dung }
const markAsRead = async (req, res) => {
  try {
    const id = req.params.id;
    const { ma_nguoi_dung } = req.body || {};
    if (!ma_nguoi_dung) {
      return res.status(400).json({ success: false, message: 'Thiếu ma_nguoi_dung' });
    }

    const doc = await ThongBao.findOne({ _id: id });
    if (!doc) return res.status(404).json({ success: false, message: 'Không tìm thấy thông báo' });

    if (!doc.da_doc_cho.includes(ma_nguoi_dung)) {
      doc.da_doc_cho.push(ma_nguoi_dung);
      await doc.save();
    }

    res.json({ success: true });
  } catch (e) {
    res.status(500).json({ success: false, message: e.message });
  }
};

module.exports = { listForUser, markAsRead };

