package fun.bmoqing.internship.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CheckinRequest {
    private BigDecimal latitude;
    private BigDecimal longitude;
}
