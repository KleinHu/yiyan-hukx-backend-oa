package com.cac.oa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/** Gives servlet-based streaming responses a five-minute idle window. */
@Configuration
@SuppressWarnings("deprecation")
public class AiStreamingWebMvcConfiguration extends WebMvcConfigurerAdapter {
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(300_000L);
    }
}
