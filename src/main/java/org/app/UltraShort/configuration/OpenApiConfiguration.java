package org.app.UltraShort.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

@OpenAPIDefinition(
        info = @Info(
                version = "1.0",
                description = "UltraShort API",
                termsOfService = "Terms Of Service",
                license = @License(
                        name = "Henry"
                ),
                contact = @Contact(
                        url = "henry.vercel.app",
                        name = "Fakorode Henry",
                        email = "fakorodehenry@gmail.com"
                ),
                title = "UltraShort"
        )
)
public class OpenApiConfiguration {
}
