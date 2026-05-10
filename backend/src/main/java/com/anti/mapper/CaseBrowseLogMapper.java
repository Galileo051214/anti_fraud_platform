package com.anti.mapper;

import com.anti.entity.CaseBrowseLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 案例浏览记录Mapper接口
 */
@Mapper
public interface CaseBrowseLogMapper extends BaseMapper<CaseBrowseLog> {

    /**
     * 分页查询用户浏览记录
     */
    @Select("SELECT * FROM case_browse_log WHERE user_id = #{userId} ORDER BY browse_time DESC")
    IPage<CaseBrowseLog> selectByUserId(Page<CaseBrowseLog> page, @Param("userId") Long userId);

    /**
     * 统计用户浏览案例数
     */
    @Select("SELECT COUNT(DISTINCT case_id) FROM case_browse_log WHERE user_id = #{userId}")
    int countDistinctCasesByUserId(@Param("userId") Long userId);

    /**
     * 统计用户浏览某案例的次数（用于积分规则：首次浏览发放积分）
     */
    @Select("SELECT COUNT(*) FROM case_browse_log WHERE user_id = #{userId} AND case_id = #{caseId}")
    int countByUserIdAndCaseId(@Param("userId") Long userId, @Param("caseId") Long caseId);
}
