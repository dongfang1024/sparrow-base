package com.cnsa.sparrow.swagger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

@Import(Swagger2Configuration.class)
@EnableConfigurationProperties(SwaggerProperties.class)
public class SwaggerAutoConfiguration implements BeanFactoryAware {

    private static Logger log = LoggerFactory.getLogger(SwaggerAutoConfiguration.class);

    private static final String AUTH_KEY = "token";

    @Resource
    SwaggerProperties swaggerProperties;

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "sparrow.swagger.enabled", havingValue = "true", matchIfMissing = true)
    public List<Docket> createRestApi() {
        ConfigurableBeanFactory configurableBeanFactory = (ConfigurableBeanFactory) beanFactory;
        List<Docket> docketList = new LinkedList<>();
        // 没有分组
        if (swaggerProperties.getDocketMap()== null || swaggerProperties.getDocketMap().isEmpty()) {
            Docket docket = createDocket(swaggerProperties);
            configurableBeanFactory.registerSingleton(swaggerProperties.getTitle(), docket);
            docketList.add(docket);
            return docketList;
        }
        // 分组创建
        for (String groupName : swaggerProperties.getDocketMap().keySet()) {
            SwaggerProperties.DocketInfo docketInfo = swaggerProperties.getDocketMap().get(groupName);
            ApiInfo apiInfo = new ApiInfoBuilder()
                    .title(docketInfo.getTitle().isEmpty() ? swaggerProperties.getTitle() : docketInfo.getTitle())
                    .description(docketInfo.getDescription().isEmpty() ? swaggerProperties.getDescription() : docketInfo.getDescription())
                    .version(docketInfo.getVersion().isEmpty() ? swaggerProperties.getVersion() : docketInfo.getVersion())
                    .license(docketInfo.getLicense().isEmpty() ? swaggerProperties.getLicense() : docketInfo.getLicense())
                    .licenseUrl(docketInfo.getLicenseUrl().isEmpty() ? swaggerProperties.getLicenseUrl() : docketInfo.getLicenseUrl())
                    .contact(
                            new Contact(
                                    docketInfo.getContact().getName().isEmpty() ? swaggerProperties.getContact().getName() : docketInfo.getContact().getName(),
                                    docketInfo.getContact().getUrl().isEmpty() ? swaggerProperties.getContact().getUrl() : docketInfo.getContact().getUrl(),
                                    docketInfo.getContact().getEmail().isEmpty() ? swaggerProperties.getContact().getEmail() : docketInfo.getContact().getEmail()
                            )
                    )
                    .termsOfServiceUrl(docketInfo.getTermsOfServiceUrl().isEmpty() ? swaggerProperties.getTermsOfServiceUrl() : docketInfo.getTermsOfServiceUrl())
                    .build();
            // base-path处理
            // 当没有配置任何path的时候，解析/**
            if (docketInfo.getBasePath().isEmpty()) {
                docketInfo.getBasePath().add("/**");
            }
            List<Predicate<String>> basePath = new ArrayList<>(docketInfo.getBasePath().size());
            for (String path : docketInfo.getBasePath()) {
                basePath.add(PathSelectors.ant(path));
            }
            // exclude-path处理
            List<Predicate<String>> excludePath = new ArrayList<>(docketInfo.getExcludePath().size());
            for (String path : docketInfo.getExcludePath()) {
                excludePath.add(PathSelectors.ant(path));
            }
            Docket docket = new Docket(DocumentationType.SWAGGER_2)
                    .host(swaggerProperties.getHost())
                    .apiInfo(apiInfo)
                    .groupName(docketInfo.getGroup())
                    .select()
                    .apis(RequestHandlerSelectors.basePackage(docketInfo.getBasePackage()))
                    .paths(PathSelectors.any())
                    .build()
                    .securityContexts(securityContexts());
            configurableBeanFactory.registerSingleton(groupName, docket);
            docketList.add(docket);
        }
        return docketList;
    }

    /**
     * 创建 Docket对象
     *
     * @param swaggerProperties swagger配置
     * @return Docket
     */
    private Docket createDocket(SwaggerProperties swaggerProperties) {
        //API 基础信息
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title(swaggerProperties.getTitle())
                .description(swaggerProperties.getDescription())
                .version(swaggerProperties.getVersion())
                .license(swaggerProperties.getLicense())
                .licenseUrl(swaggerProperties.getLicenseUrl())
                .contact(swaggerProperties.getContact())
                .termsOfServiceUrl(swaggerProperties.getTermsOfServiceUrl())
                .build();
        // base-path处理
        // 当没有配置任何path的时候，解析/**
        log.info("basePath 是否为null: {}", swaggerProperties.getBasePath() == null);
        if (swaggerProperties.getBasePath().isEmpty()) {
            swaggerProperties.getBasePath().add("/**");
        }
        List<Predicate<String>> basePath = new ArrayList<>();
        for (String path : swaggerProperties.getBasePath()) {
            basePath.add(PathSelectors.ant(path));
        }
        // exclude-path处理
        List<Predicate<String>> excludePath = new ArrayList<>();
        log.info("excludePath 是否为null：{}", swaggerProperties.getExcludePath() == null);
        for (String path : swaggerProperties.getExcludePath()) {
            excludePath.add(PathSelectors.ant(path));
        }
        log.info("return docket");
        return new Docket(DocumentationType.SWAGGER_2)
                .host(swaggerProperties.getHost())
                .apiInfo(apiInfo)
                .groupName(swaggerProperties.getGroup())
                .select()
                .apis(RequestHandlerSelectors.basePackage(swaggerProperties.getBasePackage()))
                .paths(PathSelectors.any())
                .build()
                .securityContexts(securityContexts());
    }

    private List<Response> getResponse() {
        List<Response> collect = Arrays.asList(
                new Response("0", "成功",false, null, null,null, null),
                new Response("-1", "系统繁忙",false, null, null,null, null),
                new Response("40001", "服务超时",false, null, null,null, null),
                new Response("40003", "会话超时，请重新登录",false, null, null,null, null),
                new Response("0", "缺少token参数",false, null, null,null, null)
        );
        return collect;
    }

    private List<SecurityContext> securityContexts() {
        List<SecurityContext> contexts = new ArrayList<>(1);
        SecurityContext securityContext = SecurityContext.builder()
                .securityReferences(defaultAuth())
                .build();
        contexts.add(securityContext);
        return contexts;
    }

    private List<SecurityReference> defaultAuth() {
        return Arrays.asList(new SecurityReference(AUTH_KEY, new AuthorizationScope[]{new AuthorizationScope("global", "accessEverything")}));
    }

}
