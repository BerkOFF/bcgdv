package com.progimage.repository.service.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.progimage.repository.model.ImageMetadata;
import com.progimage.repository.service.CatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class DynamoDBCatalogService implements CatalogService {

    @Autowired
    private DynamoDBMapper dynamoDBMapper;

    @Value("${dynamodb.table.name}")
    private String tableName;

    @Override
    public String create(ImageMetadata imageMetadata) {
        imageMetadata.setCreatedAt(Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
        imageMetadata.setUpdatedAt(Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
        dynamoDBMapper.save(imageMetadata, new DynamoDBMapperConfig(new DynamoDBMapperConfig.TableNameOverride(tableName)));
        return imageMetadata.getId();
    }

    @Override
    public void update(ImageMetadata imageMetadata) {
        imageMetadata.setUpdatedAt(Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
        dynamoDBMapper.save(imageMetadata, new DynamoDBMapperConfig(new DynamoDBMapperConfig.TableNameOverride(tableName)));
    }

    @Override
    public ImageMetadata read(String id) {
        return dynamoDBMapper.load(ImageMetadata.class, id, new DynamoDBMapperConfig(new DynamoDBMapperConfig.TableNameOverride(tableName)));
    }

    @Override
    public void delete(ImageMetadata imageMetadata) {
        dynamoDBMapper.delete(imageMetadata, new DynamoDBMapperConfig(new DynamoDBMapperConfig.TableNameOverride(tableName)));
    }
}
