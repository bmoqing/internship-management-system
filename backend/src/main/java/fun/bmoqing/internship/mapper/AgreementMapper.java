/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

package fun.bmoqing.internship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.entity.Agreement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AgreementMapper extends BaseMapper<Agreement> {

    @Select("SELECT a.*, s.name AS studentName, s.username AS studentNo, c.name AS companyName, " +
            "u.name AS uploaderName, rv.name AS reviewerName, t.name AS teacherName " +
            "FROM internship_agreement a " +
            "LEFT JOIN sys_user s ON a.student_id = s.id " +
            "LEFT JOIN internship_company c ON a.company_id = c.id " +
            "LEFT JOIN sys_user u ON a.uploader_id = u.id " +
            "LEFT JOIN sys_user rv ON a.reviewer_id = rv.id " +
            "LEFT JOIN sys_user t ON a.teacher_id = t.id " +
            "WHERE (s.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR s.username LIKE CONCAT('%', #{keyword}, '%') " +
            "OR a.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR c.name LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY a.status ASC, a.upload_time DESC, a.id DESC")
    Page<Agreement> selectPageForAdmin(Page<Agreement> page, @Param("keyword") String keyword);

    @Select("SELECT a.*, s.name AS studentName, s.username AS studentNo, c.name AS companyName, " +
            "u.name AS uploaderName, rv.name AS reviewerName, t.name AS teacherName " +
            "FROM internship_agreement a " +
            "LEFT JOIN sys_user s ON a.student_id = s.id " +
            "LEFT JOIN internship_company c ON a.company_id = c.id " +
            "LEFT JOIN sys_user u ON a.uploader_id = u.id " +
            "LEFT JOIN sys_user rv ON a.reviewer_id = rv.id " +
            "LEFT JOIN sys_user t ON a.teacher_id = t.id " +
            "LEFT JOIN internship_assignment ia ON a.assignment_id = ia.id " +
            "WHERE (a.teacher_id = #{teacherId} OR ia.teacher_id = #{teacherId}) " +
            "AND (s.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR s.username LIKE CONCAT('%', #{keyword}, '%') " +
            "OR a.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR c.name LIKE CONCAT('%', #{keyword}, '%')) " +
            "GROUP BY a.id " +
            "ORDER BY a.status ASC, a.upload_time DESC, a.id DESC")
    Page<Agreement> selectPageForTeacher(Page<Agreement> page,
                                         @Param("keyword") String keyword,
                                         @Param("teacherId") Long teacherId);

    @Select("SELECT a.*, s.name AS studentName, s.username AS studentNo, c.name AS companyName, " +
            "u.name AS uploaderName, rv.name AS reviewerName, t.name AS teacherName " +
            "FROM internship_agreement a " +
            "LEFT JOIN sys_user s ON a.student_id = s.id " +
            "LEFT JOIN internship_company c ON a.company_id = c.id " +
            "LEFT JOIN sys_user u ON a.uploader_id = u.id " +
            "LEFT JOIN sys_user rv ON a.reviewer_id = rv.id " +
            "LEFT JOIN sys_user t ON a.teacher_id = t.id " +
            "WHERE a.company_id = #{companyId} " +
            "AND (s.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR s.username LIKE CONCAT('%', #{keyword}, '%') " +
            "OR a.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR c.name LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY a.status ASC, a.upload_time DESC, a.id DESC")
    Page<Agreement> selectPageForCompany(Page<Agreement> page,
                                         @Param("keyword") String keyword,
                                         @Param("companyId") Long companyId);

    @Select("SELECT a.*, s.name AS studentName, s.username AS studentNo, c.name AS companyName, " +
            "u.name AS uploaderName, rv.name AS reviewerName, t.name AS teacherName " +
            "FROM internship_agreement a " +
            "LEFT JOIN sys_user s ON a.student_id = s.id " +
            "LEFT JOIN internship_company c ON a.company_id = c.id " +
            "LEFT JOIN sys_user u ON a.uploader_id = u.id " +
            "LEFT JOIN sys_user rv ON a.reviewer_id = rv.id " +
            "LEFT JOIN sys_user t ON a.teacher_id = t.id " +
            "WHERE a.student_id = #{studentId} " +
            "ORDER BY a.upload_time DESC, a.id DESC")
    Page<Agreement> selectPageForStudent(Page<Agreement> page, @Param("studentId") Long studentId);

    @Select("SELECT COUNT(*) FROM internship_agreement WHERE student_id = #{studentId} AND status = 0")
    long countPendingByStudent(@Param("studentId") Long studentId);

    @Select("SELECT COUNT(*) FROM internship_agreement WHERE assignment_id = #{assignmentId} AND status IN (1, 3)")
    long countApprovedByAssignmentId(@Param("assignmentId") Long assignmentId);
}
