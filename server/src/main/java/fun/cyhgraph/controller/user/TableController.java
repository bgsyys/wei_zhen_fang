package fun.cyhgraph.controller.user;

import fun.cyhgraph.result.Result;
import fun.cyhgraph.service.UtableIonfoService;
import fun.cyhgraph.vo.TableInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user/table")
@Slf4j
public class TableController {
    @Autowired
    private UtableIonfoService utableInfoService;

    /**
     * 获取所有空闲餐桌
     */
    @GetMapping("/available")
    public Result<List<TableInfoVO>> getAvailableTables() {
        return utableInfoService.getAvailableTables();
    }

    /**
     * 检查餐桌是否可用
     */
    @GetMapping("/check/{tableId}")
    public Result<Boolean> checkTableAvailability(@PathVariable Long tableId) {
        boolean isAvailable = utableInfoService.isTableAvailable(tableId);
        return Result.success(isAvailable);
    }

    /**
     * 根据ID获取餐桌信息（只返回空闲的）
     */
    @GetMapping("/info/{tableId}")
    public Result<TableInfoVO> getTableInfo(@PathVariable Long tableId) {
        TableInfoVO tableInfo = utableInfoService.getById(tableId);
        if (tableInfo == null) {
            return Result.error("餐桌不存在");
        }

        // 只有空闲餐桌才返回给用户
        if (tableInfo.getStatus() != 1) {
            return Result.error("餐桌当前不可用");
        }

        return Result.success(tableInfo);
    }

}