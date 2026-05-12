package com.motorcycle.repair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.motorcycle.repair.config.SpringContextHolder;
import com.motorcycle.repair.entity.Message;
import com.motorcycle.repair.entity.User;
import com.motorcycle.repair.mapper.MessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageService extends ServiceImpl<MessageMapper, Message> {

    public Page<Message> getMessagePage(Long userId, Integer pageNum, Integer pageSize) {
        Page<Message> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Message> w = new LambdaQueryWrapper<>();
        w.eq(Message::getUserId, userId); w.orderByDesc(Message::getCreateTime);
        return this.page(page, w);
    }

    public Long getUnreadCount(Long userId) {
        return (long) this.count(new LambdaQueryWrapper<Message>().eq(Message::getUserId, userId).eq(Message::getIsRead, 0));
    }

    public void markAsRead(Long id) {
        Message m = this.getById(id);
        if (m != null) { m.setIsRead(1); this.updateById(m); }
    }

    public void markAllAsRead(Long userId) {
        Message update = new Message();
        update.setIsRead(1);
        this.update(update, new LambdaQueryWrapper<Message>().eq(Message::getUserId, userId).eq(Message::getIsRead, 0));
    }

    public void sendMessage(Long userId, String title, String content, Integer type) {
        Message m = new Message();
        m.setUserId(userId); m.setTitle(title); m.setContent(content); m.setType(type); m.setIsRead(0);
        this.save(m);
    }

    public void sendMessage(Long userId, Long senderId, Integer senderRole, String title, String content, Integer type) {
        Message m = new Message();
        m.setUserId(userId); m.setSenderId(senderId); m.setSenderRole(senderRole);
        m.setTitle(title); m.setContent(content); m.setType(type); m.setIsRead(0);
        this.save(m);
    }

    public void sendToShopTechnicians(Long shopId, Long senderId, String title, String content, Integer type) {
        ShopTechnicianService shopTechnicianService = SpringContextHolder.getBean(ShopTechnicianService.class);
        java.util.List<User> techs = shopTechnicianService.getTechniciansByShop(shopId);
        for (User tech : techs) {
            sendMessage(tech.getId(), senderId, 2, title, content, type);
        }
    }
}
