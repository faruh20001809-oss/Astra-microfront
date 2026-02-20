package ru.astrakhan.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderItemRequest {

    private Long id;
    private String name;
    private Double price;
    private Integer qty;
    private String category;
    private String currency;

    public Integer getQty() {
        return (qty != null && qty > 0) ? qty : 1;
    }
}