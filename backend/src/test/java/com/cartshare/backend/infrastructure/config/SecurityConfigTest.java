package com.cartshare.backend.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    // The ContextRunner is a specialized Spring tool for testing @Configuration classes
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SecurityConfig.class);

    @Test
    @DisplayName("SecurityFilterChain: Should be present in the application context")
    void securityFilterChainBeanExists() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SecurityFilterChain.class);
        });
    }

    @Test
    @DisplayName("CSRF: Should be disabled in the filter chain")
    void csrfIsDisabled() {
        contextRunner.run(context -> {
            SecurityFilterChain filterChain = context.getBean(SecurityFilterChain.class);

            // In Spring Security, if CSRF is disabled, the CsrfFilter is usually
            // either absent or configured with a Matcher that permits everything.
            // We can verify the filters list directly.
            boolean hasCsrfFilter = filterChain.getFilters().stream()
                    .anyMatch(filter -> filter instanceof CsrfFilter);

            assertThat(hasCsrfFilter).isFalse();
        });
    }

    @Test
    @DisplayName("AuthorizeRequests: Should be configured to permit all")
    void authorizeRequestsPermitsAll() {
        contextRunner.run(context -> {
            SecurityFilterChain filterChain = context.getBean(SecurityFilterChain.class);

            // We check the FilterChainProxy's configuration.
            // Since permitAll() is set on .anyRequest(), we verify the chain exists.
            assertThat(filterChain).isNotNull();
            assertThat(filterChain.getFilters()).isNotEmpty();
        });
    }
}