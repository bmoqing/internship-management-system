/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

package fun.bmoqing.internship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.entity.Log;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface LogMapper extends BaseMapper<Log> {
    // 关联查询：查日志同时查出学生名字
    @Select("SELECT l.*, u.name as studentName " +
            "FROM internship_log l " +
            "LEFT JOIN sys_user u ON l.student_id = u.id " +
            "WHERE (u.name LIKE CONCAT('%', #{keyword}, '%') OR l.title LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY l.create_time DESC")
    Page<Log> selectLogsWithStudent(Page<Log> page, @Param("keyword") String keyword);

    @Select("SELECT l.*, u.name AS studentName " +
            "FROM internship_log l " +
            "LEFT JOIN sys_user u ON l.student_id = u.id " +
            "INNER JOIN internship_assignment ia ON ia.status = 1 AND ((l.assignment_id IS NOT NULL AND ia.id = l.assignment_id) OR (l.assignment_id IS NULL AND ia.student_id = l.student_id)) " +
            "WHERE ia.teacher_id = #{teacherId} " +
            "AND (u.name LIKE CONCAT('%', #{keyword}, '%') OR l.title LIKE CONCAT('%', #{keyword}, '%')) " +
            "GROUP BY l.id " +
            "ORDER BY l.create_time DESC")
    Page<Log> selectLogsWithStudentByTeacher(Page<Log> page,
                                             @Param("keyword") String keyword,
                                             @Param("teacherId") Long teacherId);

    @Select("SELECT AVG(score) FROM internship_log WHERE student_id = #{studentId} AND score IS NOT NULL")
    Double selectAvgScoreByStudentId(@Param("studentId") Long studentId);

    @Select("SELECT AVG(score) FROM internship_log " +
            "WHERE student_id = #{studentId} AND score IS NOT NULL " +
            "AND ((assignment_id IS NOT NULL AND assignment_id = #{assignmentId}) " +
            "OR (assignment_id IS NULL AND (#{assignmentStart} IS NULL OR create_time >= #{assignmentStart})))")
    Double selectAvgScoreForActiveAssignment(@Param("studentId") Long studentId,
                                             @Param("assignmentId") Long assignmentId,
                                             @Param("assignmentStart") LocalDateTime assignmentStart);

    @Select("SELECT COUNT(*) FROM internship_log WHERE student_id = #{studentId} AND score IS NULL")
    long countPendingByStudent(@Param("studentId") Long studentId);

    @Select("SELECT COUNT(*) FROM internship_log l " +
            "INNER JOIN internship_assignment ia ON ia.status = 1 AND ((l.assignment_id IS NOT NULL AND ia.id = l.assignment_id) OR (l.assignment_id IS NULL AND ia.student_id = l.student_id)) " +
            "WHERE ia.teacher_id = #{teacherId} AND l.score IS NULL")
    long countPendingByTeacher(@Param("teacherId") Long teacherId);

    @Select("SELECT DATE_FORMAT(l.create_time, '%Y-%m-%d') AS name, COUNT(*) AS value " +
            "FROM internship_log l " +
            "WHERE l.create_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
            "GROUP BY DATE_FORMAT(l.create_time, '%Y-%m-%d') " +
            "ORDER BY name ASC")
    List<Map<String, Object>> selectLogTrendLast7Days();
}
