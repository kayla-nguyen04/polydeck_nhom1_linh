const express = require('express');
const router = express.Router();
const { getQuizByTopic, submitQuiz, getHistoryByUser } = require('../controllers/quizController');

router.get('/by-topic/:ma_chu_de', getQuizByTopic);
router.post('/submit', submitQuiz);
router.get('/history/:ma_nguoi_dung', getHistoryByUser);

module.exports = router;

