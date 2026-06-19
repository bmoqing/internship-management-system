package fun.bmoqing.internship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.entity.Appeal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AppealMapper extends BaseMapper<Appeal> {

    @Select("SELECT a.*, s.name AS studentName, s.username AS studentNo, " +
            "t.name AS teacherName, ad.name AS adminName, c.name AS companyName, p.title AS positionTitle " +
            "FROM internship_appeal a " +
            "LEFT JOIN sys_user s ON a.student_id = s.id " +
            "LEFT JOIN sys_user t ON a.teacher_id = t.id " +
            "LEFT JOIN sys_user ad ON a.admin_id = ad.id " +
            "LEFT JOIN internship_assignment ia ON a.assignment_id = ia.id " +
            "LEFT JOIN internship_company c ON ia.company_id = c.id " +
            "LEFT JOIN internship_position p ON ia.position_id = p.id " +
            "WHERE a.student_id = #{studentId} " +
            "ORDER BY a.create_time DESC, a.id DESC")
    Page<Appeal> selectPageForStudent(Page<Appeal> page, @Param("studentId") Long studentId);

    @Select("SELECT a.*, s.name AS studentName, s.username AS studentNo, " +
            "t.name AS teacherName, ad.name AS adminName, c.name AS companyName, p.title AS positionTitle " +
            "FROM internship_appeal a " +
            "LEFT JOIN sys_user s ON a.student_id = s.id " +
            "LEFT JOIN sys_user t ON a.teacher_id = t.id " +
            "LEFT JOIN sys_user ad ON a.admin_id = ad.id " +
            "LEFT JOIN internship_assignment ia ON a.assignment_id = ia.id " +
            "LEFT JOIN internship_company c ON ia.company_id = c.id " +
            "LEFT JOIN internship_position p ON ia.position_id = p.id " +
            "WHERE ia.teacher_id = #{teacherId} " +
            "AND (s.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR s.username LIKE CONCAT('%', #{keyword}, '%') " +
            "OR a.target_type LIKE CONCAT('%', #{keyword}, '%') " +
            "OR a.reason LIKE CONCAT('%', #{keyword}, '%') " +
            "OR p.title LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND (#{status} IS NULL OR a.status = #{status}) " +
            "ORDER BY a.status ASC, a.create_time DESC, a.id DESC")
    Page<Appeal> selectPageForTeacher(Page<Appeal> page,
                                      @Param("keyword") String keyword,
                                      @Param("status") Integer status,
                                      @Param("teacherId") Long teacherId);

    @Select("SELECT a.*, s.name AS studentName, s.username AS studentNo, " +
            "t.name AS teacherName, ad.name AS adminName, c.name AS companyName, p.title AS positionTitle " +
            "FROM internship_appeal a " +
            "LEFT JOIN sys_user s ON a.student_id = s.id " +
            "LEFT JOIN sys_user t ON a.teacher_id = t.id " +
            "LEFT JOIN sys_user ad ON a.admin_id = ad.id " +
            "LEFT JOIN internship_assignment ia ON a.assignment_id = ia.id " +
            "LEFT JOIN internship_company c ON ia.company_id = c.id " +
            "LEFT JOIN internship_position p ON ia.position_id = p.id " +
            "WHERE (s.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR s.username LIKE CONCAT('%', #{keyword}, '%') " +
            "OR a.target_type LIKE CONCAT('%', #{keyword}, '%') " +
            "OR a.reason LIKE CONCAT('%', #{keyword}, '%') " +
            "OR p.title LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND (#{status} IS NULL OR a.status = #{status}) " +
            "ORDER BY a.status ASC, a.create_time DESC, a.id DESC")
    Page<Appeal> selectPageForAdmin(Page<Appeal> page,
                                    @Param("keyword") String keyword,
                                    @Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM internship_appeal a " +
            "LEFT JOIN internship_assignment ia ON a.assignment_id = ia.id " +
            "WHERE a.status = 0 AND ia.teacher_id = #{teacherId}")
    long countPendingByTeacher(@Param("teacherId") Long teacherId);

    @Select("SELECT COUNT(*) FROM internship_appeal WHERE status = 1")
    long countPendingForAdmin();

    @Select("SELECT COUNT(*) FROM internship_appeal " +
            "WHERE student_id = #{studentId} AND status IN (0, 1, 2)")
    long countInProgressByStudent(@Param("studentId") Long studentId);
}
