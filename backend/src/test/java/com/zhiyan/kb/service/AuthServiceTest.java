package com.zhiyan.kb.service;

import com.zhiyan.kb.entity.SysUser;
import com.zhiyan.kb.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuthServiceTest {
    @Test
    void upgradesLegacySha256PasswordToBcrypt() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        AuthService service = new AuthService(userMapper, encoder);
        SysUser user = new SysUser();
        user.setId(7L);
        user.setPassword("8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92");

        assertThat(service.passwordMatches("123456", user.getPassword())).isTrue();
        service.upgradePasswordIfLegacy(user, "123456");

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(7L);
        assertThat(captor.getValue().getPassword()).startsWith("$2");
        assertThat(encoder.matches("123456", captor.getValue().getPassword())).isTrue();
    }
}
