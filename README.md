# mybatis-plus-enhance

MyBatis Plus 增强扩展 ： 实现基于注解的数据字段国际化组件 .


- [x] 支持基于注解的数据字段国际化
1. 通过注解 `@I18nField` 标记需要国际化的字段
2. 通过注解 `@I18nField` 的 `locale` 属性指定国际化字段的语言
3. 通过注解 `@I18nField` 的 `value` 属性指定国际化字段的值
4. 通过注解 `@I18nField` 的 `field` 属性指定国际化字段的名称
- [x] 支持基于注解的数据字段国际化查询
1. 通过注解 `@I18nField` 的 `locale` 属性指定国际化字段的语言
2. 通过注解 `@I18nField` 的 `field` 属性指定国际化字段的名称
3. 通过注解 `@I18nField` 的 `value` 属性指定国际化字段的值
4. 通过注解 `@I18nField` 的 `query` 属性指定国际化字段的查询方式
- [x] 基于Mybatis-Plus拦截器实现数据加解密，满足存储机密性要求
- [x] 基于Mybatis-Plus拦截器实现数据签名与验签，满足数据存储完整性保护
