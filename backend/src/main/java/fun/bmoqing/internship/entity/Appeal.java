package fun.bmoqing.internship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("internship_appeal")
public class Appeal {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private Long assignmentId;
    private String targetType;
    private Long targetId;
    private String reason;
    private String evidenceUrl;
    private Integer status; // 0:待教师初审 1:待管理员复议 2:复议通过 3:复议驳回 4:已关闭

    private Long teacherId;
    private String teacherReply;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime teacherReviewTime;

    private Long adminId;
    private String adminReply;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime adminReviewTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closeTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String studentName;

    @TableField(exist = false)
    private String studentNo;

    @TableField(exist = false)
    private String teacherName;

    @TableField(exist = false)
    private String adminName;

    @TableField(exist = false)
    private String companyName;

    @TableField(exist = false)
    private String positionTitle;
}
