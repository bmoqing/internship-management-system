/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

package fun.bmoqing.internship.security;

import fun.bmoqing.internship.entity.User;
import fun.bmoqing.internship.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class PasswordMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PasswordMigrationRunner.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public PasswordMigrationRunner(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<User> users = userMapper.selectList(null);
        int migratedCount = 0;

        for (User user : users) {
            String password = user.getPassword();
            if (!StringUtils.hasText(password) || isBcryptHash(password)) {
                continue;
            }

            User update = new User();
            update.setId(user.getId());
            update.setPassword(passwordEncoder.encode(password));
            userMapper.updateById(update);
            migratedCount++;
        }

        if (migratedCount > 0) {
            log.info("Migrated {} legacy plaintext passwords to BCrypt", migratedCount);
        }
    }

    private boolean isBcryptHash(String password) {
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }
}
