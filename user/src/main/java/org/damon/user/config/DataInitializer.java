package org.damon.user.config;

import org.damon.user.domain.UserAccount;
import org.damon.user.mapper.UserAccountMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserAccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserAccountMapper accountMapper,
                           PasswordEncoder passwordEncoder) {
        this.accountMapper = accountMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (accountMapper.selectByUsername("admin") != null) {
            return;
        }

        UserAccount admin = new UserAccount();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("123456"));
        admin.setNickname("System Administrator");
        admin.setAuthorities("ROLE_USER,ROLE_ADMIN");
        admin.setStatus(1);
        accountMapper.insert(admin);
        log.info("Initialized demo user admin / 123456");
    }
}
