const ThongBao = require('../models/ThongBao');

// GET /api/thongbao?ma_nguoi_dung=USER_ID
// Trả về thông báo chung (ma_nguoi_dung=null) + thông báo cá nhân của user
const listForUser = async (req, res) => {
  try {
    const userId = (req.query.ma_nguoi_dung || '').trim();
    const filter = userId
      ? { $or: [{ ma_nguoi_dung: null }, { ma_nguoi_dung: userId }] }
      : { ma_nguoi_dung: null };

    const items = await ThongBao.find(filter).sort({ ngay_gui: -1 }).lean();

    res.json({
      success: true,
      data: items
    });
  } catch (e) {
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

