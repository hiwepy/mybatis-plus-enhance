### 项目介绍

> 基于 [MyBatis Plus](https://baomidou.com/introduce/) 的 `数据加解密(Data Encryption And Decryption)`、`数据签名与验签(Data Signature)`、`数据脱敏(Data Masking)`、`数据权限(Data Permission)`、`多租户数据隔离(Multi Tenant Data Isolation)`、`数据国际化(Data Internationalized)` 增强扩展。

Github： https://github.com/hiwepy/mybatis-plus-enhance

**MyBatis Plus 增强扩展计划提供以下支持：**

- [x] 提供`3级等保密评`要求的`存储机密性`解决方案
- [x] 提供`3级等保密评`要求的`存储完整性`解决方案
- [ ] 提供`网络数据安全管理条例`要求的`数据权限`解决方案
- [ ] 提供`网络数据安全管理条例`要求的`数据脱敏`解决方案
- [ ] 提供`多租户SaaS平台`需要的`业务数据的隔离`解决方案
- [ ] 提供`多语言系统`需要的`数据国际化`解决方案

**说明：**

该组件参考了以下资料：
- https://blog.csdn.net/tianmaxingkonger/article/details/130986784


### 先决条件

`mybatis-plus-enhance` 依赖 `MyBatis Plus`，请确保您的项目中已经引入了 `MyBatis Plus` 相关依赖。

#### 依赖配置

请将以下依赖项添加到项目的 Maven `pom.xml` 文件中：

```xml
<!-- For Mybatis -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
</dependency>
<dependency>
   <groupId>com.github.hiwepy</groupId>
   <artifactId>mybatis-plus-enhance</artifactId>
   <version>1.0.0-SNAPSHOT</version>
</dependency>
```

或者，在你的 Gradle 构建文件 `build.gradle` 中添加：

```groovy
dependencies {
    implementation 'com.baomidou:mybatis-plus-boot-starter',
    implementation 'com.github.hiwepy:mybatis-plus-enhance'
}
```

### 实现原理

#### 一、基于 MyBatis Plus 插件的 `数据加解密(Data Encryption And Decryption)` 实现原理

> `数据加解密(Data Encryption And Decryption)` 是对数据进行保护的一种方式，通过对数据进行加密`加密`和`解密`，可以保证数据在传输和存储过程中的机密性。

1、增强 `MyBatis Plus` 拦截器逻辑，新增 `EnhanceInnerInterceptor`，用于增强`数据库查询`完成后的能力。

```java
public interface EnhanceInnerInterceptor extends InnerInterceptor {

    /**
     * 判断是否执行 {@link Executor#query(MappedStatement, Object, RowBounds, ResultHandler, CacheKey, BoundSql)}
     * <p>
     * 如果不执行query操作,则返回 {@link Collections#emptyList()}
     *
     * @param executor      Executor(可能是代理对象)
     * @param ms            MappedStatement
     * @param parameter     parameter
     * @param rowBounds     rowBounds
     * @param resultHandler resultHandler
     * @param boundSql      boundSql
     * @return 新的 boundSql
     */
    default boolean willDoAfterQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql, List<Object> rtList) throws SQLException {
        return true;
    }

    /**
     * {@link Executor#query(MappedStatement, Object, RowBounds, ResultHandler, CacheKey, BoundSql)} 操作前置处理
     * <p>
     * 改改sql啥的
     *
     * @param executor      Executor(可能是代理对象)
     * @param ms            MappedStatement
     * @param parameter     parameter
     * @param rowBounds     rowBounds
     * @param resultHandler resultHandler
     * @param boundSql      boundSql
     */
    default void afterQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql, List<Object> rtList) throws SQLException {
        // do nothing
    }

}
```

2、自定义注解 `@EncryptedTable` 和 `@EncryptField`，用于标记需要加密和解密的实体类和字段。

```java
/**
 * 需要加解密的实体类用这个注解
 * @author wandl
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface EncryptedTable {

}
/**
 * 需要加解密的字段用这个注解
 * @author wandl
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EncryptedField {
}
```

3、自定义 `MyBatis Plus` Inner 插件 `DataEncryptionInnerInterceptor`，用于拦截数据库`更新操作`，对数据进行加密。

```java
/**
 * 数据加解密和签名拦截器，用于对新增/更新数据进行加密和签名操作
 */
@Slf4j
public class DataEncryptionInnerInterceptor extends JsqlParserSupport implements InnerInterceptor {
    // 详细实现请参考源码
}
```

4、自定义 `MyBatis Plus` Inner 插件 `DataDecryptionInnerInterceptor`，用于拦截数据库`查询操作`，对查询结果进行解密。

```java
/**
 * 数据解密拦截器，用于对查询结果进行解密操作
 */
@Slf4j
public class DataDecryptionInnerInterceptor implements EnhanceInnerInterceptor {
    // 详细实现请参考源码
}
```

5、自定义 `MyBatis Plus` 全局插件 `MybatisPlusEnhanceInterceptor` 替代默认的 `MybatisPlusInterceptor`，在原有的基础上增强了查询后的数据处理能力。

```java
/**
 * MybatisPlus 解密和签名验证拦截器，用于替代 MybatisPlus 的原生拦截器，实现对数据库字段的解密和签名验证操作
 * 参考：
 * - https://blog.csdn.net/tianmaxingkonger/article/details/130986784
 */
@Intercepts(
    {
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}),
        @Signature(type = StatementHandler.class, method = "getBoundSql", args = {}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
    }
)
public class MybatisPlusEnhanceInterceptor extends MybatisPlusInterceptor {
    // 详细实现请参考源码
}
```

#### 二、基于 MyBatis Plus 插件的 `数据签名与验签(Data Signature)` 实现原理

> `数据签名与验签(Data Signature)` 是对数据进行完整性验证的一种方式，通过对数据进行签名`签名`和`验签`，可以保证数据在传输和存储过程中的完整性。
- 此方案逻辑是通过 `MyBatis Plus` 自定义插件和`自定义注解`实现对数据库中的数据进行`签名`和`验签`。
- 此方案逻辑是在 `数据加解密(Data Encryption And Decryption)` 方案基础上的扩展，对数据进行签名和验签操作。

1、增强 `MyBatis Plus` 拦截器逻辑，新增 `EnhanceInnerInterceptor`，用于增强`数据库查询`完成后的能力。

```java
public interface EnhanceInnerInterceptor extends InnerInterceptor {

    /**
     * 判断是否执行 {@link Executor#query(MappedStatement, Object, RowBounds, ResultHandler, CacheKey, BoundSql)}
     * <p>
     * 如果不执行query操作,则返回 {@link Collections#emptyList()}
     *
     * @param executor      Executor(可能是代理对象)
     * @param ms            MappedStatement
     * @param parameter     parameter
     * @param rowBounds     rowBounds
     * @param resultHandler resultHandler
     * @param boundSql      boundSql
     * @return 新的 boundSql
     */
    default boolean willDoAfterQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql, List<Object> rtList) throws SQLException {
        return true;
    }

    /**
     * {@link Executor#query(MappedStatement, Object, RowBounds, ResultHandler, CacheKey, BoundSql)} 操作前置处理
     * <p>
     * 改改sql啥的
     *
     * @param executor      Executor(可能是代理对象)
     * @param ms            MappedStatement
     * @param parameter     parameter
     * @param rowBounds     rowBounds
     * @param resultHandler resultHandler
     * @param boundSql      boundSql
     */
    default void afterQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql, List<Object> rtList) throws SQLException {
        // do nothing
    }

}
```

2、自定义注解 `@TableSignature` 和 `@TableSignatureField`，用于标记需要签名和验签的实体类和字段。

```java
/**
 * 需要签名的实体类用这个注解
 * @author wandl
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface TableSignature {

    /**
     * 是否将实体类的所有字段进行联合签名
     */
    boolean unionAll() default false;

}
/**
 * 需要签名或存储签名的字段用这个注解
 * @author wandl
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface TableSignatureField {

    /**
     * 待加密字段的顺序，用于多个字段进行加密签名时保证加密字段的顺序
     */
    int order() default 0;

    /**
     * 是否作为签名结果的存储字段
     */
    boolean stored() default false;

}
```

3、自定义 `MyBatis Plus` Inner 插件 `DataSignatureInnerInterceptor`，用于拦截`数据库查询`和`数据库更新`操作，对数据进行签名和验签。

```java
/**
 * 数据签名和验签拦截器
 * 1、用于对新增/更新数据进行签名操作
 * 2、用于对查询数据进行验签操作
 */
@Slf4j
public class DataSignatureInnerInterceptor extends JsqlParserSupport implements EnhanceInnerInterceptor {
    // 详细实现请参考源码
}
```

4、自定义 `MyBatis Plus` 全局插件 `MybatisPlusEnhanceInterceptor` 替代默认的 `MybatisPlusInterceptor`，在原有的基础上增强了查询后的数据处理能力。

```java
/**
 * MybatisPlus 解密和签名验证拦截器，用于替代 MybatisPlus 的原生拦截器，实现对数据库字段的解密和签名验证操作
 * 参考：
 * - https://blog.csdn.net/tianmaxingkonger/article/details/130986784
 */
@Intercepts(
    {
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}),
        @Signature(type = StatementHandler.class, method = "getBoundSql", args = {}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
    }
)
public class MybatisPlusEnhanceInterceptor extends MybatisPlusInterceptor {
    // 详细实现请参考源码
}
```


#### 三、基于 MyBatis Plus 插件的 `数据脱敏(Data Masking)` 实现原理

> 通过 `MyBatis Plus` 插件实现数据写入或读取时对数据进行`脱敏`，保证数据在数据库中的存储安全性。

**数据脱敏的方法**

数据脱敏可以通过多种方法实现，常用的方法包括：

* ‌掩码算法（Masking）‌：例如，将身份证号码的前几位保留，其他位用“X”或“*”代替。
* ‌伪造姓名（Pseudonymization）‌：将真实姓名替换成随机生成的假名。
* ‌删除‌：随机删除敏感数据中的部分内容，如电话号码中的某些数字。
* ‌重排‌：打乱原始数据中某些字符或字段的顺序。
* ‌加噪‌：在数据中注入随机生成的字符或噪音。
* ‌加密‌：使用哈希函数如MD5或SHA-256将敏感数据转换为密文。



#### 四、基于 MyBatis Plus 插件的 `数据权限(Data Permission)` 实现原理

#### 五、基于 MyBatis Plus 插件的 `多租户数据隔离(Multi Tenant Data Isolation)` 实现原理

#### 六、基于 MyBatis Plus 插件的 `数据国际化(Data Internationalized)` 实现原理


### 使用指南

#### 一、基于 MyBatis Plus 插件的 `数据加解密(Data Encryption And Decryption)` 使用指南

1、实现 `EncryptedFieldHandler` 的接口的加解密和签名逻辑或使用默认实现 `DefaultEncryptedFieldHandler` 创建Handler实例。

```java
@Bean
public EncryptedFieldHandler encryptedFieldHandler(ObjectProvider<ObjectMapper> objectMapperProvider) {
    // 随机生成sm4加密key
    String sm4Key = RandomUtil.randomString(RandomUtil.BASE_CHAR_NUMBER, 16);
    System.out.println("sm4Key:"+sm4Key);
    sm4Key = Base64.encode(sm4Key.getBytes());
    System.out.println("sm4Key-Base64:"+ sm4Key);
    /**
     * 偏移向量，加盐
     */
    String sm4Iv = RandomStringUtils.secureStrong().nextAscii(16);
    System.out.println("sm4Iv:"+sm4Iv);
    sm4Iv = Base64.encode(sm4Iv.getBytes());
    System.out.println("sm4Iv-Base64:"+ sm4Iv);

    ObjectMapper objectMapper =  objectMapperProvider.getIfAvailable(ObjectMapper::new);
    return new DefaultEncryptedFieldHandler(objectMapper, SymmetricAlgorithmType.SM4, HmacAlgorithm.HmacSM3,
            Mode.CBC, Padding.PKCS5Padding, sm4Key, sm4Iv);
}
```

2、配置 `DataEncryptionInnerInterceptor` 拦截器

```java
/**
* mybatis-plus 扩展：数据加解密拦截器，用于对新增/更新数据进行加密操作
*/
@Bean
@Order(3)
public DataEncryptionInnerInterceptor dataEncryptionInnerInterceptor(ObjectProvider<EncryptedFieldHandler> encryptedFieldHandlerProvider) {
	return new DataEncryptionInnerInterceptor(encryptedFieldHandlerProvider.getIfAvailable());
}
```

3、配置 `DataDecryptionInnerInterceptor` 拦截器【可选】

```java
/**
 * mybatis-plus 扩展：数据加解密拦截器，用于对新增/更新数据进行加密操作
 */
@Bean
@Order(5)
public DataDecryptionInnerInterceptor dataDecryptionInnerInterceptor(ObjectProvider<EncryptedFieldHandler> encryptedFieldHandlerProvider) {
	return new DataDecryptionInnerInterceptor(true, encryptedFieldHandlerProvider.getIfAvailable());
}
```

4、配置 `Mybatis Plus` 全局拦截器 `MybatisPlusEnhanceInterceptor` ，用于替代 Mybatis Plus 的原生拦截器 `MybatisPlusInterceptor`，实现对查询后操作的增强。

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor(ObjectProvider<EncryptedFieldHandler> encryptedFieldHandlerProvider,
													 ObjectProvider<InnerInterceptor> innerInterceptorProvider) {
	// MybatisPlus 解密和签名验证拦截器，用于替代 MybatisPlus 的原生拦截器，实现对数据库字段的解密和签名验证操作
	MybatisPlusEnhanceInterceptor interceptor = new MybatisPlusEnhanceInterceptor();
	// 更新操作执行顺序：DataEncryptionInnerInterceptor -> DataSignatureInnerInterceptor
	// 查询操作执行顺序：OptimisticLockerInnerInterceptor -> PaginationInnerInterceptor -> DataSignatureInnerInterceptor -> DataDecryptionInnerInterceptor
	innerInterceptorProvider.stream().sorted().forEach(interceptor::addInnerInterceptor);
	return interceptor;
}
```

5、`Mybatis Plus` 完整配置

>[danger] 这里要特别的注意，如果你的业务中，加解密拦截器和签名拦截器一同使用时，务必要调整好拦截器的执行顺序，注意对象的 @Order 注解值
- **更新操作执行顺序**：DataEncryptionInnerInterceptor -> DataSignatureInnerInterceptor
- **查询操作执行顺序**：OptimisticLockerInnerInterceptor -> PaginationInnerInterceptor -> DataSignatureInnerInterceptor -> DataDecryptionInnerInterceptor

```java
@Configuration
@MapperScan({ "com.xxx.**.dao", "com.xxx.**.mapper" })
public class MybatisPlusConfiguration {

    /**
     * 新的分页插件,一缓和二缓遵循mybatis的规则,需要设置 MybatisConfiguration#useDeprecatedExecutor =
     * false 避免缓存出现问题(该属性会在旧插件移除后一同移除)
     */
	@Bean
	public MybatisPlusInterceptor mybatisPlusInterceptor(ObjectProvider<EncryptedFieldHandler> encryptedFieldHandlerProvider,
														 ObjectProvider<InnerInterceptor> innerInterceptorProvider) {
		// MybatisPlus 解密和签名验证拦截器，用于替代 MybatisPlus 的原生拦截器，实现对数据库字段的解密和签名验证操作
		MybatisPlusEnhanceInterceptor interceptor = new MybatisPlusEnhanceInterceptor();
		// 更新操作执行顺序：DataEncryptionInnerInterceptor -> DataSignatureInnerInterceptor
		// 查询操作执行顺序：OptimisticLockerInnerInterceptor -> PaginationInnerInterceptor -> DataSignatureInnerInterceptor -> DataDecryptionInnerInterceptor
		innerInterceptorProvider.stream().sorted().forEach(interceptor::addInnerInterceptor);
		return interceptor;
	}

    @Bean
    public EncryptedFieldHandler encryptedFieldHandler(ObjectProvider<ObjectMapper> objectMapperProvider) {
        // 随机生成sm4加密key
        String sm4Key = RandomUtil.randomString(RandomUtil.BASE_CHAR_NUMBER, 16);
        System.out.println("sm4Key:"+sm4Key);
        sm4Key = Base64.encode(sm4Key.getBytes());
        System.out.println("sm4Key-Base64:"+ sm4Key);
        /**
         * 偏移向量，加盐
         */
        String sm4Iv = RandomStringUtils.secureStrong().nextAscii(16);
        System.out.println("sm4Iv:"+sm4Iv);
        sm4Iv = Base64.encode(sm4Iv.getBytes());
        System.out.println("sm4Iv-Base64:"+ sm4Iv);

        ObjectMapper objectMapper =  objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new DefaultEncryptedFieldHandler(objectMapper, SymmetricAlgorithmType.SM4, HmacAlgorithm.HmacSM3,
                Mode.CBC, Padding.PKCS5Padding, sm4Key, sm4Iv);
    }

/**
	 * mybatis-plus：乐观锁插件
	 */
	@Bean
	@Order(1)
	public OptimisticLockerInnerInterceptor optimisticLockerInterceptor() {
		return new OptimisticLockerInnerInterceptor();
	}

	/**
	 * mybatis-plus：分页插件<br>
	 * 文档：http://mp.baomidou.com<br>
	 */
	@Bean
	@Order(2)
	public PaginationInnerInterceptor paginationInterceptor() {
		PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
		paginationInterceptor.setOverflow(false);
		return paginationInterceptor;
	}

	/**
	 * mybatis-plus 扩展：数据加解密拦截器，用于对新增/更新数据进行加密操作
	 */
	@Bean
	@Order(3)
	public DataEncryptionInnerInterceptor dataEncryptionInnerInterceptor(ObjectProvider<EncryptedFieldHandler> encryptedFieldHandlerProvider) {
		return new DataEncryptionInnerInterceptor(encryptedFieldHandlerProvider.getIfAvailable());
	}

	/**
	 * mybatis-plus 扩展：数据加解密拦截器，用于对新增/更新数据进行加密操作
	 */
	@Bean
	@Order(5)
	public DataDecryptionInnerInterceptor dataDecryptionInnerInterceptor(ObjectProvider<EncryptedFieldHandler> encryptedFieldHandlerProvider) {
		return new DataDecryptionInnerInterceptor(true, encryptedFieldHandlerProvider.getIfAvailable());
	}

    /**
     * mybatis-plus：防止全表更新与删除插件
     @Bean
     public BlockAttackInnerInterceptor blockAttackInnerInterceptor() {
     BlockAttackInnerInterceptor sqlExplainInterceptor = new BlockAttackInnerInterceptor();
     return sqlExplainInterceptor;
     }
     */

    /**
     * 注入sql注入器
     */
    @Bean
    public ISqlInjector sqlInjector() {
        return new DefaultSqlInjector();
    }

    /**
     * 注入主键生成器
     @Bean
     public IKeyGenerator keyGenerator() {
     return new H2KeyGenerator();
     }
     */

    /*
     * oracle数据库配置JdbcTypeForNull
     * 参考：https://gitee.com/baomidou/mybatisplus-boot-starter/issues/IHS8X
     * 不需要这样配置了，参考 yml: mybatis-plus: confuguration dbc-type-for-null: 'null'
     *
     * @Bean public ConfigurationCustomizer configurationCustomizer(){ return new
     * MybatisPlusCustomizers(); }
     *
     * class MybatisPlusCustomizers implements ConfigurationCustomizer {
     *
     * @Override public void customize(org.apache.ibatis.session.Configuration
     * configuration) { configuration.setJdbcTypeForNull(JdbcType.NULL); } }
     */

}
```

6、定义PO类，使用 `@EncryptedTable` 和 `@EncryptedField` 注解

> 实体类上使用自定义注解，来标记需要进行加解密

```java
// 必须使用@EncryptedTable注解
@EncryptedTable
@TableName(value = "wsp_user")
public class UserEntity implements Serializable {
private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String name;
    // 使用@EncryptedField注解
    @EncryptedField
    private String mobile;
    // 使用@EncryptedField注解
    @EncryptedField
    private String email;
}
```

7、定义API接口

> 通过`Mybatis Plus` 提供的 `BaseMapper` API、Lambda、自定义mapper接口三种方式进行使用

```java
/**
 * 用户表控制器
 */
@RestController
@RequestMapping("/user")
public class UserController {
 
    @Resource(name = "userServiceImpl")
    private IUserService userService;
    @Resource(name = "userXmlServiceImpl")
    private IUserService userXmlService;
 
    /**
     * 测试解密
     */
    @GetMapping(name = "测试解密", value = "/detail")
    public UserEntity detail(Long id) {
        // 测试MP API
//        UserEntity entity = userService.getById(id);
 
        // 测试自定义Mapper接口
        UserEntity entity = userXmlService.getById(id);
        if (null == entity) {
            return new UserEntity();
        }
        return entity;
    }
 
    /**
     * 新增用户表，测试加密
     */
    @GetMapping(name = "新增用户表，测试加密", value = "/add")
    public UserEntity add(UserEntity entity) {
        // 测试MP API
//        userService.save(entity);
 
        // 测试自定义Mapper接口
        userXmlService.save(entity);
        return entity;
    }
 
    /**
     * 修改用户表
     */
    @GetMapping(name = "修改用户表", value = "/update")
    public UserEntity update(UserEntity entity) {
        // 测试MP API
//        userService.updateById(entity);
 
        // 测试Lambda
//        LambdaUpdateWrapper<UserEntity> wrapper = new LambdaUpdateWrapper<>();
//        wrapper.eq(UserEntity::getId, entity.getId());
//        wrapper.set(UserEntity::getMobile, entity.getMobile());
//        wrapper.set(UserEntity::getName, entity.getName());
//        wrapper.set(UserEntity::getEmail, entity.getEmail());
//        userService.update(wrapper);
 
        // 测试自定义Mapper接口
        userXmlService.updateById(entity);
        return entity;
    }
}
```

#### 二、基于 MyBatis Plus 插件的 `数据签名与验签(Data Signature)` 使用指南


1、实现 `EncryptedFieldHandler` 的接口的加解密和签名逻辑或使用默认实现 `DefaultEncryptedFieldHandler` 创建Handler实例。

```java
@Bean
public EncryptedFieldHandler encryptedFieldHandler(ObjectProvider<ObjectMapper> objectMapperProvider) {
    // 随机生成sm4加密key
    String sm4Key = RandomUtil.randomString(RandomUtil.BASE_CHAR_NUMBER, 16);
    System.out.println("sm4Key:"+sm4Key);
    sm4Key = Base64.encode(sm4Key.getBytes());
    System.out.println("sm4Key-Base64:"+ sm4Key);
    /**
     * 偏移向量，加盐
     */
    String sm4Iv = RandomStringUtils.secureStrong().nextAscii(16);
    System.out.println("sm4Iv:"+sm4Iv);
    sm4Iv = Base64.encode(sm4Iv.getBytes());
    System.out.println("sm4Iv-Base64:"+ sm4Iv);

    ObjectMapper objectMapper =  objectMapperProvider.getIfAvailable(ObjectMapper::new);
    return new DefaultEncryptedFieldHandler(objectMapper, SymmetricAlgorithmType.SM4, HmacAlgorithm.HmacSM3,
            Mode.CBC, Padding.PKCS5Padding, sm4Key, sm4Iv);
}
```

2、配置 Mybatis Plus 数字签名和签名验证拦截器 `DataSignatureInnerInterceptor`

```java
/**
 * mybatis-plus 扩展：数据签名和验签拦截器
 * 1、用于对新增/更新数据进行签名操作
 * 2、用于对查询数据进行验签操作
 * 3、更新操作执行顺序：DataEncryptionInnerInterceptor -> DataSignatureInnerInterceptor
 */
@Bean
@Order(4)
public DataSignatureInnerInterceptor dataSignatureInnerInterceptor(ObjectProvider<EncryptedFieldHandler> encryptedFieldHandlerProvider) {
	return new DataSignatureInnerInterceptor(encryptedFieldHandlerProvider.getIfAvailable());
}
```

3、配置 `Mybatis Plus` 全局拦截器 `MybatisPlusEnhanceInterceptor` ，用于替代 Mybatis Plus 的原生拦截器 `MybatisPlusInterceptor`，实现对查询后操作的增强。

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor(ObjectProvider<EncryptedFieldHandler> encryptedFieldHandlerProvider,
													 ObjectProvider<InnerInterceptor> innerInterceptorProvider) {
	// MybatisPlus 解密和签名验证拦截器，用于替代 MybatisPlus 的原生拦截器，实现对数据库字段的解密和签名验证操作
	MybatisPlusEnhanceInterceptor interceptor = new MybatisPlusEnhanceInterceptor();
	// 更新操作执行顺序：DataEncryptionInnerInterceptor -> DataSignatureInnerInterceptor
	// 查询操作执行顺序：OptimisticLockerInnerInterceptor -> PaginationInnerInterceptor -> DataSignatureInnerInterceptor -> DataDecryptionInnerInterceptor
	innerInterceptorProvider.stream().sorted().forEach(interceptor::addInnerInterceptor);
	return interceptor;
}
```

4、`Mybatis Plus` 完整配置

>[danger] 这里要特别的注意，如果你的业务中，加解密拦截器和签名拦截器一同使用时，务必要调整好拦截器的执行顺序，注意对象的 @Order 注解值
- **更新操作执行顺序**：DataEncryptionInnerInterceptor -> DataSignatureInnerInterceptor
- **查询操作执行顺序**：OptimisticLockerInnerInterceptor -> PaginationInnerInterceptor -> DataSignatureInnerInterceptor -> DataDecryptionInnerInterceptor

```java
@Configuration
@MapperScan({ "com.xxx.**.dao", "com.xxx.**.mapper" })
public class MybatisPlusConfiguration {

    /**
     * 新的分页插件,一缓和二缓遵循mybatis的规则,需要设置 MybatisConfiguration#useDeprecatedExecutor =
     * false 避免缓存出现问题(该属性会在旧插件移除后一同移除)
     */
	@Bean
	public MybatisPlusInterceptor mybatisPlusInterceptor(ObjectProvider<EncryptedFieldHandler> encryptedFieldHandlerProvider,
														 ObjectProvider<InnerInterceptor> innerInterceptorProvider) {
		// MybatisPlus 解密和签名验证拦截器，用于替代 MybatisPlus 的原生拦截器，实现对数据库字段的解密和签名验证操作
		MybatisPlusEnhanceInterceptor interceptor = new MybatisPlusEnhanceInterceptor();
		// 更新操作执行顺序：DataEncryptionInnerInterceptor -> DataSignatureInnerInterceptor
		// 查询操作执行顺序：OptimisticLockerInnerInterceptor -> PaginationInnerInterceptor -> DataSignatureInnerInterceptor -> DataDecryptionInnerInterceptor
		innerInterceptorProvider.stream().sorted().forEach(interceptor::addInnerInterceptor);
		return interceptor;
	}

    @Bean
    public EncryptedFieldHandler encryptedFieldHandler(ObjectProvider<ObjectMapper> objectMapperProvider) {
        // 随机生成sm4加密key
        String sm4Key = RandomUtil.randomString(RandomUtil.BASE_CHAR_NUMBER, 16);
        System.out.println("sm4Key:"+sm4Key);
        sm4Key = Base64.encode(sm4Key.getBytes());
        System.out.println("sm4Key-Base64:"+ sm4Key);
        /**
         * 偏移向量，加盐
         */
        String sm4Iv = RandomStringUtils.secureStrong().nextAscii(16);
        System.out.println("sm4Iv:"+sm4Iv);
        sm4Iv = Base64.encode(sm4Iv.getBytes());
        System.out.println("sm4Iv-Base64:"+ sm4Iv);

        ObjectMapper objectMapper =  objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new DefaultEncryptedFieldHandler(objectMapper, SymmetricAlgorithmType.SM4, HmacAlgorithm.HmacSM3,
                Mode.CBC, Padding.PKCS5Padding, sm4Key, sm4Iv);
    }

/**
	 * mybatis-plus：乐观锁插件
	 */
	@Bean
	@Order(1)
	public OptimisticLockerInnerInterceptor optimisticLockerInterceptor() {
		return new OptimisticLockerInnerInterceptor();
	}

	/**
	 * mybatis-plus：分页插件<br>
	 * 文档：http://mp.baomidou.com<br>
	 */
	@Bean
	@Order(2)
	public PaginationInnerInterceptor paginationInterceptor() {
		PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
		paginationInterceptor.setOverflow(false);
		return paginationInterceptor;
	}

	/**
	 * mybatis-plus 扩展：数据签名和验签拦截器
	 * 1、用于对新增/更新数据进行签名操作
	 * 2、用于对查询数据进行验签操作
	 * 3、更新操作执行顺序：DataEncryptionInnerInterceptor -> DataSignatureInnerInterceptor
	 */
	@Bean
	@Order(4)
	public DataSignatureInnerInterceptor dataSignatureInnerInterceptor(ObjectProvider<EncryptedFieldHandler> encryptedFieldHandlerProvider) {
		return new DataSignatureInnerInterceptor(encryptedFieldHandlerProvider.getIfAvailable());
	}

    /**
     * mybatis-plus：防止全表更新与删除插件
     @Bean
     public BlockAttackInnerInterceptor blockAttackInnerInterceptor() {
     BlockAttackInnerInterceptor sqlExplainInterceptor = new BlockAttackInnerInterceptor();
     return sqlExplainInterceptor;
     }
     */

    /**
     * 注入sql注入器
     */
    @Bean
    public ISqlInjector sqlInjector() {
        return new DefaultSqlInjector();
    }

    /**
     * 注入主键生成器
     @Bean
     public IKeyGenerator keyGenerator() {
     return new H2KeyGenerator();
     }
     */

    /*
     * oracle数据库配置JdbcTypeForNull
     * 参考：https://gitee.com/baomidou/mybatisplus-boot-starter/issues/IHS8X
     * 不需要这样配置了，参考 yml: mybatis-plus: confuguration dbc-type-for-null: 'null'
     *
     * @Bean public ConfigurationCustomizer configurationCustomizer(){ return new
     * MybatisPlusCustomizers(); }
     *
     * class MybatisPlusCustomizers implements ConfigurationCustomizer {
     *
     * @Override public void customize(org.apache.ibatis.session.Configuration
     * configuration) { configuration.setJdbcTypeForNull(JdbcType.NULL); } }
     */

}
```

5、定义PO类，使用 `@TableSignature` 和 `@TableSignatureField`注解

> 实体类上使用自定义注解，来标记需要进行加解密

```java
@TableSignature
@TableName(value = "wsp_user")
public class UserEntity implements Serializable {
private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String name;
    // 使用@TableSignatureField 注解
    @TableSignatureField(order = 1)
    private String mobile;
    // 使用@TableSignatureField 注解
    @TableSignatureField(order = 2)
    private String email;
    // 需要存储HMAC的字段用这个注解
    @TableSignatureField(stored = true)
    private String hamc;
}
```

6、定义API接口

> 通过`Mybatis Plus` 提供的 `BaseMapper` API、Lambda、自定义mapper接口三种方式进行使用

```java
/**
 * 用户表控制器
 */
@RestController
@RequestMapping("/user")
public class UserController {
 
    @Resource(name = "userServiceImpl")
    private IUserService userService;
    @Resource(name = "userXmlServiceImpl")
    private IUserService userXmlService;
 
    /**
     * 测试解密
     */
    @GetMapping(name = "测试解密", value = "/detail")
    public UserEntity detail(Long id) {
        // 测试MP API
//        UserEntity entity = userService.getById(id);
 
        // 测试自定义Mapper接口
        UserEntity entity = userXmlService.getById(id);
        if (null == entity) {
            return new UserEntity();
        }
        return entity;
    }
 
    /**
     * 新增用户表，测试加密
     */
    @GetMapping(name = "新增用户表，测试加密", value = "/add")
    public UserEntity add(UserEntity entity) {
        // 测试MP API
//        userService.save(entity);
 
        // 测试自定义Mapper接口
        userXmlService.save(entity);
        return entity;
    }
 
    /**
     * 修改用户表
     */
    @GetMapping(name = "修改用户表", value = "/update")
    public UserEntity update(UserEntity entity) {
        // 测试MP API
//        userService.updateById(entity);
 
        // 测试Lambda
//        LambdaUpdateWrapper<UserEntity> wrapper = new LambdaUpdateWrapper<>();
//        wrapper.eq(UserEntity::getId, entity.getId());
//        wrapper.set(UserEntity::getMobile, entity.getMobile());
//        wrapper.set(UserEntity::getName, entity.getName());
//        wrapper.set(UserEntity::getEmail, entity.getEmail());
//        userService.update(wrapper);
 
        // 测试自定义Mapper接口
        userXmlService.updateById(entity);
        return entity;
    }
}
```

#### 三、基于 MyBatis Plus 插件的 `数据脱敏(Data Masking)` 使用指南


#### 四、基于 MyBatis Plus 插件的 `数据权限(Data Permission)` 使用指南

#### 五、基于 MyBatis Plus 插件的 `多租户数据隔离(Multi Tenant Data Isolation)` 使用指南

#### 六、基于 MyBatis Plus 插件的 `数据国际化(Data Internationalized)` 使用指南
