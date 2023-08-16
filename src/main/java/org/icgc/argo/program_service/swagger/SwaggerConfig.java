package org.icgc.argo.program_service.swagger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.web.*;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@Configuration
public class SwaggerConfig {

  private SecurityScheme createAPIKeyScheme() {
    return new SecurityScheme().type(SecurityScheme.Type.HTTP)
            .bearerFormat("JWT")
            .scheme("bearer");
  }

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI().addSecurityItem(new SecurityRequirement().
                    addList("Bearer Authentication"))
            .components(new Components().addSecuritySchemes
                    ("Bearer Authentication", createAPIKeyScheme()))
            .info(new Info().title("Program Service REST API")
                    .description("Program API for the central point to create and manage programs and maintain their metadata.")
                    .version("1.0")
                    .license(new License().name("AGPL-3.0 license")));
  }

  /*private ApiInfo apiInfo() {
    return new ApiInfoBuilder()
        .title("Program Service")
        .description(
            "Public API for the central point to create and manage programs and maintain their metadata.")
        .license("AGPL-3.0 license")
        .version("1.0")
        .build();
  }*/

  @Bean
  public WebMvcEndpointHandlerMapping webEndpointServletHandlerMapping(
      WebEndpointsSupplier webEndpointsSupplier,
      ServletEndpointsSupplier servletEndpointsSupplier,
      ControllerEndpointsSupplier controllerEndpointsSupplier,
      EndpointMediaTypes endpointMediaTypes,
      CorsEndpointProperties corsProperties,
      WebEndpointProperties webEndpointProperties,
      Environment environment) {
    List<ExposableEndpoint<?>> allEndpoints = new ArrayList<>();
    Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
    allEndpoints.addAll(webEndpoints);
    allEndpoints.addAll(servletEndpointsSupplier.getEndpoints());
    allEndpoints.addAll(controllerEndpointsSupplier.getEndpoints());
    String basePath = webEndpointProperties.getBasePath();
    EndpointMapping endpointMapping = new EndpointMapping(basePath);
    boolean shouldRegisterLinksMapping =
        this.shouldRegisterLinksMapping(webEndpointProperties, environment, basePath);
    return new WebMvcEndpointHandlerMapping(
        endpointMapping,
        webEndpoints,
        endpointMediaTypes,
        corsProperties.toCorsConfiguration(),
        new EndpointLinksResolver(allEndpoints, basePath),
        shouldRegisterLinksMapping,
        null);
  }

  private boolean shouldRegisterLinksMapping(
      WebEndpointProperties webEndpointProperties, Environment environment, String basePath) {
    return webEndpointProperties.getDiscovery().isEnabled()
        && (StringUtils.hasText(basePath)
            || ManagementPortType.get(environment).equals(ManagementPortType.DIFFERENT));
  }
}
