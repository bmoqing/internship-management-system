package fun.bmoqing.internship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import fun.bmoqing.internship.entity.Notice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {

    @Select("SELECT * FROM sys_notice " +
            "WHERE IFNULL(status, 1) = 1 " +
            "ORDER BY create_time DESC, id DESC " +
            "LIMIT #{limit}")
    List<Notice> selectLatestActive(@Param("limit") Integer limit);
}
