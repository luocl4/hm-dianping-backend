package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        Long userId = UserHolder.getUser().getId();
//        1.判断到底是关注还是取关
        if (isFollow) {
//            2.关注,新增数据; 用户userId 关注了 用户followUserId
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            save(follow);
        } else {
//            3.取关,删除 delete from tb_follow where userId = ? and follow_user_id = ?
//            用到mybatis-plus的 https://baomidou.com/guides/data-interface/#remove
            remove(new QueryWrapper<Follow>().
                    eq("user_id", userId).eq("follow_user_id", followUserId));
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserid) {
//        1.查询是否关注 select * from tb_follow where user_id=? and follow_user_id=?
        Integer count = query().eq("user_id", UserHolder.getUser().getId()).eq("follow_user_id", followUserid).count();
//        2.关注了就会大于0
        return Result.ok(count > 0);
    }
}
