package fun.cyhgraph.mapper;

import fun.cyhgraph.entity.TableInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TableInfoMapper {

    /**
     * 新增餐桌
     */
    @Insert("INSERT INTO table_info (table_number, table_name, capacity, status, sort, create_user, update_user, create_time, update_time) " +
            "VALUES (#{tableNumber}, #{tableName}, #{capacity}, #{status}, #{sort}, #{createUser}, #{updateUser}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(TableInfo tableInfo);

    /**
     * 更新餐桌
     */
    @Update("UPDATE table_info SET " +
            "table_number = #{tableNumber}, " +
            "table_name = #{tableName}, " +
            "capacity = #{capacity}, " +
            "status = #{status}, " +
            "sort = #{sort}, " +
            "update_user = #{updateUser}, " +
            "update_time = #{updateTime} " +
            "WHERE id = #{id}")
    void update(TableInfo tableInfo);

    /**
     * 根据ID删除餐桌
     */
    @Delete("DELETE FROM table_info WHERE id = #{id}")
    void deleteById(Long id);

    /**
     * 根据ID查询餐桌
     */
    @Select("SELECT * FROM table_info WHERE id = #{id}")
    TableInfo selectById(Long id);

    /**
     * 查询所有餐桌
     */
    @Select("SELECT * FROM table_info ORDER BY sort ASC")
    List<TableInfo> selectAll();

    /**
     * 根据餐桌编号查询（用于校验重复）
     */
    @Select("SELECT * FROM table_info WHERE table_number = #{tableNumber}")
    TableInfo selectByTableNumber(String tableNumber);

    /**
     * 根据条件查询
     */
    List<TableInfo> selectByCondition(@Param("tableNumber") String tableNumber,
                                      @Param("status") Integer status);

    /**
     * 根据编号查询（排除指定ID）
     */
    @Select("SELECT * FROM table_info WHERE table_number = #{tableNumber} AND id != #{id}")
    TableInfo selectByTableNumberExcludeId(@Param("tableNumber") String tableNumber,
                                           @Param("id") Long id);

    /**
     * 检查餐桌是否正在使用中
     */
    @Select("SELECT COUNT(*) FROM orders WHERE table_id = #{tableId} AND status IN (1,2,3,4)")
    int countUsingOrders(Long tableId);
}