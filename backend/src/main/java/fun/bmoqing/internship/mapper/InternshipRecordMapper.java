package fun.bmoqing.internship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.entity.InternshipRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InternshipRecordMapper extends BaseMapper<InternshipRecord> {

    @Select("SELECT r.*, s.name AS studentName, s.username AS studentNo, o.name AS operatorName " +
            "FROM internship_record r " +
            "LEFT JOIN sys_user s ON r.student_id = s.id " +
            "LEFT JOIN sys_user o ON r.operator_id = o.id " +
            "WHERE r.student_id = #{studentId} " +
            "ORDER BY r.create_time DESC, r.id DESC")
    Page<InternshipRecord> selectPageForStudent(Page<InternshipRecord> page, @Param("studentId") Long studentId);

    @Select("SELECT r.*, s.name AS studentName, s.username AS studentNo, o.name AS operatorName " +
            "FROM internship_record r " +
            "LEFT JOIN sys_user s ON r.student_id = s.id " +
            "LEFT JOIN sys_user o ON r.operator_id = o.id " +
            "WHERE (s.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR s.username LIKE CONCAT('%', #{keyword}, '%') " +
            "OR r.event_type LIKE CONCAT('%', #{keyword}, '%') " +
            "OR r.event_detail LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY r.create_time DESC, r.id DESC")
    Page<InternshipRecord> selectPageForAdmin(Page<InternshipRecord> page, @Param("keyword") String keyword);

    @Select("SELECT r.*, s.name AS studentName, s.username AS studentNo, o.name AS operatorName " +
            "FROM internship_record r " +
            "LEFT JOIN sys_user s ON r.student_id = s.id " +
            "LEFT JOIN sys_user o ON r.operator_id = o.id " +
            "INNER JOIN internship_assignment ia ON ia.student_id = r.student_id AND ia.status = 1 " +
            "WHERE ia.teacher_id = #{teacherId} " +
            "AND (s.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR s.username LIKE CONCAT('%', #{keyword}, '%') " +
            "OR r.event_type LIKE CONCAT('%', #{keyword}, '%') " +
            "OR r.event_detail LIKE CONCAT('%', #{keyword}, '%')) " +
            "GROUP BY r.id " +
            "ORDER BY r.create_time DESC, r.id DESC")
    Page<InternshipRecord> selectPageForTeacher(Page<InternshipRecord> page,
                                                @Param("keyword") String keyword,
                                                @Param("teacherId") Long teacherId);

    @Select("SELECT r.*, s.name AS studentName, s.username AS studentNo, o.name AS operatorName " +
            "FROM internship_record r " +
            "LEFT JOIN sys_user s ON r.student_id = s.id " +
            "LEFT JOIN sys_user o ON r.operator_id = o.id " +
            "INNER JOIN internship_assignment ia ON ia.student_id = r.student_id AND ia.status = 1 " +
            "WHERE ia.company_id = #{companyId} " +
            "AND (s.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR s.username LIKE CONCAT('%', #{keyword}, '%') " +
            "OR r.event_type LIKE CONCAT('%', #{keyword}, '%') " +
            "OR r.event_detail LIKE CONCAT('%', #{keyword}, '%')) " +
            "GROUP BY r.id " +
            "ORDER BY r.create_time DESC, r.id DESC")
    Page<InternshipRecord> selectPageForCompany(Page<InternshipRecord> page,
                                                @Param("keyword") String keyword,
                                                @Param("companyId") Long companyId);
}
