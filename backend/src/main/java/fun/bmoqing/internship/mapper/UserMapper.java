/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

package fun.bmoqing.internship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Select("SELECT CASE role " +
            "WHEN 'ADMIN' THEN '管理员' " +
            "WHEN 'TEACHER' THEN '教师' " +
            "WHEN 'STUDENT' THEN '学生' " +
            "WHEN 'COMPANY' THEN '企业' " +
            "ELSE role END AS name, COUNT(*) AS value " +
            "FROM sys_user GROUP BY role")
    List<Map<String, Object>> selectCountByRole();

    @Select("SELECT id, username, name, role, company_id AS companyId, teacher_id AS teacherId, create_time AS createTime " +
            "FROM sys_user WHERE role = #{role} ORDER BY id DESC")
    List<User> selectByRole(@Param("role") String role);

    @Select("SELECT id, username, name, role, company_id AS companyId, teacher_id AS teacherId, create_time AS createTime " +
            "FROM sys_user WHERE role = 'COMPANY' AND company_id = #{companyId} ORDER BY id DESC")
    List<User> selectCompanyUsersByCompanyId(@Param("companyId") Long companyId);

    @Select("SELECT COUNT(*) FROM sys_user WHERE role = 'STUDENT' AND teacher_id = #{teacherId}")
    long countStudentsByTeacherId(@Param("teacherId") Long teacherId);

    @Select("SELECT u.id FROM sys_user u " +
            "LEFT JOIN (SELECT mentor_id, COUNT(*) AS cnt FROM internship_assignment WHERE status = 1 AND mentor_id IS NOT NULL GROUP BY mentor_id) ia " +
            "ON u.id = ia.mentor_id " +
            "WHERE u.role = 'COMPANY' AND u.company_id = #{companyId} " +
            "ORDER BY COALESCE(ia.cnt, 0) ASC, u.id ASC LIMIT 1")
    Long selectLeastLoadedMentorByCompanyId(@Param("companyId") Long companyId);

    @Select("SELECT s.id, s.username, s.name, s.role, s.company_id AS companyId, s.teacher_id AS teacherId, s.create_time AS createTime, " +
            "t.name AS teacherName, p.title AS positionTitle, ia.status AS assignmentStatus, p.company_name AS companyName, ia.assign_time AS assignTime " +
            "FROM sys_user s " +
            "LEFT JOIN sys_user t ON s.teacher_id = t.id " +
            "LEFT JOIN internship_assignment ia ON ia.student_id = s.id AND ia.status = 1 " +
            "LEFT JOIN internship_position p ON ia.position_id = p.id " +
            "WHERE s.role = 'STUDENT' AND s.teacher_id = #{teacherId} " +
            "AND (s.username LIKE CONCAT('%', #{keyword}, '%') OR s.name LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY CASE WHEN ia.id IS NULL THEN 1 ELSE 0 END, ia.assign_time DESC, s.id DESC")
    Page<User> selectManagedStudents(Page<User> page,
                                     @Param("teacherId") Long teacherId,
                                     @Param("keyword") String keyword);
}
