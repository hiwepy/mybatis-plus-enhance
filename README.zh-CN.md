# mybatis-plus-enhance

[English](./README.md) | [简体中文](./README.zh-CN.md)

面向 MyBatis-Plus 的模块化增强框架：透明加解密、表级签名、租户与数据权限支持、国际化与 SQL 观测

> **当前分支**：`feature/1.0.x`
> **版本**：`1.0.x.20260630-SNAPSHOT`
> **JDK 基线**：8
> **项目状态**：维护中（1.0.x 线）。尚未发布 Maven Central；制品通过 Aliyun Maven 仓库与 GitHub Releases 分发。

## 目录

- [1. 项目概述](#1-项目概述)
- [2. 能力与状态](#2-features--status)
- [3. 运行要求与兼容性](#3-requirements--compatibility)
- [4. 架构与模块](#4-architecture--modules)
- [5. 引入依赖](#5-installation)
- [6. 快速开始](#6-quick-start)
- [7. 配置](#7-configuration)
- [8. 核心用法](#8-core-usage)
- [9. 测试与构建](#9-testing--build)
- [10. 版本线与分支](#10-versioning--branches)
- [11. 贡献与许可证](#11-contributing--license)

## 1. 项目概述

### 1.1 是什么

**mybatis-plus-enhance** 是面向 MyBatis-Plus（3.5.14）的基础增强组件，在不替代官方插件体系的前提下补充以下能力：

- 字段透明加密、查询结果解密与 HMAC 校验；
- 表级数据签名、验签与历史数据补签；
- 租户上下文与官方 `TenantLineInnerInterceptor` 的默认适配器；
- 面向官方 `DataPermissionInterceptor` 的注解数据范围元数据与表达式扩展点；
- 实体字段国际化（i18n）映射；
- MySQL `INSERT IGNORE` 作用域；
- 超长 SQL 检测、真实执行耗时观测与慢 SQL 日志；
- 带查询后、更新后与执行完成回调的统一增强拦截器链。

### 1.2 不是什么

- **不是 MyBatis-Plus 的替代品**。分页、乐观锁、租户 SQL 解析、数据权限解析等仍由官方拦截器负责；本组件的租户与数据权限能力应与官方拦截器**组合**使用。
- **不是 Spring Boot Starter**。Spring 集成（事务、依赖注入）隔离在 `mybatis-plus-enhance-spring` 模块，`core` 与 `extension` 保持 Spring 无关。
- **不重新实现 SQL 改写**。SQL 条件注入委托给官方插件。

### 1.3 典型使用场景

| 场景 | 推荐入口 | 结果 |
|---|---|---|
| 敏感字段（手机号、身份证号）透明加密 | `DataEncryptionInnerInterceptor` + `EncryptedFieldHandler` | 落库为密文，读取还原为明文 |
| 检测绕过应用的关键业务行篡改 | `DataSignatureInnerInterceptor` + `@TableSignature` | 读取时校验 HMAC 行签名 |
| 多租户应用且不重新解析 SQL | `TenantContext` + `DefaultTenantLineHandler` + 官方 `TenantLineInnerInterceptor` | 安全注入租户条件 |
| SQL 条件之外的行级授权 | `@DataScopePlus` + `DataScopeExpressionProvider` | 对授权查询生成数据范围表达式 |
| 多语言实体字段 | `DataI18nInnerInterceptor` + `I18nContext` | 同行返回翻译值，无额外 SQL |
| 发现巨型语句与真实延迟 | `LongSqlInnerInterceptor` + `SqlObservationInnerInterceptor` | 结构信号与性能信号分离 |

<a id="2-features--status"></a>
## 2. 能力与状态

| 能力 | 状态 | 说明 |
|---|:---:|---|
| 字段透明加解密 | 可用 | `@EncryptedField`；每次加密随机 IV；携带 `version/keyId/algorithm/mode/padding/iv/ciphertext/mac` 的版本化信封 |
| HMAC 校验 | 可用 | `EncryptedFieldHandler.verifyHmac`；加密密钥与认证密钥分离 |
| 表级签名与验签 | 可用 | `@TableSignature` / `@TableSignatureField`；通过带签名语义的 Service API 批量补签 |
| 多租户上下文 | 可用 | 基于 TTL 的 `TenantContext`；缺少租户 ID 时快速失败，不生成无租户条件的 SQL |
| 数据权限（数据范围）扩展 | 可用 | `@DataScopePlus` + `DataScopeAnnotationHandler` + `DataScopeExpressionProvider` |
| 实体国际化映射 | 可用 | `DefaultDataI18nHandler` 处理 `@I18nColumn`；不产生 N+1 查询 |
| MySQL `INSERT IGNORE` | 可用 | 仅显式作用域内改写；仅限 MySQL 方言 |
| 超长 SQL 检测 | 可用 | 按字符数检测，不执行 `EXPLAIN`，不依赖耗时判断 |
| SQL 观测与慢 SQL 日志 | 可用 | `SqlObservation` 携带 Mapper ID、SQL、纳秒耗时与异常；接收器支持 API 或 `ServiceLoader` 注册 |
| 查询后 / 更新后 / 执行完成生命周期 | 可用 | `EnhanceInnerInterceptor` 在官方 `InnerInterceptor` 钩子之上新增三个回调 |
| 增强 SQL 注入器 | 可用 | `EnhanceSqlInjector` 注入 `selectIgnoreDecryptById`、`selectIgnoreDecryptList`、`updateSignatureById` 等方法 |

<a id="3-requirements--compatibility"></a>
## 3. 运行要求与兼容性

### 3.1 基线（当前分支）

| 组件 | 版本 | 说明 |
|---|---:|---|
| JDK | 8+ | 由 `maven-enforcer-plugin` 强制校验 |
| Maven | 3.0+ | Enforcer 下限；CI-Friendly `${revision}` 需要 Maven 3.5+ |
| MyBatis | 3.5.19 | 固定版本 |
| MyBatis-Plus | 3.5.14 | 固定版本 |
| JSqlParser | 4.9 | 通过 `mybatis-plus-jsqlparser-4.9` 引入 |
| Spring Framework | 5.3.39 | 仅 `mybatis-plus-enhance-spring` 模块 |
| Hutool core + crypto | 5.8.40 | 默认密码处理器使用 |
| Jackson | 2.17.2 | 信封 JSON 序列化 |
| API 命名空间 | `javax.*` | `javax.annotation-api` 1.3.2 |

### 3.2 版本线矩阵

| 版本线 | 分支 | JDK | 版本模式 | 技术栈 |
|---|---|---:|---|---|
| 1.0.x | `feature/1.0.x` | 8 | `1.0.x.*` | Spring 5.3.x、`javax.*`、JSqlParser 4.9 |
| 2.0.x | `feature/2.0.x` | 17 | `2.0.x.*` | Spring 6.2.x、`jakarta.*`、`mybatis-plus-jsqlparser` |
| 3.0.x | `feature/3.0.x` | 21 | `3.0.x.*` | `jakarta.*`、`mybatis-plus-jsqlparser` |

各版本线按分支独立维护（不做共享源码的 Profile 切换）。`1.0.x` 线只接受兼容性修复与仍可在 JDK 8 运行的依赖升级。完整规则见 `COMPATIBILITY.md`。

<a id="4-architecture--modules"></a>
## 4. 架构与模块

```text
[ 业务应用 ]
        |
        | MyBatis-Plus + mybatis-plus-enhance
        v
+-----------------------------------------+
| MybatisPlusEnhanceInterceptor（拦截器链）|
|  SQL_REWRITE       租户 / 数据范围       |
|  PARAMETER_ENCRYPT   数据加密            |
|  DATA_SIGNATURE      数据签名            |
|  RESULT_DECRYPTION   结果解密            |
|  RESULT_I18N         国际化              |
|  OBSERVATION         SQL 观测            |
+-----------------------------------------+
        |
        v
[ MyBatis / MyBatis-Plus + JDBC ] -> [ 数据库 ]
```

| 模块 | 职责 |
|---|---|
| `mybatis-plus-enhance-core` | 增强拦截器契约、查询后 / 更新后 / 执行完成生命周期（`EnhanceInnerInterceptor`、`EnhancePhase`）与公共基础类。Spring 无关。 |
| `mybatis-plus-enhance-extension` | 可插拔实现：加密、签名、租户、数据权限、国际化、SQL 处理与执行观测。Spring 无关。传递引入 `mybatis-plus-enhance-core` 与 `mybatis-enhance-annotation`。 |
| `mybatis-plus-enhance-spring` | Spring 集成：带签名语义的 `IEnhanceService` / `EnhanceServiceImpl`、依赖注入与事务支持。 |

选型原则：普通 MyBatis-Plus 项目按能力选 `core` 或 `extension`；需要带签名语义的 Service、依赖注入与事务时选 `spring`。

<a id="5-installation"></a>
## 5. 引入依赖

Maven：

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>mybatis-plus-enhance-extension</artifactId>
    <version>1.0.x.20260630-SNAPSHOT</version>
</dependency>
```

Gradle：

```groovy
implementation 'io.github.easy4j:mybatis-plus-enhance-extension:1.0.x.20260630-SNAPSHOT'
```

需要带签名语义的 Service 与事务时改用 `mybatis-plus-enhance-spring`（传递引入 extension）；只需要最小拦截器契约时依赖 `mybatis-plus-enhance-core`。

快照版本需要启用对应快照仓库（`pom.xml` 中 `distributionManagement` 指向 Aliyun Maven 仓库）。生产环境建议锁定经过验证的发布版本，不要使用动态版本范围。

<a id="6-quick-start"></a>
## 6. 快速开始

以下配置一次启用「写入加密 → 写入签名 → 读取验签解密 → 慢 SQL 日志」：

```java
@Configuration
public class MybatisConfiguration {

    @Bean
    public MybatisPlusEnhanceInterceptor mybatisPlusInterceptor(
            EncryptedFieldHandler encryptedFieldHandler,
            DataSignatureHandler dataSignatureHandler) {

        MybatisPlusEnhanceInterceptor interceptor = new MybatisPlusEnhanceInterceptor();

        // 写入：先加密，再基于最终密文生成行签名。
        interceptor.addInnerInterceptor(
                new DataEncryptionInnerInterceptor(encryptedFieldHandler, true));
        interceptor.addInnerInterceptor(
                new DataSignatureInnerInterceptor(dataSignatureHandler, true, true));

        // 读取：注册顺序决定 afterQuery 顺序——先验签，后解密。
        interceptor.addInnerInterceptor(
                new DataDecryptionInnerInterceptor(encryptedFieldHandler, true));

        interceptor.addInnerInterceptor(
                new SqlObservationInnerInterceptor(new SlowSqlLoggingSink(500)));
        return interceptor;
    }
}
```

声明实体：

```java
@EncryptedTable
public class CustomerPO {
    private Long id;
    @EncryptedField
    private String mobile;
    @EncryptedField
    private String idCard;
}
```

**预期结果**：插入时 `mobile` / `idCard` 列落库为密文（并附带 HMAC）；查询时透明验签并解密还原为明文；超过 500 ms 的语句由 `SlowSqlLoggingSink` 输出慢 SQL 日志。

<a id="7-configuration"></a>
## 7. 配置

本库通过代码配置拦截器链，无 Spring 配置属性（Spring 模块基于 Bean 装配）。拦截器顺序是正确性约束，不只是性能配置：

1. 租户、数据权限等 SQL 条件插件先完成条件注入；
2. 写入参数先加密，再基于最终入库值生成签名；
3. 查询结果先验签，再解密为业务值；
4. SQL 观测放在链尾，接收最终执行结果。

同一个 MyBatis `Configuration` 不要同时注册官方 `MybatisPlusInterceptor` 与 `MybatisPlusEnhanceInterceptor`，否则内部插件可能重复执行。

关键扩展点：

| 扩展点 | 用途 |
|---|---|
| `EncryptedFieldHandler` | 加解密 / HMAC 契约；提供 `DefaultEncryptedFieldHandler`（Hutool + Jackson + HMAC） |
| `CryptoKeyProvider` | 提供当前密钥与受控历史密钥；测试可用 `StaticCryptoKeyProvider` |
| `DataSignatureHandler` / `DataSignatureReadWriteProvider` | 行签名计算与存储（实体字段或外部存储） |
| `TenantContext` | 基于 TTL 的租户持有者，提供 `open(tenantId)` 作用域 API |
| `DataScopeExpressionProvider` | 从自有认证模型生成 JSqlParser 表达式（可使用 `DataScopeExpressions` 工具） |
| `I18nContext` | 基于 TTL 的 Locale 持有者，提供 `open(Locale)` 作用域 API |
| `SqlObservationSink` | 消费 `SqlObservation`；可通过 Java `ServiceLoader` 自动发现 |
| `ResultObjectCopier` | 默认 `ReflectionResultObjectCopier`；禁用 MyBatis 本地缓存时可改用 `noCopy()` |

密钥、IV 与 HMAC 密钥必须由受控密钥系统提供：禁止提交到 Git、打印到日志或暴露在异常信息中。加密与认证建议使用不同密钥，并记录密钥版本（`keyId`）以支持轮换。

<a id="8-core-usage"></a>
## 8. 核心用法

### 8.1 表级签名

```java
@TableSignature
public class OrderPO {
    @TableSignatureField(order = 1)
    private String orderNo;
    @TableSignatureField(order = 2)
    private BigDecimal amount;
    @TableSignatureField(stored = true)
    private String rowSignature;   // 保存签名结果，不参与原文拼接
}
```

```java
public interface OrderMapper extends EnhanceBaseMapper<OrderPO> { }

@Service
public class OrderService extends EnhanceServiceImpl<OrderMapper, OrderPO> { }
```

同时配置 `EnhanceSqlInjector` 以启用 `selectIgnoreDecryptById`、`selectIgnoreDecryptList`、`updateSignatureById` 等内部查询。应用若已有自定义 `ISqlInjector`，应合并其方法列表，不能注册两个互相覆盖的注入器。`saveSigned`、`saveBatchSigned`、`updateSignedById`、`pageSigned` 等方法将签名 / 验签纳入调用语义；批量写入必须在事务中执行，确保业务数据与签名同时提交或回滚。

### 8.2 自定义生命周期增强

```java
public final class AuditInnerInterceptor implements EnhanceInnerInterceptor {

    @Override
    public void afterExecution(Executor executor, MappedStatement ms,
            Object parameter, BoundSql boundSql, Object result,
            Throwable failure, long elapsedNanos) {
        // 只做轻量旁路处理。
    }
}
```

`afterQuery` / `afterUpdate` 属于主执行链，异常会影响业务调用；`afterExecution` 是旁路通知，其运行时异常会被记录并隔离——需要强一致审计时不要依赖被隔离的旁路回调，应使用业务事务或 Outbox。标记 `@IgnoreEncrypted` 的 Mapper 方法会跳过参数加密 / 结果解密，应限制调用范围并纳入审计。

<a id="9-testing--build"></a>
## 9. 测试与构建

```bash
mvn clean verify
```

- 父 POM 统一校验 Maven 与 Java 版本，并禁止 `core` / `extension` 的传递依赖中出现 Spring、`mybatis-spring`、`mybatis-plus-spring`（仅 `mybatis-plus-enhance-spring` 放行 Spring）。
- JaCoCo 在 `verify` 阶段执行 `prepare-agent`、`report` 与 `check`，行覆盖率规则为 **90%**（`haltOnFailure=false`，报告照常生成）。
- 按 `COMPATIBILITY.md` 要求，1.0.x 线必须执行真实 H2 组合测试，覆盖缓存、加密、签名、补签与增强 SQL Injector。仓库工作流 `.github/workflows/compatibility-matrix.yml` 会检出三条版本线并在对应 JDK 基线上执行 `mvn clean verify`。
- CI-Friendly 版本：根 POM 与全部子模块统一使用 `${revision}`（默认 `1.0.x.20260630-SNAPSHOT`），经 `flatten-maven-plugin` 固化。无需修改 POM 即可构建指定正式版本：

```bash
mvn -Drevision=1.0.0 -DskipTests package
```

- 不签名、不上传地检查发布产物：

```bash
mvn -Prelease -Dgpg.skip=true -DskipTests package
```

`mvn -Prelease deploy` 生成主构件、sources、Javadoc 与 GPG 签名；`release` Profile 对接 Sonatype Central Publishing 且 `autoPublish=false`（需在 Central Portal 确认发布）。普通 `mvn deploy` 按版本后缀将 SNAPSHOT 与正式版本路由到 Aliyun Maven 仓库（见 `distributionManagement`）。仓库凭据只能放在本机或 CI 的 `settings.xml`，不得写入项目 POM。

<a id="10-versioning--branches"></a>
## 10. 版本线与分支

| 分支 | 版本模式 | JDK | 维护策略 |
|---|---|---|---|
| `feature/1.0.x`（当前分支） | `1.0.x.*` | 8 | 仅接受兼容性修复与 JDK 8 安全的依赖升级 |
| `feature/2.0.x` | `2.0.x.*` | 17 | 面向 JDK 17 / Spring 6 / `jakarta.*` 的变更 |
| `feature/3.0.x` | `3.0.x.*` | 21 | 面向 JDK 21 语言特性与运行时 |

跨版本线升级允许破坏二进制兼容，但必须在发布说明中列出包名、配置与依赖迁移项；同一功能应优先保持公共 API 语义一致。

<a id="11-contributing--license"></a>
## 11. 贡献与许可证

欢迎贡献。提交 Pull Request 前请执行 `mvn clean verify`，并说明兼容性、测试、文档与迁移影响。新增密码算法、权限表达式或拦截器时，应补充单元测试与真实数据库集成测试，尤其要验证参数对象是否被就地修改、上下文是否在异常后恢复、插件顺序是否符合预期。

本项目采用 [Apache License 2.0](LICENSE) 许可证。
