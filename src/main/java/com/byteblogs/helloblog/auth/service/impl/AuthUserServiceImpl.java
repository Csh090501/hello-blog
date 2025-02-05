package com.byteblogs.helloblog.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.byteblogs.common.base.domain.Result;
import com.byteblogs.common.base.domain.vo.UserSessionVO;
import com.byteblogs.common.constant.ErrorConstants;
import com.byteblogs.common.util.ExceptionUtil;
import com.byteblogs.common.util.PageUtil;
import com.byteblogs.common.util.SessionUtil;
import com.byteblogs.helloblog.auth.dao.AuthTokenDao;
import com.byteblogs.helloblog.auth.dao.AuthUserDao;
import com.byteblogs.helloblog.auth.domain.po.AuthToken;
import com.byteblogs.helloblog.auth.domain.po.AuthUser;
import com.byteblogs.helloblog.auth.domain.vo.AuthUserVO;
import com.byteblogs.helloblog.auth.service.AuthUserService;
import com.byteblogs.system.enums.RoleEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author byteblogs
 * @since 2019-08-28
 */
@Service
public class AuthUserServiceImpl extends ServiceImpl<AuthUserDao, AuthUser> implements AuthUserService {

    @Autowired
    private AuthUserDao authUserDao;

    @Autowired
    private AuthTokenDao authTokenDao;

    @Override
    public Result getUserInfo(AuthUserVO authUserVO) {
        UserSessionVO userSessionInfo = SessionUtil.getUserSessionInfo();
        AuthUser authUser = this.authUserDao.selectById(userSessionInfo.getId());
        return Result.createWithModel(new AuthUserVO()
                .setRoles(Collections.singletonList(RoleEnum.getEnumTypeMap().get(authUser.getRoleId()).getRoleName()))
                .setName(authUser.getName())
                .setIntroduction(authUser.getIntroduction())
                .setAvatar(authUser.getAvatar())
                .setEmail(authUser.getEmail())
        );
    }

    @Override
    public Result getMasterUserInfo() {
        AuthUser authUser = this.authUserDao.selectOne(new LambdaQueryWrapper<AuthUser>().eq(AuthUser::getRoleId, RoleEnum.ADMIN.getRoleId()));
        AuthUserVO authUserVO = new AuthUserVO();
        if (authUser != null) {
            authUserVO.setName(authUser.getName())
                    .setIntroduction(authUser.getIntroduction())
                    .setHtmlUrl(authUser.getHtmlUrl())
                    .setEmail(authUser.getEmail())
                    .setAvatar(authUser.getAvatar());
        }

        return Result.createWithModel(authUserVO);
    }

    @Override
    public Result getUserList(AuthUserVO authUserVO) {
        Page page = Optional.ofNullable(PageUtil.checkAndInitPage(authUserVO)).orElse(PageUtil.initPage());
        LambdaQueryWrapper<AuthUser> authUserLambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(authUserVO.getKeywords())) {
            authUserLambdaQueryWrapper.and(i -> i.like(AuthUser::getName, authUserVO.getKeywords())
            );
        }

        IPage<AuthUser> authUserIPage = this.authUserDao.selectPage(page, authUserLambdaQueryWrapper.orderByDesc(AuthUser::getRoleId).orderByDesc(AuthUser::getCreateTime));
        List<AuthUser> records = authUserIPage.getRecords();

        List<AuthUserVO> authUserVOList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(records)) {
            records.forEach(authUser -> {
                authUserVOList.add(new AuthUserVO()
                        .setName(authUser.getName())
                        .setRoleId(authUser.getRoleId())
                        .setIntroduction(authUser.getIntroduction())
                );
            });
        }

        return Result.createWithPaging(authUserVOList, PageUtil.initPageInfo(page));
    }

    @Override
    public Result logout() {
        UserSessionVO userSessionInfo = SessionUtil.getUserSessionInfo();
        this.authTokenDao.delete(new LambdaQueryWrapper<AuthToken>().eq(AuthToken::getUserId, userSessionInfo.getId()));
        return Result.createWithSuccessMessage();
    }

    @Override
    public Result updateUser(AuthUserVO authUserVO) {

        if (authUserVO == null) {
            ExceptionUtil.rollback("参数异常", ErrorConstants.PARAM_INCORRECT);
        }

        UserSessionVO userSessionInfo = SessionUtil.getUserSessionInfo();
        this.authUserDao.updateById(new AuthUser()
                .setId(userSessionInfo.getId())
                .setEmail(authUserVO.getEmail())
                .setAvatar(authUserVO.getAvatar())
                .setName(authUserVO.getName())
                .setIntroduction(authUserVO.getIntroduction())
        );

        return Result.createWithSuccessMessage();
    }
}
