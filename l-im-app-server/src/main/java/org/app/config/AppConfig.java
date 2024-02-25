package org.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author: Chackylee
 * @description:
 **/
@Data
@Component
@ConfigurationProperties(prefix = "appconfig")
public class AppConfig {

    private String imUrl;

    private String imVersion;

    private Integer appId;

    private String adminId;

    private String privateKey;

    private String jwtKey;

    private Integer jwtExpireTime;

}
