package com.app.service.impl;

import com.app.Exception.EmptyCommitMessageException;
import com.app.Exception.WrongCharacterPositionException;
import com.app.dao.CommitDao;
import com.app.model.GitlabWebhook;
import com.app.model.NotionSync;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.eq;

import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class NotionSyncServiceImplTest {

    @Mock
    private CommitDao commitDao;

    @Mock
    private RestTemplate restTemplate;

    @Spy
    @InjectMocks
    private NotionSyncServiceImpl notionSyncServiceImpl;

    private String parentParams = "parent";

    private String propertiesParams = "properties";

    private String titleParams = "title";

    private String platformParams = "Platform";

    private String topicParams = "Topic";

    private String difficultyParams = "Difficulty";

    private String commitDateParams = "Commit Date";

    private String pathURL = "URL";

    private String NOTION_DATABASE_ID = "TEST--database-ID";


    private NotionSync notionSync, notionSync2, notionSync3;

//    private List<NotionSync> syncedToNotionRecords;

    private List<NotionSync> unsyncedToNotionRecords;

    private List<Map<String, Object>> requestBody;

    private GitlabWebhook gitlabWebhook, gitlabWebhookDup;

    @BeforeEach
    private void initData(){
        notionSync = new NotionSync();
        notionSync.setId("1234");
        notionSync.setPlatform("Hackerrank");
        notionSync.setTitle("Coin Change");
        notionSync.setTopic("Dynamic Programming");
        notionSync.setDifficulty("Medium");
        notionSync.setPath("https://hackerrank.com/");
        notionSync.setSyncedToNotion(false);
        notionSync.setCreatedAt(ZonedDateTime.now(ZoneId.of("Asia/Jakarta")).toInstant());
        notionSync.setUpdatedAt(ZonedDateTime.now(ZoneId.of("Asia/Jakarta")).toInstant());

        notionSync2 = new NotionSync();
        notionSync2.setId("12345");
        notionSync2.setPlatform("Leetcode");
        notionSync2.setTitle("Longest Common Subsequence");
        notionSync2.setTopic("Dynamic Programming");
        notionSync2.setDifficulty("Easy");
        notionSync2.setPath("https://leetcode.com/");
        notionSync2.setSyncedToNotion(false);
        notionSync2.setCreatedAt(ZonedDateTime.now(ZoneId.of("Asia/Jakarta")).toInstant());
        notionSync2.setUpdatedAt(ZonedDateTime.now(ZoneId.of("Asia/Jakarta")).toInstant());

        notionSync3 = notionSync2;

        unsyncedToNotionRecords = new ArrayList<>(Arrays.asList(notionSync, notionSync2));
        requestBody = requestBodyHelper(unsyncedToNotionRecords);

        gitlabWebhook = new GitlabWebhook();

        gitlabWebhook.setRef("test");
        gitlabWebhook.setCommit("Coin Change [Dynamic Programming][Hackerrank][Medium][https://hackerrank.com/]");
        gitlabWebhook.setPusher("devinwilliam");


        gitlabWebhookDup = new GitlabWebhook();
        gitlabWebhookDup.setRef("test - dup");
        gitlabWebhookDup.setCommit("Longest Common Subsequence [Dynamic Programming][Leetcode][Easy][https://leetcode.com/]");
        gitlabWebhookDup.setPusher("devinwilliam");

    }

    private List<Map<String, Object>> requestBodyHelper(List<NotionSync> notionSyncRecords){
        List<Map<String, Object>> request = new ArrayList<>();

        for (NotionSync sync: notionSyncRecords) {
            Map<String, Object> mappings = new HashMap<>();
            Map<String, Map<String, String>> options = new HashMap<>();
            Map<String, Object> properties = new HashMap<>();

            mappings.put(parentParams, Map.of("database_id", NOTION_DATABASE_ID));

            properties.put(titleParams, Map.of("title", Arrays.asList(Map.of("text", Map.of("content", sync.getTitle())))));

            properties.put(platformParams, Map.of("select", Map.of("name", sync.getPlatform())));

            properties.put(topicParams, Map.of("select", Map.of("name", sync.getTopic())));

            properties.put(difficultyParams, Map.of("select", Map.of("name", sync.getDifficulty())));

            properties.put(commitDateParams, Map.of("date", Map.of("start", sync.getCreatedAt().toString())));

            properties.put(pathURL, Map.of("url", sync.getPath()));

            mappings.put(propertiesParams, properties);

            request.add(mappings);
        }

        return request;
    }

    @Test
    void getSynedToNotionFalse() {
        when(commitDao.findBySyncedToNotionFalse()).thenReturn(new ArrayList<>(Arrays.asList(notionSync)));

        assertEquals(1,notionSyncServiceImpl.getSynedToNotionFalse().size());
        verify(commitDao).findBySyncedToNotionFalse();

    }

    @Test
    void syncToNotion_successfully() {

        NotionSync filteredRecord = notionSyncServiceImpl.convertMessage(gitlabWebhook);

        List<NotionSync> syncedToNotionRecords = new ArrayList<>();
        syncedToNotionRecords.addAll(syncedToNotionRecordsHelper());
        syncedToNotionRecords.add(filteredRecord);

//        int idx = 0;
//        log.info("======================================================");
//        log.info("filteredRecord: {}", filteredRecord);
//        log.info("======================================================\n");
//
//        log.info("======================================================");
//        for (NotionSync syn: syncedToNotionRecords) {
//            log.info("{} -- titile: {}, platform: {}", ++idx, syn.getTitle(), syn.getPlatform());
//        }
//        log.info("======================================================");

        when(notionSyncServiceImpl.convertMessage(gitlabWebhook)).thenReturn(filteredRecord);
//        when(notionSyncServiceImpl.getSynedToNotionFalse()).thenReturn(syncedToNotionRecordsHelper());
        when(commitDao.findBySyncedToNotionFalse()).thenReturn(syncedToNotionRecordsHelper());
        when(commitDao.saveAll(anyList())).thenReturn(syncedToNotionRecords);

        doReturn(new ArrayList<>()).when(notionSyncServiceImpl).pushToNotion(anyList());

        notionSyncServiceImpl.syncToNotion(gitlabWebhook);

        verify(commitDao, times(1)).saveAll(anyList());
        verify(notionSyncServiceImpl).pushToNotion(anyList());

    }

    //Database error
    @Test
    void syncToNotion_whenCommittingDuplicateRecords_thenThrowDuplicateKeyException(){
        List<NotionSync> sameRecords = Arrays.asList(notionSync2, notionSync3);

        when(commitDao.saveAll(anyList())).thenThrow(new DuplicateKeyException("Record already exists"));

        assertThrows(DuplicateKeyException.class, () -> {
            notionSyncServiceImpl.syncToNotion(gitlabWebhookDup);
        });

    }

    //Database error
    @Test
    void syncToNotion_whenInsertingRecordsToDB_thenThrowDataAccessException(){
        when(commitDao.saveAll(anyList())).thenThrow(new DataAccessException("Database Error") {
            @Override
            public String getMessage() {
                return super.getMessage();
            }
        });

        assertThrows(DataAccessException.class, () -> {
           notionSyncServiceImpl.syncToNotion(gitlabWebhook);
        });
    }

    //http error (general)
    @Test
    void syncToNotion_whenRequestNotionAPI_thenThrowNestedRuntimeException(){
        ReflectionTestUtils.setField(notionSyncServiceImpl,"ADD_ROW_URL","https://api.notion.com/v1/pages");
        ReflectionTestUtils.setField(notionSyncServiceImpl,"NOTION_TOKEN", "ERROR");
        ReflectionTestUtils.setField(notionSyncServiceImpl,"NOTION_DATABASE_ID", "221080b9b84981c3b821dc17dd19100e");

        when(restTemplate.postForEntity(eq("https://api.notion.com/v1/pages"), any(), eq(String.class))).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED,""));

        assertThrows(NestedRuntimeException.class, () -> {
            notionSyncServiceImpl.syncToNotion(gitlabWebhook);
        });

