/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

package fun.bmoqing.internship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.entity.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReportMapper extends BaseMapper<Report> {

    @Select("SELECT r.*, u.name AS studentName, u.username AS studentNo, rv.name AS reviewerName " +
            "FROM internship_report r " +
            "LEFT JOIN sys_user u ON r.student_id = u.id " +
            "LEFT JOIN sys_user rv ON r.reviewer_id = rv.id " +
            "WHERE r.student_id = #{studentId} " +
            "ORDER BY r.submit_time DESC, r.id DESC")
    Page<Report> selectPageForStudent(Page<Report> page, @Param("studentId") Long studentId);

    @Select("SELECT r.*, u.name AS studentName, u.username AS studentNo, rv.name AS reviewerName " +
            "FROM internship_report r " +
            "LEFT JOIN sys_user u ON r.student_id = u.id " +
            "LEFT JOIN sys_user rv ON r.reviewer_id = rv.id " +
            "WHERE (u.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR u.username LIKE CONCAT('%', #{keyword}, '%') " +
            "OR r.title LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY r.status ASC, r.submit_time DESC")
    Page<Report> selectPageForAdmin(Page<Report> page, @Param("keyword") String keyword);

    @Select("SELECT r.*, u.name AS studentName, u.username AS studentNo, rv.name AS reviewerName " +
            "FROM internship_report r " +
            "LEFT JOIN sys_user u ON r.student_id = u.id " +
            "LEFT JOIN sys_user rv ON r.reviewer_id = rv.id " +
            "INNER JOIN internship_assignment ia ON ia.status = 1 AND ((r.assignment_id IS NOT NULL AND ia.id = r.assignment_id) OR (r.assignment_id IS NULL AND ia.student_id = r.student_id)) " +
            "WHERE ia.teacher_id = #{teacherId} " +
            "AND (u.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR u.username LIKE CONCAT('%', #{keyword}, '%') " +
            "OR r.title LIKE CONCAT('%', #{keyword}, '%')) " +
            "GROUP BY r.id " +
            "ORDER BY r.status ASC, r.submit_time DESC")
    Page<Report> selectPageForTeacher(Page<Report> page,
                                       @Param("keyword") String keyword,
                                       @Param("teacherId") Long teacherId);

    @Select("SELECT COUNT(*) FROM internship_report " +
            "WHERE student_id = #{studentId} AND status = 1")
    long countPendingByStudent(@Param("studentId") Long studentId);

    @Select("SELECT COUNT(*) FROM internship_report r " +
            "INNER JOIN internship_assignment ia ON ia.status = 1 AND ((r.assignment_id IS NOT NULL AND ia.id = r.assignment_id) OR (r.assignment_id IS NULL AND ia.student_id = r.student_id)) " +
            "WHERE ia.teacher_id = #{teacherId} AND r.status = 1")
    long countPendingByTeacher(@Param("teacherId") Long teacherId);
}
