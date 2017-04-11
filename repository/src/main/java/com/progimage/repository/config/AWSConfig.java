package com.progimage.repository.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.transfer.TransferManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;

@Configuration
public class AWSConfig {

    @Value("${aws.credentials.accessKey}")
    private String accessKey;

    @Value("${aws.credentials.secretKey}")
    private String secretKey;

    @Value("${aws.region}")
    private String region;

    @Bean
    public AWSCredentials awsCredentials() {
        return new BasicAWSCredentials(accessKey, secretKey);
    }

    @Bean
    public ClientConfiguration clientConfiguration() {
        return new ClientConfiguration().
                withProtocol(Protocol.HTTP).
                withMaxConnections(50).
                withConnectionTimeout(5000).
                withConnectionMaxIdleMillis(5000);
    }

    @Bean
    public AmazonS3 amazonS3() {
        return new AmazonS3Client(awsCredentials(), clientConfiguration()).
                withRegion(RegionUtils.getRegion(region));
    }

    @Bean
    public TransferManager transferManager() {
        return new TransferManager(amazonS3());
    }

    @Bean(destroyMethod = "shutdown")
    ExecutorService dynamoDBAsyncExecutorService() {
        return java.util.concurrent.Executors
                .newFixedThreadPool(clientConfiguration()
                        .getMaxConnections());
    }

    @Bean
    public AmazonDynamoDBAsync amazonDynamoDBAsync() {
        return new AmazonDynamoDBAsyncClient(awsCredentials(),
                clientConfiguration(), dynamoDBAsyncExecutorService()).
                withRegion(RegionUtils.getRegion(region));
    }

    @Bean
    public DynamoDBMapper dynamoDBMapper() {
        return new DynamoDBMapper(amazonDynamoDBAsync());
    }
}
