package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {


    @Autowired
    IFollowService followService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private IUserService userService;

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户以及当前用户是否点赞
        records.forEach(blog->{
            isBlogLiked(blog);
            fillBlogUser(blog);
        });
        return Result.ok(records);
    }

    @Override
    public Result queryById(Long id) {
        //1.获取博客对象
        Blog blog = getById(id);
        if(blog==null){
            return Result.fail("不存在对应id的博客");
        }
        //2.获取博客作者 id 并获取用户的icon以及name
        fillBlogUser(blog);
        isBlogLiked(blog);
        return Result.ok(blog);

    }


    @Override
    public Result likeBlog(Long id) {
        UserDTO user = UserHolder.getUser();
        if(user==null){
            return Result.fail("请先登录！");
        }
        Long userId = user.getId();
        Blog blog= getById(id);
        if(blog==null){
            return Result.fail("不存在对应id博客");
        }
        isBlogLiked(blog);
        String key=BLOG_LIKES_KEY+id;
        Boolean isLiked = blog.getIsLike();
        if (BooleanUtil.isFalse(isLiked)){
            //如果没有点赞就
            boolean isSuccess =  update().setSql("liked = liked + 1").eq("id",id).update();
            if(isSuccess){
                stringRedisTemplate.opsForZSet().add(key,userId.toString(),System.currentTimeMillis());
            }
        }
        else {
            boolean isSuccess =  update().setSql("liked = liked - 1").eq("id",id).update();
            if(isSuccess){
                stringRedisTemplate.opsForZSet().remove(key,userId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {
        //1.获取 redis 中的列表
        String key=BLOG_LIKES_KEY+id;
        Set<String> range = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        //点赞列表为空或不存在就直接返回
        if(range==null || range.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = range.stream().map(Long::valueOf).collect(Collectors.toList());
        String idStr = StrUtil.join(",", ids);
        List<UserDTO> userDTOS = userService.query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list().stream().map(
                user -> BeanUtil.copyProperties(user, UserDTO.class)
        ).collect(Collectors.toList());
        return Result.ok(userDTOS);

    }

    @Override
    public Result saveBlog(Blog blog) {
        save(blog);
        UserDTO user = UserHolder.getUser();
        if(user==null){
            return Result.fail("请先登录");
        }
        Long id = user.getId();
        List<Follow> followUserIds = followService.query().eq("follow_user_id", id).list();
        for(Follow follow : followUserIds){
            String fanKey=FEED_KEY+follow.getUserId();
            stringRedisTemplate.opsForZSet().add(fanKey,blog.getId().toString(),System.currentTimeMillis());
        }

        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow(Long lastId, Integer offset) {
        //目标：根据传入的参数来按照顺序获取 redis 中的 blogId，然后根据 blogId 查询然后返回封装好的 ScrollResult 类数据
        //1.获取当前用户 id
        UserDTO user = UserHolder.getUser();
        if(user==null){
            return Result.fail("请先登录");
        }
        Long userId = user.getId();
        //2.从 redis 中查询用户的收件箱
        String key = FEED_KEY+userId;
        //3.获取元组集合
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet().
                rangeByScoreWithScores(key, 0, lastId, offset, BLOG_NUM_PER_PAGE);
        if(tuples==null|| tuples.isEmpty()){
            return Result.ok();
        }
        List<Long> ids=new ArrayList<>(tuples.size());
        int newOffSet= 1;
        long newLastId = 0;
        for(ZSetOperations.TypedTuple<String> tuple:tuples){
            String blogId = tuple.getValue();
            ids.add(Long.valueOf(blogId));

            long score = tuple.getScore().longValue();
            if( score ==newLastId){
                newOffSet++;
            }
            else{
                newOffSet=1;
                newLastId=score;
            }
        }
        String idStr = StrUtil.join(",", ids);
        List<Blog> blogs = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        //5.封装并返回
        ScrollResult scrollResult= new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setOffset(newOffSet);
        scrollResult.setMinTime(newLastId);
        return Result.ok(scrollResult);
    }


    //检测是否被当前用户点赞过，并记录到返回给前端的 blog 对象中，需要在 hot 接口和 blog/{id} 接口的实现中都接入
    private void isBlogLiked(Blog blog) {
        UserDTO user = UserHolder.getUser();
        if(user==null){
            return;
        }
        Long userId = user.getId();
        //2.获取当前 blogId
        Long blogId = blog.getId();
        String key="blog:like:"+blogId;
        Double isLiked = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(isLiked!=null);
    }

    //将博客作者个人信息填充到 blog 对象中，需要在 blog/{id} 接口 中接入，hot 不需要
    private void fillBlogUser(Blog blog) {
        Long bloggerId = blog.getUserId();
        User blogger = userService.getById(bloggerId);
        blog.setIcon(blogger.getIcon());
        blog.setName(blogger.getNickName());
    }
}