//        when(restTemplate.postForEntity(eq("ADD_ROW"), eq(request), eq(String.class))).thenReturn(ResponseEntity.("Success"));


        verify(notionSyncServiceImpl, times(1)).pushToNotion(anyList());
    }

    @Test
    void syncToNotion_whenURLisNotRegisteredInEnv_thenThrowNullPointerException(){
        ReflectionTestUtils.setField(notionSyncServiceImpl,"ADD_ROW_URL",null);
        ReflectionTestUtils.setField(notionSyncServiceImpl,"NOTION_TOKEN", "ntn_t21596549851CySiZsEC2TSST2KLzkMscHPD6vnwDVjgeE");
        ReflectionTestUtils.setField(notionSyncServiceImpl,"NOTION_DATABASE_ID", "221080b9b84981c3b821dc17dd19100e");

        assertThrows(NullPointerException.class, () -> {
            notionSyncServiceImpl.syncToNotion(gitlabWebhook);
        });

        verify(notionSyncServiceImpl, times(1)).pushToNotion(anyList());
    }

    @Test
    void syncToNotion_whenNotionDatabaseIdisNotRegisteredInEnv_thenThrowNullPointerException(){
        ReflectionTestUtils.setField(notionSyncServiceImpl,"ADD_ROW_URL","https://api.notion.com/v1/pages");
        ReflectionTestUtils.setField(notionSyncServiceImpl,"NOTION_TOKEN", "ntn_t21596549851CySiZsEC2TSST2KLzkMscHPD6vnwDVjgeE");
        ReflectionTestUtils.setField(notionSyncServiceImpl,"NOTION_DATABASE_ID", null);

        assertThrows(NullPointerException.class, () -> {
            notionSyncServiceImpl.syncToNotion(gitlabWebhook);
        });

        verify(notionSyncServiceImpl, times(1)).pushToNotion(anyList());
    }

    @Test
    void syncToNotion_whenNotionTokenisNotRegisteredInEnv_thenThrowNullPointerException(){
        ReflectionTestUtils.setField(notionSyncServiceImpl,"ADD_ROW_URL","https://api.notion.com/v1/pages");
        ReflectionTestUtils.setField(notionSyncServiceImpl,"NOTION_TOKEN", null);
        ReflectionTestUtils.setField(notionSyncServiceImpl,"NOTION_DATABASE_ID", "221080b9b84981c3b821dc17dd19100e");

        assertThrows(NullPointerException.class, () -> {
            notionSyncServiceImpl.syncToNotion(gitlabWebhook);
        });

        verify(notionSyncServiceImpl, times(1)).pushToNotion(anyList());
    }


    //500 Error
