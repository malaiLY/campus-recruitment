package com.campus.recruitment.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.recruitment.common.context.LoginUserContext;
import com.campus.recruitment.common.enums.UserStatus;
import com.campus.recruitment.common.enums.UserType;
import com.campus.recruitment.common.exception.BizException;
import com.campus.recruitment.common.exception.ErrorCode;
import com.campus.recruitment.common.constant.RedisConstants;
import com.campus.recruitment.entity.LoginLog;
import com.campus.recruitment.entity.SysMenu;
import com.campus.recruitment.entity.SysRole;
import com.campus.recruitment.entity.SysRoleMenu;
import com.campus.recruitment.entity.SysUser;
import com.campus.recruitment.entity.SysUserRole;
import com.campus.recruitment.mapper.LoginLogMapper;
import com.campus.recruitment.mapper.SysMenuMapper;
import com.campus.recruitment.mapper.SysRoleMapper;
import com.campus.recruitment.mapper.SysRoleMenuMapper;
import com.campus.recruitment.mapper.SysUserMapper;
import com.campus.recruitment.mapper.SysUserRoleMapper;
import com.campus.recruitment.module.auth.dto.LoginRequest;
import com.campus.recruitment.module.auth.dto.RegisterRequest;
import com.campus.recruitment.module.auth.service.AuthService;
import com.campus.recruitment.module.auth.vo.LoginVO;
import com.campus.recruitment.module.auth.vo.RegisterVO;
import com.campus.recruitment.module.auth.vo.TokenRefreshVO;
import com.campus.recruitment.module.auth.vo.UserInfoVO;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final LoginLogMapper loginLogMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.ttl-hours}")
    private int jwtTtlHours;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterVO register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "两次密码输入不一致");
        }

        validateUserType(request.getUserType());

        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, request.getUsername());
        if (sysUserMapper.selectCount(queryWrapper) > 0) {
            throw new BizException(ErrorCode.USER_EXIST);
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setUserType(request.getUserType());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setStatus(UserStatus.NORMAL.name());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.insert(user);

        String roleCode = resolveRoleCode(request.getUserType());
        SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode));
        if (role == null) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "系统角色未初始化: " + roleCode);
        }

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRole.setCreateTime(LocalDateTime.now());
        sysUserRoleMapper.insert(userRole);

        initPermissionsToRedis(user.getId());

        RegisterVO vo = new RegisterVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setUserType(user.getUserType());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginRequest request, String ip, String userAgent) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));
        if (user == null) {
            saveLoginLog(null, request.getUsername(), request.getUserType(), ip, userAgent, "FAIL", "用户名或密码错误");
            throw new BizException(ErrorCode.PASSWORD_ERROR);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            saveLoginLog(user.getId(), user.getUsername(), user.getUserType(), ip, userAgent, "FAIL", "用户名或密码错误");
            throw new BizException(ErrorCode.PASSWORD_ERROR);
        }

        if (UserStatus.DISABLED.name().equals(user.getStatus())) {
            saveLoginLog(user.getId(), user.getUsername(), user.getUserType(), ip, userAgent, "FAIL", "账号被禁用");
            throw new BizException(ErrorCode.USER_DISABLED);
        }

        if (!request.getUserType().equals(user.getUserType())) {
            saveLoginLog(user.getId(), user.getUsername(), user.getUserType(), ip, userAgent, "FAIL", "用户类型不匹配");
            throw new BizException(ErrorCode.USER_TYPE_MISMATCH);
        }

        user.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(user);

        String token = generateJwtToken(user.getId(), user.getUsername(), user.getUserType());
        LocalDateTime expireTime = LocalDateTime.now().plusHours(jwtTtlHours);

        String tokenKey = RedisConstants.LOGIN_TOKEN_PREFIX + token;
        stringRedisTemplate.opsForValue().set(tokenKey, user.getId().toString(), RedisConstants.LOGIN_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);

        String userInfoKey = "campus:login:user:" + user.getId();
        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("username", user.getUsername());
        userInfo.put("userType", user.getUserType());
        stringRedisTemplate.opsForHash().putAll(userInfoKey, userInfo);
        stringRedisTemplate.expire(userInfoKey, RedisConstants.LOGIN_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);

        initPermissionsToRedis(user.getId());

        saveLoginLog(user.getId(), user.getUsername(), user.getUserType(), ip, userAgent, "SUCCESS", null);

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setExpireTime(expireTime);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setUserType(user.getUserType());
        vo.setPermissions(getPermissionList(user.getId()));
        return vo;
    }

    @Override
    public void logout() {
        Long userId = LoginUserContext.getUserId();
        if (userId == null) {
            return;
        }

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String token = extractTokenFromRequest(request);
        if (token != null) {
            String tokenKey = RedisConstants.LOGIN_TOKEN_PREFIX + token;
            stringRedisTemplate.delete(tokenKey);
        }

        String userInfoKey = "campus:login:user:" + userId;
        stringRedisTemplate.delete(userInfoKey);

        String permissionKey = RedisConstants.USER_PERMISSION_PREFIX + userId;
        stringRedisTemplate.delete(permissionKey);
    }

    @Override
    public UserInfoVO getCurrentUser() {
        Long userId = LoginUserContext.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        List<String> roleCodes = getRoleCodeList(userId);
        List<String> permissions = getPermissionList(userId);

        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setUserType(user.getUserType());
        vo.setRoles(roleCodes);
        vo.setPermissions(permissions);
        return vo;
    }

    @Override
    public List<String> getPermissions() {
        Long userId = LoginUserContext.getUserId();
        if (userId == null) {
            return Collections.emptyList();
        }
        return getPermissionList(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenRefreshVO refreshToken(String oldToken) {
        String tokenKey = RedisConstants.LOGIN_TOKEN_PREFIX + oldToken;
        String userIdStr = stringRedisTemplate.opsForValue().get(tokenKey);
        if (userIdStr == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Token 已过期，请重新登录");
        }

        Long userId = Long.parseLong(userIdStr);
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }

        if (UserStatus.DISABLED.name().equals(user.getStatus())) {
            throw new BizException(ErrorCode.USER_DISABLED);
        }

        stringRedisTemplate.delete(tokenKey);

        String userInfoKey = "campus:login:user:" + userId;
        stringRedisTemplate.delete(userInfoKey);

        String newToken = generateJwtToken(user.getId(), user.getUsername(), user.getUserType());
        LocalDateTime expireTime = LocalDateTime.now().plusHours(jwtTtlHours);

        String newTokenKey = RedisConstants.LOGIN_TOKEN_PREFIX + newToken;
        stringRedisTemplate.opsForValue().set(newTokenKey, user.getId().toString(), RedisConstants.LOGIN_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);

        String newUserInfoKey = "campus:login:user:" + user.getId();
        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("username", user.getUsername());
        userInfo.put("userType", user.getUserType());
        stringRedisTemplate.opsForHash().putAll(newUserInfoKey, userInfo);
        stringRedisTemplate.expire(newUserInfoKey, RedisConstants.LOGIN_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);

        TokenRefreshVO vo = new TokenRefreshVO();
        vo.setToken(newToken);
        vo.setExpireTime(expireTime);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setUserType(user.getUserType());
        return vo;
    }

    private String generateJwtToken(Long userId, String username, String userType) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtTtlHours * 3600 * 1000L);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("userType", userType)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    private void initPermissionsToRedis(Long userId) {
        List<String> permissions = getPermissionList(userId);
        String permissionKey = RedisConstants.USER_PERMISSION_PREFIX + userId;
        stringRedisTemplate.delete(permissionKey);
        if (!permissions.isEmpty()) {
            stringRedisTemplate.opsForSet().add(permissionKey, permissions.toArray(new String[0]));
        }
    }

    private List<String> getPermissionList(Long userId) {
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());

        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds));
        if (roleMenus.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> menuIds = roleMenus.stream().map(SysRoleMenu::getMenuId).distinct().collect(Collectors.toList());

        List<SysMenu> menus = sysMenuMapper.selectBatchIds(menuIds);
        return menus.stream()
                .map(SysMenu::getPermission)
                .filter(Objects::nonNull)
                .filter(p -> !p.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> getRoleCodeList(Long userId) {
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
        return roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList());
    }

    private void validateUserType(String userType) {
        try {
            UserType type = UserType.valueOf(userType);
            if (type == UserType.ADMIN) {
                throw new BizException(ErrorCode.PARAM_ERROR, "不允许注册管理员账号");
            }
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "无效的用户类型");
        }
    }

    private String resolveRoleCode(String userType) {
        switch (userType) {
            case "STUDENT":
                return "ROLE_STUDENT";
            case "COMPANY":
                return "ROLE_COMPANY";
            case "ADMIN":
                return "ROLE_ADMIN";
            default:
                throw new BizException(ErrorCode.PARAM_ERROR, "无效的用户类型");
        }
    }

    private void saveLoginLog(Long userId, String username, String userType, String ip, String userAgent, String status, String failReason) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setUserType(userType);
        log.setLoginIp(ip);
        log.setUserAgent(userAgent);
        log.setStatus(status);
        log.setFailReason(failReason);
        log.setLoginTime(LocalDateTime.now());
        loginLogMapper.insert(log);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
