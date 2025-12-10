package com.nhom1.polydeck.ui.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.AdminStats;
import com.nhom1.polydeck.data.model.BoTu;
import com.nhom1.polydeck.data.model.User;
import com.nhom1.polydeck.ui.adapter.UserStatisticsAdapter;

import java.util.ArrayList;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminStatisticsActivity extends AppCompatActivity {

    private ImageView btnBack, btnFilter, btnCloseFilter;
    private TextView tvQuickFilter, tvDateRangeDisplay;
    private TextView tvTotalUsers, tvActiveUsers, tvNewUsers, tvTotalSessions;
    private TextView tvTotalUsersChange, tvActiveUsersChange, tvNewUsersChange, tvTotalSessionsChange;
    private TextView tvTotalUsersPrevious, tvActiveUsersPrevious, tvNewUsersPrevious, tvTotalSessionsPrevious;
    private TextView tvTotalDecks, tvTotalVocab;
    private TextView tvTotalDecksChange, tvTotalVocabChange;
    private TextView tvTotalDecksPrevious, tvTotalVocabPrevious;
    private LinearLayout bottomSheetFilter;
    private LinearLayout btnSelectFromDate, btnSelectToDate;
    private TextView tvFromDate, tvToDate;
    private TextView chipToday, chipThisWeek, chipThisMonth;
    private Button btnApplyFilter;
    private RecyclerView rvUsers;
    private TextView tvUserCount;
    private UserStatisticsAdapter userAdapter;

    private APIService apiService;
    private String selectedFilter = "month"; // today, week, month, custom
    private Calendar fromDate, toDate;
    private Calendar currentPeriodStart, currentPeriodEnd;
    private Calendar previousPeriodStart, previousPeriodEnd;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_statistics);

        apiService = RetrofitClient.getApiService();
        initViews();
        setupClickListeners();
        
        // Set default: Tháng này
        setDefaultMonthFilter();
        loadStatistics();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnFilter = findViewById(R.id.btnFilter);
        btnCloseFilter = findViewById(R.id.btnCloseFilter);
        tvQuickFilter = findViewById(R.id.tvQuickFilter);
        tvDateRangeDisplay = findViewById(R.id.tvDateRangeDisplay);
        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvActiveUsers = findViewById(R.id.tvActiveUsers);
        tvNewUsers = findViewById(R.id.tvNewUsers);
        tvTotalSessions = findViewById(R.id.tvTotalSessions);
        tvTotalUsersChange = findViewById(R.id.tvTotalUsersChange);
        tvActiveUsersChange = findViewById(R.id.tvActiveUsersChange);
        tvNewUsersChange = findViewById(R.id.tvNewUsersChange);
        tvTotalSessionsChange = findViewById(R.id.tvTotalSessionsChange);
        tvTotalUsersPrevious = findViewById(R.id.tvTotalUsersPrevious);
        tvActiveUsersPrevious = findViewById(R.id.tvActiveUsersPrevious);
        tvNewUsersPrevious = findViewById(R.id.tvNewUsersPrevious);
        tvTotalSessionsPrevious = findViewById(R.id.tvTotalSessionsPrevious);
        tvTotalDecks = findViewById(R.id.tvTotalDecks);
        tvTotalVocab = findViewById(R.id.tvTotalVocab);
        tvTotalDecksChange = findViewById(R.id.tvTotalDecksChange);
        tvTotalVocabChange = findViewById(R.id.tvTotalVocabChange);
        tvTotalDecksPrevious = findViewById(R.id.tvTotalDecksPrevious);
        tvTotalVocabPrevious = findViewById(R.id.tvTotalVocabPrevious);
        bottomSheetFilter = findViewById(R.id.bottomSheetFilter);
        btnSelectFromDate = findViewById(R.id.btnSelectFromDate);
        btnSelectToDate = findViewById(R.id.btnSelectToDate);
        tvFromDate = findViewById(R.id.tvFromDate);
        tvToDate = findViewById(R.id.tvToDate);
        chipToday = findViewById(R.id.chipToday);
        chipThisWeek = findViewById(R.id.chipThisWeek);
        chipThisMonth = findViewById(R.id.chipThisMonth);
        btnApplyFilter = findViewById(R.id.btnApplyFilter);
        rvUsers = findViewById(R.id.rvUsers);
        tvUserCount = findViewById(R.id.tvUserCount);
        
        setupRecyclerView();
    }
    
    private void setupRecyclerView() {
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserStatisticsAdapter(this);
        rvUsers.setAdapter(userAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());
        
        btnFilter.setOnClickListener(v -> showBottomSheet());
        
        btnCloseFilter.setOnClickListener(v -> hideBottomSheet());
        
        // Quick filter chips
        chipToday.setOnClickListener(v -> selectQuickFilter("today"));
        chipThisWeek.setOnClickListener(v -> selectQuickFilter("week"));
        chipThisMonth.setOnClickListener(v -> selectQuickFilter("month"));
        
        // Date pickers
        btnSelectFromDate.setOnClickListener(v -> showDatePicker(true));
        btnSelectToDate.setOnClickListener(v -> showDatePicker(false));
        
        // Apply filter
        btnApplyFilter.setOnClickListener(v -> applyCustomFilter());
    }

    private void setDefaultMonthFilter() {
        selectedFilter = "month";
        calculateMonthPeriod();
        updateFilterDisplay();
    }

    private void selectQuickFilter(String filter) {
        selectedFilter = filter;
        
        // Update chip selection
        resetChipStyles();
        switch (filter) {
            case "today":
                chipToday.setBackgroundResource(R.drawable.bg_chip_filled);
                chipToday.setTextColor(getResources().getColor(android.R.color.white));
                calculateTodayPeriod();
                break;
            case "week":
                chipThisWeek.setBackgroundResource(R.drawable.bg_chip_filled);
                chipThisWeek.setTextColor(getResources().getColor(android.R.color.white));
                calculateWeekPeriod();
                break;
            case "month":
                chipThisMonth.setBackgroundResource(R.drawable.bg_chip_filled);
                chipThisMonth.setTextColor(getResources().getColor(android.R.color.white));
                calculateMonthPeriod();
                break;
        }
        
        updateFilterDisplay();
        hideBottomSheet();
        loadStatistics();
    }

    private void resetChipStyles() {
        chipToday.setBackgroundResource(R.drawable.bg_chip_outlined);
        chipToday.setTextColor(getResources().getColor(R.color.purple_primary));
        chipThisWeek.setBackgroundResource(R.drawable.bg_chip_outlined);
        chipThisWeek.setTextColor(getResources().getColor(R.color.purple_primary));
        chipThisMonth.setBackgroundResource(R.drawable.bg_chip_outlined);
        chipThisMonth.setTextColor(getResources().getColor(R.color.purple_primary));
    }

    private void calculateTodayPeriod() {
        Calendar now = Calendar.getInstance();
        currentPeriodStart = (Calendar) now.clone();
        currentPeriodStart.set(Calendar.HOUR_OF_DAY, 0);
        currentPeriodStart.set(Calendar.MINUTE, 0);
        currentPeriodStart.set(Calendar.SECOND, 0);
        currentPeriodEnd = (Calendar) now.clone();
        currentPeriodEnd.set(Calendar.HOUR_OF_DAY, 23);
        currentPeriodEnd.set(Calendar.MINUTE, 59);
        currentPeriodEnd.set(Calendar.SECOND, 59);
        
        // Previous day
        previousPeriodStart = (Calendar) currentPeriodStart.clone();
        previousPeriodStart.add(Calendar.DAY_OF_MONTH, -1);
        previousPeriodEnd = (Calendar) previousPeriodStart.clone();
        previousPeriodEnd.set(Calendar.HOUR_OF_DAY, 23);
        previousPeriodEnd.set(Calendar.MINUTE, 59);
        previousPeriodEnd.set(Calendar.SECOND, 59);
        
        tvQuickFilter.setText("Hôm nay");
    }

    private void calculateWeekPeriod() {
        Calendar now = Calendar.getInstance();
        int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        int daysFromMonday = (dayOfWeek == Calendar.SUNDAY) ? 6 : dayOfWeek - Calendar.MONDAY;
        
        currentPeriodStart = (Calendar) now.clone();
        currentPeriodStart.add(Calendar.DAY_OF_MONTH, -daysFromMonday);
        currentPeriodStart.set(Calendar.HOUR_OF_DAY, 0);
        currentPeriodStart.set(Calendar.MINUTE, 0);
        currentPeriodStart.set(Calendar.SECOND, 0);
        currentPeriodEnd = (Calendar) now.clone();
        currentPeriodEnd.set(Calendar.HOUR_OF_DAY, 23);
        currentPeriodEnd.set(Calendar.MINUTE, 59);
        currentPeriodEnd.set(Calendar.SECOND, 59);
        
        // Previous week
        previousPeriodEnd = (Calendar) currentPeriodStart.clone();
        previousPeriodEnd.add(Calendar.SECOND, -1);
        previousPeriodStart = (Calendar) previousPeriodEnd.clone();
        previousPeriodStart.add(Calendar.DAY_OF_MONTH, -6);
        previousPeriodStart.set(Calendar.HOUR_OF_DAY, 0);
        previousPeriodStart.set(Calendar.MINUTE, 0);
        previousPeriodStart.set(Calendar.SECOND, 0);
        
        tvQuickFilter.setText("Tuần này");
    }

    private void calculateMonthPeriod() {
        Calendar now = Calendar.getInstance();
        currentPeriodStart = Calendar.getInstance();
        currentPeriodStart.set(Calendar.DAY_OF_MONTH, 1);
        currentPeriodStart.set(Calendar.HOUR_OF_DAY, 0);
        currentPeriodStart.set(Calendar.MINUTE, 0);
        currentPeriodStart.set(Calendar.SECOND, 0);
        currentPeriodEnd = (Calendar) now.clone();
        currentPeriodEnd.set(Calendar.HOUR_OF_DAY, 23);
        currentPeriodEnd.set(Calendar.MINUTE, 59);
        currentPeriodEnd.set(Calendar.SECOND, 59);
        
        // Previous month
        previousPeriodEnd = (Calendar) currentPeriodStart.clone();
        previousPeriodEnd.add(Calendar.SECOND, -1);
        previousPeriodStart = (Calendar) previousPeriodEnd.clone();
        previousPeriodStart.set(Calendar.DAY_OF_MONTH, 1);
        previousPeriodStart.set(Calendar.HOUR_OF_DAY, 0);
        previousPeriodStart.set(Calendar.MINUTE, 0);
        previousPeriodStart.set(Calendar.SECOND, 0);
        
        tvQuickFilter.setText("Tháng này");
    }

    private void applyCustomFilter() {
        if (fromDate == null || toDate == null) {
            Toast.makeText(this, "Vui lòng chọn đầy đủ ngày bắt đầu và ngày kết thúc", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (fromDate.after(toDate)) {
            Toast.makeText(this, "Ngày bắt đầu phải trước ngày kết thúc", Toast.LENGTH_SHORT).show();
            return;
        }
        
        selectedFilter = "custom";
        currentPeriodStart = (Calendar) fromDate.clone();
        currentPeriodStart.set(Calendar.HOUR_OF_DAY, 0);
        currentPeriodStart.set(Calendar.MINUTE, 0);
        currentPeriodStart.set(Calendar.SECOND, 0);
        currentPeriodEnd = (Calendar) toDate.clone();
        currentPeriodEnd.set(Calendar.HOUR_OF_DAY, 23);
        currentPeriodEnd.set(Calendar.MINUTE, 59);
        currentPeriodEnd.set(Calendar.SECOND, 59);
        
        // Calculate previous period (same duration)
        long duration = currentPeriodEnd.getTimeInMillis() - currentPeriodStart.getTimeInMillis();
        previousPeriodEnd = (Calendar) currentPeriodStart.clone();
        previousPeriodEnd.add(Calendar.SECOND, -1);
        previousPeriodStart = Calendar.getInstance();
        previousPeriodStart.setTimeInMillis(previousPeriodEnd.getTimeInMillis() - duration);
        
        resetChipStyles();
        updateFilterDisplay();
        hideBottomSheet();
        loadStatistics();
    }

    private void updateFilterDisplay() {
        if (currentPeriodStart != null && currentPeriodEnd != null) {
            String dateRange = dateFormat.format(currentPeriodStart.getTime()) + " - " + 
                             dateFormat.format(currentPeriodEnd.getTime());
            tvDateRangeDisplay.setText(dateRange);
        }
    }

    private void showDatePicker(boolean isFromDate) {
        final boolean isFrom = isFromDate;
        Calendar initialCalendar = isFrom ? fromDate : toDate;
        if (initialCalendar == null) {
            initialCalendar = Calendar.getInstance();
        }
        
        final int initialYear = initialCalendar.get(Calendar.YEAR);
        final int initialMonth = initialCalendar.get(Calendar.MONTH);
        final int initialDay = initialCalendar.get(Calendar.DAY_OF_MONTH);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth);
                    if (isFrom) {
                        fromDate = selectedCalendar;
                        tvFromDate.setText(dateFormat.format(selectedCalendar.getTime()));
                    } else {
                        toDate = selectedCalendar;
                        tvToDate.setText(dateFormat.format(selectedCalendar.getTime()));
                    }
                },
                initialYear,
                initialMonth,
                initialDay
        );
        datePickerDialog.show();
    }

    private void showBottomSheet() {
        bottomSheetFilter.setVisibility(View.VISIBLE);
        // Initialize custom dates if not set
        if (fromDate == null) {
            fromDate = Calendar.getInstance();
            fromDate.add(Calendar.DAY_OF_MONTH, -30);
            tvFromDate.setText(dateFormat.format(fromDate.getTime()));
        } else {
            tvFromDate.setText(dateFormat.format(fromDate.getTime()));
        }
        if (toDate == null) {
            toDate = Calendar.getInstance();
            tvToDate.setText(dateFormat.format(toDate.getTime()));
        } else {
            tvToDate.setText(dateFormat.format(toDate.getTime()));
        }
    }

    private void hideBottomSheet() {
        bottomSheetFilter.setVisibility(View.GONE);
    }

    private void loadStatistics() {
        if (currentPeriodStart == null || currentPeriodEnd == null) {
            calculateMonthPeriod();
        }
        
        String currentStartStr = apiDateFormat.format(currentPeriodStart.getTime());
        String currentEndStr = apiDateFormat.format(currentPeriodEnd.getTime());
        String previousStartStr = apiDateFormat.format(previousPeriodStart.getTime());
        String previousEndStr = apiDateFormat.format(previousPeriodEnd.getTime());
        
        // Load users and decks first to calculate new counts
        apiService.getUsersByDateRange(currentStartStr, currentEndStr).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                List<User> currentUsers = response.isSuccessful() && response.body() != null 
                        ? response.body() : new ArrayList<>();
                
                // Calculate new users (users created in this period)
                int newUsers = 0;
                try {
                    Calendar startCal = Calendar.getInstance();
                    Calendar endCal = Calendar.getInstance();
                    startCal.setTime(apiDateFormat.parse(currentStartStr));
                    endCal.setTime(apiDateFormat.parse(currentEndStr));
                    endCal.set(Calendar.HOUR_OF_DAY, 23);
                    endCal.set(Calendar.MINUTE, 59);
                    endCal.set(Calendar.SECOND, 59);
                    
                    SimpleDateFormat userDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    for (User user : currentUsers) {
                        if (user.getNgayThamGia() != null && !user.getNgayThamGia().isEmpty()) {
                            try {
                                Calendar userDate = Calendar.getInstance();
                                userDate.setTime(userDateFormat.parse(user.getNgayThamGia()));
                                if (!userDate.before(startCal) && !userDate.after(endCal)) {
                                    newUsers++;
                                }
                            } catch (Exception e) {
                                // Skip if date parsing fails
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                final int finalNewUsers = newUsers;
                final int currentTotal = currentUsers.size();
                int activeCount = 0;
                for (User user : currentUsers) {
                    if (user.getTrangThai() != null && user.getTrangThai().equals("active")) {
                        activeCount++;
                    }
                }
                final int currentActive = activeCount;
                
                // Load decks to calculate new decks
                apiService.getAllChuDe().enqueue(new Callback<List<BoTu>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<BoTu>> call, @NonNull Response<List<BoTu>> response) {
                        List<BoTu> allDecks = response.isSuccessful() && response.body() != null 
                                ? response.body() : new ArrayList<>();
                        
                        // Calculate new decks (decks created in this period)
                        int newDecks = 0;
                        try {
                            Calendar startCal = Calendar.getInstance();
                            Calendar endCal = Calendar.getInstance();
                            startCal.setTime(apiDateFormat.parse(currentStartStr));
                            endCal.setTime(apiDateFormat.parse(currentEndStr));
                            endCal.set(Calendar.HOUR_OF_DAY, 23);
                            endCal.set(Calendar.MINUTE, 59);
                            endCal.set(Calendar.SECOND, 59);
                            
                            for (BoTu deck : allDecks) {
                                if (deck.getNgayTao() != null) {
                                    Calendar deckDate = Calendar.getInstance();
                                    deckDate.setTime(deck.getNgayTao());
                                    if (!deckDate.before(startCal) && !deckDate.after(endCal)) {
                                        newDecks++;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        
                        final int finalNewDecks = newDecks;
                        
                        // Load admin stats by date range
                        apiService.getAdminStatsByDateRange(currentStartStr, currentEndStr).enqueue(new Callback<AdminStats>() {
                            @Override
                            public void onResponse(@NonNull Call<AdminStats> call, @NonNull Response<AdminStats> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    AdminStats currentStats = response.body();
                                    
                                    // Load previous period stats for comparison
                                    apiService.getAdminStatsByDateRange(previousStartStr, previousEndStr).enqueue(new Callback<AdminStats>() {
                                        @Override
                                        public void onResponse(@NonNull Call<AdminStats> call, @NonNull Response<AdminStats> response) {
                                            AdminStats previousStats = response.isSuccessful() && response.body() != null 
                                                    ? response.body() : new AdminStats();
                                            
                                            // Calculate previous period new users and decks
                                            apiService.getUsersByDateRange(previousStartStr, previousEndStr).enqueue(new Callback<List<User>>() {
                                                @Override
                                                public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                                                    List<User> previousUsers = response.isSuccessful() && response.body() != null 
                                                            ? response.body() : new ArrayList<>();
                                                    int previousNewUsers = previousUsers.size(); // All users in period are new
                                                    
                                                    updateStatsWithComparison(
                                                            currentTotal,
                                                            currentActive,
                                                            finalNewUsers,
                                                            0, // Sessions not available
                                                            currentStats.getTongBoTu(),
                                                            currentStats.getTongTuVung(),
                                                            previousUsers.size(),
                                                            previousStats.getNguoiHoatDong(),
                                                            previousNewUsers,
                                                            0,
                                                            previousStats.getTongBoTu(),
                                                            previousStats.getTongTuVung()
                                                    );
                                                }

                                                @Override
                                                public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {
                                                    updateStatsWithComparison(
                                                            currentTotal,
                                                            currentActive,
                                                            finalNewUsers,
                                                            0,
                                                            currentStats.getTongBoTu(),
                                                            currentStats.getTongTuVung(),
                                                            0, 0, 0, 0, 0, 0
                                                    );
                                                }
                                            });
                                        }

                                        @Override
                                        public void onFailure(@NonNull Call<AdminStats> call, @NonNull Throwable t) {
                                            updateStatsWithComparison(
                                                    currentTotal,
                                                    currentActive,
                                                    finalNewUsers,
                                                    0,
                                                    currentStats.getTongBoTu(),
                                                    currentStats.getTongTuVung(),
                                                    0, 0, 0, 0, 0, 0
                                            );
                                        }
                                    });
                                } else {
                                    loadOverallStats();
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<AdminStats> call, @NonNull Throwable t) {
                                loadOverallStats();
                            }
                        });
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<BoTu>> call, @NonNull Throwable t) {
                        loadOverallStats();
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {
                loadOverallStats();
            }
        });
        
        // Load users for the period
        loadUsersByDateRange(currentStartStr, currentEndStr);
    }
    
    private void loadUsersByDateRange(String startDate, String endDate) {
        apiService.getUsersByDateRange(startDate, endDate).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userAdapter.updateData(response.body());
                    updateUserCount(response.body().size());
                } else {
                    userAdapter.updateData(new ArrayList<>());
                    updateUserCount(0);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {
                userAdapter.updateData(new ArrayList<>());
                updateUserCount(0);
            }
        });
    }
    
    private void updateUserCount(int count) {
        tvUserCount.setText(String.format(Locale.getDefault(), "%d người dùng", count));
    }

    private void updateStatsWithComparison(int currentTotal, int currentActive, int currentNew, int currentSessions,
                                           int currentDecks, int currentVocab,
                                           int previousTotal, int previousActive, int previousNew, int previousSessions,
                                           int previousDecks, int previousVocab) {
        tvTotalUsers.setText(String.format(Locale.getDefault(), "%,d", currentTotal));
        tvActiveUsers.setText(String.format(Locale.getDefault(), "%,d", currentActive));
        tvNewUsers.setText(String.format(Locale.getDefault(), "%,d", currentNew));
        tvTotalSessions.setText(String.format(Locale.getDefault(), "%,d", currentSessions));
        tvTotalDecks.setText(String.format(Locale.getDefault(), "%,d", currentDecks));
        tvTotalVocab.setText(String.format(Locale.getDefault(), "%,d", currentVocab));
        
        updateChangeText(tvTotalUsersChange, previousTotal, currentTotal);
        updateChangeText(tvActiveUsersChange, previousActive, currentActive);
        updateChangeText(tvNewUsersChange, previousNew, currentNew);
        updateChangeText(tvTotalSessionsChange, previousSessions, currentSessions);
        updateChangeText(tvTotalDecksChange, previousDecks, currentDecks);
        updateChangeText(tvTotalVocabChange, previousVocab, currentVocab);
        
        // Update previous period values
        tvTotalUsersPrevious.setText(String.format(Locale.getDefault(), "Kỳ trước: %,d", previousTotal));
        tvActiveUsersPrevious.setText(String.format(Locale.getDefault(), "Kỳ trước: %,d", previousActive));
        tvNewUsersPrevious.setText(String.format(Locale.getDefault(), "Kỳ trước: %,d", previousNew));
        tvTotalSessionsPrevious.setText(String.format(Locale.getDefault(), "Kỳ trước: %,d", previousSessions));
        tvTotalDecksPrevious.setText(String.format(Locale.getDefault(), "Kỳ trước: %,d", previousDecks));
        tvTotalVocabPrevious.setText(String.format(Locale.getDefault(), "Kỳ trước: %,d", previousVocab));
    }

    private void updateChangeText(TextView textView, int previous, int current) {
        if (previous == 0) {
            textView.setText(current > 0 ? "↑ 100%" : "0%");
            textView.setTextColor(0xFFA7F3D0);
        } else {
            float change = ((float)(current - previous) / previous) * 100f;
            String sign = change >= 0 ? "↑" : "↓";
            String formatted = String.format(Locale.getDefault(), "%s %.1f%%", sign, Math.abs(change));
            textView.setText(formatted);
            textView.setTextColor(change >= 0 ? 0xFFA7F3D0 : 0xFFFF6B6B);
        }
    }

    private void loadOverallStats() {
        apiService.getAdminStats().enqueue(new Callback<AdminStats>() {
            @Override
            public void onResponse(@NonNull Call<AdminStats> call, @NonNull Response<AdminStats> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AdminStats stats = response.body();
                    updateStatsWithComparison(
                            stats.getTongNguoiDung(),
                            stats.getNguoiHoatDong(),
                            0, // New users
                            0, // Sessions
                            stats.getTongBoTu(),
                            stats.getTongTuVung(),
                            0, 0, 0, 0, 0, 0 // Previous period (no comparison)
                    );
                }
            }

            @Override
            public void onFailure(@NonNull Call<AdminStats> call, @NonNull Throwable t) {
                Toast.makeText(AdminStatisticsActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