//    @Test
//    void syncToNotion_whenRequestNotionAPI_thenThrowNestedRuntimeException(){
//        doThrow(new NestedRuntimeException("Bad Request") {}).when(notionSyncServiceImpl).pushToNotion(anyList());
//
//        assertThrows(NestedRuntimeException.class, () -> {
//            notionSyncServiceImpl.syncToNotion(gitlabWebhook);
//        });
//    }

    //Bad Request 400 (Invalid Payload, Invalid Database ID, etc)
    @Test
    void ZsyncToNotion_whenRequestNotionAPIWithAWrongPayload_thenThrowNestedRuntimeException(){
        ReflectionTestUtils.setField(notionSyncServiceImpl,"ADD_ROW_URL","https://api.notion.com/v1/pages");
        ReflectionTestUtils.setField(notionSyncServiceImpl,"NOTION_TOKEN", "ntn_t21596549851CySiZsEC2TSST2KLzkMscHPD6vnwDVjgeE");
        ReflectionTestUtils.setField(notionSyncServiceImpl,"NOTION_DATABASE_ID", "ERROR");

        when(restTemplate.postForEntity(eq("https://api.notion.com/v1/pages"), any(), eq(String.class))).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST,""));


        assertThrows(NestedRuntimeException.class, () -> {
            notionSyncServiceImpl.syncToNotion(gitlabWebhook);
        });

        verify(notionSyncServiceImpl, times(1)).pushToNotion(anyList());
    }


    private List<NotionSync> syncedToNotionRecordsHelper(){
        List<NotionSync> syncedToNotionRecords = new ArrayList<>();

        NotionSync syncA = new NotionSync();
        syncA.setPlatform("Hackerrank");
        syncA.setId("1234");
        syncA.setTitle("Coin Change");
        syncA.setTopic("Dynamic Programming");
        syncA.setDifficulty("Medium");
        syncA.setPath("https://hackerrank.com/");
        syncA.setSyncedToNotion(true);
        syncA.setCreatedAt(ZonedDateTime.now(ZoneId.of("Asia/Jakarta")).toInstant());
        syncA.setUpdatedAt(ZonedDateTime.now(ZoneId.of("Asia/Jakarta")).toInstant());
        syncedToNotionRecords.add(syncA);


        NotionSync sync2 = new NotionSync();
        sync2.setId("12345");
        sync2.setPlatform("Leetcode");
        sync2.setTitle("Longest Common Subsequence");
        sync2.setTopic("Dynamic Programming");
        sync2.setDifficulty("Easy");
        sync2.setPath("https://leetcode.com/");
        sync2.setSyncedToNotion(true);
        sync2.setCreatedAt(ZonedDateTime.now(ZoneId.of("Asia/Jakarta")).toInstant());
        sync2.setUpdatedAt(ZonedDateTime.now(ZoneId.of("Asia/Jakarta")).toInstant());

        syncedToNotionRecords.add(sync2);


        NotionSync sync3 = new NotionSync();
        sync3.setId("123456");
        sync3.setPlatform("Leetcode");
        sync3.setTitle("Two Sum");
        sync3.setTopic("Binary Search");
        sync3.setDifficulty("Medium");
        sync3.setPath("https://leetcode.com/");
        sync3.setSyncedToNotion(true);
        sync3.setCreatedAt(ZonedDateTime.now(ZoneId.of("Asia/Jakarta")).toInstant());
        sync3.setUpdatedAt(ZonedDateTime.now(ZoneId.of("Asia/Jakarta")).toInstant());
        syncedToNotionRecords.add(sync3);

        return syncedToNotionRecords;

    }

    @Test
    void pushToNotion_successfully() {
        List<ResponseEntity<String>> allResponse = new ArrayList<>();
        List<ResponseEntity<String>> exepected = Arrays.asList(ResponseEntity.ok("Success"), ResponseEntity.ok("Success"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("NOTION-TOKEN");
        headers.set("Notion-Version", "2022-06-28");

        List<Map<String, Object>> allMappingsToNotion = requestBody;

        for (Map<String, Object> mapping: allMappingsToNotion) {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(mapping, headers);
            when(restTemplate.postForEntity(eq("ADD_ROW"), eq(request), eq(String.class))).thenReturn(ResponseEntity.ok("Success"));

            allResponse.add(restTemplate.postForEntity("ADD_ROW", request, String.class));

        }

        assertEquals(allResponse, exepected);

    }

    @Test
    void shouldConvertDataFromNotionSyncToRequestBody_successfully() {
        List<Map<String, Object>> allNotionSync = new ArrayList<>();

        for (NotionSync sync: unsyncedToNotionRecords) {
            Map<String, Object> mappings =  new HashMap<>();
            Map<String, Object> properties = new HashMap<>();

            mappings.put(parentParams, Map.of("database_id", NOTION_DATABASE_ID));

            properties.put(titleParams, Map.of("title", Arrays.asList(Map.of("text", Map.of("content", sync.getTitle())))));

            properties.put(platformParams, Map.of("select", Map.of("name", sync.getPlatform())));

            properties.put(topicParams, Map.of("select", Map.of("name", sync.getTopic())));

            properties.put(difficultyParams, Map.of("select", Map.of("name", sync.getDifficulty())));

            properties.put(commitDateParams, Map.of("date", Map.of("start", sync.getCreatedAt().toString())));

            properties.put(pathURL, Map.of("url", sync.getPath()));

            mappings.put(propertiesParams, properties);

            allNotionSync.add(mappings);

        }

        assertEquals(requestBody, allNotionSync);
    }

    @Test
    void shouldConvertMessageSuccessfully() {

        String commit = gitlabWebhook.getCommit();

        String regex = "(.*?)\\s*\\[(.*?)\\]\\[(.*?)\\]\\[(.*?)\\]\\[(.*?)\\]";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(commit);

        String title = "", topic = "", platform = "", difficulty = "", path = "";

        if (matcher.matches() && matcher.groupCount() == 5){
            title = matcher.group(1);
            topic = matcher.group(2);
            platform = matcher.group(3);
            difficulty = matcher.group(4);
            path = matcher.group(5);
        }

        assertEquals(notionSync.getTitle(), title);
        assertEquals(notionSync.getTopic(), topic);
        assertEquals(notionSync.getPlatform(), platform);
        assertEquals(notionSync.getDifficulty(), difficulty);
        assertEquals(notionSync.getPath(), path);

    }

    @Test
    void convertMessage_whenDoingCommitWithWrongCommitMessageFormat(){
        GitlabWebhook wrongCommitFormat = new GitlabWebhook();
        wrongCommitFormat.setPusher("Devinovski");
        wrongCommitFormat.setRef("XX--00");
        wrongCommitFormat.setCommit("Title's Devin");

        assertThrows(WrongCharacterPositionException.class, () -> {
                notionSyncServiceImpl.syncToNotion(wrongCommitFormat);
        });

    }

    @Test
    void convertMessage_whenCommitMessageIsNullorEmptyString_thenEmptyCommitMessageException(){
        GitlabWebhook emptyGitlabWebhook = new GitlabWebhook();
        emptyGitlabWebhook.setCommit("");

        assertThrows(EmptyCommitMessageException.class, () -> {
            notionSyncServiceImpl.syncToNotion(emptyGitlabWebhook);
        });

    }
}