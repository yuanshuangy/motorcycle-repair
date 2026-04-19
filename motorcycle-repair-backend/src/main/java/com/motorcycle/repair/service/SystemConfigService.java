package com.motorcycle.repair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.motorcycle.repair.entity.SystemConfig;
import com.motorcycle.repair.mapper.SystemConfigMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemConfigService extends ServiceImpl<SystemConfigMapper, SystemConfig> {

    public String getConfigValue(String key) {
        SystemConfig config = this.getOne(new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key));
        return config != null ? config.getConfigValue() : null;
    }

    public Map<String, String> getAllConfigs() {
        List<SystemConfig> list = this.list();
        Map<String, String> map = new HashMap<>();
        for (SystemConfig c : list) map.put(c.getConfigKey(), c.getConfigValue());
        return map;
    }
}
