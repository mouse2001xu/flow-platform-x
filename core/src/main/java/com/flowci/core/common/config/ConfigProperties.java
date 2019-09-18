/*
 * Copyright 2018 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flowci.core.common.config;

import java.net.URI;
import java.nio.file.Path;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

/**
 * @author yang
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "app")
public class ConfigProperties {

    private Path workspace;

    private Path flowDir;

    @NotBlank
    private String serverAddress;

    @NotBlank
    @Length(max = 16, min = 16)
    private String secret;

    @Bean("adminProperties")
    @ConfigurationProperties(prefix = "app.admin")
    public Admin admin() {
        return new Admin();
    }

    @Bean("zkProperties")
    @ConfigurationProperties(prefix = "app.zookeeper")
    public Zookeeper zk() {
        return new Zookeeper();
    }

    @Bean("jobProperties")
    @ConfigurationProperties(prefix = "app.job")
    public Job job() {
        return new Job();
    }

    @Bean("pluginProperties")
    @ConfigurationProperties(prefix = "app.plugin")
    public Plugin plugin() {
        return new Plugin();
    }

    @Bean("rabbitProperties")
    @ConfigurationProperties(prefix = "app.rabbitmq")
    public RabbitMQ rabbitMQ() {
        return new RabbitMQ();
    }

    @Bean("authProperties")
    @ConfigurationProperties(prefix = "app.auth")
    public Auth auth() {
        return new Auth();
    }

    @Data
    @Validated
    public static class Admin {

        @NotBlank
        @Email
        private String defaultEmail;

        @NotBlank
        private String defaultPassword;
    }

    @Data
    public static class Job {

        private Long expireInSeconds;

        private Long retryWaitingSeconds;
    }

    @Data
    public static class Plugin {

        private String defaultRepo;

        private Boolean autoUpdate;
    }


    @Data
    public static class Zookeeper {

        private Boolean embedded;

        private String host;

        private String agentRoot;

        private String cronRoot;

        private Integer timeout;

        private Integer retry;

        private String dataDir;
    }

    @Data
    public static class RabbitMQ {

        private URI uri;

        private String callbackQueueName;

        private String loggingQueueName;
    }

    @Data
    public static class Auth {

        private Boolean enabled;

        // expired for token
        private Integer expireSeconds;

        // expired for refresh token
        private Integer refreshExpiredSeconds;
    }
}