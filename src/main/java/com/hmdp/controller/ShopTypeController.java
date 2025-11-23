package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IShopTypeService;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/shop-type")
@Api(tags = "商铺类型信息")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    /**
     * 店铺类型查询业务
     *
     * @Return Result
     */
    @GetMapping("list")
    public Result queryTypeList() {
        return typeService.queryTypeList();
    }
}
