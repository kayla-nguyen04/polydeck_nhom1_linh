package com.nhom1.polydeck.data.api;

import com.nhom1.polydeck.data.model.AdminStats;
import com.nhom1.polydeck.data.model.ApiResponse;
import com.nhom1.polydeck.data.model.UserMonthlyStats;
import com.nhom1.polydeck.data.model.UserDailyStats;
import com.nhom1.polydeck.data.model.BaiQuiz;
import com.nhom1.polydeck.data.model.BoTu;
import com.nhom1.polydeck.data.model.ChangePasswordRequest;
import com.nhom1.polydeck.data.model.ForgotPasswordRequest;
import com.nhom1.polydeck.data.model.GoogleLoginRequest;
import com.nhom1.polydeck.data.model.LichSuLamBai;
import com.nhom1.polydeck.data.model.LoginRequest;
import com.nhom1.polydeck.data.model.LoginResponse;
import com.nhom1.polydeck.data.model.BaiQuiz;
import com.nhom1.polydeck.data.model.Quiz;
import com.nhom1.polydeck.data.model.QuizBundle;
import com.nhom1.polydeck.data.model.QuizResult;
import com.nhom1.polydeck.data.model.ReadRequest;
import com.nhom1.polydeck.data.model.RegisterRequest;
import com.nhom1.polydeck.data.model.RegisterResponse;
import com.nhom1.polydeck.data.model.SubmitQuizRequest;
import com.nhom1.polydeck.data.model.ThongBao;
import com.nhom1.polydeck.data.model.TuVung;
import com.nhom1.polydeck.data.model.User;
import com.nhom1.polydeck.data.model.FavoriteRequest;
import com.nhom1.polydeck.data.model.YeuCauHoTro;
import com.nhom1.polydeck.data.model.DeckProgress;
import com.nhom1.polydeck.data.model.UpdateProgressRequest;
import com.nhom1.polydeck.data.model.AddXpRequest;
import com.nhom1.polydeck.data.model.AddXpResponse;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface APIService {

    // ============= AUTHENTICATION =============
    @POST("api/auth/register")
    Call<ApiResponse<RegisterResponse>> register(@Body RegisterRequest request);

    @POST("api/auth/login")
    Call<ApiResponse<LoginResponse>> login(@Body LoginRequest request);

    @POST("api/auth/google")
    Call<ApiResponse<LoginResponse>> googleLogin(@Body GoogleLoginRequest request);

    @POST("api/auth/change-password")
    Call<ApiResponse<Void>> changePassword(@Body ChangePasswordRequest request);

    @POST("api/auth/forgot-password")
    Call<ApiResponse<Void>> forgotPassword(@Body ForgotPasswordRequest request);

    // ============= ADMIN =============
    @GET("api/admin/stats")
    Call<AdminStats> getAdminStats();

    @GET("api/admin/user-statistics")
    Call<List<UserMonthlyStats>> getUserStatistics(@Query("year") Integer year);

    @GET("api/admin/user-daily-statistics")
    Call<List<UserDailyStats>> getUserDailyStatistics(@Query("year") Integer year, @Query("month") Integer month);

    @GET("api/users/filter-by-date")
    Call<List<User>> getUsersByDateRange(@Query("startDate") String startDate, @Query("endDate") String endDate);

    @GET("api/admin/stats-by-date")
    Call<AdminStats> getAdminStatsByDateRange(@Query("startDate") String startDate, @Query("endDate") String endDate);

    @POST("api/admin/thong-bao")
    Call<Void> createSystemNotification(@Body ThongBao thongBao);

    // ============= NOTIFICATIONS =============
    @GET("api/thongbao/{userId}")
    Call<ApiResponse<List<ThongBao>>> getThongBao(@Path("userId") String userId);

    @POST("api/thongbao/{id}/read")
    Call<ApiResponse<Void>> markThongBaoRead(@Path("id") String thongBaoId, @Body ReadRequest request);

    // ============= USER MANAGEMENT =============
    @GET("api/users")
    Call<List<User>> getAllUsers();

    @GET("api/users/search")
    Call<List<User>> searchUsers(@Query("q") String query);

    @GET("api/users/{id}")
    Call<User> getUserDetail(@Path("id") String userId);

    @PUT("api/users/{id}")
    Call<User> updateUser(@Path("id") String userId, @Body User user);

    @Multipart
    @POST("api/users/{id}/upload-avatar")
    Call<User> uploadUserAvatar(@Path("id") String userId, @Part MultipartBody.Part file);

    @PUT("api/users/{id}/block")
    Call<Void> blockUser(@Path("id") String userId);

    // Favorites
    @GET("api/users/{id}/favorites")
    Call<ApiResponse<List<TuVung>>> getUserFavorites(@Path("id") String userId);

    @POST("api/users/{id}/favorites")
    Call<ApiResponse<Void>> addFavorite(@Path("id") String userId, @Body FavoriteRequest body);

    // Cập nhật streak khi học flashcard
    @POST("api/users/{id}/update-streak")
    Call<ApiResponse<Void>> updateStreak(@Path("id") String userId);

    @DELETE("api/users/{id}/favorites/{fav}")
    Call<ApiResponse<Void>> removeFavorite(@Path("id") String userId, @Path("fav") String favId);


    // ============= DECK (CHUDE) MANAGEMENT =============
    @GET("api/chude")
    Call<List<BoTu>> getAllChuDe();

    @GET("api/chude/{id}")
    Call<BoTu> getChuDeDetail(@Path("id") String chuDeId);

    @GET("api/chude/search")
    Call<List<BoTu>> searchChuDe(@Query("q") String query);

    @POST("api/chude")
    Call<BoTu> createChuDe(@Body BoTu boTu);

    @Multipart
    @POST("api/chude/chude_with_image")
    Call<BoTu> createChuDeWithImage(@Part MultipartBody.Part file, @Part("ten_chu_de") RequestBody tenChuDe);

    @Multipart
    @POST("api/chude/chude_with_image")
    Call<BoTu> updateChuDeWithImage(@Part("id") RequestBody id, @Part MultipartBody.Part file, @Part("ten_chu_de") RequestBody tenChuDe);

    @PUT("api/chude/{id}")
    Call<BoTu> updateChuDe(@Path("id") String chuDeId, @Body BoTu boTu);

    @DELETE("api/chude/{id}")
    Call<Void> deleteChuDe(@Path("id") String chuDeId, @Query("deleteVocab") Boolean deleteVocab);

    // ============= VOCABULARY MANAGEMENT =============
    @POST("api/chude/{chuDeId}/them-tu-vung")
    Call<TuVung> addTuVungToChuDe(@Path("chuDeId") String chuDeId, @Body TuVung tuVung);

    @GET("api/chude/{chuDeId}/tuvung")
    Call<List<TuVung>> getTuVungByBoTu(@Path("chuDeId") String chuDeId);

    @POST("api/chude/{chuDeId}/import-vocab")
    Call<Void> importVocab(@Path("chuDeId") String chuDeId, @Body List<TuVung> vocabList);

    @DELETE("api/tuvung/{id}")
    Call<Void> deleteTuVung(@Path("id") String tuVungId);

    // Xóa một từ vựng trong bộ từ (thử endpoint này nếu endpoint trên không hoạt động)
    @DELETE("api/chude/{chuDeId}/tuvung/{id}")
    Call<Void> deleteTuVungInChuDe(@Path("chuDeId") String chuDeId, @Path("id") String tuVungId);

    // Xóa tất cả từ vựng của một bộ từ (nếu backend hỗ trợ)
    @DELETE("api/chude/{chuDeId}/tuvung")
    Call<Void> deleteAllTuVungByChuDe(@Path("chuDeId") String chuDeId);

    // Tiến độ học tập cho 1 chủ đề của 1 người dùng
    @GET("api/chude/{id}/progress")
    Call<ApiResponse<DeckProgress>> getDeckProgress(@Path("id") String chuDeId, @Query("userId") String userId);

    // Cập nhật tiến độ học tập cho một từ vựng
    @POST("api/chude/{chuDeId}/progress")
    Call<ApiResponse<Void>> updateWordProgress(@Path("chuDeId") String chuDeId, @Body UpdateProgressRequest request);

    // Cộng XP khi học flashcard
    @POST("api/users/{id}/add-xp")
    Call<ApiResponse<AddXpResponse>> addXp(@Path("id") String userId, @Body AddXpRequest request);

    // ============= QUIZ MANAGEMENT =============
    @GET("api/quizzes")
    Call<List<BaiQuiz>> getAllQuizzes();

    @GET("api/quizzes/{id}")
    Call<BaiQuiz> getQuizById(@Path("id") String quizId);

    @POST("api/quizzes")
    Call<ApiResponse<Quiz>> createQuiz(@Body Quiz quiz);

    @DELETE("api/quizzes/{id}")
    Call<Void> deleteQuiz(@Path("id") String quizId);

    @GET("api/quizzes/by-topic/{chuDeId}")
    Call<ApiResponse<QuizBundle>> getQuizByTopic(@Path("chuDeId") String deckId);

    @POST("api/quizzes/submit")
    Call<ApiResponse<QuizResult>> submitQuiz(@Body SubmitQuizRequest request);

    // FIX: Added the missing getQuizHistory method
    @GET("api/quizzes/history/{userId}")
    Call<ApiResponse<List<LichSuLamBai>>> getQuizHistory(@Path("userId") String userId);

    // ============= SUPPORT REQUESTS =============
    @GET("api/hotro")
    Call<List<YeuCauHoTro>> getAllSupportRequests();

    @POST("api/hotro")
    Call<ApiResponse<YeuCauHoTro>> createSupportRequest(@Body YeuCauHoTro request);

    @DELETE("api/hotro/{id}")
    Call<Void> deleteSupportRequest(@Path("id") String requestId);
}