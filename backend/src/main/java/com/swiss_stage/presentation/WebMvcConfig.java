package com.swiss_stage.presentation;

import com.swiss_stage.presentation.auth.CurrentUserArgumentResolver;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    public WebMvcConfig(CurrentUserArgumentResolver currentUserArgumentResolver) {
        this.currentUserArgumentResolver = currentUserArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }

    /** ルート直アクセスもSPAを返す(welcome page相当。add-mappings無効化に伴い明示) */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }

    /**
     * SPA配信(04_react_router_patterns.md §5、14_performance_optimization.md §4)。
     * フロントエンドのビルド成果物は classpath:/static/ に配置される(11_cicd_design.md)。
     *
     * <ul>
     *   <li>/assets/**(Viteのハッシュ付きファイル)… 1年 + immutable</li>
     *   <li>その他 … no-cache。存在しないパスは index.html へフォールバックし、
     *       /tournaments/xxx 等への直アクセスでもSPAが起動する</li>
     *   <li>/api/** はフォールバックしない(未知のAPIパスは404のまま)</li>
     * </ul>
     *
     * <p>デフォルトの静的リソースマッピングは application.yml
     * (spring.web.resources.add-mappings=false)で無効化している。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache())
                .resourceChain(true)
                .addResolver(new SpaFallbackResolver());
    }

    private static final class SpaFallbackResolver extends PathResourceResolver {
        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource requested = location.createRelative(resourcePath);
            if (requested.isReadable()) {
                return requested;
            }
            if (resourcePath.startsWith("api/")) {
                return null;
            }
            Resource index = location.createRelative("index.html");
            return index.isReadable() ? index : null;
        }
    }
}
