package com.motorcycle.repair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.motorcycle.repair.entity.ChampionMessage;
import com.motorcycle.repair.mapper.ChampionMessageMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ChampionMessageService extends ServiceImpl<ChampionMessageMapper, ChampionMessage> {

    public ChampionMessage getByUserAndDate(Long userId, LocalDate championDate) {
        return this.getOne(new LambdaQueryWrapper<ChampionMessage>()
                .eq(ChampionMessage::getUserId, userId)
                .eq(ChampionMessage::getChampionDate, championDate));
    }

    public ChampionMessage saveOrUpdateMessage(Long userId, LocalDate championDate, String message) {
        ChampionMessage existing = getByUserAndDate(userId, championDate);
        if (existing != null) {
            existing.setMessage(message);
            this.updateById(existing);
            return existing;
        } else {
            ChampionMessage cm = new ChampionMessage();
            cm.setUserId(userId);
            cm.setChampionDate(championDate);
            cm.setMessage(message);
            this.save(cm);
            return cm;
        }
    }
}
