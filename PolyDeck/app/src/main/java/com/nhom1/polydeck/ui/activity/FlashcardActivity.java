package com.nhom1.polydeck.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.ApiResponse;
import com.nhom1.polydeck.data.model.FavoriteRequest;
import com.nhom1.polydeck.data.model.TuVung;
import com.nhom1.polydeck.utils.LearningStatusManager;
import com.nhom1.polydeck.utils.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FlashcardActivity extends AppCompatActivity {

    public static final String EXTRA_DECK_ID = "EXTRA_DECK_ID";
    public static final String EXTRA_DECK_NAME = "EXTRA_DECK_NAME";
    public static final String EXTRA_REVIEW_UNKNOWN_ONLY = "EXTRA_REVIEW_UNKNOWN_ONLY";
    public static final String EXTRA_RESUME_INDEX = "EXTRA_RESUME_INDEX";

    private APIService api;
    private List<TuVung> cards = new ArrayList<>();
    private int index = 0;
    private boolean showMeaning = false;
    private int known = 0;
    private int unknown = 0;

    private TextView tvProgress, tvWord, tvPron, tvMeaning, tvHint, tvHintBack, tvWordBack, tvPronBack, tvExampleBack;
    private ProgressBar barProgress;
    private View cardView;
    private FrameLayout cardFront, cardBack;
    private ImageButton btnSound, btnSoundBack, btnBack, btnFav;
    private View btnKnown, btnUnknown;
    private TextToSpeech tts;
    private boolean isFlipping = false;
    private boolean ttsReady = false;

    private String deckId;
    private String deckName;
    private String userId;
    private final Set<String> favoriteIds = new HashSet<>();
    private LearningStatusManager learningStatusManager;
    private boolean reviewUnknownOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard);

        api = RetrofitClient.getApiService();
        deckId = getIntent().getStringExtra(EXTRA_DECK_ID);
        deckName = getIntent().getStringExtra(EXTRA_DECK_NAME);
        reviewUnknownOnly = getIntent().getBooleanExtra(EXTRA_REVIEW_UNKNOWN_ONLY, false);
        int resumeIndex = getIntent().getIntExtra(EXTRA_RESUME_INDEX, -1);
        SessionManager sm = new SessionManager(this);
        if (sm.getUserData() != null) userId = sm.getUserData().getId();
        learningStatusManager = new LearningStatusManager(this);

        bindViews();
        initTts();
        loadCards(resumeIndex);
        loadFavorites();
    }

    private void bindViews() {
        tvProgress = findViewById(R.id.tv_progress);
        tvWord = findViewById(R.id.tv_word);
        tvPron = findViewById(R.id.tv_pron);
        tvMeaning = findViewById(R.id.tv_meaning);
        tvHint = findViewById(R.id.tv_hint);
        tvHintBack = findViewById(R.id.tv_hint_back);
        tvWordBack = findViewById(R.id.tv_word_back);
        tvPronBack = findViewById(R.id.tv_pron_back);
        tvExampleBack = findViewById(R.id.tv_example_back);
        barProgress = findViewById(R.id.bar_progress);
        cardView = findViewById(R.id.card_view);
        cardFront = findViewById(R.id.card_front);
        cardBack = findViewById(R.id.card_back);
        btnSound = findViewById(R.id.btn_sound);
        btnSoundBack = findViewById(R.id.btn_sound_back);
        btnBack = findViewById(R.id.btn_back);
        btnFav = findViewById(R.id.btn_fav);
        btnKnown = findViewById(R.id.btn_known);
        btnUnknown = findViewById(R.id.btn_unknown);

        btnBack.setOnClickListener(v -> onBackPressed());
        btnFav.setOnClickListener(v -> addFavorite());
        
        // Icon loa - đảm bảo nhận click event trước cardView
        if (btnSound != null) {
            btnSound.setOnClickListener(v -> {
                Log.d("FlashcardActivity", "btnSound clicked - calling speak()");
                speak();
            });
            // Consume tất cả touch events để ngăn cardView nhận click
            btnSound.setOnTouchListener((v, event) -> {
                int action = event.getAction();
                if (action == android.view.MotionEvent.ACTION_UP || action == android.view.MotionEvent.ACTION_DOWN) {
                    Log.d("FlashcardActivity", "btnSound touch event: " + action);
                    if (action == android.view.MotionEvent.ACTION_UP) {
                        v.performClick();
                    }
                    return true; // Consume tất cả events
                }
                return true;
            });
            btnSound.setClickable(true);
            btnSound.setFocusable(true);
            btnSound.bringToFront();
            Log.d("FlashcardActivity", "btnSound initialized");
        } else {
            Log.e("FlashcardActivity", "btnSound is null!");
        }
        
        if (btnSoundBack != null) {
            btnSoundBack.setOnClickListener(v -> {
                Log.d("FlashcardActivity", "btnSoundBack clicked - calling speak()");
                speak();
            });
            // Consume tất cả touch events để ngăn cardView nhận click
            btnSoundBack.setOnTouchListener((v, event) -> {
                int action = event.getAction();
                if (action == android.view.MotionEvent.ACTION_UP || action == android.view.MotionEvent.ACTION_DOWN) {
                    Log.d("FlashcardActivity", "btnSoundBack touch event: " + action);
                    if (action == android.view.MotionEvent.ACTION_UP) {
                        v.performClick();
                    }
                    return true; // Consume tất cả events
                }
                return true;
            });
            btnSoundBack.setClickable(true);
            btnSoundBack.setFocusable(true);
            btnSoundBack.bringToFront();
            Log.d("FlashcardActivity", "btnSoundBack initialized");
        } else {
            Log.e("FlashcardActivity", "btnSoundBack is null!");
        }
        
        // Xử lý click vào card để flip
        cardView.setOnClickListener(v -> flipCard());
        btnKnown.setOnClickListener(v -> {
            TuVung current = getCurrent();
            if (current != null && current.getId() != null) {
                learningStatusManager.markAsKnown(deckId, current.getId());
                Toast.makeText(this, "Đã lưu: Đã nhớ", Toast.LENGTH_SHORT).show();
            }
            known++;
            next();
        });
        btnUnknown.setOnClickListener(v -> {
            TuVung current = getCurrent();
            if (current != null && current.getId() != null) {
                learningStatusManager.markAsUnknown(deckId, current.getId());
                Toast.makeText(this, "Đã lưu: Chưa nhớ (sẽ học lại)", Toast.LENGTH_SHORT).show();
            }
            unknown++;
            next();
        });
    }

    private void initTts() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA) {
                    Log.e("FlashcardActivity", "TTS Language data missing - need to install TTS engine");
                    ttsReady = false;
                    // Hướng dẫn người dùng cài đặt TTS engine
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Cần cài đặt Text-to-Speech engine. Vui lòng cài Google TTS từ Play Store.", Toast.LENGTH_LONG).show();
                    });
                } else if (result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("FlashcardActivity", "TTS Language not supported");
                    ttsReady = false;
                } else {
                    Log.d("FlashcardActivity", "TTS initialized successfully");
                    ttsReady = true;
                }
            } else {
                Log.e("FlashcardActivity", "TTS initialization failed - status: " + status);
                ttsReady = false;
                runOnUiThread(() -> {
                    Toast.makeText(this, "Text-to-Speech không khả dụng. Vui lòng kiểm tra cài đặt.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void speak() {
        Log.d("FlashcardActivity", "speak() called");
        TuVung c = getCurrent();
        if (c == null) {
            Log.e("FlashcardActivity", "Current word is null");
            Toast.makeText(this, "Không có từ để phát âm", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String word = c.getTuTiengAnh();
        if (word == null || word.trim().isEmpty()) {
            Log.e("FlashcardActivity", "Word text is empty");
            Toast.makeText(this, "Từ vựng trống", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d("FlashcardActivity", "Word to speak: " + word + ", TTS ready: " + ttsReady + ", TTS null: " + (tts == null));
        
        // Chỉ dùng TTS - không dùng link âm thanh để tránh delay
        Log.d("FlashcardActivity", "Using TTS for instant playback");
        speakWithTTS(word);
    }
    
    private void speakWithTTS(String word) {
        // Dùng TTS
        if (tts == null) {
            Log.d("FlashcardActivity", "TTS is null, initializing...");
            initTts();
            // Đợi một chút để TTS khởi tạo
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (ttsReady && tts != null) {
                    speakWord(word);
                } else {
                    Log.e("FlashcardActivity", "TTS still not ready after initialization");
                    Toast.makeText(this, "Text-to-Speech chưa sẵn sàng. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                }
            }, 1000);
        } else if (ttsReady) {
            speakWord(word);
        } else {
            Log.d("FlashcardActivity", "TTS not ready yet, waiting...");
            // Đợi TTS sẵn sàng
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (ttsReady && tts != null) {
                    speakWord(word);
                } else {
                    Log.e("FlashcardActivity", "TTS still not ready");
                    Toast.makeText(this, "Text-to-Speech chưa sẵn sàng. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                }
            }, 1000);
        }
    }
    
    // Không dùng audio URL nữa - chỉ dùng TTS
    
    private void speakWord(String word) {
        if (tts == null) {
            Log.e("FlashcardActivity", "TTS is still null");
            Toast.makeText(this, "Không thể phát âm", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d("FlashcardActivity", "Speaking with TTS: " + word);
        // Dừng bất kỳ phát âm nào đang diễn ra
        tts.stop();
        int result = tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, "word");
        if (result == TextToSpeech.ERROR) {
            Log.e("FlashcardActivity", "TTS speak error");
            Toast.makeText(this, "Lỗi phát âm", Toast.LENGTH_SHORT).show();
        } else {
            Log.d("FlashcardActivity", "TTS speak started successfully");
        }
    }
    
    // Không dùng audio URL nữa - chỉ dùng TTS

    private void loadCards(int resumeIndex) {
        api.getTuVungByBoTu(deckId).enqueue(new Callback<List<TuVung>>() {
            @Override public void onResponse(@NonNull Call<List<TuVung>> call, @NonNull Response<List<TuVung>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cards.clear();
                    List<TuVung> allCards = response.body();
                    
                    // Nếu chỉ học lại từ chưa nhớ, lọc danh sách
                    if (reviewUnknownOnly) {
                        Set<String> unknownWordIds = learningStatusManager.getUnknownWords(deckId);
                        for (TuVung card : allCards) {
                            if (card.getId() != null && unknownWordIds.contains(card.getId())) {
                                cards.add(card);
                            }
                        }
                        
                        if (cards.isEmpty()) {
                            Toast.makeText(FlashcardActivity.this, "Không có từ nào cần học lại!", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    } else {
                        cards.addAll(allCards);
                    }
                    
                    // Xáo trộn danh sách từ vựng để học ngẫu nhiên
                    Collections.shuffle(cards);
                    
                    // Nếu có resumeIndex, tiếp tục từ vị trí đó
                    if (resumeIndex >= 0 && resumeIndex < cards.size()) {
                        index = resumeIndex;
                    } else {
                        index = 0;
                    }
                    showMeaning = false;
                    render();
                    
                    // Không preload audio nữa vì chỉ dùng TTS
                } else {
                    Toast.makeText(FlashcardActivity.this, "Không tải được thẻ từ", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            @Override public void onFailure(@NonNull Call<List<TuVung>> call, @NonNull Throwable t) {
                Toast.makeText(FlashcardActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadFavorites() {
        if (userId == null) return;
        api.getUserFavorites(userId).enqueue(new Callback<ApiResponse<List<TuVung>>>() {
            @Override public void onResponse(Call<ApiResponse<List<TuVung>>> call, Response<ApiResponse<List<TuVung>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    favoriteIds.clear();
                    List<TuVung> favs = response.body().getData();
                    if (favs != null) for (TuVung t : favs) if (t.getId() != null) favoriteIds.add(t.getId());
                    updateFavIcon();
                }
            }
            @Override public void onFailure(Call<ApiResponse<List<TuVung>>> call, Throwable t) { }
        });
    }

    private TuVung getCurrent() {
        if (index >= 0 && index < cards.size()) return cards.get(index);
        return null;
    }

    private void addFavorite() {
        TuVung c = getCurrent();
        if (c == null || userId == null) return;
        api.addFavorite(userId, new FavoriteRequest(c.getId())).enqueue(new Callback<ApiResponse<Void>>() {
            @Override public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (c.getId() != null) favoriteIds.add(c.getId());
                animateHeart();
                updateFavIcon();
                Toast.makeText(FlashcardActivity.this, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
            }
            @Override public void onFailure(Call<ApiResponse<Void>> call, Throwable t) { }
        });
    }

    private void updateFavIcon() {
        TuVung c = getCurrent();
        boolean isFav = c != null && c.getId() != null && favoriteIds.contains(c.getId());
        btnFav.setImageResource(isFav ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);
    }

    private void animateHeart() {
        PropertyValuesHolder sx = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.3f, 1f);
        PropertyValuesHolder sy = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.3f, 1f);
        ObjectAnimator.ofPropertyValuesHolder(btnFav, sx, sy).setDuration(250).start();
    }

    private void flipCard() {
        if (isFlipping) return;
        isFlipping = true;
        showMeaning = !showMeaning;
        
        float currentRotation = cardView.getRotationY();
        float targetRotation = showMeaning ? 180f : 0f;
        
        ObjectAnimator flip = ObjectAnimator.ofFloat(cardView, "rotationY", currentRotation, targetRotation);
        flip.setDuration(600);
        flip.setInterpolator(new DecelerateInterpolator());
        
        // Add update listener to flip text in opposite direction
        flip.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float rotation = (Float) animation.getAnimatedValue();
                // Flip text content in opposite direction to keep it readable
                if (showMeaning) {
                    cardBack.setRotationY(-rotation);
                } else {
                    cardFront.setRotationY(-rotation);
                }
            }
        });
        
        flip.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                // Switch visibility at midpoint (90 degrees)
                cardView.postDelayed(() -> {
                    if (showMeaning) {
                        cardBack.setVisibility(View.VISIBLE);
                        cardFront.setVisibility(View.GONE);
                    } else {
                        cardFront.setVisibility(View.VISIBLE);
                        cardBack.setVisibility(View.GONE);
                    }
                }, 300);
            }
            
            @Override
            public void onAnimationEnd(Animator animation) {
                // Reset text rotation after animation
                if (showMeaning) {
                    cardBack.setRotationY(-180f);
                } else {
                    cardFront.setRotationY(0f);
                }
                isFlipping = false;
            }
        });
        
        flip.start();
    }

    private void render() {
        int total = Math.max(1, cards.size());
        int current = Math.min(index + 1, total);
        // Hiển thị "Từ 1/5" thay vì chỉ "1/5"
        tvProgress.setText("Từ " + current + "/" + total);
        barProgress.setMax(total);
        barProgress.setProgress(current);

        TuVung c = getCurrent();
        if (c == null) return;
        
        // Front of card
        tvWord.setText(c.getTuTiengAnh() != null ? c.getTuTiengAnh() : "");
        tvPron.setText(c.getPhienAm() != null ? c.getPhienAm() : "");
        
        // Back of card
        tvWordBack.setText(c.getTuTiengAnh() != null ? c.getTuTiengAnh() : "");
        tvPronBack.setText(c.getPhienAm() != null ? c.getPhienAm() : "");
        tvMeaning.setText(c.getNghiaTiengViet() != null ? c.getNghiaTiengViet() : "");
        
        // Example sentence
        if (c.getCauViDu() != null && !c.getCauViDu().trim().isEmpty()) {
            tvExampleBack.setText("\"" + c.getCauViDu() + "\"");
            tvExampleBack.setVisibility(View.VISIBLE);
        } else {
            tvExampleBack.setVisibility(View.GONE);
        }

        // Reset card to front when new card is loaded
        if (!showMeaning) {
            cardFront.setVisibility(View.VISIBLE);
            cardBack.setVisibility(View.GONE);
            cardView.setRotationY(0f);
            cardFront.setRotationY(0f);
            cardBack.setRotationY(0f);
        }
        updateFavIcon();
    }

    private void next() {
        showMeaning = false;
        index++;
        // Lưu tiến độ mỗi khi chuyển từ
        learningStatusManager.saveFlashcardProgress(deckId, index, reviewUnknownOnly);
        
        if (index >= cards.size()) {
            // Done - Xóa tiến độ đã lưu khi hoàn thành
            learningStatusManager.clearFlashcardProgress(deckId);
            FlashcardDoneActivity.start(this, deckId, deckName, known, unknown, cards.size());
            finish();
            return;
        }
        render();
        // Không preload audio nữa vì chỉ dùng TTS
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Lưu tiến độ khi người dùng thoát
        if (deckId != null && !cards.isEmpty() && index < cards.size()) {
            learningStatusManager.saveFlashcardProgress(deckId, index, reviewUnknownOnly);
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}


