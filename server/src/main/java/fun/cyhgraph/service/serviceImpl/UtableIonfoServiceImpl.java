package fun.cyhgraph.service.serviceImpl;

import fun.cyhgraph.entity.TableInfo;
import fun.cyhgraph.mapper.UtableInfoMapper;
import fun.cyhgraph.result.Result;
import fun.cyhgraph.service.UtableIonfoService;
import fun.cyhgraph.vo.TableInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
@Service
@Slf4j
public class UtableIonfoServiceImpl  implements UtableIonfoService {
    @Autowired
    private UtableInfoMapper utableInfoMapper;
    @Override
    public Result<List<TableInfoVO>> getAvailableTables() {
        try {
            // 查询状态为1（空闲）的餐桌
            List<TableInfo> tables = utableInfoMapper.selectAvailableTables();

            List<TableInfoVO> tableVOs = tables.stream()
                    .map(table -> {
                        TableInfoVO vo = new TableInfoVO();
                        BeanUtils.copyProperties(table, vo);
                        // 可以设置状态描述
                        vo.setStatusDesc("空闲");
                        return vo;
                    })
                    .collect(Collectors.toList());

            return Result.success(tableVOs);
        } catch (Exception e) {
            log.error("获取空闲餐桌失败", e);
            return Result.error("获取餐桌信息失败");
        }
    }

    @Override
    public boolean isTableAvailable(Long tableId) {
        if (tableId == null) {
            return false;
        }
        TableInfo table = utableInfoMapper.selectById(tableId);
        return table != null && table.getStatus() == 1; // 状态为1表示空闲
    }

    @Override
    public TableInfoVO getById(Long tableId) {
        return null;
    }


}
