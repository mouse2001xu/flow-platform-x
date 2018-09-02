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

package com.flowci.agent.config;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.domain.Jsonable;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author yang
 */
@Log4j2
@Configuration
public class AgentConfig implements WebMvcConfigurer {

    private final static List<HttpMessageConverter<?>> DefaultConverters = Lists.newArrayList(
        new ByteArrayHttpMessageConverter(),
        new MappingJackson2HttpMessageConverter(Jsonable.getMapper()),
        new ResourceHttpMessageConverter(),
        new AllEncompassingFormHttpMessageConverter()
    );

    @Autowired
    private AgentProperties agentProperties;

    @PostConstruct
    public void initWorkspace() {
        try {
            Path path = Paths.get(agentProperties.getWorkspace());
            Files.createDirectory(path);
        } catch (FileAlreadyExistsException ignore) {

        } catch (IOException e) {
            log.error("Unable to init workspace directory: {}", agentProperties.getWorkspace());
        }
    }

    @Bean("objectMapper")
    public ObjectMapper objectMapper() {
        return Jsonable.getMapper();
    }

    @Bean("restTemplate")
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(DefaultConverters);
        return restTemplate;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("*")
            .allowedMethods(GET.name(), POST.name(), PUT.name(), DELETE.name())
            .allowedHeaders("*");
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.clear();
        converters.addAll(DefaultConverters);
    }
}
