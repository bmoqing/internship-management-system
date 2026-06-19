/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

package fun.bmoqing.internship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import fun.bmoqing.internship.entity.Position;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PositionMapper extends BaseMapper<Position> {
}