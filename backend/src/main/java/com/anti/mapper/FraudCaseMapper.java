package com.anti.mapper;

import com.anti.entity.FraudCase;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 诈骗案例Mapper接口
 */
@Mapper
public interface FraudCaseMapper extends BaseMapper<FraudCase> {

    /**
     * 分页查询已启用的案例
     */
    @Select("SELECT * FROM fraud_case WHERE status = 1 ORDER BY is_featured DESC, wilson_score DESC, publish_time DESC")
    IPage<FraudCase> selectEnabledCases(Page<FraudCase> page);

    /**
     * 根据标签ID分页查询案例
     */
    @Select("SELECT c.* FROM fraud_case c " +
            "INNER JOIN case_tag_relation r ON c.id = r.case_id " +
            "WHERE r.tag_id = #{tagId} AND c.status = 1 " +
            "ORDER BY c.wilson_score DESC, c.publish_time DESC")
    IPage<FraudCase> selectCasesByTagId(Page<FraudCase> page, @Param("tagId") Long tagId);

    /**
     * 获取热度排行榜(TOP N)
     */
    @Select("SELECT * FROM fraud_case WHERE status = 1 ORDER BY view_count DESC, like_count DESC LIMIT #{limit}")
    java.util.List<FraudCase> selectHotCases(@Param("limit") int limit);

    /**
     * 增加浏览量
     */
    @Update("UPDATE fraud_case SET view_count = view_count + 1 WHERE id = #{caseId}")
    void incrementViewCount(@Param("caseId") Long caseId);

    /**
     * 更新点赞数和点赞率
     */
    @Update("UPDATE fraud_case SET like_count = #{likeCount}, like_rate = #{likeRate}, wilson_score = #{wilsonScore} WHERE id = #{caseId}")
    void updateLikeStats(@Param("caseId") Long caseId, @Param("likeCount") Integer likeCount,
                         @Param("likeRate") java.math.BigDecimal likeRate, @Param("wilsonScore") java.math.BigDecimal wilsonScore);

    /**
     * 根据案例ID查询关联的标签ID列表
     */
    @Select("SELECT tag_id FROM case_tag_relation WHERE case_id = #{caseId}")
    java.util.List<Long> findTagIdsByCaseId(@Param("caseId") Long caseId);

    /**
     * 根据标签ID查询关联的案例ID列表
     */
    @Select("SELECT case_id FROM case_tag_relation WHERE tag_id = #{tagId}")
    java.util.List<Long> findCaseIdsByTagId(@Param("tagId") Long tagId);
}
