package com.nhom1.polydeck.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.media.MediaPlayer;
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
import com.nhom1.polydeck.utils.SessionManager;

import java.util.ArrayList;
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
    private MediaPlayer mediaPlayer;
    private boolean isFlipping = false;

    private String deckId;
    private String deckName;
    private String userId;
    private final Set<String> favoriteIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard);

        api = RetrofitClient.getApiService();
        deckId = getIntent().getStringExtra(EXTRA_DECK_ID);
        deckName = getIntent().getStringExtra(EXTRA_DECK_NAME);
        SessionManager sm = new SessionManager(this);
        if (sm.getUserData() != null) userId = sm.getUserData().getMaNguoiDung();

        bindViews();
        initTts();
        loadCards();
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
        btnSound.setOnClickListener(v -> speak());
        btnSoundBack.setOnClickListener(v -> speak());
        cardView.setOnClickListener(v -> flipCard());
        btnKnown.setOnClickListener(v -> {
            known++;
            next();
        });
        btnUnknown.setOnClickListener(v -> {
            unknown++;
            next();
        });
    }

    private void initTts() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
            }
        });
    }

    private void speak() {
        TuVung c = getCurrent();
        if (c == null) return;
        
        // Ưu tiên phát audio từ URL nếu có
        String audioUrl = c.getAmThanh();
        if (audioUrl != null && !audioUrl.trim().isEmpty()) {
            playAudioFromUrl(audioUrl);
        } else if (tts != null && c.getTuTiengAnh() != null) {
            // Nếu không có audio URL, dùng TTS
            tts.speak(c.getTuTiengAnh(), TextToSpeech.QUEUE_FLUSH, null, "word");
        }
    }
    
    private void playAudioFromUrl(String url) {
        try {
            // Dừng media player cũ nếu đang phát
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
            
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
            });
            
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e("FlashcardActivity", "MediaPlayer error: " + what + ", " + extra);
                // Nếu phát audio thất bại, fallback về TTS
                TuVung c = getCurrent();
                if (tts != null && c != null && c.getTuTiengAnh() != null) {
                    tts.speak(c.getTuTiengAnh(), TextToSpeech.QUEUE_FLUSH, null, "word");
                }
                return true;
            });
            
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                mediaPlayer = null;
            });
            
        } catch (Exception e) {
            Log.e("FlashcardActivity", "Error playing audio: " + e.getMessage());
            // Fallback về TTS nếu có lỗi
            TuVung c = getCurrent();
            if (tts != null && c != null && c.getTuTiengAnh() != null) {
                tts.speak(c.getTuTiengAnh(), TextToSpeech.QUEUE_FLUSH, null, "word");
            }
        }
    }

    private void loadCards() {
        api.getTuVungByBoTu(deckId).enqueue(new Callback<List<TuVung>>() {
            @Override public void onResponse(@NonNull Call<List<TuVung>> call, @NonNull Response<List<TuVung>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cards.clear();
                    cards.addAll(response.body());
                    index = 0;
                    showMeaning = false;
                    render();
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
        tvProgress.setText((Math.min(index + 1, total)) + "/" + total);
        barProgress.setMax(total);
        barProgress.setProgress(Math.min(index + 1, total));

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
        if (index >= cards.size()) {
            // Done
            FlashcardDoneActivity.start(this, deckId, deckName, known, unknown, cards.size());
            finish();
            return;
        }
        render();
    }

    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}


