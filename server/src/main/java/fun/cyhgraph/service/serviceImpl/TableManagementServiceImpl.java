// TableManagementServiceImpl.java
package fun.cyhgraph.service.serviceImpl;

import fun.cyhgraph.entity.TableInfo;
import fun.cyhgraph.mapper.UtableInfoMapper;
import fun.cyhgraph.service.TableManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class TableManagementServiceImpl implements TableManagementService {

    @Autowired
    private UtableInfoMapper utableInfoMapper;

    @Override
    @Transactional
    public boolean occupyTable(Long tableId, Integer orderId) {
        try {
            // 检查餐桌是否存在且空闲
            TableInfo table = utableInfoMapper.selectById(tableId);
            if (table == null || table.getStatus() != 1) {
                log.error("餐桌不可用，tableId: {}, status: {}", tableId,
                        table != null ? table.getStatus() : "null");
                return false;
            }

            // 更新餐桌状态为占用中
            utableInfoMapper.updateStatus(tableId, 2);

            log.info("餐桌占用成功，tableId: {}, orderId: {}", tableId, orderId);
            return true;
        } catch (Exception e) {
            log.error("占用餐桌失败", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public boolean releaseTable(Long tableId) {
        try {
            TableInfo table = utableInfoMapper.selectById(tableId);
            if (table == null) {
                log.error("餐桌不存在，tableId: {}", tableId);
                return false;
            }

            // 更新餐桌状态为空闲
            utableInfoMapper.updateStatus(tableId, 1);

            log.info("餐桌释放成功，tableId: {}", tableId);
            return true;
        } catch (Exception e) {
            log.error("释放餐桌失败", e);
            throw e;
        }
    }

    @Override
    public List<TableInfo> getAvailableTables() {
        return utableInfoMapper.selectAvailableTables();
    }

    @Override
    public boolean isTableAvailable(Long tableId) {
        if (tableId == null) return false;
        TableInfo table = utableInfoMapper.selectById(tableId);
        return table != null && table.getStatus() == 1;
    }
}