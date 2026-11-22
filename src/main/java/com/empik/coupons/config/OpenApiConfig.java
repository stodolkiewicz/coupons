package com.empik.coupons.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
		info = @Info(
				title = "Coupons API",
				version = "v1",
				description = "REST service for creating and redeeming discount coupons."
		)
)
public class OpenApiConfig {
}
