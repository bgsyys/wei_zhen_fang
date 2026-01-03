package fun.cyhgraph.mapper;

import fun.cyhgraph.entity.TableInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UtableInfoMapper {
    /**
     * 查询所有空闲餐桌（status=1）
     */
    @Select("SELECT * FROM table_info WHERE status = 1 AND status != 4 ORDER BY sort, table_number")
    List<TableInfo> selectAvailableTables();

    /**
     * 根据ID查询餐桌状态
     */
    @Select("SELECT status FROM table_info WHERE id = #{id}")
    Integer selectStatusById(Long id);

    @Select("SELECT * FROM table_info WHERE id = #{id}")
    TableInfo selectById(Long id);

    @Update("UPDATE table_info SET status = #{status}, update_time = NOW() WHERE id = #{tableId}")
    void updateStatus(@Param("tableId") Long tableId, @Param("status") Integer status);
}
