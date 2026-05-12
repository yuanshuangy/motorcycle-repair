package com.motorcycle.repair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.motorcycle.repair.entity.Announcement;
import com.motorcycle.repair.mapper.AnnouncementMapper;
import org.springframework.stereotype.Service;

@Service
public class AnnouncementService extends ServiceImpl<AnnouncementMapper, Announcement> {

    public Page<Announcement> getAnnouncementPage(Integer pageNum, Integer pageSize) {
        Page<Announcement> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Announcement> w = new LambdaQueryWrapper<>();
        w.eq(Announcement::getStatus, 1); w.orderByDesc(Announcement::getCreateTime);
        return this.page(page, w);
    }

    public void addAnnouncement(Announcement a) { a.setStatus(1); this.save(a); }
    public void updateAnnouncement(Announcement a) { this.updateById(a); }
    public void deleteAnnouncement(Long id) { this.removeById(id); }
}
