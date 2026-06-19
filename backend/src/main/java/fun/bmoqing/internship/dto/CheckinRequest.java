/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

package fun.bmoqing.internship.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CheckinRequest {
    private BigDecimal latitude;
    private BigDecimal longitude;
}
