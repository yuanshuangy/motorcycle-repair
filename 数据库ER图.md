# 数据库 E-R 图（实体联系图）

## 核心实体关系

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              用户 (sys_user)                                        │
│  ┌───────────────────────────────────────────────────────────────────────────────┐  │
│  │  id          BIGINT       PK        ◄────────────────┐                       │  │
│  │  username    VARCHAR(50)  UNIQUE    │                │                       │  │
│  │  password    VARCHAR(255)            │                │                       │  │
│  │  real_name   VARCHAR(100)           │                │                       │  │
│  │  phone       VARCHAR(20)            │                │                       │  │
│  │  email       VARCHAR(100)           │                │                       │  │
│  │  avatar      VARCHAR(500)           │                │                       │  │
│  │  role        INT           (1:用户 2:店铺 3:技师 4:管理员)  │                │  │
│  │  status      INT           (0:禁用 1:正常)              │                │  │
│  │  skill       VARCHAR(500)           │                │                       │  │
│  │  create_time DATETIME               │                │                       │  │
│  │  update_time DATETIME               │                │                       │  │
│  │  deleted     INT           (逻辑删除)│                │                       │  │
│  └──────────────────────────────────────┴────────────────┘                       │  │
│                                              │                                    │  │
│                                              │ 1:N                                │  │
│                                              │ (一个用户可拥有多个预约)           │  │
│                                              │                                    │  │
│                                              │ 1:1                                │  │
│                                              │ (用户拥有店铺)                     │  │
│                                              ▼                                    │  │
│  ┌───────────────────────────────────────────────────────────────────────────────┐  │
│  │                              维修店铺 (repair_shop)                          │  │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐ │  │
│  │  │  id            BIGINT       PK    ◄─────────┐                          │ │  │
│  │  │  user_id       BIGINT       FK ────────────┼──────┐                    │ │  │
│  │  │  shop_name     VARCHAR(100)                 │      │                    │ │  │
│  │  │  address       VARCHAR(500)                 │      │                    │ │  │
│  │  │  phone         VARCHAR(20)                  │      │                    │ │  │
│  │  │  description   TEXT                          │      │                    │ │  │
│  │  │  business_hours VARCHAR(200)               │      │                    │ │  │
│  │  │  license      VARCHAR(500)                  │      │                    │ │  │
│  │  │  cover_image  VARCHAR(500)                  │      │                    │ │  │
│  │  │  rating       DOUBLE                        │      │                    │ │  │
│  │  │  status       INT             (0:禁用 1:营业)│      │                    │ │  │
│  │  │  audit_status INT             (审核状态)     │      │                    │ │  │
│  │  │  auto_confirm INT             (自动确认订单) │      │                    │ │  │
│  │  │  create_time  DATETIME                   │      │                    │ │  │
│  │  │  update_time  DATETIME                   │      │                    │ │  │
│  │  │  deleted      INT                         │      │                    │ │  │
│  │  └────────────────────────────────────────────┴──────┴────────────────────┘ │  │
│  │                                              │                                  │  │
│                                              └──────────────┬───────────────────────┘  │
│                                                              │                        │
│                                              1:N            │                        │
│                                              (一个店铺可接收多个预约)               │
│                                              ▼                                    │
│  ┌───────────────────────────────────────────────────────────────────────────────┐  │
│  │                              预约订单 (appointment)                          │  │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐ │  │
│  │  │  id                  BIGINT       PK                                    │ │  │
│  │  │  order_no            VARCHAR(50)  UNIQUE                                │ │  │
│  │  │  user_id             BIGINT       FK ────────────┐                      │ │  │
│  │  │  shop_id             BIGINT       FK ────────────┼──────┐               │ │  │
│  │  │  service_id          BIGINT       FK             │      │               │ │  │
│  │  │  employee_id         BIGINT       FK (技师ID)    │      │               │ │  │
│  │  │  motorcycle_brand   VARCHAR(50)                 │      │               │ │  │
│  │  │  motorcycle_model   VARCHAR(50)                 │      │               │ │  │
│  │  │  problem_description TEXT                        │      │               │ │  │
│  │  │  appointment_time    DATETIME                    │      │               │ │  │
│  │  │  status              INT        (0:待确认 1:已确认 2:进行中 3:已完成 4:已取消)│ │  │
│  │  │  total_amount        DECIMAL(10,2)               │      │               │ │  │
│  │  │  pay_status          INT        (0:未支付 1:已支付)│     │               │ │  │
│  │  │  pay_time            DATETIME                    │      │               │ │  │
│  │  │  complete_time       DATETIME                    │      │               │ │  │
│  │  │  tow_service         INT        (0:无需 1:需要)   │      │               │ │  │
│  │  │  create_time         DATETIME                    │      │               │ │  │
│  │  │  update_time         DATETIME                    │      │               │ │  │
│  │  │  deleted             INT                         │      │               │ │  │
│  │  └─────────────────────────────────────────────────┴──────┴───────────────┘ │  │
│  └───────────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

## 关系说明

```
┌─────────────────┐         1         ┌─────────────────┐
│                 │ ───────────────── │                 │
│      User       │                   │   RepairShop    │
│    (用户)       │         1         │    (维修店)      │
│                 │ ◄─────────────────│                 │
└─────────────────┘                   └─────────────────┘
       │ 1                                   │ 1
       │                                      │
       │ N                                    │ N
       │                                      │
       └────────────┐                         │
                    │                         │
                    ▼                         │
          ┌─────────────────┐                 │
          │   Appointment   │◄────────────────┘
          │    (预约订单)    │
          └─────────────────┘
```

## 实体关联说明

| 关系 | 主体 | 关联实体 | 说明 |
|:----:|:----:|:--------:|:-----|
| **1:1** | User ↔ RepairShop | - | 一个用户只能拥有一个维修店铺 |
| **1:N** | User → Appointment | user_id | 一个用户可以创建多个预约订单 |
| **1:N** | RepairShop → Appointment | shop_id | 一个维修店可以接收多个预约订单 |

## 核心业务逻辑

```
用户浏览店铺 ──────► 提交预约订单 ──────► 店铺确认 ──────► 技师接单 ──────► 维修完成
   │                    │                   │               │              │
   │                    ▼                   ▼               ▼              ▼
   │              appointment        appointment      appointment    appointment
   │              (user_id)           (shop_id)        (employee_id)  (status=3)
   │                                                              │
   │                                                              ▼
   │                                                      支付订单 (pay_status=1)
   │                                                              │
   │                                                              ▼
   │                                                      评价服务 (review表)
   │                                                              │
   └──────────────────────────────────────────────────────────────┘
```

## 字段类型速查表

| 表名 | 实体类 | 主要字段 |
|:----:|:------:|:---------|
| **sys_user** | User | id, username, role(1用户/2店铺/3技师/4管理员), status, skill |
| **repair_shop** | RepairShop | id, user_id(FK), shop_name, address, phone, auto_confirm(自动确认) |
| **appointment** | Appointment | id, order_no, user_id(FK), shop_id(FK), service_id(FK), status, total_amount |
