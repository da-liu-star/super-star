# MVC 分层与 MyBatis-Plus 持久层重构设计

## 1. 背景与目标

当前项目已经拆分为 `common`、`oauth` 和 `user` 三个 Maven 模块，但
`oauth` 与 `user` 的业务服务、基础设施和持久化访问仍有部分混杂。此次
重构的目标是统一两个微服务的 MVC 分层，并将 `user` 微服务自有数据的
持久层迁移到 MyBatis-Plus。

目标如下：

- `common` 保持共享契约模块定位，不强制采用 MVC。
- `oauth`、`user` 统一采用 `controller -> service -> mapper` 的调用方向。
- `service` 目录只放服务接口，接口使用 `I` 前缀。
- `service/impl` 目录放服务实现类。
- 原 `repository` 包和类型全部替换为 `mapper`。
- `user` 的 `user_account`、`user_identity` 表使用 MyBatis-Plus。
- `oauth` 的授权数据继续使用 Spring Authorization Server 官方 JDBC 实现。
- 不改变现有 HTTP 路径、请求响应契约、数据库表名和已验证的 OAuth 流程。

## 2. 已确认的边界

### 2.1 模块职责

`common` 只保留跨服务 DTO、响应模型和通用异常处理，不增加数据库访问、
MyBatis-Plus 配置或具体业务实现。

`user` 负责：

- 用户账号、密码、资料、权限和状态；
- 第三方身份绑定；
- 第三方 Token 快照和状态；
- 用户内部接口和资源服务器接口；
- `user_account`、`user_identity` 的数据库访问。

`oauth` 负责：

- OAuth2/OIDC 授权码、Access Token、Refresh Token 和同意记录；
- 浏览器登录会话；
- 第三方 OAuth 登录回调；
- 对 User 服务的远程调用；
- 兼容现有的 `/api/user/me` 和 `/api/third-auth/*` 接口。

### 2.2 持久化技术边界

`user` 使用 `mybatis-plus-spring-boot3-starter`，基于 `BaseMapper<T>` 访问
自有业务表。实体字段通过 MyBatis-Plus 注解和下划线转驼峰配置映射到
现有数据库字段。

`oauth` 不迁移 Spring Authorization Server 的授权数据到 MyBatis-Plus。
以下官方实现继续保留：

- `JdbcRegisteredClientRepository`
- `JdbcOAuth2AuthorizationService`
- `JdbcOAuth2AuthorizationConsentService`

这些类型属于 Spring Authorization Server 的协议存储实现，不属于项目
自定义的 `repository` 层。为保持官方表结构、序列化格式和 OAuth 兼容性，
不对其进行重复封装或重写。

## 3. 目标包结构

### 3.1 User 微服务

```text
org.damon.user
├─ config
│  ├─ DataInitializer
│  ├─ InternalApiKeyFilter
│  ├─ MybatisPlusConfig
│  └─ SecurityConfig
├─ controller
│  ├─ InternalUserController
│  └─ UserController
├─ domain
│  ├─ UserAccount
│  └─ UserIdentity
├─ mapper
│  ├─ UserAccountMapper
│  └─ UserIdentityMapper
├─ service
│  ├─ IThirdPartyTokenRefresher
│  └─ IUserService
└─ service/impl
   ├─ UnsupportedTokenRefresherImpl
   └─ UserServiceImpl
```

Mapper 直接继承 `BaseMapper`：

```java
public interface UserAccountMapper extends BaseMapper<UserAccount> {
    UserAccount selectByUsername(String username);
}

public interface UserIdentityMapper extends BaseMapper<UserIdentity> {
    UserIdentity selectByProviderAndOpenid(String provider, String openid);
    List<UserIdentity> selectByUserId(Long userId);
}
```

复杂查询通过 `@Select` 或 XML 实现，优先使用类型安全且易读的 Mapper
方法；不在 Service 中拼接 SQL，也不在 Controller 中执行数据库操作。

### 3.2 OAuth 微服务

