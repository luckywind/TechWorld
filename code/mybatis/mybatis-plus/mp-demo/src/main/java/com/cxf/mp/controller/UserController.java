package com.cxf.mp.controller;

import com.cxf.mp.domain.User;
import com.cxf.mp.service.UserService;
import com.cxf.mp.util.Result;
import com.cxf.mp.util.ResultUtil;
import org.springframework.web.bind.annotation.*;
import io.swagger.annotations.ApiOperation;

import javax.annotation.Resource;

/**
 * (User)表控制层
 *
 * @author chengxingfu
 * @since 2020-08-21 08:44:53
 */
@RestController
@RequestMapping("user")
public class UserController {
    /**
     * 服务对象
     */
    @Resource
    private UserService userService;

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @RequestMapping(value = "/{id}",method = RequestMethod.GET)
    @ApiOperation(value = "/findById",notes = "明细")
    public User findById(int id) {
        return this.userService.getById(id);
    }

   @RequestMapping(value = "/insert",method = RequestMethod.POST)
   @ApiOperation(value = "/insert",notes = "新增")
   public Result<Boolean> insert(@RequestBody  User user){
    boolean res = userService.save(user);
    return ResultUtil.success(res);
  }


   @RequestMapping(value = "/delete",method = RequestMethod.DELETE)
   @ApiOperation(value = "/delete",notes = "删除")
   public Result<Boolean> delete(int id){
    boolean res = userService.removeById(id);
    return ResultUtil.success(res);
  }


   @RequestMapping(value = "/update",method = RequestMethod.PUT)
   @ApiOperation(value = "/update",notes = "修改")
   public Result<Boolean> update(@RequestBody  User user){
    boolean update = userService.updateById(user);
    return ResultUtil.success(update);
  }

}
