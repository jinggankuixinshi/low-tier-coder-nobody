package com.example.evaluation.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret = "ACJfAWP2rUC7fyw8A5afuczHv0gyENE9sTpBbMo1mDB1E00bkjX17PusJpzc+DjVtnZoMy/M449TjaoSU4kddg==";

    private long expiration = 7200000L;

    private String header = "Authorization";

    private String prefix = "Bearer ";
}
