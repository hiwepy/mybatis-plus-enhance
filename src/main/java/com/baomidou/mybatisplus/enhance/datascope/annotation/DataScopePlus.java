package com.baomidou.mybatisplus.enhance.datascope.annotation;


import java.lang.annotation.*;

/**
 * 原文：https://blog.csdn.net/qq_35542689/article/details/140710986
 * @author:
 * @date:
 * @Description: 数据权限注解。仅在Mapper层使用
 * 可以使用在类上，也可以使用在方法上。
 * - 如果 Mapper类加上注解，表示 Mapper提供的方法以及自定义的方法都会被加上数据权限
 * - 如果 Mapper类的方法加在上注解，表示该方法会被加上数据权限
 * - 如果 Mapper类和其方法同时加上注解，优先级为：【类上 > 方法上】
 * - 如果不需要数据权限，可以不加注解，也可以使用 @DataScopePlus(enabled = false)
 * - 如果使用的是mybatis-plus自带的查询，需要在Mapper层重写对应的方法，然后在方法上加注解，
 *   也可以不重写，在整个Mapper类上加注解，但是会影响所有方法包括baseMapper里的。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScopePlus {

    /**
     * 是否生效，默认true-生效
     */
    boolean enabled() default true;

    /**
     * 表别名 自定义sql语句有表别名就设置
     */
    String tableAlias() default "";

    /**
     * 用户表的别名
     */
    //public String userAlias() default "";

    /**
     * 部门限制范围的字段名称
     */
    //String deptScopeName() default "dept_id";

    /**
     * 本人限制范围的字段名称
     */
    String oneselfScopeName() default "create_id";


}
