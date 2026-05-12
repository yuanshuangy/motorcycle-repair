package com.motorcycle.repair.dto;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class StatisticsDTO {
    private Long userCount;
    private Long shopCount;
    private Long serviceCount;
    private Long appointmentCount;
    private Long todayAppointmentCount;
    private Long pendingAppointmentCount;
    private Long processingAppointmentCount;
    private Long completedAppointmentCount;
    private Long cancelledAppointmentCount;
    private Double totalRevenue;
    private Double todayRevenue;
    private Double yesterdayRevenue;
    private Double weekRevenue;
    private Double monthRevenue;
    private Double quarterRevenue;
    private Double yearRevenue;
    private Double todayProfit;
    private Double yesterdayProfit;
    private Double weekProfit;
    private Double monthProfit;
    private Double quarterProfit;
    private Double yearProfit;
    private Long myAppointmentCount;
    private Long myPendingCount;
    private Long myCompletedCount;
    private Long unreadMessageCount;
    private Long technicianCount;
    private Long employeeCount;
    private List<Map<String, Object>> revenueRanking;
    private List<Map<String, Object>> orderRanking;
    private List<Map<String, Object>> yesterdayRanking;
    private Long myRankInShop;
    private Long myOrderRank;
}
