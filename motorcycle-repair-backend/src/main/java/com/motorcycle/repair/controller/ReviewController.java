package com.motorcycle.repair.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.motorcycle.repair.dto.Result;
import com.motorcycle.repair.dto.ReviewDTO;
import com.motorcycle.repair.entity.Review;
import com.motorcycle.repair.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    @Autowired private ReviewService reviewService;

    @GetMapping("/page")
    public Result<Page<ReviewDTO>> getReviewPage(
            @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long shopId, @RequestParam(required = false) Long userId) {
        return Result.success(reviewService.getReviewPage(pageNum, pageSize, shopId, userId));
    }

    @PostMapping
    public Result<Void> addReview(@RequestBody Review review) { reviewService.addReview(review); return Result.success(); }
}
