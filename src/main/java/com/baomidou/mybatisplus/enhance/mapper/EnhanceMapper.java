package com.baomidou.mybatisplus.enhance.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.enhance.crypto.annotation.IgnoreEncrypted;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface EnhanceMapper<T> extends BaseMapper<T> {

    /**
     * 根据ID 查询一条数据
     * @param id 主键ID
     * @return 实体对象
     */
    @IgnoreEncrypted
    T selectIgnoreDecryptById(Serializable id);

    /**
     * 根据ID集合，批量查询数据
     * @param idList 主键ID列表(不能为 null 以及 empty)
     * @return 实体对象集合
     */
    @IgnoreEncrypted
    List<T> selectIgnoreDecryptBatchIds(@Param(Constants.COLL) Collection<? extends Serializable> idList);

    /**
     * 根据 Wrapper 条件，查询全部记录
     *
     * @param queryWrapper 实体对象封装操作类
     */
    @IgnoreEncrypted
    List<Map<String, Object>> selectIgnoreDecryptMaps(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper);

    /**
     * 查询（根据 columnMap 条件）
     *
     * @param columnMap 表字段 map 对象
     */
    default List<T> selectIgnoreDecryptByMap(Map<String, Object> columnMap) {
        return this.selectIgnoreDecryptList(Wrappers.<T>query().allEq(columnMap));
    }

    /**
     * 根据 entity 条件，查询全部记录
     *
     * @param queryWrapper 实体对象封装操作类（可以为 null）
     */
    @IgnoreEncrypted
    List<T> selectIgnoreDecryptList(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper);

    /**
     * 根据 Wrapper 条件，查询全部记录
     * <p>注意： 只返回第一个字段的值</p>
     *
     * @param queryWrapper 实体对象封装操作类（可以为 null）
     */
    @IgnoreEncrypted
    <E> List<E> selectIgnoreDecryptObjs(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper);

}
