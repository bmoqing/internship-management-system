/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

package fun.bmoqing.internship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.entity.Attendance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AttendanceMapper extends BaseMapper<Attendance> {

    @Select("SELECT a.*, u.name AS studentName " +
            "FROM internship_attendance a " +
            "LEFT JOIN sys_user u ON a.student_id = u.id " +
            "WHERE (u.name LIKE CONCAT('%', #{keyword}, '%') OR u.username LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY a.checkin_time DESC")
    Page<Attendance> selectWithStudent(Page<Attendance> page, @Param("keyword") String keyword);

    @Select("SELECT a.*, u.name AS studentName " +
            "FROM internship_attendance a " +
            "LEFT JOIN sys_user u ON a.student_id = u.id " +
            "INNER JOIN internship_assignment ia ON ia.student_id = a.student_id AND ia.status = 1 " +
            "WHERE ia.teacher_id = #{teacherId} " +
            "AND (u.name LIKE CONCAT('%', #{keyword}, '%') OR u.username LIKE CONCAT('%', #{keyword}, '%')) " +
            "GROUP BY a.id " +
            "ORDER BY a.checkin_time DESC")
    Page<Attendance> selectWithStudentByTeacher(Page<Attendance> page,
                                                @Param("keyword") String keyword,
                                                @Param("teacherId") Long teacherId);

    @Select("SELECT DATE_FORMAT(a.checkin_time, '%Y-%m-%d') AS name, COUNT(*) AS value " +
            "FROM internship_attendance a " +
            "WHERE a.checkin_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
            "GROUP BY DATE_FORMAT(a.checkin_time, '%Y-%m-%d') " +
            "ORDER BY name ASC")
    List<Map<String, Object>> selectCheckinTrendLast7Days();
}
