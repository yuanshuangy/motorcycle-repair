package com.motorcycle.repair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.motorcycle.repair.entity.ServiceTemplate;
import com.motorcycle.repair.entity.ServiceType;
import com.motorcycle.repair.mapper.ServiceTemplateMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServiceTemplateService extends ServiceImpl<ServiceTemplateMapper, ServiceTemplate> {
    @Autowired private ServiceTypeService serviceTypeService;

    public List<ServiceTemplate> getAllTemplates() {
        return this.list();
    }

    public void initShopServices(Long shopId) {
        List<ServiceTemplate> templates = this.list();
        for (ServiceTemplate t : templates) {
            long dup = serviceTypeService.count(new LambdaQueryWrapper<ServiceType>()
                    .eq(ServiceType::getShopId, shopId)
                    .eq(ServiceType::getServiceName, t.getServiceName())
                    .eq(ServiceType::getStatus, 1));
            if (dup > 0) continue;
            ServiceType st = new ServiceType();
            st.setShopId(shopId);
            st.setServiceName(t.getServiceName());
            st.setPrice(t.getPrice() != null ? t.getPrice().doubleValue() : 0.0);
            st.setDuration(t.getDuration());
            st.setDescription(t.getDescription());
            st.setStatus(1);
            serviceTypeService.save(st);
        }
    }
}
