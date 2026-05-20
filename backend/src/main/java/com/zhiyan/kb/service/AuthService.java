package com.zhiyan.kb.service;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyan.kb.common.StatusConstants;
import com.zhiyan.kb.entity.SysUser;
import com.zhiyan.kb.mapper.SysUserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final SysUserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(SysUserMapper userMapper, BCryptPasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public SysUser findEnabledUser(String username) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        return user != null && StatusConstants.ENABLED.equals(user.getStatus()) ? user : null;
    }

    public boolean passwordMatches(String raw, String stored) {
        if (stored == null) {
            return false;
        }
        if (isBcrypt(stored)) {
            return passwordEncoder.matches(raw, stored);
        }
        return SecureUtil.sha256(raw).equalsIgnoreCase(stored);
    }

    public void upgradePasswordIfLegacy(SysUser user, String rawPassword) {
        if (user == null || user.getPassword() == null || isBcrypt(user.getPassword())) {
            return;
        }
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(rawPassword));
        userMapper.updateById(update);
    }

    private boolean isBcrypt(String stored) {
        return stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$");
    }
}
