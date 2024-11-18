package com.baomidou.mybatisplus.enhance.injector;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.enhance.injector.methods.*;
import org.apache.ibatis.session.Configuration;

import java.util.List;

/**
 * 自定义sql注入器，增加通用方法
 */
public class EnhanceSqlInjector extends DefaultSqlInjector {

    @Override
    public List<AbstractMethod> getMethodList(Configuration configuration, Class<?> mapperClass, TableInfo tableInfo) {
        List<AbstractMethod> methodList = super.getMethodList(configuration, mapperClass, tableInfo);
        methodList.add(new SelectIgnoreDecryptMaps());
        methodList.add(new SelectIgnoreDecryptObjs());
        methodList.add(new SelectIgnoreDecryptList());
        if (tableInfo.havePK()) {
            // 根据ID 查询一条数据, 不解密
            methodList.add(new SelectIgnoreDecryptById());
            // 根据ID集合，批量查询数据，不解密
            methodList.add(new SelectIgnoreDecryptBatchIds());
        } else {
            logger.warn(String.format("%s ,Not found @TableId annotation, Cannot use Mybatis-Plus 'xxById' Method.",
                    tableInfo.getEntityType()));
        }

        return methodList;
    }

}
