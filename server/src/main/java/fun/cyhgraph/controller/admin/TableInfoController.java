package fun.cyhgraph.controller.admin;

import fun.cyhgraph.dto.TableInfoDTO;
import fun.cyhgraph.result.Result;
import fun.cyhgraph.service.TableInfoService;
import fun.cyhgraph.vo.TableInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/table")
@Slf4j
public class TableInfoController {

    @Autowired
    private TableInfoService tableInfoService;

    /**
     * 新增餐桌
     */
    @PostMapping
    public Result<String> save(@Validated @RequestBody TableInfoDTO tableInfoDTO) {
        log.info("新增餐桌：{}", tableInfoDTO);
        try {
            tableInfoService.saveTable(tableInfoDTO);
            return Result.success("新增餐桌成功");
        } catch (Exception e) {
            log.error("新增餐桌失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改餐桌
     */
    @PutMapping
    public Result<String> update(@Validated @RequestBody TableInfoDTO tableInfoDTO) {
        log.info("修改餐桌：{}", tableInfoDTO);
        try {
            tableInfoService.updateTable(tableInfoDTO);
            return Result.success("修改餐桌成功");
        } catch (Exception e) {
            log.error("修改餐桌失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除餐桌
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除餐桌：{}", id);
        try {
            tableInfoService.deleteById(id);
            return Result.success("删除餐桌成功");
        } catch (Exception e) {
            log.error("删除餐桌失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据ID查询餐桌
     */
    @GetMapping("/{id}")
    public Result<TableInfoVO> getById(@PathVariable Long id) {
        log.info("根据ID查询餐桌：{}", id);
        TableInfoVO tableInfoVO = tableInfoService.getById(id);
        return Result.success(tableInfoVO);
    }

    /**
     * 查询所有餐桌
     */
    @GetMapping("/list")
    public Result<List<TableInfoVO>> listAll() {
        log.info("查询所有餐桌");
        List<TableInfoVO> list = tableInfoService.listAll();
        return Result.success(list);
    }

    /**
     * 条件查询餐桌
     */
    @GetMapping("/page")
    public Result<Map<String, Object>> page(
            @RequestParam(required = false) String tableNumber,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("条件查询餐桌，tableNumber：{}，status：{}，page：{}，pageSize：{}",
                tableNumber, status, page, pageSize);

        List<TableInfoVO> list = tableInfoService.listByCondition(tableNumber, status);

        // 创建分页响应格式
        Map<String, Object> result = new HashMap<>();
        result.put("records", list);  // 使用records属性，与分类管理保持一致
        result.put("total", list.size());
        result.put("size", pageSize);
        result.put("current", page);

        return Result.success(result);
    }

    /**
     * 修改餐桌状态
     */
    @PutMapping("/status/{id}")
    public Result<String> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        log.info("修改餐桌状态，id：{}，status：{}", id, status);
        try {
            TableInfoDTO dto = new TableInfoDTO();
            dto.setId(id);
            dto.setStatus(status);
            tableInfoService.updateTable(dto);
            return Result.success("修改状态成功");
        } catch (Exception e) {
            log.error("修改状态失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 批量删除餐桌
     */
    @DeleteMapping("/batch")
    public Result<String> deleteBatch(@RequestBody List<Long> ids) {
        log.info("批量删除餐桌：{}", ids);
        try {
            for (Long id : ids) {
                tableInfoService.deleteById(id);
            }
            return Result.success("批量删除成功");
        } catch (Exception e) {
            log.error("批量删除失败", e);
            return Result.error(e.getMessage());
        }
    }
}