package ca.simplestep.gateway;

import org.assertj.core.api.Assert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigurationTest {

    @Test
    void name() {
        AntPathMatcher matcher = new AntPathMatcher();
        Assertions.assertThat(matcher.match("/**", "/asdf.json")).isTrue();
        Assertions.assertThat(matcher.match("/**/*.css", "/static/css/2.17e5ed98.chunk.css")).isTrue();
        Assertions.assertThat(matcher.match("/**", "/oauth2/authorization/login-client")).isTrue();
        Assertions.assertThat(matcher.match("/**", "/asdf.json")).isTrue();
    }
}