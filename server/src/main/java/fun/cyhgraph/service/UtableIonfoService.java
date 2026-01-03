package fun.cyhgraph.service;
import fun.cyhgraph.result.Result;
import fun.cyhgraph.vo.TableInfoVO;

import java.util.List;
public interface UtableIonfoService {
    /**
     * 获取所有空闲餐桌（status=1）
     *
     * @return 空闲餐桌列表
     */
    Result<List<TableInfoVO>> getAvailableTables();

    /**
     * 检查餐桌是否可用
     *
     * @param tableId 餐桌ID
     * @return 是否可用
     */
    boolean isTableAvailable(Long tableId);

    TableInfoVO getById(Long tableId);
}