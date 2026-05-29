package com.campus.recruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.recruitment.entity.Job;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface JobMapper extends BaseMapper<Job> {

    @Update("UPDATE job SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);

    @Update("UPDATE job SET favorite_count = favorite_count + 1 WHERE id = #{id}")
    int incrementFavoriteCount(@Param("id") Long id);

    @Update("UPDATE job SET favorite_count = GREATEST(favorite_count - 1, 0) WHERE id = #{id}")
    int decrementFavoriteCount(@Param("id") Long id);

    @Update("UPDATE job SET apply_count = apply_count + 1 WHERE id = #{id}")
    int incrementApplyCount(@Param("id") Long id);
}
