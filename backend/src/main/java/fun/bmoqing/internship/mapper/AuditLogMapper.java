package fun.bmoqing.internship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {

    @Select("SELECT a.*, u.name AS operatorName, u.username AS operatorUsername " +
            "FROM sys_audit_log a " +
            "LEFT JOIN sys_user u ON a.operator_id = u.id " +
            "WHERE (#{keyword} = '' " +
            "   OR a.action LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR a.target_type LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR a.detail LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR u.name LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR u.username LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND (#{action} = '' OR a.action = #{action}) " +
            "AND (#{targetType} = '' OR a.target_type = #{targetType}) " +
            "ORDER BY a.create_time DESC, a.id DESC")
    Page<AuditLog> selectPageForAdmin(Page<AuditLog> page,
                                      @Param("keyword") String keyword,
                                      @Param("action") String action,
                                      @Param("targetType") String targetType);
}
