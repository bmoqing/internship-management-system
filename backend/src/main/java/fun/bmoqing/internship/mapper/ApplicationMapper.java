package fun.bmoqing.internship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.entity.Application;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ApplicationMapper extends BaseMapper<Application> {

    // 自定义关联查询：查出申请记录的同时，关联查出 position 表和 sys_user 表的信息
    @Select("SELECT a.*, " +
            "p.title as positionTitle, p.company_name as companyName, p.company_id as companyId, " +
            "u.name as studentName, u.resume_url as studentResumeUrl, " +
            "t.name as teacherName, m.name as mentorName, rt.name as reviewTeacherName " +
            "FROM internship_application a " +
            "LEFT JOIN internship_position p ON a.position_id = p.id " +
            "LEFT JOIN sys_user u ON a.student_id = u.id " +
            "LEFT JOIN internship_assignment ia ON ia.application_id = a.id AND ia.status = 1 " +
            "LEFT JOIN sys_user t ON ia.teacher_id = t.id " +
            "LEFT JOIN sys_user rt ON a.review_teacher_id = rt.id " +
            "LEFT JOIN sys_user m ON ia.mentor_id = m.id " +
            "WHERE a.student_id = #{studentId} " + // 只查当前学生的
            "ORDER BY a.apply_time DESC")
    Page<Application> selectStudentApplications(Page<Application> page, @Param("studentId") Long studentId);

    @Select("SELECT a.*, " +
            "p.title as positionTitle, p.company_name as companyName, p.company_id as companyId, " +
            "u.name as studentName, u.resume_url as studentResumeUrl, " +
            "t.name as teacherName, m.name as mentorName, rt.name as reviewTeacherName " +
            "FROM internship_application a " +
            "LEFT JOIN internship_position p ON a.position_id = p.id " +
            "LEFT JOIN sys_user u ON a.student_id = u.id " +
            "LEFT JOIN internship_assignment ia ON ia.application_id = a.id AND ia.status = 1 " +
            "LEFT JOIN sys_user t ON ia.teacher_id = t.id " +
            "LEFT JOIN sys_user rt ON a.review_teacher_id = rt.id " +
            "LEFT JOIN sys_user m ON ia.mentor_id = m.id " +
            "WHERE (u.name LIKE CONCAT('%', #{keyword}, '%') OR p.title LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND a.review_teacher_id = #{teacherId} " +
            "ORDER BY CASE WHEN a.status = 1 THEN 0 ELSE 1 END, a.apply_time DESC")
    Page<Application> selectTeacherApplications(Page<Application> page,
                                                 @Param("keyword") String keyword,
                                                 @Param("teacherId") Long teacherId);

    @Select("SELECT a.*, " +
            "p.title as positionTitle, p.company_name as companyName, p.company_id as companyId, " +
            "u.name as studentName, u.resume_url as studentResumeUrl, " +
            "t.name as teacherName, m.name as mentorName, rt.name as reviewTeacherName " +
            "FROM internship_application a " +
            "LEFT JOIN internship_position p ON a.position_id = p.id " +
            "LEFT JOIN sys_user u ON a.student_id = u.id " +
            "LEFT JOIN internship_assignment ia ON ia.application_id = a.id AND ia.status = 1 " +
            "LEFT JOIN sys_user t ON ia.teacher_id = t.id " +
            "LEFT JOIN sys_user rt ON a.review_teacher_id = rt.id " +
            "LEFT JOIN sys_user m ON ia.mentor_id = m.id " +
            "WHERE (u.name LIKE CONCAT('%', #{keyword}, '%') OR p.title LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY CASE WHEN a.status = 2 THEN 0 WHEN a.status = 1 THEN 1 ELSE 2 END, a.apply_time DESC")
    Page<Application> selectAdminApplications(Page<Application> page, @Param("keyword") String keyword);

    @Select("SELECT a.*, p.title AS positionTitle, p.company_name AS companyName, p.company_id AS companyId, u.name AS studentName, u.resume_url AS studentResumeUrl, rt.name AS reviewTeacherName " +
            "FROM internship_application a " +
            "LEFT JOIN internship_position p ON a.position_id = p.id " +
            "LEFT JOIN sys_user u ON a.student_id = u.id " +
            "LEFT JOIN sys_user rt ON a.review_teacher_id = rt.id " +
            "LEFT JOIN internship_assignment ia ON ia.application_id = a.id AND ia.status = 1 " +
            "WHERE a.status = 4 AND ia.id IS NULL " +
            "AND (u.name LIKE CONCAT('%', #{keyword}, '%') OR p.title LIKE CONCAT('%', #{keyword}, '%') OR p.company_name LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY a.apply_time DESC")
    Page<Application> selectApprovedWithoutAssignment(Page<Application> page, @Param("keyword") String keyword);

    @Select("SELECT a.*, p.title AS positionTitle, p.company_name AS companyName, p.company_id AS companyId, u.name AS studentName, u.resume_url AS studentResumeUrl, rt.name AS reviewTeacherName, t.name AS teacherName, m.name AS mentorName " +
            "FROM internship_application a " +
            "LEFT JOIN internship_position p ON a.position_id = p.id " +
            "LEFT JOIN sys_user u ON a.student_id = u.id " +
            "LEFT JOIN sys_user rt ON a.review_teacher_id = rt.id " +
            "LEFT JOIN internship_assignment ia ON ia.application_id = a.id AND ia.status = 1 " +
            "LEFT JOIN sys_user t ON ia.teacher_id = t.id " +
            "LEFT JOIN sys_user m ON ia.mentor_id = m.id " +
            "WHERE p.company_id = #{companyId} " +
            "AND (u.name LIKE CONCAT('%', #{keyword}, '%') OR p.title LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY CASE WHEN a.status = 0 THEN 0 ELSE 1 END, a.apply_time DESC")
    Page<Application> selectCompanyApplications(Page<Application> page,
                                                 @Param("keyword") String keyword,
                                                 @Param("companyId") Long companyId);
    // 统计每个岗位的申请数量，用于画饼图
    @Select("SELECT p.title as name, COUNT(a.id) as value " +
            "FROM internship_application a " +
            "LEFT JOIN internship_position p ON a.position_id = p.id " +
            "GROUP BY p.title")
    List<Map<String, Object>> selectCountByPosition();

    @Select("SELECT CASE a.status " +
            "WHEN 0 THEN '待企业预审' " +
            "WHEN 1 THEN '待教师审核' " +
            "WHEN 2 THEN '待管理员终审' " +
            "WHEN 3 THEN '已驳回' " +
            "WHEN 4 THEN '待分配' " +
            "WHEN 5 THEN '已分配' " +
            "ELSE '其他' END AS name, COUNT(*) AS value " +
            "FROM internship_application a " +
            "GROUP BY a.status")
    List<Map<String, Object>> selectCountByStatus();

    @Select("SELECT COUNT(*) FROM internship_application WHERE student_id = #{studentId}")
    long countByStudentId(@Param("studentId") Long studentId);

    @Select("SELECT COUNT(*) FROM internship_application a " +
            "INNER JOIN internship_position p ON a.position_id = p.id " +
            "WHERE p.company_id = #{companyId}")
    long countByCompanyId(@Param("companyId") Long companyId);

    @Select("SELECT COUNT(*) FROM internship_application WHERE review_teacher_id = #{teacherId}")
    long countByReviewTeacherId(@Param("teacherId") Long teacherId);

    @Select("SELECT COUNT(DISTINCT student_id) FROM internship_application WHERE review_teacher_id = #{teacherId}")
    long countDistinctStudentByReviewTeacherId(@Param("teacherId") Long teacherId);

    @Select("SELECT COUNT(*) FROM internship_application WHERE status = #{status}")
    long countByStatus(@Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM internship_application WHERE status = #{status} AND review_teacher_id IS NULL")
    long countByStatusAndNoReviewTeacher(@Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM internship_application WHERE review_teacher_id = #{teacherId} AND status = #{status}")
    long countByReviewTeacherIdAndStatus(@Param("teacherId") Long teacherId,
                                         @Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM internship_application a " +
            "INNER JOIN internship_position p ON a.position_id = p.id " +
            "WHERE p.company_id = #{companyId} AND a.status = #{status}")
    long countByCompanyIdAndStatus(@Param("companyId") Long companyId,
                                   @Param("status") Integer status);

    @Select("SELECT IFNULL(p.title, '未知岗位') AS name, COUNT(a.id) AS value " +
            "FROM internship_application a " +
            "LEFT JOIN internship_position p ON a.position_id = p.id " +
            "GROUP BY p.id, p.title " +
            "ORDER BY value DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectTopPositions(@Param("limit") Integer limit);

    @Select("SELECT DATE_FORMAT(a.apply_time, '%Y-%m-%d') AS name, COUNT(*) AS value " +
            "FROM internship_application a " +
            "WHERE a.apply_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
            "GROUP BY DATE_FORMAT(a.apply_time, '%Y-%m-%d') " +
            "ORDER BY name ASC")
    List<Map<String, Object>> selectApplyTrendLast7Days();

    @Update("UPDATE internship_application SET review_teacher_id = #{teacherId} " +
            "WHERE student_id = #{studentId} AND status IN (0, 1)")
    int updateReviewTeacherForPendingByStudentId(@Param("studentId") Long studentId,
                                                  @Param("teacherId") Long teacherId);
}
