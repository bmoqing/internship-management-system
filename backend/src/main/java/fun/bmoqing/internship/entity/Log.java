package fun.bmoqing.internship.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("internship_log")
public class Log {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assignmentId;
    private Long studentId;
    private String title;
    private String content;
    private String teacherComment; // 老师评语
    private Integer score;         // 分数
    private Integer status;        // 0=正常, 3=打回待修改

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // 辅助字段：前端展示学生名字用
    @TableField(exist = false)
    private String studentName;
}
