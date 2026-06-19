package fun.bmoqing.internship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.entity.Assignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AssignmentMapper extends BaseMapper<Assignment> {

    @Select("SELECT ia.*, s.name AS studentName, s.username AS studentNo, " +
            "p.title AS positionTitle, p.company_name AS companyName, " +
            "t.name AS teacherName, m.name AS mentorName " +
            "FROM internship_assignment ia " +
            "LEFT JOIN sys_user s ON ia.student_id = s.id " +
            "LEFT JOIN internship_position p ON ia.position_id = p.id " +
            "LEFT JOIN sys_user t ON ia.teacher_id = t.id " +
            "LEFT JOIN sys_user m ON ia.mentor_id = m.id " +
            "WHERE (s.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR s.username LIKE CONCAT('%', #{keyword}, '%') " +
            "OR p.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR p.company_name LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY ia.assign_time DESC, ia.id DESC")
    Page<Assignment> selectPageForAdmin(Page<Assignment> page, @Param("keyword") String keyword);

    @Select("SELECT ia.*, s.name AS studentName, s.username AS studentNo, " +
            "p.title AS positionTitle, p.company_name AS companyName, " +
            "t.name AS teacherName, m.name AS mentorName " +
            "FROM internship_assignment ia " +
            "LEFT JOIN sys_user s ON ia.student_id = s.id " +
            "LEFT JOIN internship_position p ON ia.position_id = p.id " +
            "LEFT JOIN sys_user t ON ia.teacher_id = t.id " +
            "LEFT JOIN sys_user m ON ia.mentor_id = m.id " +
            "WHERE ia.teacher_id = #{teacherId} " +
            "AND (s.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR s.username LIKE CONCAT('%', #{keyword}, '%') " +
            "OR p.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR p.company_name LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY ia.assign_time DESC, ia.id DESC")
    Page<Assignment> selectPageForTeacher(Page<Assignment> page,
                                          @Param("keyword") String keyword,
                                          @Param("teacherId") Long teacherId);

    @Select("SELECT ia.*, s.name AS studentName, s.username AS studentNo, " +
            "p.title AS positionTitle, p.company_name AS companyName, " +
            "t.name AS teacherName, m.name AS mentorName " +
            "FROM internship_assignment ia " +
            "LEFT JOIN sys_user s ON ia.student_id = s.id " +
            "LEFT JOIN internship_position p ON ia.position_id = p.id " +
            "LEFT JOIN sys_user t ON ia.teacher_id = t.id " +
            "LEFT JOIN sys_user m ON ia.mentor_id = m.id " +
            "WHERE ia.mentor_id = #{mentorId} " +
            "AND (s.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR s.username LIKE CONCAT('%', #{keyword}, '%') " +
            "OR p.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR p.company_name LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY ia.assign_time DESC, ia.id DESC")
    Page<Assignment> selectPageForMentor(Page<Assignment> page,
                                         @Param("keyword") String keyword,
                                         @Param("mentorId") Long mentorId);

    @Select("SELECT ia.*, s.name AS studentName, s.username AS studentNo, " +
            "p.title AS positionTitle, p.company_name AS companyName, " +
            "t.name AS teacherName, m.name AS mentorName " +
            "FROM internship_assignment ia " +
            "LEFT JOIN sys_user s ON ia.student_id = s.id " +
            "LEFT JOIN internship_position p ON ia.position_id = p.id " +
            "LEFT JOIN sys_user t ON ia.teacher_id = t.id " +
            "LEFT JOIN sys_user m ON ia.mentor_id = m.id " +
            "WHERE ia.company_id = #{companyId} " +
            "AND (s.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR s.username LIKE CONCAT('%', #{keyword}, '%') " +
            "OR p.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR p.company_name LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY ia.assign_time DESC, ia.id DESC")
    Page<Assignment> selectPageForCompany(Page<Assignment> page,
                                          @Param("keyword") String keyword,
                                          @Param("companyId") Long companyId);

    @Select("SELECT ia.*, s.name AS studentName, s.username AS studentNo, " +
            "p.title AS positionTitle, p.company_name AS companyName, " +
            "t.name AS teacherName, m.name AS mentorName " +
            "FROM internship_assignment ia " +
            "LEFT JOIN sys_user s ON ia.student_id = s.id " +
            "LEFT JOIN internship_position p ON ia.position_id = p.id " +
            "LEFT JOIN sys_user t ON ia.teacher_id = t.id " +
            "LEFT JOIN sys_user m ON ia.mentor_id = m.id " +
            "WHERE ia.student_id = #{studentId} " +
            "ORDER BY ia.assign_time DESC, ia.id DESC")
    Page<Assignment> selectPageForStudent(Page<Assignment> page, @Param("studentId") Long studentId);

    @Select("SELECT COUNT(*) FROM internship_assignment " +
            "WHERE application_id = #{applicationId} AND status = 1")
    long countActiveByApplicationId(@Param("applicationId") Long applicationId);

    @Select("SELECT COUNT(*) FROM internship_assignment WHERE student_id = #{studentId} AND status = 1")
    long countActiveByStudentId(@Param("studentId") Long studentId);

    @Select("SELECT student_id FROM internship_assignment WHERE teacher_id = #{teacherId} AND status = 1")
    List<Long> selectStudentIdsByTeacherId(@Param("teacherId") Long teacherId);

    @Select("SELECT student_id FROM internship_assignment WHERE company_id = #{companyId} AND status = 1")
    List<Long> selectStudentIdsByCompanyId(@Param("companyId") Long companyId);

    @Select("SELECT COUNT(*) FROM internship_assignment WHERE company_id = #{companyId} AND status = 1")
    long countActiveByCompanyId(@Param("companyId") Long companyId);

    @Select("SELECT COUNT(*) FROM internship_assignment " +
            "WHERE company_id = #{companyId} AND status = 1 AND mentor_id IS NULL")
    long countActiveWithoutMentorByCompanyId(@Param("companyId") Long companyId);

    @Select("SELECT COUNT(DISTINCT company_id) FROM internship_assignment WHERE teacher_id = #{teacherId} AND status = 1")
    long countDistinctCompanyByTeacherId(@Param("teacherId") Long teacherId);

    @Select("SELECT COUNT(DISTINCT position_id) FROM internship_assignment WHERE teacher_id = #{teacherId} AND status = 1")
    long countDistinctPositionByTeacherId(@Param("teacherId") Long teacherId);

    @Select("SELECT COUNT(*) FROM internship_assignment WHERE teacher_id = #{teacherId} AND student_id = #{studentId} AND status = 1")
    long countTeacherStudentRelation(@Param("teacherId") Long teacherId, @Param("studentId") Long studentId);

    @Select("SELECT COUNT(*) FROM internship_assignment WHERE mentor_id = #{mentorId} AND student_id = #{studentId} AND status = 1")
    long countMentorStudentRelation(@Param("mentorId") Long mentorId, @Param("studentId") Long studentId);

    @Select("SELECT * FROM internship_assignment WHERE student_id = #{studentId} AND status = 1 " +
            "ORDER BY assign_time DESC, id DESC LIMIT 1")
    Assignment selectActiveByStudentId(@Param("studentId") Long studentId);

    @Select("SELECT assign_time FROM internship_assignment WHERE student_id = #{studentId} AND status = 1 " +
            "ORDER BY assign_time DESC, id DESC LIMIT 1")
    LocalDateTime selectActiveAssignTimeByStudentId(@Param("studentId") Long studentId);
}
