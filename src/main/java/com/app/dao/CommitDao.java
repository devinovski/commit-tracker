package com.app.dao;

import com.app.model.NotionSync;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommitDao extends MongoRepository<NotionSync, String> {
    List<NotionSync> findBySyncedToNotionFalse();

    List<NotionSync> findBySyncedToNotionTrue();

    @Aggregation(pipeline = {
            "{ '$match': {'syncedToNotion': true}}",
            "{'$count': 'status'}"
    })
    Long countTrueValueInSyncedToNotion();

    @Aggregation(pipeline = {
            "{ '$match': {'syncedToNotion': false}}",
            "{'$count': 'status'}"
    })
    Long countFalseValueInSyncedToNotion();
}
