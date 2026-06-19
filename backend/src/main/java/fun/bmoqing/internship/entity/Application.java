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
@TableName("internship_application")
public class Application {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private Long positionId;
    private Long reviewTeacherId;

    // 0:待企业预审 1:企业通过待教师审核 2:教师通过待管理员终审 3:驳回 4:管理员通过待分配 5:已分配
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applyTime;

    private String remark; // 审核意见

    // --- 下面是数据库表中不存在的字段，仅用于前端展示 ---

    @TableField(exist = false) // 告诉MyBatis-Plus这不是数据库字段
    private String studentName; // 学生姓名

    @TableField(exist = false)
    private String studentResumeUrl; // 学生个人简历URL

    @TableField(exist = false)
    private String positionTitle; // 岗位名称

    @TableField(exist = false)
    private String companyName; // 公司名称

    @TableField(exist = false)
    private Long companyId;

    @TableField(exist = false)
    private String teacherName;

    @TableField(exist = false)
    private String reviewTeacherName;

    @TableField(exist = false)
    private String mentorName;
}
