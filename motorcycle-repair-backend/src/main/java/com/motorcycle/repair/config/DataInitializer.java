package com.motorcycle.repair.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.motorcycle.repair.entity.*;
import com.motorcycle.repair.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {
    @Autowired private SystemConfigService systemConfigService;
    @Autowired private DictDataService dictDataService;
    @Autowired private DictTypeService dictTypeService;
    @Autowired private ServiceTemplateService serviceTemplateService;
    @Autowired private EmployeeService employeeService;
    @Autowired private UserService userService;
    @Autowired private RepairShopService repairShopService;
    @Autowired private ShopTechnicianService shopTechnicianService;
    @Autowired private ServiceTypeService serviceTypeService;
    @Autowired private TowPricingService towPricingService;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initSystemConfig();
        initDictType();
        initDictData();
        initServiceTemplate();
        initUsers();
        initShop();
        initShopServices();
        initEmployees();
        initShopTechnicians();
        initTowPricing();
    }

    private void initSystemConfig() {
        if (systemConfigService.count() > 0) return;
        saveConfig("system_name", "摩托车维修预约系统", "系统名称");
        saveConfig("shop_display_name", "张师傅维修店", "用户端店铺显示名称");
        saveConfig("default_avatar", "https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png", "默认头像URL");
        saveConfig("default_business_hours", "08:00-20:00", "默认营业时间");
        saveConfig("default_service_duration", "60", "默认服务时长(分钟)");
        saveConfig("review_tags", "服务态度好,技术专业,速度快,价格合理,沟通顺畅,环境整洁,耐心细致,准时交付", "评价标签");
        saveConfig("rate_texts", "很差,较差,一般,满意,非常满意", "评分文案");
        saveConfig("rest_rules", "①每人每周休2天 ②不能连续休息 ③每项维修技能至少2人在岗 ④不考虑司机技能（人人都是司机）⑤优先安排休息少的技师", "排休规则说明");
        saveConfig("brand_slogan", "专业 · 便捷 · 高效", "品牌口号");
        saveConfig("brand_features", "在线预约维修服务,实时查看维修进度,专业技师团队保障,透明价格安心消费", "品牌特色功能列表");
        saveConfig("login_welcome", "欢迎回来", "登录页欢迎语");
        saveConfig("login_subtitle", "请登录您的账号", "登录页副标题");
        saveConfig("admin_welcome_title", "管理员工作台", "管理员工作台标题");
        saveConfig("shop_welcome_title", "维修店管理台", "维修店工作台标题");
        saveConfig("tech_welcome_title", "技师工作台", "技师工作台标题");
        saveConfig("user_welcome_title", "首页", "用户首页标题");
        saveConfig("shop_intro_title", "店铺运营概览", "维修店介绍标题");
        saveConfig("shop_intro_text", "管理店铺信息、上架维修服务、处理客户预约、分配维修技师、查看维修记录。", "维修店介绍文本");
        saveConfig("tech_intro_title", "维修工作概览", "技师介绍标题");
        saveConfig("tech_intro_text", "查看派单工单、处理维修任务、填写维修记录、维护专业技能。", "技师介绍文本");
        saveConfig("praise_template", "恭喜【{name}】在{period}接单排行榜中荣获{rank}！您的高效服务和专业精神深受客户信赖，是团队中的佼佼者。感谢您的努力付出，期待您再创辉煌！", "表扬信模板");
        saveConfig("rank_labels", "🥇冠军,🥈亚军,🥉季军", "排行名称标签");
        saveConfig("period_labels", "今日,昨日,本周,本月", "时间段标签");
        saveConfig("revenue_periods", "今日,昨日,本周,本月,本季度,今年", "流水统计时间段");
    }

    private void saveConfig(String key, String value, String desc) {
        SystemConfig c = new SystemConfig();
        c.setConfigKey(key);
        c.setConfigValue(value);
        c.setConfigDesc(desc);
        systemConfigService.save(c);
    }

    private void initDictType() {
        if (dictTypeService.count() > 0) return;
        String[][] types = {{"appointment_status","预约状态"},{"pickup_status","接车状态"},{"user_role","用户角色"},{"user_status","用户状态"},{"audit_status","审核状态"},{"message_type","消息类型"},{"pay_status","支付状态"}};
        for (String[] t : types) {
            DictType dt = new DictType();
            dt.setDictType(t[0]);
            dt.setDictName(t[1]);
            dt.setStatus(1);
            dictTypeService.save(dt);
        }
    }

    private void initDictData() {
        if (dictDataService.count() > 0) return;
        String[][] data = {
            {"appointment_status","待确认","0","1","info"},{"appointment_status","已确认","1","2","warning"},
            {"appointment_status","维修中","2","3","primary"},{"appointment_status","已完成","3","4","success"},
            {"appointment_status","已取消","4","5","danger"},{"appointment_status","爽约","5","6","warning"},
            {"pickup_status","待接车","0","1","info"},{"pickup_status","已接车","1","2","primary"},
            {"pickup_status","车辆到店","2","3","success"},{"pickup_status","未接到","3","4","danger"},
            {"user_role","管理员","1","1","danger"},{"user_role","维修店","2","2","warning"},
            {"user_role","用户","3","3","info"},{"user_role","技师","4","4","primary"},
            {"user_status","正常","1","1","success"},{"user_status","禁用","0","2","danger"},
            {"audit_status","待审核","0","1","warning"},{"audit_status","已通过","1","2","success"},
            {"audit_status","已拒绝","2","3","danger"},
            {"message_type","系统","0","1","info"},{"message_type","预约","1","2","warning"},
            {"message_type","聊天","3","3","primary"},{"message_type","表扬","4","4","success"},
            {"pay_status","未支付","0","1","info"},{"pay_status","已支付","1","2","success"},
        };
        for (String[] d : data) {
            DictData dd = new DictData();
            dd.setDictType(d[0]); dd.setDictLabel(d[1]); dd.setDictValue(d[2]);
            dd.setDictSort(Integer.parseInt(d[3])); dd.setCssClass(d[4]); dd.setStatus(1);
            dictDataService.save(dd);
        }
    }

    private void initServiceTemplate() {
        if (serviceTemplateService.count() > 0) return;
        String[][] svcs = {
            {"常规保养","150.00","60","机油更换、滤清器更换、链条检查调整","保养","1"},
            {"轮胎更换","200.00","45","轮胎拆卸安装、动平衡调试","轮胎","2"},
            {"刹车检修","180.00","40","刹车片更换、刹车油更换、刹车系统调试","制动","3"},
            {"发动机维修","500.00","120","发动机拆装、故障检测、零部件更换","发动机","4"},
            {"电路检修","120.00","60","电路系统检查、电瓶更换、线路修复","电气","5"},
            {"链条更换","80.00","30","链条拆卸安装、松紧度调整","传动","6"},
            {"减震器维修","300.00","90","前叉/后减震拆装、油封更换、阻尼调整","悬挂","7"},
            {"化油器清洗","100.00","45","化油器拆洗、混合比调整","燃油","8"},
            {"整车喷漆","800.00","180","车身打磨、底漆面漆喷涂、清漆保护","外观","9"},
            {"年检代办","100.00","30","代为办理摩托车年检手续","代办","10"},
        };
        for (String[] s : svcs) {
            ServiceTemplate t = new ServiceTemplate();
            t.setServiceName(s[0]); t.setPrice(new BigDecimal(s[1]));
            t.setDuration(Integer.parseInt(s[2])); t.setDescription(s[3]);
            t.setCategory(s[4]); t.setSortOrder(Integer.parseInt(s[5]));
            serviceTemplateService.save(t);
        }
    }

    private void initUsers() {
        String encoded = passwordEncoder.encode("admin123");
        String[][] users = {
            {"admin","系统管理员","13800000001","admin@moto.com","1","1","1",""},
            {"shop1","张师傅","13800000002","shop1@moto.com","2","1","1",""},
            {"user1","李明","13800000003","user1@moto.com","3","1","0",""},
            {"zhang001","张伟","13800001001","zhang001@moto.com","4","1","1","司机,常规保养,轮胎更换,刹车检修"},
            {"zhang002","张强","13800001002","zhang002@moto.com","4","1","1","司机,常规保养,电路检修,化油器清洗"},
            {"zhang003","张磊","13800001003","zhang003@moto.com","4","1","1","司机,轮胎更换,减震器维修,链条更换"},
            {"zhang004","张鹏","13800001004","zhang004@moto.com","4","1","1","司机,刹车检修,发动机维修,整车喷漆"},
            {"zhang005","张杰","13800001005","zhang005@moto.com","4","1","1","司机,电路检修,化油器清洗,年检代办"},
            {"zhang006","张涛","13800001006","zhang006@moto.com","4","1","1","司机,链条更换,常规保养,轮胎更换"},
            {"zhang007","张超","13800001007","zhang007@moto.com","4","1","1","司机,减震器维修,发动机维修,刹车检修"},
            {"zhang008","张斌","13800001008","zhang008@moto.com","4","1","1","司机,整车喷漆,年检代办,化油器清洗"},
        };
        for (String[] u : users) {
            User existing = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, u[0]));
            if (existing == null) {
                User user = new User();
                user.setUsername(u[0]); user.setPassword(encoded);
                user.setRealName(u[1]); user.setPhone(u[2]); user.setEmail(u[3]);
                user.setRole(Integer.parseInt(u[4])); user.setStatus(Integer.parseInt(u[5]));
                user.setVerified(Integer.parseInt(u[6])); user.setSkill(u[7].isEmpty() ? null : u[7]);
                userService.save(user);
            } else {
                existing.setRealName(u[1]); existing.setPhone(u[2]); existing.setEmail(u[3]);
                existing.setSkill(u[7].isEmpty() ? null : u[7]);
                userService.updateById(existing);
            }
        }
    }

    private void initShop() {
        User shopUser = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "shop1"));
        if (shopUser == null) return;
        RepairShop existing = repairShopService.getOne(new LambdaQueryWrapper<RepairShop>().eq(RepairShop::getUserId, shopUser.getId()));
        if (existing != null) {
            existing.setShopName("张师傅摩托车维修中心");
            existing.setAddress("北京市朝阳区建国路88号");
            existing.setPhone("010-88888888");
            existing.setDescription("专业摩托车维修保养，20年经验技师团队，提供发动机维修、电路检修、常规保养等全方位服务");
            existing.setBusinessHours("08:00-20:00");
            existing.setRating(4.80);
            existing.setAuditStatus(1);
            existing.setAutoAssign(1);
            existing.setAutoAcceptTech(1);
            repairShopService.updateById(existing);
            return;
        }
        RepairShop shop = new RepairShop();
        shop.setUserId(shopUser.getId());
        shop.setShopName("张师傅摩托车维修中心");
        shop.setAddress("北京市朝阳区建国路88号");
        shop.setPhone("010-88888888");
        shop.setDescription("专业摩托车维修保养，20年经验技师团队，提供发动机维修、电路检修、常规保养等全方位服务");
        shop.setBusinessHours("08:00-20:00");
        shop.setRating(4.80);
        shop.setStatus(1);
        shop.setAuditStatus(1);
        shop.setAutoAssign(1);
        shop.setAutoAcceptTech(1);
        repairShopService.save(shop);
    }

    private void initShopServices() {
        RepairShop shop = repairShopService.getOne(new LambdaQueryWrapper<RepairShop>().eq(RepairShop::getShopName, "张师傅摩托车维修中心"));
        if (shop == null) return;
        List<ServiceTemplate> templates = serviceTemplateService.list();
        for (ServiceTemplate t : templates) {
            ServiceType existing = serviceTypeService.getOne(new LambdaQueryWrapper<ServiceType>()
                    .eq(ServiceType::getShopId, shop.getId()).eq(ServiceType::getServiceName, t.getServiceName()));
            if (existing == null) {
                ServiceType st = new ServiceType();
                st.setShopId(shop.getId()); st.setServiceName(t.getServiceName());
                st.setDescription(t.getDescription()); st.setPrice(t.getPrice().doubleValue());
                st.setDuration(t.getDuration()); st.setStatus(1);
                serviceTypeService.save(st);
            }
        }
    }

    private void initEmployees() {
        RepairShop shop = repairShopService.getOne(new LambdaQueryWrapper<RepairShop>().eq(RepairShop::getShopName, "张师傅摩托车维修中心"));
        if (shop == null) return;
        String[][] emps = {
            {"刘技师","13900000002","维修技师","司机,常规保养,轮胎更换,化油器清洗"},
            {"王技师","13900000003","维修技师","司机,刹车检修,发动机维修,减震器维修"},
            {"赵技师","13900000004","维修技师","司机,电路检修,链条更换,整车喷漆,年检代办"},
        };
        for (String[] e : emps) {
            Employee existing = employeeService.getOne(new LambdaQueryWrapper<Employee>()
                    .eq(Employee::getShopId, shop.getId()).eq(Employee::getEmployeeName, e[0]));
            if (existing == null) {
                Employee emp = new Employee();
                emp.setShopId(shop.getId()); emp.setEmployeeName(e[0]);
                emp.setPhone(e[1]); emp.setPosition(e[2]); emp.setSkill(e[3]); emp.setStatus(1);
                employeeService.save(emp);
            } else {
                existing.setPhone(e[1]); existing.setPosition(e[2]); existing.setSkill(e[3]);
                employeeService.updateById(existing);
            }
        }
        List<Employee> all = employeeService.list(new LambdaQueryWrapper<Employee>().eq(Employee::getShopId, shop.getId()));
        for (Employee emp : all) {
            if (!emp.getEmployeeName().equals("刘技师") && !emp.getEmployeeName().equals("王技师") && !emp.getEmployeeName().equals("赵技师")) {
                employeeService.removeById(emp.getId());
            }
        }
    }

    private void initShopTechnicians() {
        RepairShop shop = repairShopService.getOne(new LambdaQueryWrapper<RepairShop>().eq(RepairShop::getShopName, "张师傅摩托车维修中心"));
        if (shop == null) return;
        String[] techUsernames = {"zhang001","zhang002","zhang003","zhang004","zhang005","zhang006","zhang007","zhang008"};
        for (String username : techUsernames) {
            User tech = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
            if (tech == null) continue;
            ShopTechnician existing = shopTechnicianService.getOne(new LambdaQueryWrapper<ShopTechnician>()
                    .eq(ShopTechnician::getShopId, shop.getId()).eq(ShopTechnician::getUserId, tech.getId()));
            if (existing == null) {
                ShopTechnician st = new ShopTechnician();
                st.setShopId(shop.getId()); st.setUserId(tech.getId()); st.setStatus(1); st.setRestStatus(0);
                shopTechnicianService.save(st);
            }
        }
    }

    private void initTowPricing() {
        if (towPricingService.count() > 0) return;
        double[][] pricings = {{0,3,50,1},{3,10,80,2},{10,20,120,3},{20,999,180,4}};
        for (double[] p : pricings) {
            TowPricing tp = new TowPricing();
            tp.setMinDistance(p[0]); tp.setMaxDistance(p[1]);
            tp.setPrice(new BigDecimal(String.valueOf((int)p[2])));
            tp.setSortOrder((int)p[3]); tp.setStatus(1);
            towPricingService.save(tp);
        }
    }
}
