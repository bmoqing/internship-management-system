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
@TableName("internship_change")
public class InternshipChange {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private Long assignmentId;
    private Integer type; // 1=转岗, 2=换企业, 3=提前离职
    private String reason;
    private Integer status; // 0=待企业审核, 1=待学校审核, 2=已通过, 3=已驳回
    
    private Long companyId;
    private Long teacherId;
    
    private Long targetCompanyId;
    private Long targetPositionId;
    
    private String companyRemark;
    private String teacherRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String studentName;
    
    @TableField(exist = false)
    private String studentNo;
}
