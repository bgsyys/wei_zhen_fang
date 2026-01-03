package fun.cyhgraph.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderSubmitDTO implements Serializable {

    private Integer addressId; // 地址簿id
    private int payMethod; // 付款方式
    private String remark; // 备注
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime estimatedDeliveryTime; // 预计送达时间
    private Integer deliveryStatus; // 配送状态  1立即送出  0选择具体时间
    private Integer tablewareNumber; // 餐具数量
    private Integer tablewareStatus; // 餐具数量状态  1按餐量提供  0选择具体数量
    private Integer packAmount; // 打包费
    private BigDecimal amount; // 总金额
    // 新增字段：就餐方式和餐桌信息
    private Integer diningType; // 就餐方式：1-外卖，2-堂食
    private Long tableId; // 餐桌ID（当dining_type=2时使用）
    private String tableNumber; // 餐桌编号
}