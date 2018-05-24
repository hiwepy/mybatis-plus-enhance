<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration PUBLIC "-//mybatis.org//DTD Config 3.0//EN" "http://mybatis.org/dtd/mybatis-3-config.dtd">
<!--配置文件的详细介绍可参考:http://limingnihao.iteye.com/blog/1060764-->
<configuration>
	
	<!-- 定义配置外在化 -->
	<properties resource="jdbc.properties" />
    <!-- 全局配置 -->
    <settings>
		<!-- 
		       指定MyBatis如何自动映射列到字段/属性。PARTIAL只会自动映射简单，没有嵌套的结果。
		　　FULL会自动映射任意复杂的结果（嵌套的或其他情况）
		　　有效值：NONE,PARTIAL,FULL
		　　默认值：PARTIAL
		 -->
		<setting name="autoMappingBehavior" value="PARTIAL" />
		<!-- 新增了一个 settings 配置的参数 autoMappingUnknownColumnBehavior ，当检测出未知列（或未知属性）时，如何处理，
			默认情况下没有任何提示，这在测试的时候很不方便，不容易找到错误 -->
		<setting name="autoMappingUnknownColumnBehavior" value="WARNING"/>
		<!--当启用时，使用延迟加载属性的对象在发起任何延迟属性的调用时会被完全加载。否则，每个属性在请求时就加载。默认值：true -->
		<setting name="aggressiveLazyLoading" value="false" />
		<!-- 配置使全局的映射器启用或禁用缓存; 默认值：true-->
		<setting name="cacheEnabled" value="true" />
		<setting name="callSettersOnNulls" value="false" />
		<setting name="databaseId" value="OTHER" />
		<setting name="defaultFetchSize" value="OTHER" />
		
		<!-- 设置超时时间，它决定驱动等待一个数据库响应的时间。
		　　有效值：Any，positive，integer
		　　默认值：Not Set(null)
		<setting name="defaultStatementTimeout" value="25" />
		 -->
		<!-- 配置默认的执行器。SIMPLE 执行器没有什么特别之处。REUSE执行器重用预处理语句。BATCH 执行器重用语句和批量更新
		　　有效值：SIMPLE,REUSE,BATCH
		　　默认值：SIMPLE
		 -->
		<setting name="defaultExecutorType" value="SIMPLE" />
		<setting name="jdbcTypeForNull" value="OTHER" />
		<!-- 允许或不允许多种结果集从一个单独的语句中返回（需要适合的驱动）
		　　有效值：true,false
		　　默认值：true
		-->
		<setting name="multipleResultSetsEnabled" value="true" />
		<setting name="useActualParameterName" value="false" />
		<!-- 使用列标签代替列名。不同的驱动在这方便表现不同。参考驱动文档或充分测试两种方法来决定所使用的驱动。
		　　有效值：true,false
		　　默认值：true
		 -->
		<setting name="useColumnLabel" value="true" />
		<!-- 允许JDBC支持生成的键。需要适合的驱动。如果设置为true则这个设置强制生成的键被使用，尽管一些驱动拒绝兼容但仍然有效（比如 Derby）
		　　有效值：true,false
		　　默认值：false
		 -->
		<setting name="useGeneratedKeys" value="false" />
		<setting name="safeRowBoundsEnabled" value="false" />
		<setting name="safeResultHandlerEnabled" value="true" />
		<setting name="mapUnderscoreToCamelCase" value="false" />
		<!--全局地禁用或启用延迟加载。禁用时，所有关联查询会被马上加载;默认值：true -->
		<setting name="lazyLoadingEnabled" value="true" />
		<setting name="lazyLoadTriggerMethods" value="equals,clone,hashCode,toString" />
		<setting name="localCacheScope" value="SESSION" />
		<!--
			日志名称前缀：该值会影响log4j对mybatis输出日志；
			增加了该前缀后，日志的名称将会变成类似以下结构：
			[Mybatis-3.4.1]com.zfsoft.dao.daointerface.xxx.xxx
			故如果需要输出SQL日志，配置需改为：			
			log4j.logger.[Mybatis-3.4.1]com.zfsoft = DEBUG
		 -->
		<setting name="logPrefix" value="[Mybatis-3.4.1]">
		<setting name="logImpl" value="LOG4J">
	</settings>
    <typeAliases>
        <typeAlias alias="XXXModel" type="com.vsoft.dao.entities.XXXModel"/>
    </typeAliases>
    <typeHandlers>
    	
    </typeHandlers>
	<!-- 加载映射文件 -->  
    <mappers>  
    
    	<!-- 第一种方式：通过resource指定 -->
    	<!-- <mapper resource="com/zfsoft/dao/sqlmap/User.xml"/> -->
     	
     	<!-- 第二种方式， 通过class指定接口，进而将接口与对应的xml文件形成映射关系  -->
     	<!-- 遵循规范：需要将mapper接口类名与xml文件映射名称保持一致，且在一个目录中 上边规范的前提是：使用的是mapper代理的方法 -->  
      	<!-- <mapper class="com.zfsoft.mapper.UserMapper"/> -->  
      
      	<!-- 第三种方式，指定接口的包名称，自动扫描包下的所有mapper接口进行加载  -->
      	<!-- <package name="com.zfsoft.taglibs.dao.daointerface"/> -->
      	
      	<!-- 第四种方式：通过url指定mapper文件位置 -->
      	<!-- <mapper url="file://........"/> -->
        
    </mappers>  
    <plugins>
		<plugin interceptor="com.vsoft.fastorm.mybatis.interceptor.DynamicMapperInterceptor">
			<property name="dialect" value="oracle"/>
			<property name="dynamicID" value=".*Dynamic*.*"/>
		</plugin>
		<plugin interceptor="com.vsoft.fastorm.mybatis.interceptor.PaginationInterceptor">
			<property name="dialect" value="oracle"/>
			<property name="paginationID" value=".*Paged*.*"/>
		</plugin>
		<plugin interceptor="com.vsoft.fastorm.mybatis.interceptor.PartitionPaginationInterceptor">
			<property name="dialect" value="oracle"/>
			<property name="paginationID" value=".*Paged*.*"/>
		</plugin>
	</plugins>
	<!-- 对事务的管理和连接池的配置:与Spring集成时该 -->
    <environments default="development">  
        <environment id="development">  
            <transactionManager type="JDBC" />  
            <dataSource type="POOLED">  
                <property name="driver" value="oracle.jdbc.driver.OracleDriver" />  
                <property name="url" value="jdbc:oracle:thin:@localhost:1521:orcl" />  
                <property name="username" value="ibatis" />  
                <property name="password" value="ibatis" />  
            </dataSource>  
        </environment>  
    </environments
</configuration>  
