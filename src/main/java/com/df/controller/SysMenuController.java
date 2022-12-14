package com.df.controller;


import cn.hutool.core.map.MapUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.df.common.dto.SysMenuDto;
import com.df.common.lang.Result;
import com.df.entity.SysMenu;
import com.df.entity.SysRoleMenu;
import com.df.entity.SysUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
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
@RequestMapping("/sys/menu")
public class SysMenuController extends BaseController {
//    编写nav接口
    @GetMapping("/nav")
    public Result nav (Principal principal) {
       SysUser sysUser = sysUserService.getByUsername(principal.getName());
//       获取权限信息
       String authorityInfo = sysUserService.getUserAuthorityInfo(sysUser.getId());
       String[] authorityInfoArray = StringUtils.tokenizeToStringArray(authorityInfo,",");
//       获取导航栏信息
        List<SysMenuDto> navs = sysMenuService.getCurrentUserNav();
        return Result.succ(MapUtil.builder()
                .put("authoritys",authorityInfoArray)
                .put("nav",navs)
                .map()
        );

    }

    @GetMapping("/info/{id}")
    @PreAuthorize("hasAuthority('sys:menu:list')")
    public Result info(@PathVariable(name = "id") Long id) {
        return Result.succ(sysMenuService.getById(id));
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('sys:menu:list')")
    public Result list() {
        List<SysMenu> menus = sysMenuService.tree();
        return Result.succ(menus);
    }

    @PostMapping("/save")
    @PreAuthorize("hasAuthority('sys:menu:save')")
     public Result save(@Validated @RequestBody SysMenu sysMenu) {
//        sysMenu.setCreated(LocalDateTime.now());
        sysMenuService.save(sysMenu);
        return Result.succ(sysMenu);
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('sys:menu:update')")
     public Result update(@Validated @RequestBody SysMenu sysMenu) {
//        sysMenu.setCreated(LocalDateTime.now());
        sysMenuService.updateById(sysMenu);
//        清除所有与该菜单相关的权限缓存
        sysUserService.clearUserAuthorityInfoByMenuId(sysMenu.getId());
        return Result.succ(sysMenu);
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('sys:menu:delete')")
     public Result delete(@PathVariable("id") Long id) {
        int count = sysMenuService.count(new QueryWrapper<SysMenu>().eq("parent_id",id));
        if(count > 0) {
            return Result.fail("请先删除子菜单");
        }
//      清除所有与该菜单相关的权限缓存
        sysUserService.clearUserAuthorityInfoByMenuId(id);
        sysMenuService.removeById(id);
//       同步删除中间关联表
        sysRoleMenuService.remove(new QueryWrapper<SysRoleMenu>().eq("menu_id",id));
        return Result.succ("");
    }

}
