package com.app.service;

import com.app.model.GitlabWebhook;
import com.app.model.NotionSync;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;


public interface NotionSyncService {

    List<NotionSync> getSynedToNotionFalse();

    void syncToNotion(GitlabWebhook gitlabWebhook);

    List<ResponseEntity<String>> pushToNotion(List<NotionSync> notionSyncAll);

    List<Map<String, Object>> dataToNotion(List<NotionSync> notionSync);

    NotionSync convertMessage(GitlabWebhook gitlabWebhook);

    Long countSyncedToNotionIsTrue();

    Long countSyncedToNotionIsFalse();

}
