package com.nhom1.polydeck.data.model;

import com.google.gson.annotations.SerializedName;

public class UserDailyStats {
    @SerializedName(value = "day", alternate = {"ngay"})
    private int day;

    @SerializedName(value = "month", alternate = {"thang"})
    private int month;

    @SerializedName(value = "year", alternate = {"nam"})
    private int year;

    @SerializedName(value = "total_users", alternate = {"tong_nguoi_dung"})
    private int totalUsers;

    @SerializedName(value = "new_users", alternate = {"nguoi_dung_moi"})
    private int newUsers;

    @SerializedName(value = "active_users", alternate = {"nguoi_dung_hoat_dong"})
    private int activeUsers;

    public UserDailyStats() {}

    public UserDailyStats(int day, int month, int year, int totalUsers, int newUsers, int activeUsers) {
        this.day = day;
        this.month = month;
        this.year = year;
        this.totalUsers = totalUsers;
        this.newUsers = newUsers;
        this.activeUsers = activeUsers;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(int totalUsers) {
        this.totalUsers = totalUsers;
    }

    public int getNewUsers() {
        return newUsers;
    }

    public void setNewUsers(int newUsers) {
        this.newUsers = newUsers;
    }

    public int getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(int activeUsers) {
        this.activeUsers = activeUsers;
    }
}


