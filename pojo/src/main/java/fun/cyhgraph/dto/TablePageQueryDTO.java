package fun.cyhgraph.dto;

import lombok.Data;

@Data
public class TablePageQueryDTO {
    private Integer page = 1;
    private Integer pageSize = 10;
    private String number;
}