/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

package fun.bmoqing.internship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("internship_assignment")
public class Assignment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applicationId;
    private Long studentId;
    private Long positionId;
    private Long teacherId;
    private Long mentorId;
    private Long companyId;
    private Integer status; // 1:进行中 0:已结束
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime assignTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String studentName;

    @TableField(exist = false)
    private String studentNo;

    @TableField(exist = false)
    private String positionTitle;

    @TableField(exist = false)
    private String companyName;

    @TableField(exist = false)
    private String teacherName;

    @TableField(exist = false)
    private String mentorName;
}
