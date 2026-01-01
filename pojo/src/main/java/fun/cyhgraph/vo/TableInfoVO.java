package fun.cyhgraph.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 餐桌信息返回视图对象
 */
@Data
public class TableInfoVO {

    private Long id;
    private String tableNumber;
    private String tableName;
    private Integer capacity;
    private Integer status;
    private String statusDesc;
    private Integer sort;
    private String createUser;
    private String updateUser;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}