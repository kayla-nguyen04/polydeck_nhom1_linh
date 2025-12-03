package com.nhom1.polydeck.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.Answer;
import com.nhom1.polydeck.data.model.ApiResponse;
import com.nhom1.polydeck.data.model.BaiQuiz;
import com.nhom1.polydeck.data.model.BoTu;
import com.nhom1.polydeck.data.model.Question;
import com.nhom1.polydeck.data.model.Quiz;
import com.nhom1.polydeck.data.model.QuizBundle;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateQuizActivity extends AppCompatActivity {

    private static final String TAG = "CreateQuizActivity";
    private static final int PICK_EXCEL_FILE_REQUEST = 2;

    private ImageView btnBack, btnSaveHeader;
    private Spinner spinnerDecks;
    private LinearLayout containerQuestions;
    private LinearLayout containerQuestionTabs;
    private MaterialButton btnAddQuestion, btnSaveQuiz;
    private TextView btnImportExcel, tvTitle;

    private APIService apiService;
    private List<BoTu> deckList = new ArrayList<>();
    private List<View> questionViews = new ArrayList<>();
    private List<View> tabViews = new ArrayList<>();
    private int currentSelectedQuestionIndex = -1;
    private BoTu selectedDeck;
    private String currentQuizId; // For edit mode

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_quiz);

        apiService = RetrofitClient.getApiService();

        initViews();
        setupToolbar();
        setupListeners();

        // Check if editing existing quiz
        currentQuizId = getIntent().getStringExtra("QUIZ_ID");
        String deckId = getIntent().getStringExtra("DECK_ID");
        
        fetchDecksForSpinner();
        
        // If editing, load quiz data after decks are loaded
        if (currentQuizId != null && !currentQuizId.isEmpty()) {
            // Will load quiz after decks are fetched
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnSaveHeader = findViewById(R.id.btnSaveHeader);
        tvTitle = findViewById(R.id.tvTitle);
        spinnerDecks = findViewById(R.id.spinnerDecks);
        containerQuestions = findViewById(R.id.containerQuestions);
        containerQuestionTabs = findViewById(R.id.containerQuestionTabs);
        btnAddQuestion = findViewById(R.id.btnAddQuestion);
        btnSaveQuiz = findViewById(R.id.btnSaveQuiz);
        btnImportExcel = findViewById(R.id.btnImportExcel);
        
        // Update title if editing
        if (currentQuizId != null && !currentQuizId.isEmpty()) {
            tvTitle.setText("Sửa Quiz");
        }
    }

    private void setupToolbar() {
        btnBack.setOnClickListener(v -> onBackPressed());
        btnSaveHeader.setOnClickListener(v -> saveQuiz());
    }

    private void setupListeners() {
        btnAddQuestion.setOnClickListener(v -> addQuestionView());
        btnSaveQuiz.setOnClickListener(v -> saveQuiz());
        btnImportExcel.setOnClickListener(v -> openFileChooser());
    }
    
    private void openFileChooser() {
        if (selectedDeck == null) {
            Toast.makeText(this, "Vui lòng chọn bộ từ trước khi import", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); // .xlsx
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Chọn tệp Excel"), PICK_EXCEL_FILE_REQUEST);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Vui lòng cài đặt một trình quản lý tệp.", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_EXCEL_FILE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            readExcelFile(uri);
        }
    }
    
    private void readExcelFile(Uri uri) {
        new Thread(() -> {
            List<Question> questionList = new ArrayList<>();
            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {

                XSSFSheet sheet = workbook.getSheetAt(0);
                Iterator<Row> rowIterator = sheet.iterator();

                // Skip header row if it exists
                if (rowIterator.hasNext()) {
                    Row firstRow = rowIterator.next();
                    Cell firstCell = firstRow.getCell(0);
                    if (firstCell != null) {
                        String firstCellValue = getCellValueAsString(firstCell).toLowerCase();
                        if (firstCellValue.contains("câu hỏi") || firstCellValue.contains("question") ||
                            firstCellValue.contains("đáp án") || firstCellValue.contains("answer") ||
                            firstCellValue.contains("đúng") || firstCellValue.contains("correct")) {
                            // This is a header row, skip it
                        } else {
                            // This is data, process it
                            rowIterator = sheet.iterator(); // Reset iterator
                            processRow(firstRow, questionList);
                        }
                    }
                }

                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    processRow(row, questionList);
                }

                if (!questionList.isEmpty()) {
                    runOnUiThread(() -> uploadQuizList(questionList));
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Không tìm thấy dữ liệu hợp lệ trong tệp Excel.", Toast.LENGTH_LONG).show();
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error reading Excel file", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Lỗi khi đọc tệp Excel: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    private void processRow(Row row, List<Question> questionList) {
        // Format: Câu hỏi | Đáp án A | Đáp án B | Đáp án C | Đáp án D | Đáp án đúng (A/B/C/D hoặc 1/2/3/4)
        Cell cell1 = row.getCell(0); // Câu hỏi
        Cell cell2 = row.getCell(1); // Đáp án A
        Cell cell3 = row.getCell(2); // Đáp án B
        Cell cell4 = row.getCell(3); // Đáp án C
        Cell cell5 = row.getCell(4); // Đáp án D
        Cell cell6 = row.getCell(5); // Đáp án đúng

        if (cell1 != null && cell2 != null && cell3 != null && cell4 != null && cell5 != null && cell6 != null) {
            String questionText = getCellValueAsString(cell1).trim();
            String answerA = getCellValueAsString(cell2).trim();
            String answerB = getCellValueAsString(cell3).trim();
            String answerC = getCellValueAsString(cell4).trim();
            String answerD = getCellValueAsString(cell5).trim();
            String correctAnswer = getCellValueAsString(cell6).trim().toUpperCase();

            if (!questionText.isEmpty() && !answerA.isEmpty() && !answerB.isEmpty() && 
                !answerC.isEmpty() && !answerD.isEmpty() && !correctAnswer.isEmpty()) {
                
                // Determine which answer is correct
                int correctIndex = -1;
                if (correctAnswer.equals("A") || correctAnswer.equals("1")) {
                    correctIndex = 0;
                } else if (correctAnswer.equals("B") || correctAnswer.equals("2")) {
                    correctIndex = 1;
                } else if (correctAnswer.equals("C") || correctAnswer.equals("3")) {
                    correctIndex = 2;
                } else if (correctAnswer.equals("D") || correctAnswer.equals("4")) {
                    correctIndex = 3;
                }
                
                if (correctIndex == -1) {
                    Log.w(TAG, "Invalid correct answer value: " + correctAnswer + ". Skipping row.");
                    return;
                }
                
                List<Answer> answers = new ArrayList<>();
                answers.add(new Answer(answerA, correctIndex == 0));
                answers.add(new Answer(answerB, correctIndex == 1));
                answers.add(new Answer(answerC, correctIndex == 2));
                answers.add(new Answer(answerD, correctIndex == 3));
                
                questionList.add(new Question(questionText, answers));
            }
        }
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == Math.floor(numericValue)) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    return cell.getCellFormula();
                default:
                    return "";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading cell value: " + e.getMessage());
            return "";
        }
    }
    
    private void uploadQuizList(List<Question> questionList) {
        if (selectedDeck == null) {
            Toast.makeText(this, "Vui lòng chọn một bộ từ", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Quiz quiz = new Quiz(selectedDeck.getId(), questionList);
        
        // Check if quiz already exists before importing
        checkQuizExistsThenCreate(quiz);
    }
    
    private void loadQuizForEdit() {
        if (currentQuizId == null || currentQuizId.isEmpty()) {
            return;
        }
        
        apiService.getQuizById(currentQuizId).enqueue(new Callback<BaiQuiz>() {
            @Override
            public void onResponse(@NonNull Call<BaiQuiz> call, @NonNull Response<BaiQuiz> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaiQuiz quiz = response.body();
                    
                    // Select the deck
                    String deckId = quiz.getMaChuDe();
                    for (int i = 0; i < deckList.size(); i++) {
                        if (deckList.get(i).getId().equals(deckId)) {
                            spinnerDecks.setSelection(i);
                            selectedDeck = deckList.get(i);
                            break;
                        }
                    }
                    
                    // Load questions
                    if (quiz.getQuestions() != null && !quiz.getQuestions().isEmpty()) {
                        for (Question question : quiz.getQuestions()) {
                            addQuestionViewWithData(question);
                        }
                        // Show first question
                        if (!questionViews.isEmpty()) {
                            showQuestion(0);
                        }
                    }
                } else {
                    Toast.makeText(CreateQuizActivity.this, "Không thể tải quiz để chỉnh sửa", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<BaiQuiz> call, @NonNull Throwable t) {
                Toast.makeText(CreateQuizActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void addQuestionViewWithData(Question question) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View questionView = inflater.inflate(R.layout.item_quiz_question, containerQuestions, false);
        
        int questionIndex = questionViews.size();
        TextView tvQuestionNumber = questionView.findViewById(R.id.tvQuestionNumber);
        tvQuestionNumber.setText("Câu hỏi " + (questionIndex + 1));
        
        // Populate question data
        EditText etQuestionText = questionView.findViewById(R.id.etQuestionText);
        etQuestionText.setText(question.getQuestionText());
        
        List<Answer> answers = question.getAnswers();
        if (answers != null && answers.size() >= 4) {
            EditText etAnswer1 = questionView.findViewById(R.id.etAnswer1);
            EditText etAnswer2 = questionView.findViewById(R.id.etAnswer2);
            EditText etAnswer3 = questionView.findViewById(R.id.etAnswer3);
            EditText etAnswer4 = questionView.findViewById(R.id.etAnswer4);
            
            etAnswer1.setText(answers.get(0).getAnswerText());
            etAnswer2.setText(answers.get(1).getAnswerText());
            etAnswer3.setText(answers.get(2).getAnswerText());
            etAnswer4.setText(answers.get(3).getAnswerText());
            
            // Set correct answer
            RadioButton rbAnswer1 = questionView.findViewById(R.id.rbAnswer1);
            RadioButton rbAnswer2 = questionView.findViewById(R.id.rbAnswer2);
            RadioButton rbAnswer3 = questionView.findViewById(R.id.rbAnswer3);
            RadioButton rbAnswer4 = questionView.findViewById(R.id.rbAnswer4);
            
            if (answers.get(0).isCorrect()) rbAnswer1.setChecked(true);
            else if (answers.get(1).isCorrect()) rbAnswer2.setChecked(true);
            else if (answers.get(2).isCorrect()) rbAnswer3.setChecked(true);
            else if (answers.get(3).isCorrect()) rbAnswer4.setChecked(true);
        }
        
        // Setup RadioGroup listeners (same as addQuestionView)
        RadioGroup rgAnswers = questionView.findViewById(R.id.rgAnswers);
        RadioButton rbAnswer1 = questionView.findViewById(R.id.rbAnswer1);
        RadioButton rbAnswer2 = questionView.findViewById(R.id.rbAnswer2);
        RadioButton rbAnswer3 = questionView.findViewById(R.id.rbAnswer3);
        RadioButton rbAnswer4 = questionView.findViewById(R.id.rbAnswer4);
        
        View answerLayout1 = (View) rbAnswer1.getParent();
        View answerLayout2 = (View) rbAnswer2.getParent();
        View answerLayout3 = (View) rbAnswer3.getParent();
        View answerLayout4 = (View) rbAnswer4.getParent();
        
        answerLayout1.setOnClickListener(v -> {
            rbAnswer2.setChecked(false);
            rbAnswer3.setChecked(false);
            rbAnswer4.setChecked(false);
            rbAnswer1.setChecked(true);
        });
        
        answerLayout2.setOnClickListener(v -> {
            rbAnswer1.setChecked(false);
            rbAnswer3.setChecked(false);
            rbAnswer4.setChecked(false);
            rbAnswer2.setChecked(true);
        });
        
        answerLayout3.setOnClickListener(v -> {
            rbAnswer1.setChecked(false);
            rbAnswer2.setChecked(false);
            rbAnswer4.setChecked(false);
            rbAnswer3.setChecked(true);
        });
        
        answerLayout4.setOnClickListener(v -> {
            rbAnswer1.setChecked(false);
            rbAnswer2.setChecked(false);
            rbAnswer3.setChecked(false);
            rbAnswer4.setChecked(true);
        });
        
        rbAnswer1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                rbAnswer2.setChecked(false);
                rbAnswer3.setChecked(false);
                rbAnswer4.setChecked(false);
            }
        });
        
        rbAnswer2.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                rbAnswer1.setChecked(false);
                rbAnswer3.setChecked(false);
                rbAnswer4.setChecked(false);
            }
        });
        
        rbAnswer3.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                rbAnswer1.setChecked(false);
                rbAnswer2.setChecked(false);
                rbAnswer4.setChecked(false);
            }
        });
        
        rbAnswer4.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                rbAnswer1.setChecked(false);
                rbAnswer2.setChecked(false);
                rbAnswer3.setChecked(false);
            }
        });
        
        questionViews.add(questionView);
        containerQuestions.addView(questionView);
        
        // Create tab for this question
        View tabView = inflater.inflate(R.layout.item_question_tab, containerQuestionTabs, false);
        TextView tvTabQuestion = tabView.findViewById(R.id.tvTabQuestion);
        tvTabQuestion.setText("Câu " + (questionIndex + 1));
        
        final int finalIndex = questionIndex;
        tabView.setOnClickListener(v -> showQuestion(finalIndex));
        
        tabViews.add(tabView);
        containerQuestionTabs.addView(tabView);
    }
    
    private void fetchDecksForSpinner() {
        apiService.getAllChuDe().enqueue(new Callback<List<BoTu>>() {
            @Override
            public void onResponse(@NonNull Call<List<BoTu>> call, @NonNull Response<List<BoTu>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    deckList = response.body();
                    List<String> deckNames = deckList.stream().map(BoTu::getTenChuDe).collect(Collectors.toList());
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(CreateQuizActivity.this, android.R.layout.simple_spinner_item, deckNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerDecks.setAdapter(adapter);
                    spinnerDecks.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedDeck = deckList.get(position);
                        }
                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            selectedDeck = null;
                        }
                    });
                    
                    // If editing, load quiz data
                    if (currentQuizId != null && !currentQuizId.isEmpty()) {
                        loadQuizForEdit();
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<BoTu>> call, @NonNull Throwable t) {
                Toast.makeText(CreateQuizActivity.this, "Không thể tải danh sách bộ từ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addQuestionView() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View questionView = inflater.inflate(R.layout.item_quiz_question, containerQuestions, false);
        
        int questionIndex = questionViews.size();
        TextView tvQuestionNumber = questionView.findViewById(R.id.tvQuestionNumber);
        tvQuestionNumber.setText("Câu hỏi " + (questionIndex + 1));
        
        // Setup RadioGroup to ensure only one answer can be selected
        RadioGroup rgAnswers = questionView.findViewById(R.id.rgAnswers);
        RadioButton rbAnswer1 = questionView.findViewById(R.id.rbAnswer1);
        RadioButton rbAnswer2 = questionView.findViewById(R.id.rbAnswer2);
        RadioButton rbAnswer3 = questionView.findViewById(R.id.rbAnswer3);
        RadioButton rbAnswer4 = questionView.findViewById(R.id.rbAnswer4);
        
        // Find parent LinearLayouts and set click listeners to select corresponding RadioButton
        View answerLayout1 = (View) rbAnswer1.getParent();
        View answerLayout2 = (View) rbAnswer2.getParent();
        View answerLayout3 = (View) rbAnswer3.getParent();
        View answerLayout4 = (View) rbAnswer4.getParent();
        
        // Set click listeners on LinearLayouts to check corresponding RadioButton
        answerLayout1.setOnClickListener(v -> {
            rbAnswer2.setChecked(false);
            rbAnswer3.setChecked(false);
            rbAnswer4.setChecked(false);
            rbAnswer1.setChecked(true);
        });
        
        answerLayout2.setOnClickListener(v -> {
            rbAnswer1.setChecked(false);
            rbAnswer3.setChecked(false);
            rbAnswer4.setChecked(false);
            rbAnswer2.setChecked(true);
        });
        
        answerLayout3.setOnClickListener(v -> {
            rbAnswer1.setChecked(false);
            rbAnswer2.setChecked(false);
            rbAnswer4.setChecked(false);
            rbAnswer3.setChecked(true);
        });
        
        answerLayout4.setOnClickListener(v -> {
            rbAnswer1.setChecked(false);
            rbAnswer2.setChecked(false);
            rbAnswer3.setChecked(false);
            rbAnswer4.setChecked(true);
        });
        
        // Set listeners on RadioButtons themselves to ensure mutual exclusivity
        rbAnswer1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                rbAnswer2.setChecked(false);
                rbAnswer3.setChecked(false);
                rbAnswer4.setChecked(false);
            }
        });
        
        rbAnswer2.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                rbAnswer1.setChecked(false);
                rbAnswer3.setChecked(false);
                rbAnswer4.setChecked(false);
            }
        });
        
        rbAnswer3.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                rbAnswer1.setChecked(false);
                rbAnswer2.setChecked(false);
                rbAnswer4.setChecked(false);
            }
        });
        
        rbAnswer4.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                rbAnswer1.setChecked(false);
                rbAnswer2.setChecked(false);
                rbAnswer3.setChecked(false);
            }
        });
        
        questionViews.add(questionView);
        containerQuestions.addView(questionView);
        
        // Create tab for this question
        View tabView = inflater.inflate(R.layout.item_question_tab, containerQuestionTabs, false);
        TextView tvTabQuestion = tabView.findViewById(R.id.tvTabQuestion);
        tvTabQuestion.setText("Câu " + (questionIndex + 1));
        
        final int finalIndex = questionIndex;
        tabView.setOnClickListener(v -> showQuestion(finalIndex));
        
        tabViews.add(tabView);
        containerQuestionTabs.addView(tabView);
        
        // Show the newly added question
        showQuestion(questionIndex);
    }
    
    private void showQuestion(int index) {
        if (index < 0 || index >= questionViews.size()) {
            return;
        }
        
        // Hide all questions
        for (View questionView : questionViews) {
            questionView.setVisibility(View.GONE);
        }
        
        // Show selected question
        questionViews.get(index).setVisibility(View.VISIBLE);
        
        // Update tab styles
        for (int i = 0; i < tabViews.size(); i++) {
            TextView tvTab = tabViews.get(i).findViewById(R.id.tvTabQuestion);
            if (i == index) {
                tvTab.setTextColor(0xFF3B82F6); // Blue
                tvTab.setBackgroundResource(R.drawable.bg_tab_selected);
            } else {
                tvTab.setTextColor(0xFF6B7280); // Gray
                tvTab.setBackground(null);
            }
        }
        
        currentSelectedQuestionIndex = index;
    }

    private void removeQuestionView(int index) {
        if (index < 0 || index >= questionViews.size()) {
            return;
        }
        
        // Remove question view
        View questionView = questionViews.remove(index);
        containerQuestions.removeView(questionView);
        
        // Remove tab view
        View tabView = tabViews.remove(index);
        containerQuestionTabs.removeView(tabView);
        
        // Update question numbers and tab texts
        for (int i = 0; i < questionViews.size(); i++) {
            View view = questionViews.get(i);
            TextView tvQuestionNumber = view.findViewById(R.id.tvQuestionNumber);
            tvQuestionNumber.setText("Câu hỏi " + (i + 1));
            
            if (i < tabViews.size()) {
                TextView tvTab = tabViews.get(i).findViewById(R.id.tvTabQuestion);
                tvTab.setText("Câu " + (i + 1));
                final int finalIndex = i;
                tabViews.get(i).setOnClickListener(v -> showQuestion(finalIndex));
            }
        }
        
        // Show the first question if any exists, or reset selection
        if (questionViews.size() > 0) {
            showQuestion(0);
        } else {
            currentSelectedQuestionIndex = -1;
        }
    }

    private void saveQuiz() {
        if (selectedDeck == null) {
            Toast.makeText(this, "Vui lòng chọn một bộ từ", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Question> questions = new ArrayList<>();

        for (View questionView : questionViews) {
            EditText etQuestionText = questionView.findViewById(R.id.etQuestionText);
            RadioButton rbAnswer1 = questionView.findViewById(R.id.rbAnswer1);
            RadioButton rbAnswer2 = questionView.findViewById(R.id.rbAnswer2);
            RadioButton rbAnswer3 = questionView.findViewById(R.id.rbAnswer3);
            RadioButton rbAnswer4 = questionView.findViewById(R.id.rbAnswer4);
            EditText etAnswer1 = questionView.findViewById(R.id.etAnswer1);
            EditText etAnswer2 = questionView.findViewById(R.id.etAnswer2);
            EditText etAnswer3 = questionView.findViewById(R.id.etAnswer3);
            EditText etAnswer4 = questionView.findViewById(R.id.etAnswer4);

            String questionText = etQuestionText.getText().toString().trim();
            if (questionText.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ nội dung câu hỏi", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check which RadioButton is checked directly
            boolean isAnswer1Correct = rbAnswer1.isChecked();
            boolean isAnswer2Correct = rbAnswer2.isChecked();
            boolean isAnswer3Correct = rbAnswer3.isChecked();
            boolean isAnswer4Correct = rbAnswer4.isChecked();
            
            // Ensure exactly one answer is selected
            int correctCount = 0;
            if (isAnswer1Correct) correctCount++;
            if (isAnswer2Correct) correctCount++;
            if (isAnswer3Correct) correctCount++;
            if (isAnswer4Correct) correctCount++;
            
            if (correctCount != 1) {
                Toast.makeText(this, "Vui lòng chọn một đáp án đúng cho mỗi câu hỏi", Toast.LENGTH_SHORT).show();
                return;
            }

            String answer1Text = etAnswer1.getText().toString().trim();
            String answer2Text = etAnswer2.getText().toString().trim();
            String answer3Text = etAnswer3.getText().toString().trim();
            String answer4Text = etAnswer4.getText().toString().trim();

            if (answer1Text.isEmpty() || answer2Text.isEmpty() || answer3Text.isEmpty() || answer4Text.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ 4 đáp án cho mỗi câu hỏi", Toast.LENGTH_SHORT).show();
                return;
            }

            List<Answer> answers = new ArrayList<>();
            answers.add(new Answer(answer1Text, isAnswer1Correct));
            answers.add(new Answer(answer2Text, isAnswer2Correct));
            answers.add(new Answer(answer3Text, isAnswer3Correct));
            answers.add(new Answer(answer4Text, isAnswer4Correct));

            questions.add(new Question(questionText, answers));
        }

        if (questions.isEmpty()) {
            Toast.makeText(this, "Vui lòng thêm ít nhất một câu hỏi", Toast.LENGTH_SHORT).show();
            return;
        }

        Quiz quiz = new Quiz(selectedDeck.getId(), questions);

        // Log the quiz data before sending
        Log.d(TAG, (currentQuizId != null ? "Updating" : "Creating") + " quiz for deck: " + selectedDeck.getId());
        Log.d(TAG, "Number of questions: " + questions.size());
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            Log.d(TAG, "Question " + (i + 1) + ": " + q.getQuestionText());
            for (int j = 0; j < q.getAnswers().size(); j++) {
                Answer a = q.getAnswers().get(j);
                Log.d(TAG, "  Answer " + (j + 1) + ": " + a.getAnswerText() + " (correct: " + a.isCorrect() + ")");
            }
        }
        
        // If editing, delete old quiz first, then create new one
        if (currentQuizId != null && !currentQuizId.isEmpty()) {
            deleteOldQuizThenCreate(quiz);
        } else {
            // Check if quiz already exists for this deck before creating new one
            checkQuizExistsThenCreate(quiz);
        }
    }
    
    private void checkQuizExistsThenCreate(Quiz quiz) {
        // Check if quiz already exists for this deck
        apiService.getQuizByTopic(quiz.getMaChuDe()).enqueue(new Callback<ApiResponse<QuizBundle>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<QuizBundle>> call, @NonNull Response<ApiResponse<QuizBundle>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    QuizBundle quizBundle = response.body().getData();
                    if (quizBundle != null && quizBundle.quiz != null) {
                        // Quiz already exists for this deck
                        Toast.makeText(CreateQuizActivity.this, 
                            "Bộ từ này đã có quiz. Vui lòng sửa quiz hiện có thay vì tạo mới.", 
                            Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                // No quiz exists, proceed to create
                // Check if this is from import or manual save
                if (currentQuizId == null || currentQuizId.isEmpty()) {
                    // This is a new quiz (either import or manual), use createNewQuiz
                    createNewQuiz(quiz);
                } else {
                    // This is edit mode, should not reach here
                    createNewQuiz(quiz);
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<ApiResponse<QuizBundle>> call, @NonNull Throwable t) {
                // If check fails, still try to create (might be network issue)
                Log.e(TAG, "Error checking existing quiz: " + t.getMessage());
                createNewQuiz(quiz);
            }
        });
    }
    
    private void deleteOldQuizThenCreate(Quiz quiz) {
        apiService.deleteQuiz(currentQuizId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    // Old quiz deleted, now create new one
                    createNewQuiz(quiz);
                } else {
                    Toast.makeText(CreateQuizActivity.this, "Không thể xóa quiz cũ", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(CreateQuizActivity.this, "Lỗi khi xóa quiz cũ: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void createNewQuiz(Quiz quiz) {
        int questionCount = quiz.getQuestions() != null ? quiz.getQuestions().size() : 0;
        
        // Log JSON để debug
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            String json = gson.toJson(quiz);
            Log.d(TAG, "Sending Quiz JSON: " + json);
            Log.d(TAG, "Quiz ma_chu_de: " + quiz.getMaChuDe());
            Log.d(TAG, "Quiz questions count: " + (quiz.getQuestions() != null ? quiz.getQuestions().size() : 0));
            if (quiz.getQuestions() != null) {
                for (int i = 0; i < quiz.getQuestions().size(); i++) {
                    Question q = quiz.getQuestions().get(i);
                    Log.d(TAG, "  Question[" + i + "]: text=" + q.getQuestionText() + ", answers=" + (q.getAnswers() != null ? q.getAnswers().size() : 0));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error serializing quiz to JSON", e);
        }
        
        apiService.createQuiz(quiz).enqueue(new Callback<ApiResponse<Quiz>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Quiz>> call, @NonNull Response<ApiResponse<Quiz>> response) {
                // Log response để debug
                Log.d(TAG, "Response code: " + response.code());
                if (response.body() != null) {
                    Log.d(TAG, "Response success: " + response.body().isSuccess());
                    Log.d(TAG, "Response message: " + response.body().getMessage());
                    if (response.body().getData() != null) {
                        Quiz returnedQuiz = response.body().getData();
                        Log.d(TAG, "Returned quiz ma_chu_de: " + returnedQuiz.getMaChuDe());
                        Log.d(TAG, "Returned quiz questions count: " + (returnedQuiz.getQuestions() != null ? returnedQuiz.getQuestions().size() : 0));
                    }
                }
                if (response.errorBody() != null) {
                    try {
                        String errorBody = response.errorBody().string();
                        Log.e(TAG, "Response error body: " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                }
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Determine message based on context
                    String message;
                    if (currentQuizId != null) {
                        message = "Cập nhật quiz thành công!";
                    } else if (questionCount > 0) {
                        // Likely from import
                        message = "Đã import thành công " + questionCount + " câu hỏi!";
                    } else {
                        message = "Tạo quiz thành công!";
                    }
                    Toast.makeText(CreateQuizActivity.this, message, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String errorMsg = "Tạo quiz thất bại";
                    int statusCode = response.code();
                    errorMsg += " (Code: " + statusCode + ")";
                    
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            // Check if it's HTML (error page)
                            if (errorBody.contains("<!DOCTYPE") || errorBody.contains("<html")) {
                                errorMsg = "Lỗi kết nối server. Vui lòng kiểm tra lại.";
                                Log.e(TAG, "Server returned HTML instead of JSON. Status: " + statusCode);
                            } else {
                                // Try to extract meaningful error message
                                if (errorBody.length() > 200) {
                                    errorBody = errorBody.substring(0, 200) + "...";
                                }
                                errorMsg += ": " + errorBody;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                            errorMsg += ". Không thể đọc thông báo lỗi.";
                        }
                    }
                    Log.e(TAG, "Create quiz failed: " + errorMsg);
                    Toast.makeText(CreateQuizActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Quiz>> call, @NonNull Throwable t) {
                Log.e(TAG, "API call failed: " + t.getMessage());
                Toast.makeText(CreateQuizActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
