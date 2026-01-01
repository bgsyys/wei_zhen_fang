package fun.cyhgraph.service;

import fun.cyhgraph.dto.TableInfoDTO;
import fun.cyhgraph.entity.TableInfo;
import fun.cyhgraph.vo.TableInfoVO;

import java.util.List;

public interface TableInfoService {

    /**
     * 新增餐桌
     */
    void saveTable(TableInfoDTO tableInfoDTO);

    /**
     * 修改餐桌
     */
    void updateTable(TableInfoDTO tableInfoDTO);

    /**
     * 删除餐桌
     */
    void deleteById(Long id);

    /**
     * 根据ID查询餐桌
     */
    TableInfoVO getById(Long id);

    /**
     * 查询所有餐桌
     */
    List<TableInfoVO> listAll();

    /**
     * 分页查询餐桌
     */
    List<TableInfoVO> listByCondition(String tableNumber, Integer status);
}