```text
org.damon.oauth
├─ client
│  └─ UserClient
├─ config
├─ controller
│  ├─ ThirdAuthController
│  └─ UserController
├─ security
│  └─ OAuth2LoginSuccessHandler
├─ service
│  ├─ ILoginSessionService
│  ├─ IRemoteUserDetailsService
│  ├─ IThirdAuthService
│  └─ IUserRemoteService
└─ service/impl
   ├─ LoginSessionServiceImpl
   ├─ RemoteUserDetailsServiceImpl
   ├─ ThirdAuthServiceImpl
   └─ UserRemoteServiceImpl
```

`UserClient` 只负责 HTTP 请求、序列化和远程异常转换。`IUserRemoteService`
及其实现负责对外暴露面向业务的远程 User 操作，Controller、安全回调和
其他业务组件只依赖 Service 接口，不直接依赖 `UserClient`。

`IRemoteUserDetailsService` 继承 Spring Security 的 `UserDetailsService`，
由 `RemoteUserDetailsServiceImpl` 实现，以便保留 Spring Security 的认证
扩展点，同时满足服务接口和实现分离要求。

`OAuth2LoginSuccessHandler` 属于 Spring Security 基础设施组件，不放入
Controller，但通过 `IThirdAuthService`、`ILoginSessionService` 等接口
调用业务逻辑。

## 4. 分层规则

### 4.1 Controller 层

Controller 只负责：

- 接收 HTTP 参数和请求体；
- 调用 Service 接口；
- 组装 HTTP 响应；
- 处理认证主体和路径参数。

Controller 不允许：

- 直接依赖 Mapper、`JdbcTemplate` 或 `UserClient`；
- 包含用户创建、身份绑定、Token 刷新等业务判断；
- 拼接 SQL 或直接处理数据库异常。

### 4.2 Service 层

Service 接口位于 `service` 包，所有实现位于 `service.impl`。实现类使用
Spring 的 `@Service` 注解，并负责：

- 用户业务规则和事务边界；
- DTO 与领域实体之间的转换；
- Mapper 调用编排；
- 远程 User API 调用编排；
- 业务异常和远程异常的语义转换。

服务实现类命名采用接口名去掉 `I` 后加 `Impl`，例如：

- `IUserService` -> `UserServiceImpl`
- `IThirdAuthService` -> `ThirdAuthServiceImpl`
- `ILoginSessionService` -> `LoginSessionServiceImpl`

### 4.3 Mapper 层

Mapper 位于 `mapper` 包，使用 MyBatis-Plus 的 `BaseMapper<T>`，只负责：

- 单表增删改查；
- 明确的查询条件；
- 数据库字段和实体映射。

Mapper 不负责业务规则、跨表业务编排或远程调用。Mapper 使用接口命名
并以 `Mapper` 结尾，例如 `UserAccountMapper`。

### 4.4 Domain 层

`UserAccount` 和 `UserIdentity` 保留为 MyBatis-Plus 持久化实体，并添加：

- `@TableName`
- `@TableId(type = IdType.AUTO)`
- 必要的 `@TableField`

实体中的 Java 字段保持现有命名，数据库字段保持现有命名。时间字段继续
使用数据库默认值和更新时间机制，避免在迁移过程中改变现有数据语义。

## 5. 数据流与调用关系

### 5.1 密码登录

1. OAuth 登录流程触发 `IRemoteUserDetailsService`。
2. `RemoteUserDetailsServiceImpl` 调用 `IUserRemoteService`。
3. `UserRemoteServiceImpl` 调用 `UserClient` 访问 User 内部接口。
4. User Controller 调用 `IUserService`。
5. `UserServiceImpl` 调用 `UserAccountMapper` 查询账号。
6. OAuth 使用返回的密码和权限完成 Spring Security 认证。

### 5.2 第三方登录

1. OAuth 接收第三方 Provider 回调。
2. `OAuth2LoginSuccessHandler` 调用 `IThirdAuthService`。
3. `ThirdAuthServiceImpl` 通过 `IUserRemoteService` 调用 User 内部接口。
4. User Controller 调用 `IUserService`。
5. `UserServiceImpl` 使用 `UserIdentityMapper` 查找或保存第三方身份，
   必要时使用 `UserAccountMapper` 创建本地账号。
6. OAuth 使用登录结果建立本地会话。

### 5.3 OAuth 授权码和 Token

OAuth Controller 和 Spring Security Filter Chain 继续使用 Spring Authorization
Server 的官方 JDBC 实现。该流程不经过 User Mapper，也不改变已有 OAuth
表结构和序列化字段。

