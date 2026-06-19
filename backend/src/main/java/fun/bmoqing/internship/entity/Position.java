package fun.bmoqing.internship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("internship_position")
public class Position {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long companyId;
    private Long ownerId;

    private String companyName; // 对应数据库 company_name
    private String title;       // 岗位名称
    private String description; // 描述
    private String location;    // 地点

    private Integer status;     // 1:招聘中 0:已截止
    private Integer auditStatus; // 0:待审核 1:已通过 2:已驳回
    private String auditRemark;  // 审核意见

    // 自动格式化时间返回给前端
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
