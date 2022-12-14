package com.df.controller;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.df.common.lang.Const;
import com.df.common.lang.Result;
import com.df.entity.SysRole;
import com.df.entity.SysUser;
import com.df.entity.SysUserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author df
 * @since 2022-02-11
 */
@RestController
@RequestMapping("/sys/user")
public class SysUserController extends BaseController {
    @Autowired
    BCryptPasswordEncoder passwordEncoder;

    @PreAuthorize("hasAuthority('sys:user:list')")
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long id) {
        SysUser sysUser = sysUserService.getById(id);
        Assert.notNull(sysUser,"我找不到管理员");
        List<SysRole> roles = sysRoleService.listRolesByUserId(id);
        sysUser.setSysRoles(roles);
        return Result.succ(sysUser);
    }
    @PreAuthorize("hasAuthority('sys:user:list')")
    @GetMapping("/list")
    public Result list(String username) {
        Page<SysUser> pageData = sysUserService
                .page(getPage(),new QueryWrapper<SysUser>()
                        .like(StrUtil.isNotBlank(username),"username",username));
//        获取角色
        pageData.getRecords().forEach(u -> {
            u.setSysRoles(sysRoleService.listRolesByUserId(u.getId()));
        });

        return Result.succ(pageData);
    }
    @PostMapping("/save")
    @PreAuthorize("hasAuthority('sys:user:save')")
    public Result save(@Validated @RequestBody SysUser sysUser) {
//        sysUser.setCreated(LocalDateTime.now());
        sysUser.setStatu(Const.STATUS_ON);
//      默认密码
        String password = passwordEncoder.encode(Const.DEFAULT_PASSWORD);
        sysUser.setPassword(password);
//        默认头像
        sysUser.setAvatar(Const.DEFAULT_AVATAR);
        sysUserService.save(sysUser);
        return Result.succ(sysUser);
    }
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('sys:user:update')")
    public Result update(@Validated @RequestBody SysUser sysUser) {
//        sysUser.setUpdated(LocalDateTime.now());
        sysUserService.updateById(sysUser);
        return Result.succ(sysUser);
    }
    @Transactional
    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('sys:user:delete')")
    public Result delete(@RequestBody Long[] ids) {
        sysUserService.removeByIds(Arrays.asList(ids));
        sysUserRoleService.remove(new QueryWrapper<SysUserRole>().in("user_id",ids));
        return Result.succ("");
    }
//    分配角色
    @Transactional
    @PostMapping("/role/{userId}")
    @PreAuthorize("hasAuthority('sys:user:role')")
    public Result rolePerm(@PathVariable("userId") Long userId, @RequestBody Long[] roleIds) {
        List<SysUserRole> userRoles = new ArrayList<>();
        Arrays.stream(roleIds).forEach(r-> {
            SysUserRole sysUserRole = new SysUserRole();
            sysUserRole.setRoleId(r);
            sysUserRole.setUserId(userId);
            userRoles.add(sysUserRole);
        });
        sysUserRoleService.remove(new QueryWrapper<SysUserRole>().eq("user_id",userId));
        sysUserRoleService.saveBatch(userRoles);
//        删除缓存
        SysUser sysUser = sysUserService.getById(userId);
        sysUserService.clearUserAuthorityInfo(sysUser.getUsername());
        return Result.succ("");
    }

//    重置密码
    @PostMapping("/repass")
    @PreAuthorize("hasAuthority('sys:user:repass')")
    public Result repass(@RequestBody Long userId) {
        SysUser sysUser = sysUserService.getById(userId);
        sysUser.setPassword(passwordEncoder.encode(Const.DEFAULT_PASSWORD));
//        sysUser.setUpdated(LocalDateTime.now());
        sysUserService.updateById(sysUser);
        return Result.succ("");
    }
}
