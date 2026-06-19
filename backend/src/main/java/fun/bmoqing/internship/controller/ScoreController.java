package fun.bmoqing.internship.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.bmoqing.internship.common.AuthUtil;
import fun.bmoqing.internship.common.Result;
import fun.bmoqing.internship.entity.Assignment;
import fun.bmoqing.internship.entity.Attendance;
import fun.bmoqing.internship.entity.Score;
import fun.bmoqing.internship.entity.User;
import fun.bmoqing.internship.mapper.AssignmentMapper;
import fun.bmoqing.internship.mapper.AttendanceMapper;
import fun.bmoqing.internship.mapper.LogMapper;
import fun.bmoqing.internship.mapper.LogMapper;
import fun.bmoqing.internship.mapper.ScoreMapper;
import fun.bmoqing.internship.mapper.SysConfigMapper;
import fun.bmoqing.internship.mapper.UserMapper;
import fun.bmoqing.internship.service.AuditLogService;
import fun.bmoqing.internship.service.InternshipRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/score")
public class ScoreController {

    @Autowired
    private ScoreMapper scoreMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private LogMapper logMapper;

    @Autowired
    private AssignmentMapper assignmentMapper;

    @Autowired
    private SysConfigMapper sysConfigMapper;

    @Autowired
    private InternshipRecordService internshipRecordService;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "10") Integer pageSize,
                          @RequestParam(defaultValue = "") String keyword) {
        if (!AuthUtil.hasRole("TEACHER", "ADMIN", "COMPANY")) {
            return Result.forbidden("仅教师/管理员/企业可查看成绩评定列表");
        }

        Page<Score> page = new Page<>(pageNum, pageSize);
        if (AuthUtil.hasRole("ADMIN")) {
            return Result.success(scoreMapper.selectStudentScores(page, keyword));
        }
        if (AuthUtil.hasRole("COMPANY")) {
            Long companyId = AuthUtil.currentUser().getCompanyId();
            if (companyId == null) {
                return Result.error("企业账号未绑定企业信息");
            }
            return Result.success(scoreMapper.selectStudentScoresByCompany(page, keyword, companyId));
        }
        return Result.success(scoreMapper.selectStudentScoresByTeacher(page, keyword, AuthUtil.currentUserId()));
    }

    @GetMapping("/my")
    public Result<?> myScore() {
        if (!AuthUtil.hasRole("STUDENT")) {
            return Result.forbidden("只有学生可以查看个人成绩");
        }

        QueryWrapper<Score> query = new QueryWrapper<>();
        query.eq("student_id", AuthUtil.currentUserId());
        return Result.success(scoreMapper.selectOne(query));
    }

    @PostMapping("/evaluate")
    public Result<?> evaluate(@RequestBody Score score) {
        if (!AuthUtil.hasRole("TEACHER", "ADMIN")) {
            return Result.forbidden("仅教师/管理员可提交成绩评定");
        }

        if (score.getStudentId() == null) {
            return Result.error("学生ID不能为空");
        }
        if (score.getTeacherScore() == null) {
            return Result.error("教师评定分不能为空");
        }
        if (score.getTeacherScore() < 0 || score.getTeacherScore() > 100) {
            return Result.error("教师评定分需在0-100之间");
        }
        if (score.getTeacherComment() != null && score.getTeacherComment().length() > 255) {
            return Result.error("教师评语不能超过255个字符");
        }

        User student = userMapper.selectById(score.getStudentId());
        if (student == null || !"STUDENT".equalsIgnoreCase(student.getRole())) {
            return Result.error("评定对象必须是学生");
        }

        Assignment activeAssignment = assignmentMapper.selectActiveByStudentId(score.getStudentId());
        if (activeAssignment == null) {
            return Result.validationError("学生当前无进行中实习分配，不能评定成绩");
        }

        if (AuthUtil.hasRole("TEACHER") && !AuthUtil.hasRole("ADMIN")) {
            long relationCount = assignmentMapper.countTeacherStudentRelation(AuthUtil.currentUserId(), score.getStudentId());
            if (relationCount <= 0) {
                return Result.forbidden("仅可评定自己指导学生的成绩");
            }
        }

        double wTeacher = getWeight("score.weight.teacher", 0.6);
        double wCompany = getWeight("score.weight.company", 0.0);
        double wLog = getWeight("score.weight.log", 0.3);
        double wAttendance = getWeight("score.weight.attendance", 0.1);

        QueryWrapper<Score> query = new QueryWrapper<>();
        query.eq("student_id", score.getStudentId());
        Score dbScore = scoreMapper.selectOne(query);

        if (wCompany > 0) {
            if (dbScore == null || dbScore.getCompanyScore() == null) {
                return Result.validationError("根据系统设定的成绩公式，企业评价权重占比 " + (wCompany * 100) + "%。请等待企业先完成打分后再计算总评。");
            }
        }

        double companyScore = (dbScore != null && dbScore.getCompanyScore() != null) ? dbScore.getCompanyScore() : 0.0;
        double teacherScore = round(score.getTeacherScore());
        double attendanceScore = calculateAttendanceScore(score.getStudentId(), activeAssignment.getAssignTime());
        double logScore = calculateLogScore(score.getStudentId(), activeAssignment.getId(), activeAssignment.getAssignTime());
        
        double finalScore = round(teacherScore * wTeacher + companyScore * wCompany + logScore * wLog + attendanceScore * wAttendance);

        boolean isCreate = dbScore == null;
        if (dbScore == null) {
            dbScore = new Score();
            dbScore.setStudentId(score.getStudentId());
        }
// 成绩实体属性装配与持久化
        dbScore.setTeacherScore(teacherScore);
        dbScore.setAttendanceScore(attendanceScore);
        dbScore.setExtraScore(logScore);
        dbScore.setFinalScore(finalScore);
        dbScore.setTeacherComment(score.getTeacherComment());
        dbScore.setUpdateTime(LocalDateTime.now());

        if (dbScore.getId() == null) {
            scoreMapper.insert(dbScore);
        } else {
            scoreMapper.updateById(dbScore);
        }

        internshipRecordService.addRecord(
                score.getStudentId(),
                "SCORE_EVALUATE",
                "完成成绩评定，最终成绩：" + finalScore,
                dbScore.getId()
        );

        auditLogService.record(
                isCreate ? "SCORE_CREATE" : "SCORE_UPDATE",
                "SCORE",
                dbScore.getId(),
                "学生ID=" + score.getStudentId() +
                        "，教师分=" + teacherScore +
                        "，考勤分=" + attendanceScore +
                        "，日志均分=" + logScore +
                        "，最终分=" + finalScore
        );

        return Result.success(dbScore);
    }

    @PostMapping("/admin-update")
    public Result<?> adminUpdate(@RequestBody Score score) {
        if (!AuthUtil.hasRole("ADMIN")) {
            return Result.forbidden("仅管理员可直接修改成绩");
        }

        if (score.getStudentId() == null) {
            return Result.error("学生ID不能为空");
        }

        User student = userMapper.selectById(score.getStudentId());
        if (student == null || !"STUDENT".equalsIgnoreCase(student.getRole())) {
            return Result.error("评定对象必须是学生");
        }

        // 验证各分数在 0-100 范围内
        if (score.getTeacherScore() != null && (score.getTeacherScore() < 0 || score.getTeacherScore() > 100)) {
            return Result.error("教师评定分需在0-100之间");
        }
        if (score.getCompanyScore() != null && (score.getCompanyScore() < 0 || score.getCompanyScore() > 100)) {
            return Result.error("企业评定分需在0-100之间");
        }
        if (score.getAttendanceScore() != null && (score.getAttendanceScore() < 0 || score.getAttendanceScore() > 100)) {
            return Result.error("考勤得分需在0-100之间");
        }
        if (score.getExtraScore() != null && (score.getExtraScore() < 0 || score.getExtraScore() > 100)) {
            return Result.error("日志均分需在0-100之间");
        }
        if (score.getFinalScore() != null && (score.getFinalScore() < 0 || score.getFinalScore() > 100)) {
            return Result.error("最终成绩需在0-100之间");
        }

        QueryWrapper<Score> query = new QueryWrapper<>();
        query.eq("student_id", score.getStudentId());
        Score dbScore = scoreMapper.selectOne(query);

        boolean isCreate = dbScore == null;
        if (dbScore == null) {
            dbScore = new Score();
            dbScore.setStudentId(score.getStudentId());
        }

        if (score.getTeacherScore() != null) dbScore.setTeacherScore(round(score.getTeacherScore()));
        if (score.getCompanyScore() != null) dbScore.setCompanyScore(round(score.getCompanyScore()));
        if (score.getAttendanceScore() != null) dbScore.setAttendanceScore(round(score.getAttendanceScore()));
        if (score.getExtraScore() != null) dbScore.setExtraScore(round(score.getExtraScore()));
        if (score.getFinalScore() != null) dbScore.setFinalScore(round(score.getFinalScore()));
        if (score.getTeacherComment() != null) dbScore.setTeacherComment(score.getTeacherComment());
        if (score.getCompanyComment() != null) dbScore.setCompanyComment(score.getCompanyComment());
        dbScore.setUpdateTime(LocalDateTime.now());

        if (dbScore.getId() == null) {
            scoreMapper.insert(dbScore);
        } else {
            scoreMapper.updateById(dbScore);
        }

        auditLogService.record(
                isCreate ? "ADMIN_SCORE_CREATE" : "ADMIN_SCORE_UPDATE",
                "SCORE",
                dbScore.getId(),
                "管理员直接修改成绩，学生ID=" + score.getStudentId() +
                        "，教师分=" + dbScore.getTeacherScore() +
                        "，企业分=" + dbScore.getCompanyScore() +
                        "，考勤分=" + dbScore.getAttendanceScore() +
                        "，日志均分=" + dbScore.getExtraScore() +
                        "，最终分=" + dbScore.getFinalScore()
        );

        return Result.success(dbScore);
    }

    @PostMapping("/company-evaluate")
    public Result<?> companyEvaluate(@RequestBody Score score) {
        if (!AuthUtil.hasRole("COMPANY")) {
            return Result.forbidden("仅企业可提交企业评价分数");
        }
        if (score.getStudentId() == null) {
            return Result.error("学生ID不能为空");
        }
        if (score.getCompanyScore() == null) {
            return Result.error("企业评定分不能为空");
        }
        if (score.getCompanyScore() < 0 || score.getCompanyScore() > 100) {
            return Result.error("企业评定分需在0-100之间");
        }

        Assignment activeAssignment = assignmentMapper.selectActiveByStudentId(score.getStudentId());
        Long myCompanyId = AuthUtil.currentUser().getCompanyId();
        if (activeAssignment == null || myCompanyId == null || !myCompanyId.equals(activeAssignment.getCompanyId())) {
            return Result.forbidden("该学生不属于当前企业或已结束实习，无法打分");
        }

        QueryWrapper<Score> query = new QueryWrapper<>();
        query.eq("student_id", score.getStudentId());
        Score dbScore = scoreMapper.selectOne(query);
        if (dbScore == null) {
            dbScore = new Score();
            dbScore.setStudentId(score.getStudentId());
            dbScore.setCompanyScore(score.getCompanyScore());
            dbScore.setCompanyComment(score.getCompanyComment());
            dbScore.setUpdateTime(LocalDateTime.now());
            scoreMapper.insert(dbScore);
        } else {
            dbScore.setCompanyScore(score.getCompanyScore());
            dbScore.setCompanyComment(score.getCompanyComment());
            dbScore.setUpdateTime(LocalDateTime.now());
            scoreMapper.updateById(dbScore);
        }

        auditLogService.record(
                "COMPANY_EVALUATE",
                "SCORE",
                dbScore.getId(),
                "学生ID=" + score.getStudentId() + "，企业打分=" + score.getCompanyScore()
        );

        return Result.success(null);
    }

    private double calculateAttendanceScore(Long studentId, LocalDateTime assignmentStart) {
        QueryWrapper<Attendance> totalQuery = new QueryWrapper<>();
        totalQuery.eq("student_id", studentId);
        if (assignmentStart != null) {
            totalQuery.ge("checkin_time", assignmentStart);
        }
        long total = attendanceMapper.selectCount(totalQuery);
        if (total <= 0) {
            return 0D;
        }

        QueryWrapper<Attendance> normalQuery = new QueryWrapper<>();
        normalQuery.eq("student_id", studentId);
        if (assignmentStart != null) {
            normalQuery.ge("checkin_time", assignmentStart);
        }
        normalQuery.eq("status", "NORMAL");
        long normal = attendanceMapper.selectCount(normalQuery);

        QueryWrapper<Attendance> lateQuery = new QueryWrapper<>();
        lateQuery.eq("student_id", studentId);
        if (assignmentStart != null) {
            lateQuery.ge("checkin_time", assignmentStart);
        }
        lateQuery.eq("status", "LATE");
        long late = attendanceMapper.selectCount(lateQuery);

        return round((normal * 100.0 + late * 80.0) / total);
    }

    private double calculateLogScore(Long studentId, Long assignmentId, LocalDateTime assignmentStart) {
        Double avg = logMapper.selectAvgScoreForActiveAssignment(studentId, assignmentId, assignmentStart);
        if (avg == null) {
            return 0D;
        }
        return round(avg);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double getWeight(String key, double defaultValue) {
        try {
            String val = sysConfigMapper.getValueByKey(key);
            if (val != null) {
                return Double.parseDouble(val);
            }
        } catch (Exception e) {
            // ignore
        }
        return defaultValue;
    }
}
