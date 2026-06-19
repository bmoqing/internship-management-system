package fun.bmoqing.internship.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import fun.bmoqing.internship.entity.Assignment;
import fun.bmoqing.internship.entity.Attendance;
import fun.bmoqing.internship.entity.Incident;
import fun.bmoqing.internship.entity.Log;
import fun.bmoqing.internship.mapper.AssignmentMapper;
import fun.bmoqing.internship.mapper.AttendanceMapper;
import fun.bmoqing.internship.mapper.IncidentMapper;
import fun.bmoqing.internship.mapper.LogMapper;
import fun.bmoqing.internship.mapper.SysConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class WarningTask {

    @Autowired
    private AssignmentMapper assignmentMapper;

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private LogMapper logMapper;

    @Autowired
    private IncidentMapper incidentMapper;

    @Autowired
    private SysConfigMapper sysConfigMapper;

    // 每天晚上 23:30 执行考勤检查
    @Scheduled(cron = "0 30 23 * * ?")
    public void checkAttendanceWarnings() {
        System.out.println("开始执行每日考勤预警检查...");
        
        // 查找所有正在进行中的实习分配
        List<Assignment> activeAssignments = assignmentMapper.selectList(
                new QueryWrapper<Assignment>().eq("status", 1)
        );

        int threshold = getWarningDaysThreshold();
        LocalDate today = LocalDate.now();

        for (Assignment assignment : activeAssignments) {
            Long studentId = assignment.getStudentId();
            
            // 检查今天是否打卡
            QueryWrapper<Attendance> todayQuery = new QueryWrapper<>();
            todayQuery.eq("student_id", studentId)
                      .ge("sign_in_time", today.atStartOfDay())
                      .lt("sign_in_time", today.plusDays(1).atStartOfDay());
            boolean todayPresent = attendanceMapper.selectCount(todayQuery) > 0;

            if (!todayPresent) {
                // 如果今天没打卡，检查过去连续 threshold 天是否都没打卡
                boolean consecutiveAbsent = true;
                for (int i = 0; i < threshold; i++) {
                    LocalDate checkDate = today.minusDays(i);
                    QueryWrapper<Attendance> q = new QueryWrapper<>();
                    q.eq("student_id", studentId)
                     .ge("sign_in_time", checkDate.atStartOfDay())
                     .lt("sign_in_time", checkDate.plusDays(1).atStartOfDay());
                    if (attendanceMapper.selectCount(q) > 0) {
                        consecutiveAbsent = false;
                        break;
                    }
                }

                if (consecutiveAbsent) {
                    // 生成预警工单 (Incident)
                    generateWarningIncident(
                            studentId, 
                            assignment.getId(), 
                            "系统自动预警：连续 " + threshold + " 天未打卡", 
                            "该学生已连续 " + threshold + " 天未在系统内完成签到打卡，可能出现脱岗或其他异常状况，请尽快核实处理。"
                    );
                }
            }
        }
        System.out.println("每日考勤预警检查结束。");
    }

    // 每周日晚上 23:45 执行日志检查
    @Scheduled(cron = "0 45 23 ? * SUN")
    public void checkLogWarnings() {
        System.out.println("开始执行每周日志预警检查...");
        List<Assignment> activeAssignments = assignmentMapper.selectList(
                new QueryWrapper<Assignment>().eq("status", 1)
        );

        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(6); // 周一

        for (Assignment assignment : activeAssignments) {
            Long studentId = assignment.getStudentId();
            
            QueryWrapper<Log> logQuery = new QueryWrapper<>();
            logQuery.eq("student_id", studentId)
                    .ge("submit_time", startOfWeek.atStartOfDay())
                    .lt("submit_time", today.plusDays(1).atStartOfDay());
            
            long count = logMapper.selectCount(logQuery);
            if (count == 0) {
                generateWarningIncident(
                        studentId, 
                        assignment.getId(), 
                        "系统自动预警：本周未提交任何实习日志", 
                        "该学生本周（" + startOfWeek + " 至 " + today + "）未提交任何日报/周报，请指导教师及时督促。"
                );
            }
        }
        System.out.println("每周日志预警检查结束。");
    }

    private int getWarningDaysThreshold() {
        try {
            String val = sysConfigMapper.getValueByKey("warning.absent.days");
            if (val != null) {
                return Integer.parseInt(val);
            }
        } catch (Exception e) {
            // ignore
        }
        return 3;
    }

    private void generateWarningIncident(Long studentId, Long assignmentId, String title, String content) {
        // 防止当天重复生成相同类型的工单
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        QueryWrapper<Incident> checkQuery = new QueryWrapper<>();
        checkQuery.eq("student_id", studentId)
                  .eq("title", title)
                  .ge("report_time", todayStart);
        
        if (incidentMapper.selectCount(checkQuery) == 0) {
            Incident incident = new Incident();
            incident.setStudentId(studentId);
            incident.setAssignmentId(assignmentId);
            incident.setTitle(title);
            incident.setContent(content);
            incident.setStatus(0); // 待处理
            incident.setReportTime(LocalDateTime.now());
            // Reporter ID is null or 0 means system
            incident.setReporterId(1L); // Assuming 1L is Admin or system user, to avoid null foreign key issues if any. Wait, the reporter_id refers to user id. Usually we can leave it null or map to an admin.
            // In Incident Entity, reporterId might not allow null if we enforce it, but let's see. 
            // Better to fetch an admin ID or just set it to student ID. Setting to studentId makes them the reporter, which is fine, but content says "system auto warning".
            incident.setReporterId(studentId); 
            incidentMapper.insert(incident);
        }
    }
}
