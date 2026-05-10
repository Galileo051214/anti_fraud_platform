package com.anti.mapper;

import com.anti.entity.Challenge;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 闯关关卡Mapper接口
 */
@Mapper
public interface ChallengeMapper extends BaseMapper<Challenge> {

    /**
     * 查询所有已启用的关卡(按顺序)
     */
    @Select("SELECT * FROM challenge WHERE status = 1 ORDER BY level_order ASC")
    List<Challenge> selectEnabledChallenges();

    /**
     * 根据类型查询关卡
     */
    @Select("SELECT * FROM challenge WHERE status = 1 AND type = #{type} ORDER BY level_order ASC")
    List<Challenge> selectChallengesByType(@Param("type") String type);

    /**
     * 获取下一个未解锁的关卡
     */
    @Select("SELECT * FROM challenge WHERE status = 1 AND level_order > #{currentLevel} ORDER BY level_order ASC LIMIT 1")
    Challenge selectNextChallenge(@Param("currentLevel") int currentLevel);
}
