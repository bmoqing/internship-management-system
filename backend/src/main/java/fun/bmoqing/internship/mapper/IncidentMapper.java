/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

package fun.bmoqing.internship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.entity.Incident;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface IncidentMapper extends BaseMapper<Incident> {

    @Select("SELECT i.*, s.name AS studentName, s.username AS studentNo, r.name AS reporterName, h.name AS handlerName, " +
            "c.name AS companyName, p.title AS positionTitle " +
            "FROM internship_incident i " +
            "LEFT JOIN internship_assignment ia ON i.assignment_id = ia.id " +
            "LEFT JOIN sys_user s ON i.student_id = s.id " +
            "LEFT JOIN sys_user r ON i.reporter_id = r.id " +
            "LEFT JOIN sys_user h ON i.handler_id = h.id " +
            "LEFT JOIN internship_company c ON ia.company_id = c.id " +
            "LEFT JOIN internship_position p ON ia.position_id = p.id " +
            "WHERE (s.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR s.username LIKE CONCAT('%', #{keyword}, '%') " +
            "OR r.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR i.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR i.content LIKE CONCAT('%', #{keyword}, '%') " +
            "OR p.title LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND (#{status} IS NULL OR i.status = #{status}) " +
            "ORDER BY i.status ASC, i.report_time DESC, i.id DESC")
    Page<Incident> selectPageForAdmin(Page<Incident> page,
                                      @Param("keyword") String keyword,
                                      @Param("status") Integer status);

    @Select("SELECT i.*, s.name AS studentName, s.username AS studentNo, r.name AS reporterName, h.name AS handlerName, " +
            "c.name AS companyName, p.title AS positionTitle " +
            "FROM internship_incident i " +
            "LEFT JOIN internship_assignment ia ON i.assignment_id = ia.id " +
            "LEFT JOIN sys_user s ON i.student_id = s.id " +
            "LEFT JOIN sys_user r ON i.reporter_id = r.id " +
            "LEFT JOIN sys_user h ON i.handler_id = h.id " +
            "LEFT JOIN internship_company c ON ia.company_id = c.id " +
            "LEFT JOIN internship_position p ON ia.position_id = p.id " +
            "WHERE (i.reporter_id = #{teacherId} OR ia.teacher_id = #{teacherId}) " +
            "AND (s.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR s.username LIKE CONCAT('%', #{keyword}, '%') " +
            "OR r.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR i.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR i.content LIKE CONCAT('%', #{keyword}, '%') " +
            "OR p.title LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND (#{status} IS NULL OR i.status = #{status}) " +
            "ORDER BY i.status ASC, i.report_time DESC, i.id DESC")
    Page<Incident> selectPageForTeacher(Page<Incident> page,
                                        @Param("keyword") String keyword,
                                        @Param("status") Integer status,
                                        @Param("teacherId") Long teacherId);

    @Select("SELECT i.*, s.name AS studentName, s.username AS studentNo, r.name AS reporterName, h.name AS handlerName, " +
            "c.name AS companyName, p.title AS positionTitle " +
            "FROM internship_incident i " +
            "LEFT JOIN internship_assignment ia ON i.assignment_id = ia.id " +
            "LEFT JOIN sys_user s ON i.student_id = s.id " +
            "LEFT JOIN sys_user r ON i.reporter_id = r.id " +
            "LEFT JOIN sys_user h ON i.handler_id = h.id " +
            "LEFT JOIN internship_company c ON ia.company_id = c.id " +
            "LEFT JOIN internship_position p ON ia.position_id = p.id " +
            "WHERE (i.reporter_id = #{userId} OR ia.company_id = #{companyId}) " +
            "AND (s.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR s.username LIKE CONCAT('%', #{keyword}, '%') " +
            "OR r.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR i.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR i.content LIKE CONCAT('%', #{keyword}, '%') " +
            "OR p.title LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND (#{status} IS NULL OR i.status = #{status}) " +
            "ORDER BY i.status ASC, i.report_time DESC, i.id DESC")
    Page<Incident> selectPageForCompany(Page<Incident> page,
                                        @Param("keyword") String keyword,
                                        @Param("status") Integer status,
                                        @Param("companyId") Long companyId,
                                        @Param("userId") Long userId);

    @Select("SELECT i.*, s.name AS studentName, s.username AS studentNo, r.name AS reporterName, h.name AS handlerName, " +
            "c.name AS companyName, p.title AS positionTitle " +
            "FROM internship_incident i " +
            "LEFT JOIN internship_assignment ia ON i.assignment_id = ia.id " +
            "LEFT JOIN sys_user s ON i.student_id = s.id " +
            "LEFT JOIN sys_user r ON i.reporter_id = r.id " +
            "LEFT JOIN sys_user h ON i.handler_id = h.id " +
            "LEFT JOIN internship_company c ON ia.company_id = c.id " +
            "LEFT JOIN internship_position p ON ia.position_id = p.id " +
            "WHERE (i.student_id = #{studentId} OR i.reporter_id = #{studentId}) " +
            "AND (#{status} IS NULL OR i.status = #{status}) " +
            "ORDER BY i.status ASC, i.report_time DESC, i.id DESC")
    Page<Incident> selectPageForStudent(Page<Incident> page,
                                         @Param("status") Integer status,
                                         @Param("studentId") Long studentId);

    @Select("SELECT COUNT(*) FROM internship_incident WHERE status IN (0, 1)")
    long countPendingForAdmin();

    @Select("SELECT COUNT(*) FROM internship_incident i " +
            "LEFT JOIN internship_assignment ia ON i.assignment_id = ia.id " +
            "WHERE i.status IN (0, 1) AND (i.reporter_id = #{teacherId} OR ia.teacher_id = #{teacherId})")
    long countPendingByTeacher(@Param("teacherId") Long teacherId);

    @Select("SELECT COUNT(*) FROM internship_incident i " +
            "LEFT JOIN internship_assignment ia ON i.assignment_id = ia.id " +
            "WHERE i.status IN (0, 1) AND (i.reporter_id = #{userId} OR ia.company_id = #{companyId})")
    long countPendingByCompany(@Param("companyId") Long companyId,
                               @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM internship_incident WHERE student_id = #{studentId} AND status IN (0, 1)")
    long countPendingByStudent(@Param("studentId") Long studentId);
}
