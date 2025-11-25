package com.nhom1.polydeck.data.api;

import com.nhom1.polydeck.data.model.AdminStats;
import com.nhom1.polydeck.data.model.ApiResponse;
import com.nhom1.polydeck.data.model.BoTu;
import com.nhom1.polydeck.data.model.ForgotPasswordRequest;
import com.nhom1.polydeck.data.model.GoogleLoginRequest;
import com.nhom1.polydeck.data.model.LoginRequest;
import com.nhom1.polydeck.data.model.LoginResponse;
import com.nhom1.polydeck.data.model.RegisterRequest;
import com.nhom1.polydeck.data.model.RegisterResponse;
import com.nhom1.polydeck.data.model.ThongBao;
import com.nhom1.polydeck.data.model.TuVung;
import com.nhom1.polydeck.data.model.User;

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

    @POST("api/auth/forgot-password")
    Call<ApiResponse<Void>> forgotPassword(@Body ForgotPasswordRequest request);

    @POST("api/auth/change-password")
    Call<ApiResponse<Void>> changePassword(@Body com.nhom1.polydeck.data.model.ChangePasswordRequest request);


    // ============= ADMIN DASHBOARD =============
    @GET("api/admin/stats")
    Call<AdminStats> getAdminStats();

    @POST("api/admin/thong-bao")
    Call<Void> createSystemNotification(@Body ThongBao thongBao);


    // ============= USER MANAGEMENT =============
    @GET("api/users")
    Call<List<User>> getAllUsers();

    @GET("api/users/search")
    Call<List<User>> searchUsers(@Query("q") String query);

    @GET("api/users/{id}")
    Call<User> getUserDetail(@Path("id") String userId);

    @PUT("api/users/{id}")
    Call<User> updateUser(@Path("id") String userId, @Body User user);

    @PUT("api/users/{id}/block")
    Call<Void> blockUser(@Path("id") String userId);


    // ============= DECK (CHUDE) MANAGEMENT =============
    @GET("api/chude")
    Call<List<BoTu>> getAllChuDe();

    @GET("api/chude/{id}")
    Call<BoTu> getChuDeDetail(@Path("id") String chuDeId);

    @GET("api/chude/search")
    Call<List<BoTu>> searchChuDe(@Query("q") String query);

    @POST("api/chude")
    Call<BoTu> createChuDe(@Body BoTu boTu);


    // Add Deck with Image
    @Multipart
    @POST("api/chude/chude_with_image")
    Call<BoTu> createChuDeWithImage(@Part MultipartBody.Part file,
                                    @Part("ten_chu_de") RequestBody tenChuDe);

    @PUT("api/chude/{id}")
    Call<BoTu> updateChuDe(@Path("id") String chuDeId, @Body BoTu boTu);

    @DELETE("api/chude/{id}")
    Call<Void> deleteChuDe(@Path("id") String chuDeId);


    // ============= VOCABULARY =============
    @POST("api/chude/{chuDeId}/them-tu-vung")
    Call<TuVung> addTuVungToChuDe(@Path("chuDeId") String chuDeId, @Body TuVung tuVung);

    @GET("api/chude/{chuDeId}/tuvung")
    Call<List<TuVung>> getTuVungByBoTu(@Path("chuDeId") String chuDeId);


    // ============= NOTIFICATIONS =============
    @GET("api/thongbao")
    Call<ApiResponse<List<ThongBao>>> getThongBao(@Query("ma_nguoi_dung") String maNguoiDung);

    @POST("api/thongbao/{id}/read")
    Call<ApiResponse<Void>> markThongBaoRead(@Path("id") String thongBaoId, @Body com.nhom1.polydeck.data.model.ReadRequest body);

    // ============= QUIZ =============
    @GET("api/quizzes/by-topic/{ma_chu_de}")
    Call<ApiResponse<com.nhom1.polydeck.data.model.QuizBundle>> getQuizByTopic(@Path("ma_chu_de") String maChuDe);

    @POST("api/quizzes/submit")
    Call<ApiResponse<com.nhom1.polydeck.data.model.QuizResult>> submitQuiz(@Body com.nhom1.polydeck.data.model.SubmitQuizRequest request);

    @GET("api/quizzes/history/{ma_nguoi_dung}")
    Call<ApiResponse<List<com.nhom1.polydeck.data.model.LichSuLamBai>>> getQuizHistory(@Path("ma_nguoi_dung") String maNguoiDung);
}
