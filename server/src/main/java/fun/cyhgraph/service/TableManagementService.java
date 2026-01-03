// TableManagementService.java
package fun.cyhgraph.service;

import fun.cyhgraph.entity.TableInfo;
import java.util.List;

public interface TableManagementService {
    /**
     * 占用餐桌
     */
    boolean occupyTable(Long tableId, Integer orderId);

    /**
     * 释放餐桌（订单完成或取消时）
     */
    boolean releaseTable(Long tableId);

    /**
     * 获取可用餐桌列表
     */
    List<TableInfo> getAvailableTables();

    /**
     * 检查餐桌是否可用
     */
    boolean isTableAvailable(Long tableId);
}