### 5.4 Bearer Token 访问 User

User 服务继续使用资源服务器 JWT 校验。认证通过后，`UserController`
调用 `IUserService`，再由 `UserServiceImpl` 使用 `UserAccountMapper`
读取用户资料。

## 6. 配置与依赖

`user/pom.xml` 新增 MyBatis-Plus Boot 3 Starter：

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.17</version>
</dependency>
```

移除 `user` 对 `spring-boot-starter-jdbc` 的直接使用，除非 Spring Boot
运行时或其他依赖仍明确需要它。数据库连接仍由 MySQL 驱动和 Spring Boot
数据源配置提供。

增加 Mapper 扫描：

```java
@MapperScan("org.damon.user.mapper")
```

增加 MyBatis-Plus 配置：

```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
```

如果使用 XML Mapper，XML 文件放在
`user/src/main/resources/mapper/`，并通过 `mapper-locations` 显式配置。
本次只在注解查询难以表达时使用 XML，不为简单单表操作增加 XML 文件。

## 7. 错误处理与事务

- User 业务异常继续使用 `BizException`，由公共异常处理器转换为统一响应。
- OAuth 到 User 的连接失败和非 2xx 响应继续转换为
  `RemoteServiceException`，HTTP 层返回 502。
- `socialLogin`、身份绑定、解绑、Token 刷新等跨 Mapper 写操作继续使用
  `@Transactional`。
- MyBatis-Plus 的数据库异常不在 Controller 捕获，由公共异常处理和服务
  边界统一处理。
- User 内部接口仍使用 `X-Internal-Token`，不因 Mapper 迁移改变鉴权方式。

## 8. 测试策略

### 8.1 单元测试

- 将 `UserServiceTest` 的依赖替换为 `UserAccountMapper`、
  `UserIdentityMapper` Mock。
- 覆盖第三方登录新建用户、更新已有身份、重复绑定、解绑和 Token 状态
  刷新等业务规则。
- OAuth Service 实现使用 `UserClient` Mock，验证 Service 委托和会话建立。

### 8.2 Mapper 测试

- 增加基于 MySQL 测试数据库或 Testcontainers 的 Mapper 集成测试。
- 验证下划线字段映射、主键自增、默认时间、按用户名查询、按 Provider 和
  OpenID 查询，以及按用户 ID 查询身份。
- 如果本地环境没有可用容器，不把 Docker 作为 Maven 单元测试的硬依赖；
  至少通过完整编译和现有服务启动验证。

### 8.3 端到端验证

保持现有验证流程：

- User 服务启动并自动初始化 `admin / 123456`；
- OAuth 服务启动并初始化 `demo-client / demo-secret`；
- 密码登录、授权码、同意页、Token 交换和 OIDC `userinfo` 成功；
- OAuth Bearer Token 访问 User `/api/user/me` 成功；
- 内部接口无 Token 返回 401，正确内部 Token 返回成功。

## 9. 迁移与兼容

- 数据库表 `user_account`、`user_identity` 不改名、不改字段语义。
- 现有 `schema.sql` 保留，并作为 MyBatis-Plus 迁移后的初始化脚本。
- 删除项目自定义的 `repository` 包和 JDBC Repository 类，不删除 OAuth
  框架内部 JDBC 实现。
- 更新测试、启动初始化器、文档和包引用。
- 迁移完成后执行 `mvn clean test` 和 `mvn clean package -DskipTests`。
- 使用 MySQL 实际启动两个服务，验证已有数据和新增数据均可读写。

## 10. 验收标准

满足以下条件视为完成：

1. `oauth`、`user` 中不存在项目自定义的 `repository` 包。
2. 所有业务 Service 接口位于 `service`，实现类位于 `service/impl`。
3. Controller 不直接依赖 Mapper 或 `UserClient`。
4. User 自定义数据库访问全部通过 MyBatis-Plus Mapper。
5. OAuth 授权相关官方 JDBC 实现和表结构保持可用。
6. Maven 聚合测试和打包通过。
7. 密码登录、第三方登录、授权码、OIDC 和资源服务器访问流程通过。
8. 数据库表名仍为 `user_account`、`user_identity` 及现有 OAuth 表名。
