package fun.cyhgraph.service.serviceImpl;

import fun.cyhgraph.dto.TableInfoDTO;
import fun.cyhgraph.entity.TableInfo;
import fun.cyhgraph.mapper.TableInfoMapper;
import fun.cyhgraph.service.EmployeeService;
import fun.cyhgraph.service.TableInfoService;
import fun.cyhgraph.vo.TableInfoVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TableInfoServiceImpl implements TableInfoService {

    @Autowired
    private TableInfoMapper tableInfoMapper;

    @Autowired
    private EmployeeService employeeService;

    @Override
    @Transactional
    public void saveTable(TableInfoDTO tableInfoDTO) {
        // 检查餐桌编号是否已存在
        TableInfo existing = tableInfoMapper.selectByTableNumber(tableInfoDTO.getTableNumber());
        if (existing != null) {
            throw new RuntimeException("餐桌编号已存在");
        }

        TableInfo tableInfo = new TableInfo();
        BeanUtils.copyProperties(tableInfoDTO, tableInfo);

        // 设置创建和更新信息
        Long currentUserId = getCurrentUserId();
        tableInfo.setCreateUser(currentUserId);
        tableInfo.setUpdateUser(currentUserId);
        tableInfo.setCreateTime(LocalDateTime.now());
        tableInfo.setUpdateTime(LocalDateTime.now());

        tableInfoMapper.insert(tableInfo);
    }

    @Override
    @Transactional
    public void updateTable(TableInfoDTO tableInfoDTO) {
        TableInfo tableInfo = tableInfoMapper.selectById(tableInfoDTO.getId());
        if (tableInfo == null) {
            throw new RuntimeException("餐桌不存在");
        }

        // 检查餐桌编号是否重复（排除自身）
        TableInfo duplicate = tableInfoMapper.selectByTableNumberExcludeId(
                tableInfoDTO.getTableNumber(), tableInfoDTO.getId());
        if (duplicate != null) {
            throw new RuntimeException("餐桌编号已存在");
        }

        BeanUtils.copyProperties(tableInfoDTO, tableInfo);

        // 设置更新信息
        Long currentUserId = getCurrentUserId();
        tableInfo.setUpdateUser(currentUserId);
        tableInfo.setUpdateTime(LocalDateTime.now());

        tableInfoMapper.update(tableInfo);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        TableInfo tableInfo = tableInfoMapper.selectById(id);
        if (tableInfo == null) {
            throw new RuntimeException("餐桌不存在");
        }

        // 检查餐桌是否正在使用中
        int usingCount = tableInfoMapper.countUsingOrders(id);
        if (usingCount > 0) {
            throw new RuntimeException("餐桌正在使用中，无法删除");
        }

        if (tableInfo.getStatus() == 2) { // 占用中
            throw new RuntimeException("餐桌正在使用中，无法删除");
        }

        tableInfoMapper.deleteById(id);
    }

    @Override
    public TableInfoVO getById(Long id) {
        TableInfo tableInfo = tableInfoMapper.selectById(id);
        if (tableInfo == null) {
            return null;
        }

        return convertToVO(tableInfo);
    }

    @Override
    public List<TableInfoVO> listAll() {
        List<TableInfo> tableInfos = tableInfoMapper.selectAll();
        return tableInfos.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TableInfoVO> listByCondition(String tableNumber, Integer status) {
        List<TableInfo> tableInfos = tableInfoMapper.selectByCondition(tableNumber, status);
        return tableInfos.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    private TableInfoVO convertToVO(TableInfo tableInfo) {
        TableInfoVO vo = new TableInfoVO();
        BeanUtils.copyProperties(tableInfo, vo);

        // 设置状态描述
        vo.setStatusDesc(getStatusDesc(tableInfo.getStatus()));

        return vo;
    }

    private String getStatusDesc(Integer status) {
        if (status == null) return "未知";

        switch (status) {
            case 1: return "空闲";
            case 2: return "占用中";
            case 3: return "已预订";
            case 4: return "停用";
            default: return "未知";
        }
    }

    private Long getCurrentUserId() {
        // TODO: 从Token或Session中获取当前登录用户ID
        // 这里先返回一个模拟的用户ID
        return 1L;
    }
}