/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

package fun.bmoqing.internship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("internship_company")
public class Company {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String contact;
    private String phone;
    private String address;
    private Integer status;

    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer radius;
    
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime workStartTime;
    
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime checkinStartTime;
    
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime checkinEndTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
