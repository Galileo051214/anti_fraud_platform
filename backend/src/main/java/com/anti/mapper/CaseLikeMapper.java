package com.anti.mapper;

import com.anti.entity.CaseLike;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 案例点赞Mapper接口
 */
@Mapper
public interface CaseLikeMapper extends BaseMapper<CaseLike> {

    /**
     * 判断用户是否已点赞
     */
    @Select("SELECT COUNT(*) > 0 FROM case_like WHERE case_id = #{caseId} AND user_id = #{userId}")
    boolean existsByCaseIdAndUserId(@Param("caseId") Long caseId, @Param("userId") Long userId);

    /**
     * 统计案例点赞数
     */
    @Select("SELECT COUNT(*) FROM case_like WHERE case_id = #{caseId}")
    int countByCaseId(@Param("caseId") Long caseId);
}
