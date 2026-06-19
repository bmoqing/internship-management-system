package fun.bmoqing.internship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.entity.Score;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ScoreMapper extends BaseMapper<Score> {

    @Select("SELECT s.id, u.id AS studentId, u.name AS studentName, u.username AS studentNo, " +
            "s.teacher_score AS teacherScore, s.attendance_score AS attendanceScore, " +
            "s.extra_score AS extraScore, s.final_score AS finalScore, " +
            "s.company_score AS companyScore, s.company_comment AS companyComment, " +
            "s.teacher_comment AS teacherComment, s.update_time AS updateTime " +
            "FROM sys_user u " +
            "LEFT JOIN internship_score s ON s.student_id = u.id " +
            "WHERE u.role = 'STUDENT' " +
            "AND (u.name LIKE CONCAT('%', #{keyword}, '%') OR u.username LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY s.update_time DESC, u.id DESC")
    Page<Score> selectStudentScores(Page<Score> page, @Param("keyword") String keyword);

    @Select("SELECT s.id, u.id AS studentId, u.name AS studentName, u.username AS studentNo, " +
            "s.teacher_score AS teacherScore, s.attendance_score AS attendanceScore, " +
            "s.extra_score AS extraScore, s.final_score AS finalScore, " +
            "s.company_score AS companyScore, s.company_comment AS companyComment, " +
            "s.teacher_comment AS teacherComment, s.update_time AS updateTime " +
            "FROM internship_assignment ia " +
            "INNER JOIN sys_user u ON ia.student_id = u.id " +
            "LEFT JOIN internship_score s ON s.student_id = u.id " +
            "WHERE ia.teacher_id = #{teacherId} AND ia.status = 1 " +
            "AND (u.name LIKE CONCAT('%', #{keyword}, '%') OR u.username LIKE CONCAT('%', #{keyword}, '%')) " +
            "GROUP BY u.id, s.id " +
            "ORDER BY s.update_time DESC, u.id DESC")
    Page<Score> selectStudentScoresByTeacher(Page<Score> page,
                                             @Param("keyword") String keyword,
                                             @Param("teacherId") Long teacherId);

    @Select("SELECT s.id, u.id AS studentId, u.name AS studentName, u.username AS studentNo, " +
            "s.teacher_score AS teacherScore, s.attendance_score AS attendanceScore, " +
            "s.extra_score AS extraScore, s.final_score AS finalScore, " +
            "s.company_score AS companyScore, s.company_comment AS companyComment, " +
            "s.teacher_comment AS teacherComment, s.update_time AS updateTime " +
            "FROM internship_assignment ia " +
            "INNER JOIN sys_user u ON ia.student_id = u.id " +
            "LEFT JOIN internship_score s ON s.student_id = u.id " +
            "WHERE ia.company_id = #{companyId} AND ia.status = 1 " +
            "AND (u.name LIKE CONCAT('%', #{keyword}, '%') OR u.username LIKE CONCAT('%', #{keyword}, '%')) " +
            "GROUP BY u.id, s.id " +
            "ORDER BY s.update_time DESC, u.id DESC")
    Page<Score> selectStudentScoresByCompany(Page<Score> page,
                                             @Param("keyword") String keyword,
                                             @Param("companyId") Long companyId);
}
