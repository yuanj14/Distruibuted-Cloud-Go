<p align="center">
  <img src="https://www.vectorlogo.zone/logos/springio/springio-icon.svg" alt="Spring Logo" width="80" height="80">
</p>

<h1 align="center" style="margin: 30px 0 30px; font-weight: bold;">sky-mybatis-plus</h1>
<h4 align="center">基于 Spring Boot + MyBatis-Plus 的外卖点餐系统</h4>

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-2.7.x-brightgreen" alt="Spring Boot">
  <img src="https://img.shields.io/badge/MyBatis--Plus-3.5.x-blue" alt="MyBatis-Plus">
  <img src="https://img.shields.io/badge/Redis-7.x-red" alt="Redis">
  <img src="https://img.shields.io/badge/MySQL-8.0-orange" alt="MySQL">
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License">
</p>

---

## 📚 项目简介

苍穹外卖是一款面向餐饮行业的外卖管理系统，包含 **管理后台** 和 **小程序端** 两部分：

- **管理后台**：供餐饮企业内部员工使用，可管理分类、菜品、套餐、订单、员工等
- **小程序端**：供消费者使用，支持浏览菜品、购物车、下单、支付、催单等

## ✨ 项目亮点

| 特性 | 说明 |
|------|------|
| 🚀 **MP 重构** | 全面使用 MyBatis-Plus 替代原生 MyBatis，减少 70% 的 SQL 代码 |
| 📦 **模块化设计** | Maven 多模块架构，层次清晰、职责分明 |
| 🔔 **来单提醒** | WebSocket 实时推送新订单通知 |
| 📊 **数据统计** | 营业额、用户、订单、销量排名等多维度报表 |
| 📈 **POI 导出** | 支持运营数据 Excel 报表导出 |
| 📦 **Redis 缓存** | 菜品、套餐数据缓存，提升查询性能 |
| ⏰ **定时任务** | 自动处理超时订单和派送中订单 |

## 🛠️ 技术栈

### 后端
| 技术 | 说明 |
|------|------|
| Spring Boot 2.7.x | 基础框架 |
| MyBatis-Plus 3.5.x | ORM 框架 |
| MySQL 8.0 | 数据库 |
| Redis 7.x | 缓存 |
| Spring Task | 定时任务 |
| WebSocket | 实时通信 |
| Apache POI | Excel 导出 |
| JWT | 身份认证 |
| Swagger/Knife4j | API 文档 |
| 阿里云 OSS | 文件存储 |


## 📁 项目结构

```
sky-take-out
├── sky-common          # 公共模块（工具类、常量、异常等）
│   ├── constant         # 常量类
│   ├── context          # 上下文工具
│   ├── exception        # 自定义异常
│   ├── properties       # 配置属性类
│   ├── result           # 统一响应结果
│   └── utils            # 工具类
├── sky-pojo            # 实体模块
│   ├── dto              # 数据传输对象
│   ├── po               # 持久化对象
│   └── vo               # 视图对象
├── sky-server          # 业务模块
│   ├── config           # 配置类
│   ├── controller       # 控制器
│   │   ├── admin        # 管理端接口
│   │   └── user         # 用户端接口
│   ├── handler          # 全局处理器
│   ├── interceptor      # 拦截器
│   ├── mapper           # 数据访问层
│   ├── service          # 业务层
│   ├── task             # 定时任务
│   └── websocket        # WebSocket
└── pom.xml              # Maven 配置
```

## 🚀 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.x+

### 安装步骤

```bash
# 1. 克隆项目
git clone https://github.com/your-username/sky-take-out.git
cd sky-take-out

# 2. 创建数据库并导入 SQL
mysql -u root -p < sky-server/src/main/resources/db/sky.sql

# 3. 修改配置文件
# 编辑 sky-server/src/main/resources/application.yaml
# 配置 MySQL、Redis、OSS 等连接信息

# 4. 启动项目
mvn spring-boot:run -pl sky-server

# 5. 访问 API 文档
# http://localhost:8080/doc.html
```

## 📝 MyBatis-Plus 改写要点

### 1️⃣ Mapper 接口继承 BaseMapper

```java
// 原 MyBatis
public interface UserMapper {
    User selectById(Long id);
    int insert(User user);
    // ... 大量方法声明
}

// MP 重写后
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 基础 CRUD 无需声明，直接调用
}
```

### 2️⃣ 使用 LambdaQueryWrapper 构建查询

```java
// 原 MyBatis：需要写 XML
List<Order> orders = orderMapper.selectByStatusAndTime(status, beginTime, endTime);

// MP 重写后：链式调用
List<Orders> orders = orderMapper.selectList(
    new LambdaQueryWrapper<Orders>()
        .eq(Orders::getStatus, status)
        .ge(Orders::getOrderTime, beginTime)
        .le(Orders::getOrderTime, endTime)
);
```

### 3️⃣ 简化分页查询

```java
// MP 分页查询
Page<DishVO> page = new Page<>(pageNum, pageSize);
IPage<DishVO> result = dishMapper.pageQuery(page, queryDTO);
return new PageResult(result.getTotal(), result.getRecords());
```

## 📊 功能模块

### 管理后台

| 模块 | 功能 |
|------|------|
| 员工管理 | 新增、编辑、禁用、分页查询 |
| 分类管理 | 菜品/套餐分类维护 |
| 菜品管理 | 菜品 CRUD、起售/停售、口味管理 |
| 套餐管理 | 套餐 CRUD、起售/停售 |
| 订单管理 | 订单查询、接单、拒单、派送、完成 |
| 数据统计 | 营业额、用户、订单、销量排名Top10 |
| 工作台 | 今日数据、订单概览、菜品/套餐总览 |

### 小程序端

| 模块 | 功能 |
|------|------|
| 微信登录 | 小程序授权登录 |
| 菜品浏览 | 分类查看、菜品详情 |
| 购物车 | 添加、删除、清空 |
| 地址管理 | 新增、编辑、删除、默认地址 |
| 在线下单 | 提交订单、支付 |
| 历史订单 | 查询、再来一单、取消订单、催单 |

## 📸 API 文档

启动项目后访问：`http://localhost:8080/doc.html`
