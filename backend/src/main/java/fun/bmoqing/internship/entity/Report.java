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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("internship_report")
public class Report {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assignmentId;
    private Long studentId;
    private String title;
    private String content;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer status; // 1:已提交 2:已批阅
    private Integer score;
    private String teacherComment;
    private Long reviewerId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submitTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String studentName;

    @TableField(exist = false)
    private String studentNo;

    @TableField(exist = false)
    private String reviewerName;
}
