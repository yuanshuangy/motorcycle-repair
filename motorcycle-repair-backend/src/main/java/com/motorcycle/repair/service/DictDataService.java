package com.motorcycle.repair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.motorcycle.repair.entity.DictData;
import com.motorcycle.repair.mapper.DictDataMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DictDataService extends ServiceImpl<DictDataMapper, DictData> {

    public List<DictData> getByType(String dictType) {
        return this.list(new LambdaQueryWrapper<DictData>()
                .eq(DictData::getDictType, dictType)
                .eq(DictData::getStatus, 1)
                .orderByAsc(DictData::getDictSort));
    }
}
