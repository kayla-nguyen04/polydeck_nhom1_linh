package com.nhom1.polydeck.ui.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.User;
import com.nhom1.polydeck.data.model.UserMonthlyStats;
import com.nhom1.polydeck.data.model.UserDailyStats;
import com.nhom1.polydeck.ui.adapter.MonthlyStatisticsAdapter;
import com.nhom1.polydeck.ui.adapter.DailyStatisticsAdapter;
import com.nhom1.polydeck.ui.adapter.UserAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatisticsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private Spinner spinnerStatsType;
    private Button btnViewDaily, btnViewMonthly, btnViewDateRange, btnFilterByDate;
    private LinearLayout layoutDateRange, layoutYearMonth, layoutMonthSelection;
    private RecyclerView rvStatistics, rvUsers;
    private TextView tvStartDate, tvEndDate, tvSelectedYear, tvSelectedMonth;
    private TextView tvTotalUsers, tvNewUsers, tvActiveUsers;
    private TextView tvTotalUsersGrowth, tvNewUsersGrowth, tvActiveUsersGrowth;
    private TextView tvHeaderTime;

    private APIService apiService;
    private MonthlyStatisticsAdapter monthlyAdapter;
    private DailyStatisticsAdapter dailyAdapter;
    private UserAdapter userAdapter;
    private int selectedYear;
    private int selectedMonth;
    private String selectedStatsType = "users"; // users, decks, quizzes, vocabulary
    private boolean isDailyView = true;
    private boolean isDateRangeView = false;
    private Calendar startDate, endDate;
    private List<UserMonthlyStats> monthlyStats = new ArrayList<>();
    private List<UserDailyStats> dailyStats = new ArrayList<>();
    private List<UserMonthlyStats> previousYearStats = new ArrayList<>();
    private List<User> filteredUsers = new ArrayList<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        apiService = RetrofitClient.getApiService();
        initViews();
        setupToolbar();
        setupDateSelectors();
        setupStatsTypeSpinner();
        setupViewTabs();
        setupRecyclerViews();
        
        Calendar cal = Calendar.getInstance();
        selectedYear = cal.get(Calendar.YEAR);
        selectedMonth = cal.get(Calendar.MONTH) + 1;
        
        // Set initial values
        tvSelectedYear.setText(String.valueOf(selectedYear));
        String[] monthNames = {"Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
                              "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"};
        tvSelectedMonth.setText(monthNames[selectedMonth - 1]);
        
        loadDailyStatistics(selectedYear, selectedMonth);
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        spinnerStatsType = findViewById(R.id.spinnerStatsType);
        btnViewDaily = findViewById(R.id.btnViewDaily);
        btnViewMonthly = findViewById(R.id.btnViewMonthly);
        btnViewDateRange = findViewById(R.id.btnViewDateRange);
        btnFilterByDate = findViewById(R.id.btnFilterByDate);
        layoutDateRange = findViewById(R.id.layoutDateRange);
        layoutYearMonth = findViewById(R.id.layoutYearMonth);
        layoutMonthSelection = findViewById(R.id.layoutMonthSelection);
        rvStatistics = findViewById(R.id.rvStatistics);
        rvUsers = findViewById(R.id.rvUsers);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvEndDate = findViewById(R.id.tvEndDate);
        tvSelectedYear = findViewById(R.id.tvSelectedYear);
        tvSelectedMonth = findViewById(R.id.tvSelectedMonth);
        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvNewUsers = findViewById(R.id.tvNewUsers);
        tvActiveUsers = findViewById(R.id.tvActiveUsers);
        tvTotalUsersGrowth = findViewById(R.id.tvTotalUsersGrowth);
        tvNewUsersGrowth = findViewById(R.id.tvNewUsersGrowth);
        tvActiveUsersGrowth = findViewById(R.id.tvActiveUsersGrowth);
        tvHeaderTime = findViewById(R.id.tvHeaderTime);

        startDate = Calendar.getInstance();
        endDate = Calendar.getInstance();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupDateSelectors() {
        // Setup year selector
        tvSelectedYear.setOnClickListener(v -> showYearPicker());
        
        // Setup month selector
        tvSelectedMonth.setOnClickListener(v -> showMonthPicker());
    }
    
    private void showYearPicker() {
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        
        // Create year picker dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Chọn năm");
        
        // Create list of years
        List<Integer> years = new ArrayList<>();
        for (int i = 2020; i <= currentYear + 1; i++) {
            years.add(i);
        }
        
        String[] yearStrings = new String[years.size()];
        for (int i = 0; i < years.size(); i++) {
            yearStrings[i] = String.valueOf(years.get(i));
        }
        
        int currentIndex = years.indexOf(selectedYear);
        if (currentIndex < 0) currentIndex = years.indexOf(currentYear);
        
        builder.setSingleChoiceItems(yearStrings, currentIndex, (dialog, which) -> {
            selectedYear = years.get(which);
            tvSelectedYear.setText(String.valueOf(selectedYear));
            dialog.dismiss();
            
            if (isDailyView) {
                loadDailyStatistics(selectedYear, selectedMonth);
            } else {
                loadMonthlyStatistics(selectedYear);
            }
        });
        
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }
    
    private void showMonthPicker() {
        String[] months = {"Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
                          "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"};
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Chọn tháng");
        
        int currentIndex = selectedMonth - 1;
        
        builder.setSingleChoiceItems(months, currentIndex, (dialog, which) -> {
            selectedMonth = which + 1;
            tvSelectedMonth.setText(months[which]);
            dialog.dismiss();
            
            if (isDailyView) {
                loadDailyStatistics(selectedYear, selectedMonth);
            }
        });
        
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void setupStatsTypeSpinner() {
        String[] statsTypes = {"Người dùng", "Bộ từ", "Quiz", "Từ vựng"};
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                statsTypes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatsType.setAdapter(adapter);
        spinnerStatsType.setSelection(0); // Default: Người dùng
        
        spinnerStatsType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String[] types = {"users", "decks", "quizzes", "vocabulary"};
                selectedStatsType = types[position];
                refreshCurrentView();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void refreshCurrentView() {
        if (isDateRangeView) {
            filterUsersByDateRange();
        } else if (isDailyView) {
            loadDailyStatistics(selectedYear, selectedMonth);
        } else {
            loadMonthlyStatistics(selectedYear);
        }
    }

    private void setupViewTabs() {
        btnViewDaily.setOnClickListener(v -> switchToDailyView());
        btnViewMonthly.setOnClickListener(v -> switchToMonthlyView());
        btnViewDateRange.setOnClickListener(v -> switchToDateRangeView());
        btnFilterByDate.setOnClickListener(v -> filterUsersByDateRange());
        
        tvStartDate.setOnClickListener(v -> showDatePicker(true));
        tvEndDate.setOnClickListener(v -> showDatePicker(false));
    }

    private void switchToDailyView() {
        isDailyView = true;
        isDateRangeView = false;
        // Active button
        btnViewDaily.setBackgroundColor(0xFF6366F1);
        btnViewDaily.setTextColor(0xFFFFFFFF);
        // Inactive buttons
        btnViewMonthly.setBackgroundColor(0xFFE5E7EB);
        btnViewMonthly.setTextColor(0xFF6B7280);
        btnViewDateRange.setBackgroundColor(0xFFE5E7EB);
        btnViewDateRange.setTextColor(0xFF6B7280);
        layoutDateRange.setVisibility(View.GONE);
        layoutYearMonth.setVisibility(View.VISIBLE);
        layoutMonthSelection.setVisibility(View.VISIBLE);
        rvStatistics.setVisibility(View.VISIBLE);
        rvUsers.setVisibility(View.GONE);
        tvHeaderTime.setText("Thống kê theo ngày");
        loadDailyStatistics(selectedYear, selectedMonth);
    }

    private void switchToMonthlyView() {
        isDailyView = false;
        isDateRangeView = false;
        // Active button
        btnViewMonthly.setBackgroundColor(0xFF6366F1);
        btnViewMonthly.setTextColor(0xFFFFFFFF);
        // Inactive buttons
        btnViewDaily.setBackgroundColor(0xFFE5E7EB);
        btnViewDaily.setTextColor(0xFF6B7280);
        btnViewDateRange.setBackgroundColor(0xFFE5E7EB);
        btnViewDateRange.setTextColor(0xFF6B7280);
        layoutDateRange.setVisibility(View.GONE);
        layoutYearMonth.setVisibility(View.VISIBLE);
        layoutMonthSelection.setVisibility(View.GONE);
        rvStatistics.setVisibility(View.VISIBLE);
        rvUsers.setVisibility(View.GONE);
        tvHeaderTime.setText("Thống kê theo tháng");
        loadMonthlyStatistics(selectedYear);
    }

    private void switchToDateRangeView() {
        isDailyView = false;
        isDateRangeView = true;
        // Active button
        btnViewDateRange.setBackgroundColor(0xFF6366F1);
        btnViewDateRange.setTextColor(0xFFFFFFFF);
        // Inactive buttons
        btnViewDaily.setBackgroundColor(0xFFE5E7EB);
        btnViewDaily.setTextColor(0xFF6B7280);
        btnViewMonthly.setBackgroundColor(0xFFE5E7EB);
        btnViewMonthly.setTextColor(0xFF6B7280);
        layoutDateRange.setVisibility(View.VISIBLE);
        layoutYearMonth.setVisibility(View.GONE);
        rvStatistics.setVisibility(View.GONE);
        rvUsers.setVisibility(View.VISIBLE);
        tvHeaderTime.setText("Danh sách người dùng");
        
        // Set default dates if not set
        if (tvStartDate.getText().toString().equals("Chọn ngày bắt đầu")) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -30); // 30 days ago
            startDate = cal;
            tvStartDate.setText(dateFormat.format(startDate.getTime()));
        }
        if (tvEndDate.getText().toString().equals("Chọn ngày kết thúc")) {
            endDate = Calendar.getInstance();
            tvEndDate.setText(dateFormat.format(endDate.getTime()));
        }
    }

    private void showDatePicker(boolean isStartDate) {
        final boolean isStart = isStartDate;
        Calendar initialCalendar = isStart ? startDate : endDate;
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
                    if (isStart) {
                        startDate = selectedCalendar;
                        tvStartDate.setText(dateFormat.format(selectedCalendar.getTime()));
                    } else {
                        endDate = selectedCalendar;
                        tvEndDate.setText(dateFormat.format(selectedCalendar.getTime()));
                    }
                },
                initialYear,
                initialMonth,
                initialDay
        );
        datePickerDialog.show();
    }

    private void filterUsersByDateRange() {
        String startDateText = tvStartDate.getText().toString();
        String endDateText = tvEndDate.getText().toString();
        
        if (startDateText.equals("Chọn ngày bắt đầu") || endDateText.equals("Chọn ngày kết thúc")) {
            Toast.makeText(this, "Vui lòng chọn đầy đủ ngày bắt đầu và ngày kết thúc", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (startDate == null || endDate == null) {
            Toast.makeText(this, "Vui lòng chọn đầy đủ ngày bắt đầu và ngày kết thúc", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (startDate.after(endDate)) {
            Toast.makeText(this, "Ngày bắt đầu phải trước ngày kết thúc", Toast.LENGTH_SHORT).show();
            return;
        }
        
        loadUsersByDateRange();
    }

    private void loadUsersByDateRange() {
        String startDateStr = formatDateForAPI(startDate);
        String endDateStr = formatDateForAPI(endDate);

        apiService.getUsersByDateRange(startDateStr, endDateStr).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    filteredUsers = response.body();
                    userAdapter.updateData(filteredUsers);
                    if (filteredUsers.isEmpty()) {
                        Toast.makeText(StatisticsActivity.this, "Không có người dùng nào trong khoảng thời gian này", Toast.LENGTH_SHORT).show();
                    } else {
                        updateSummaryForDateRange();
                    }
                } else {
                    loadAllUsersAndFilter();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {
                loadAllUsersAndFilter();
            }
        });
    }

    private void loadAllUsersAndFilter() {
        apiService.getAllUsers().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<User> allUsers = response.body();
                    filteredUsers = filterUsersByDateRange(allUsers);
                    userAdapter.updateData(filteredUsers);
                    if (filteredUsers.isEmpty()) {
                        Toast.makeText(StatisticsActivity.this, "Không có người dùng nào trong khoảng thời gian này", Toast.LENGTH_SHORT).show();
                    } else {
                        updateSummaryForDateRange();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {
                Toast.makeText(StatisticsActivity.this, "Lỗi khi tải danh sách người dùng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<User> filterUsersByDateRange(List<User> allUsers) {
        List<User> filtered = new ArrayList<>();
        for (User user : allUsers) {
            if (user.getNgayThamGia() != null && !user.getNgayThamGia().isEmpty()) {
                try {
                    Calendar userDate = Calendar.getInstance();
                    String dateStr = user.getNgayThamGia();
                    
                    SimpleDateFormat[] formats = {
                        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()),
                        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()),
                        new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                        new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    };
                    
                    boolean parsed = false;
                    for (SimpleDateFormat format : formats) {
                        try {
                            userDate.setTime(format.parse(dateStr));
                            parsed = true;
                            break;
                        } catch (Exception e) {}
                    }
                    
                    if (parsed) {
                        Calendar start = (Calendar) startDate.clone();
                        start.set(Calendar.HOUR_OF_DAY, 0);
                        start.set(Calendar.MINUTE, 0);
                        start.set(Calendar.SECOND, 0);
                        start.set(Calendar.MILLISECOND, 0);
                        
                        Calendar end = (Calendar) endDate.clone();
                        end.set(Calendar.HOUR_OF_DAY, 23);
                        end.set(Calendar.MINUTE, 59);
                        end.set(Calendar.SECOND, 59);
                        end.set(Calendar.MILLISECOND, 999);
                        
                        userDate.set(Calendar.HOUR_OF_DAY, 0);
                        userDate.set(Calendar.MINUTE, 0);
                        userDate.set(Calendar.SECOND, 0);
                        userDate.set(Calendar.MILLISECOND, 0);
                        
                        if (!userDate.before(start) && !userDate.after(end)) {
                            filtered.add(user);
                        }
                    }
                } catch (Exception e) {}
            }
        }
        return filtered;
    }

    private String formatDateForAPI(Calendar calendar) {
        SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return apiFormat.format(calendar.getTime());
    }

    private void updateSummaryForDateRange() {
        if (filteredUsers == null || filteredUsers.isEmpty()) {
            tvTotalUsers.setText("0");
            tvNewUsers.setText("0");
            tvActiveUsers.setText("0");
            return;
        }

        int totalUsers = filteredUsers.size();
        int newUsers = totalUsers; // Tất cả đều là người dùng mới trong khoảng thời gian
        int activeUsers = 0;
        for (User user : filteredUsers) {
            if (user.getTrangThai() != null && user.getTrangThai().equals("active")) {
                activeUsers++;
            }
        }

        tvTotalUsers.setText(String.format(Locale.getDefault(), "%,d", totalUsers));
        tvNewUsers.setText(String.format(Locale.getDefault(), "%,d", newUsers));
        tvActiveUsers.setText(String.format(Locale.getDefault(), "%,d", activeUsers));
        
        tvTotalUsersGrowth.setVisibility(View.GONE);
        tvNewUsersGrowth.setVisibility(View.GONE);
        tvActiveUsersGrowth.setVisibility(View.GONE);
    }

    private void setupRecyclerViews() {
        rvStatistics.setLayoutManager(new LinearLayoutManager(this));
        monthlyAdapter = new MonthlyStatisticsAdapter(new ArrayList<>());
        dailyAdapter = new DailyStatisticsAdapter(new ArrayList<>());
        
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserAdapter(this, new ArrayList<>());
        rvUsers.setAdapter(userAdapter);
    }

    private void loadDailyStatistics(int year, int month) {
        apiService.getUserDailyStatistics(year, month).enqueue(new Callback<List<UserDailyStats>>() {
            @Override
            public void onResponse(@NonNull Call<List<UserDailyStats>> call, 
                                 @NonNull Response<List<UserDailyStats>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    dailyStats = response.body();
                    updateDailyList();
                    updateDailySummary();
                } else {
                    loadMockDailyData(year, month);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<UserDailyStats>> call, 
                                @NonNull Throwable t) {
                loadMockDailyData(year, month);
            }
        });
    }

    private void loadMonthlyStatistics(int year) {
        apiService.getUserStatistics(year).enqueue(new Callback<List<UserMonthlyStats>>() {
            @Override
            public void onResponse(@NonNull Call<List<UserMonthlyStats>> call, 
                                 @NonNull Response<List<UserMonthlyStats>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    monthlyStats = response.body();
                    // Load tháng trước trong cùng năm hoặc tháng 12 năm trước nếu là tháng 1
                    loadPreviousMonthStats(year);
                    updateMonthlyList();
                    updateMonthlySummary();
                } else {
                    loadMockMonthlyData(year);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<UserMonthlyStats>> call, 
                                @NonNull Throwable t) {
                loadMockMonthlyData(year);
            }
        });
    }

    private void loadPreviousMonthStats(int year) {
        // Nếu cần so sánh tháng 1 với tháng 12 năm trước, load dữ liệu năm trước
        if (year > 2020 && monthlyStats != null && !monthlyStats.isEmpty()) {
            // Kiểm tra xem có tháng 1 không
            boolean hasJanuary = false;
            for (UserMonthlyStats stats : monthlyStats) {
                if (stats.getMonth() == 1) {
                    hasJanuary = true;
                    break;
                }
            }
            
            // Nếu có tháng 1, cần load tháng 12 năm trước để so sánh
            if (hasJanuary) {
                int previousYear = year - 1;
                apiService.getUserStatistics(previousYear).enqueue(new Callback<List<UserMonthlyStats>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<UserMonthlyStats>> call, 
                                         @NonNull Response<List<UserMonthlyStats>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            previousYearStats = response.body();
                        } else {
                            loadMockPreviousYearData(previousYear);
                        }
                        updateGrowthRates();
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<UserMonthlyStats>> call, 
                                        @NonNull Throwable t) {
                        loadMockPreviousYearData(previousYear);
                        updateGrowthRates();
                    }
                });
                return;
            }
        }
        
        // Nếu không cần, chỉ cần update growth rates
        updateGrowthRates();
    }

    private void loadMockDailyData(int year, int month) {
        dailyStats = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int currentMonth = cal.get(Calendar.MONTH) + 1;
        int currentDay = cal.get(Calendar.DAY_OF_MONTH);
        
        Calendar monthCal = Calendar.getInstance();
        monthCal.set(year, month - 1, 1);
        int daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        int cumulativeUsers = 100;
        for (int day = 1; day <= daysInMonth; day++) {
            boolean isPastDay = (year < currentYear) || (year == currentYear && month < currentMonth) || 
                               (year == currentYear && month == currentMonth && day <= currentDay);
            
            int newUsers = isPastDay ? (1 + (int)(Math.random() * 5)) : 0;
            cumulativeUsers += newUsers;
            int activeUsers = isPastDay ? (int)(cumulativeUsers * 0.6) : 0;
            
            UserDailyStats stats = new UserDailyStats(day, month, year, cumulativeUsers, newUsers, activeUsers);
            dailyStats.add(stats);
        }
        
        updateDailyList();
        updateDailySummary();
    }

    private void loadMockMonthlyData(int year) {
        monthlyStats = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        int currentMonth = cal.get(Calendar.MONTH) + 1;
        int currentYear = cal.get(Calendar.YEAR);
        
        for (int month = 1; month <= 12; month++) {
            int baseUsers = 100 + (month * 10);
            int newUsers = (month <= currentMonth && year == currentYear) ? (5 + (int)(Math.random() * 15)) : 0;
            int activeUsers = (month <= currentMonth && year == currentYear) ? (int)(baseUsers * 0.7) : 0;
            
            UserMonthlyStats stats = new UserMonthlyStats(month, year, baseUsers, newUsers, activeUsers);
            monthlyStats.add(stats);
        }
        
        // Không cần load previous year data nữa, chỉ cần update growth rates
        updateMonthlyList();
        updateMonthlySummary();
        updateGrowthRates();
    }

    private void loadMockPreviousYearData(int year) {
        previousYearStats = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        
        if (year < currentYear) {
            for (int month = 1; month <= 12; month++) {
                int baseUsers = 80 + (month * 8);
                int newUsers = 3 + (int)(Math.random() * 12);
                int activeUsers = (int)(baseUsers * 0.65);
                
                UserMonthlyStats stats = new UserMonthlyStats(month, year, baseUsers, newUsers, activeUsers);
                previousYearStats.add(stats);
            }
        }
    }

    private void updateDailyList() {
        if (dailyAdapter != null) {
            dailyAdapter.updateData(dailyStats);
            rvStatistics.setAdapter(dailyAdapter);
        }
    }

    private void updateMonthlyList() {
        if (monthlyAdapter != null) {
            // Sắp xếp danh sách theo tháng để đảm bảo thứ tự đúng
            List<UserMonthlyStats> sortedStats = new ArrayList<>(monthlyStats);
            sortedStats.sort((a, b) -> {
                if (a.getYear() != b.getYear()) {
                    return Integer.compare(a.getYear(), b.getYear());
                }
                return Integer.compare(a.getMonth(), b.getMonth());
            });
            
            monthlyAdapter.updateData(sortedStats);
            // Set previousYearStats để adapter có thể so sánh tháng 1 với tháng 12 năm trước
            monthlyAdapter.setPreviousStats(previousYearStats);
            rvStatistics.setAdapter(monthlyAdapter);
        }
    }

    private void updateDailySummary() {
        if (dailyStats == null || dailyStats.isEmpty()) {
            tvTotalUsers.setText("0");
            tvNewUsers.setText("0");
            tvActiveUsers.setText("0");
            return;
        }

        int totalUsers = 0;
        int newUsers = 0;
        int activeUsers = 0;

        for (UserDailyStats stats : dailyStats) {
            totalUsers = Math.max(totalUsers, stats.getTotalUsers());
            newUsers += stats.getNewUsers();
            activeUsers = Math.max(activeUsers, stats.getActiveUsers());
        }

        tvTotalUsers.setText(String.format(Locale.getDefault(), "%,d", totalUsers));
        tvNewUsers.setText(String.format(Locale.getDefault(), "%,d", newUsers));
        tvActiveUsers.setText(String.format(Locale.getDefault(), "%,d", activeUsers));
        
        // Ẩn growth rates cho daily view
        tvTotalUsersGrowth.setVisibility(View.GONE);
        tvNewUsersGrowth.setVisibility(View.GONE);
        tvActiveUsersGrowth.setVisibility(View.GONE);
    }

    private void updateMonthlySummary() {
        if (monthlyStats == null || monthlyStats.isEmpty()) {
            tvTotalUsers.setText("0");
            tvNewUsers.setText("0");
            tvActiveUsers.setText("0");
            return;
        }

        // Sắp xếp để lấy tháng cuối cùng
        List<UserMonthlyStats> sortedStats = new ArrayList<>(monthlyStats);
        sortedStats.sort((a, b) -> {
            if (a.getYear() != b.getYear()) {
                return Integer.compare(a.getYear(), b.getYear());
            }
            return Integer.compare(a.getMonth(), b.getMonth());
        });
        
        // Lấy tháng cuối cùng (tháng mới nhất) để hiển thị
        UserMonthlyStats latestStats = sortedStats.get(sortedStats.size() - 1);
        
        int totalUsers = latestStats.getTotalUsers();
        int newUsers = latestStats.getNewUsers();
        int activeUsers = latestStats.getActiveUsers();

        tvTotalUsers.setText(String.format(Locale.getDefault(), "%,d", totalUsers));
        tvNewUsers.setText(String.format(Locale.getDefault(), "%,d", newUsers));
        tvActiveUsers.setText(String.format(Locale.getDefault(), "%,d", activeUsers));
        
        // Hiển thị growth rates cho monthly view
        tvTotalUsersGrowth.setVisibility(View.VISIBLE);
        tvNewUsersGrowth.setVisibility(View.VISIBLE);
        tvActiveUsersGrowth.setVisibility(View.VISIBLE);
    }

    private void updateGrowthRates() {
        if (monthlyStats == null || monthlyStats.isEmpty()) {
            tvTotalUsersGrowth.setText("+0%");
            tvNewUsersGrowth.setText("+0%");
            tvActiveUsersGrowth.setText("+0%");
            return;
        }

        // Sắp xếp danh sách theo năm và tháng
        List<UserMonthlyStats> sortedStats = new ArrayList<>(monthlyStats);
        sortedStats.sort((a, b) -> {
            if (a.getYear() != b.getYear()) {
                return Integer.compare(a.getYear(), b.getYear());
            }
            return Integer.compare(a.getMonth(), b.getMonth());
        });
        
        // Lấy tháng cuối cùng (tháng mới nhất)
        UserMonthlyStats currentMonthStats = sortedStats.get(sortedStats.size() - 1);
        
        // Tìm tháng trước
        UserMonthlyStats previousMonthStats = null;
        
        int currentMonth = currentMonthStats.getMonth();
        int currentYear = currentMonthStats.getYear();
        
        if (currentMonth > 1) {
            // Tháng trước trong cùng năm
            int prevMonth = currentMonth - 1;
            for (UserMonthlyStats stats : sortedStats) {
                if (stats.getYear() == currentYear && stats.getMonth() == prevMonth) {
                    previousMonthStats = stats;
                    break;
                }
            }
        } else {
            // Tháng 1, cần lấy tháng 12 năm trước
            int prevYear = currentYear - 1;
            if (previousYearStats != null && !previousYearStats.isEmpty()) {
                for (UserMonthlyStats stats : previousYearStats) {
                    if (stats.getYear() == prevYear && stats.getMonth() == 12) {
                        previousMonthStats = stats;
                        break;
                    }
                }
            }
        }
        
        // Tính growth rate so với tháng trước
        int currentTotalUsers = currentMonthStats.getTotalUsers();
        int currentNewUsers = currentMonthStats.getNewUsers();
        int currentActiveUsers = currentMonthStats.getActiveUsers();
        
        int previousTotalUsers = previousMonthStats != null ? previousMonthStats.getTotalUsers() : 0;
        int previousNewUsers = previousMonthStats != null ? previousMonthStats.getNewUsers() : 0;
        int previousActiveUsers = previousMonthStats != null ? previousMonthStats.getActiveUsers() : 0;

        float totalUsersGrowth = calculateGrowthRate(previousTotalUsers, currentTotalUsers);
        float newUsersGrowth = calculateGrowthRate(previousNewUsers, currentNewUsers);
        float activeUsersGrowth = calculateGrowthRate(previousActiveUsers, currentActiveUsers);

        updateGrowthTextView(tvTotalUsersGrowth, totalUsersGrowth);
        updateGrowthTextView(tvNewUsersGrowth, newUsersGrowth);
        updateGrowthTextView(tvActiveUsersGrowth, activeUsersGrowth);
    }

    private float calculateGrowthRate(int previousValue, int currentValue) {
        if (previousValue == 0) {
            return currentValue > 0 ? 100f : 0f;
        }
        return ((float)(currentValue - previousValue) / previousValue) * 100f;
    }

    private void updateGrowthTextView(TextView textView, float growthRate) {
        String sign = growthRate >= 0 ? "+" : "";
        String formattedRate = String.format(Locale.getDefault(), "%s%.1f%%", sign, growthRate);
        textView.setText(formattedRate);
        
        int color = growthRate >= 0 ? 0xFF10B981 : 0xFFEF4444;
        textView.setTextColor(color);
    }
}
