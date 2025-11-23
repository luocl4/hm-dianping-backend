package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 秒杀订单消息DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeckillOrderMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    // 订单ID
    private Long orderId;
    // 用户ID
    private Long userId;
    // 优惠券ID
    private Long voucherId;
}