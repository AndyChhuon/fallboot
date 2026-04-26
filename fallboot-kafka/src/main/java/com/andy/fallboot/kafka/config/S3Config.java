package com.andy.fallboot.kafka.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class S3Config {

    @Bean
    public S3Client s3Client(@Value("${aws.region}") String region,
                             @Value("${aws.s3.endpoint-override:}") String endpointOverride) {
        var builder = S3Client.builder().region(Region.of(region));
        if (endpointOverride != null && !endpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(endpointOverride))
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }
        return builder.build();
    }
}
