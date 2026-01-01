package fun.cyhgraph.dto;

import lombok.Data;

/**
 * 餐桌信息传输对象
 */
@Data
public class TableInfoDTO {

    private Long id;


    private String tableNumber;

    private String tableName;


    private Integer capacity;


    private Integer status;

    private Integer sort;
}