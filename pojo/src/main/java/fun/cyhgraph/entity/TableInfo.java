package fun.cyhgraph.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 餐桌信息实体类
 */
@Data
public class TableInfo {

    private Long id;

    /**
     * 餐桌编号
     */
    private String tableNumber;

    /**
     * 餐桌名称
     */
    private String tableName;

    /**
     * 可容纳人数
     */
    private Integer capacity;

    /**
     * 状态：1-空闲，2-占用中，3-已预订，4-停用
     */
    private Integer status;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 创建人
     */
    private Long createUser;

    /**
     * 更新人
     */
    private Long updateUser;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}