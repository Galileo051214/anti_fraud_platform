package com.anti.mapper;

import com.anti.entity.CaseTagRelation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 案例标签关联Mapper接口
 */
@Mapper
public interface CaseTagRelationMapper extends BaseMapper<CaseTagRelation> {

    /**
     * 根据案例ID查询标签ID列表
     */
    @Select("SELECT tag_id FROM case_tag_relation WHERE case_id = #{caseId}")
    List<Long> selectTagIdsByCaseId(@Param("caseId") Long caseId);

    /**
     * 根据案例ID删除所有关联
     */
    void deleteByCaseId(@Param("caseId") Long caseId);
}
