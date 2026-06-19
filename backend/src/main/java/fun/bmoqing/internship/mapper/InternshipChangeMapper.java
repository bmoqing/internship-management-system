package fun.bmoqing.internship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.entity.InternshipChange;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InternshipChangeMapper extends BaseMapper<InternshipChange> {

    @Select("SELECT ic.*, u.name AS studentName, u.username AS studentNo " +
            "FROM internship_change ic " +
            "INNER JOIN sys_user u ON ic.student_id = u.id " +
            "WHERE (u.name LIKE CONCAT('%', #{keyword}, '%') OR u.username LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY ic.create_time DESC")
    Page<InternshipChange> selectAdminPage(Page<InternshipChange> page, @Param("keyword") String keyword);

    @Select("SELECT ic.*, u.name AS studentName, u.username AS studentNo " +
            "FROM internship_change ic " +
            "INNER JOIN sys_user u ON ic.student_id = u.id " +
            "WHERE ic.teacher_id = #{teacherId} " +
            "AND (u.name LIKE CONCAT('%', #{keyword}, '%') OR u.username LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY ic.create_time DESC")
    Page<InternshipChange> selectTeacherPage(Page<InternshipChange> page, @Param("keyword") String keyword, @Param("teacherId") Long teacherId);

    @Select("SELECT ic.*, u.name AS studentName, u.username AS studentNo " +
            "FROM internship_change ic " +
            "INNER JOIN sys_user u ON ic.student_id = u.id " +
            "WHERE ic.company_id = #{companyId} " +
            "AND (u.name LIKE CONCAT('%', #{keyword}, '%') OR u.username LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY ic.create_time DESC")
    Page<InternshipChange> selectCompanyPage(Page<InternshipChange> page, @Param("keyword") String keyword, @Param("companyId") Long companyId);

    @Select("SELECT ic.*, u.name AS studentName, u.username AS studentNo " +
            "FROM internship_change ic " +
            "INNER JOIN sys_user u ON ic.student_id = u.id " +
            "WHERE ic.student_id = #{studentId} " +
            "ORDER BY ic.create_time DESC")
    Page<InternshipChange> selectStudentPage(Page<InternshipChange> page, @Param("studentId") Long studentId);
}
