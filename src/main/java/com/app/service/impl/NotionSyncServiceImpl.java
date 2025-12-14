package com.app.service.impl;

import com.app.Exception.EmptyCommitMessageException;
import com.app.Exception.WrongCharacterPositionException;
import com.app.dao.CommitDao;
import com.app.model.GitlabWebhook;
import com.app.model.NotionSync;
import com.app.service.NotionSyncService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class NotionSyncServiceImpl implements NotionSyncService {
    private String parentParams = "parent";

    private String propertiesParams = "properties";

    private String titleParams = "title";

    private String platformParams = "Platform";

    private String topicParams = "Topic";

    private String difficultyParams = "Difficulty";

    private String commitDateParams = "Commit Date";

    private String pathURL = "URL";

    @Value("${notion.url.add-row}")
    private String ADD_ROW_URL;

    @Value("${notion.security.token}")
    private String NOTION_TOKEN;

    @Value("${notion.database.id}")
    private String NOTION_DATABASE_ID;

    private final CommitDao commitDao;

    private final RestTemplate restTemplate;

    private static Logger appLogger = LoggerFactory.getLogger("all");

    @Autowired
    public NotionSyncServiceImpl(RestTemplate restTemplate, CommitDao commitDao) {
        this.restTemplate = restTemplate;
        this.commitDao = commitDao;
    }

    @Override
    public List<NotionSync> getSynedToNotionFalse() {
        appLogger.info("within a function");
        Date utilDate = new Date();

        Timestamp sqlTimestamp = new Timestamp(utilDate.getTime());

        List<NotionSync> data = commitDao.findBySyncedToNotionFalse();
        appLogger.info("end of a method");
        return data;
    }

    @Override
    public void syncToNotion(GitlabWebhook gitlabWebhook){
        NotionSync sync = null;
        List<NotionSync> unsyncedCommits = new ArrayList<>();

        try{
            //separate the commit message into some portions
            sync = convertMessage(gitlabWebhook);
            appLogger.info("sync.getTopic()->{}", sync.getTitle());
            appLogger.info("sync.getTopic()->{}", sync.getPlatform());
            appLogger.info("sync.getTopic()->{}", sync.getTopic());
            //store it on DB
            sync.setSyncedToNotion(true);

            List<NotionSync> unsynced = getSynedToNotionFalse();

            //check if any unsynced commits stored in database
            unsyncedCommits.addAll(unsynced);
            appLogger.info("Passed 1");
            for (NotionSync notion2: unsyncedCommits) {
                notion2.setSyncedToNotion(true);
            }

            unsyncedCommits.add(sync);
            appLogger.info("Passed 3");
            List<NotionSync> synced = commitDao.saveAll(unsyncedCommits);
            appLogger.info("Passed 4");
            pushToNotion(unsyncedCommits);
        }catch(DuplicateKeyException dke){
            appLogger.error("Record already exists");

            for (NotionSync notion2: unsyncedCommits) {
                notion2.setSyncedToNotion(false);
            }

            commitDao.saveAll(unsyncedCommits);

            throw dke;
        }catch (DataAccessException dae){
            appLogger.error("Type Error: {}", dae.getRootCause());
            appLogger.error("ERROR: {}\n", dae.getMessage());

            for (NotionSync notion2: unsyncedCommits) {
                notion2.setSyncedToNotion(false);
            }

            commitDao.saveAll(unsyncedCommits);

            throw dae;
        }catch(NestedRuntimeException nre){

            //if the error about authorization (ex: invalid notion token)
            if (nre.getMessage().contains("401") || nre.getMessage().toLowerCase().contains("authorization")){
                appLogger.info("Invalid Notion Token");
            }

//            if the error is about invalid  database id
            if (nre.getMessage().contains("400")){
                appLogger.info("Invalid Request body\n{}", nre.getMessage());
            }


            for (NotionSync notion2: unsyncedCommits) {
                notion2.setSyncedToNotion(false);
            }

            commitDao.saveAll(unsyncedCommits);

            throw nre;
        }catch (WrongCharacterPositionException wcpe){
            appLogger.error("ERROR: {}\nat line :{}", wcpe.getMessage(), 80);
            throw wcpe;
        }catch(NullPointerException npe){
            appLogger.error(npe.getMessage());


            for (NotionSync notion2: unsyncedCommits) {
                notion2.setSyncedToNotion(false);
            }

            commitDao.saveAll(unsyncedCommits);

            throw npe;
        }catch(Exception ex){
            appLogger.error("Exception");
            appLogger.error("ERROR: {}", ex.getMessage());


            for (NotionSync notion2: unsyncedCommits) {
                notion2.setSyncedToNotion(false);
            }

            commitDao.saveAll(unsyncedCommits);

            throw ex;
        }

    }

    @Override
    public List<ResponseEntity<String>> pushToNotion(List<NotionSync> notionSync) {
        List<ResponseEntity<String>> allResponse = new ArrayList<>();
        HttpHeaders headers = new HttpHeaders();

        try{

            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(NOTION_TOKEN);
            headers.set("Notion-Version", "2022-06-28");
            //set auth key
            List<Map<String, Object>> allMappingsToNotion = dataToNotion(notionSync);
            appLogger.info("============================= PUSH TO NOTION - START =============================");
            appLogger.info("enterr");
            for (Map<String, Object> mappings: allMappingsToNotion) {
                appLogger.info("forEach");
                appLogger.info("properties: {}", mappings.get("properties"));
                appLogger.info("token: {}", NOTION_TOKEN);
                appLogger.info("uri: {}", ADD_ROW_URL);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(mappings,  headers);
                appLogger.info("request: {}", request);
                ResponseEntity<String> response = restTemplate.postForEntity(ADD_ROW_URL, request, String.class);
                appLogger.info("--response--");
                appLogger.info("response: " + response);
                appLogger.info("response.getBody->{}", response.getBody());
                appLogger.info("response.getHeaders->{}", response.getHeaders());
                appLogger.info("response.getStatusCode->{}", response.getStatusCode());
                appLogger.info("response.getStatusCodeValue->{}", response.getStatusCodeValue());
                appLogger.info("response.getClass->{}", response.getClass());
                allResponse.add(response);
            }
            appLogger.info("============================= PUSH TO NOTION - END =============================");
        }catch(HttpClientErrorException hcee){
            appLogger.info("HttpClientErrorException: {}", hcee.getMessage());
            if (hcee.getRawStatusCode() == 400 || hcee.getRawStatusCode() == 404){

                String message = "";
                int code = 0;
                if (!hcee.getResponseBodyAsString().isEmpty()){
                    JSONObject jsonObject = new JSONObject(hcee.getResponseBodyAsString());
                    code = jsonObject.getInt("status");
                    message = jsonObject.getString("message");
                }else {
                    code = hcee.getRawStatusCode();
                    message = hcee.getMessage();
                }
;
                appLogger.info("code: " +code);
                throw new NestedRuntimeException(String.format("Status code: %d\nMessage: %s",code,message)){};
            } else if (hcee.getRawStatusCode() == 401 && hcee.getResponseBodyAsString().isEmpty()) {
                throw new NestedRuntimeException(String.format("Status: %s, Message: %s",hcee.getStatusCode(),hcee.getMessage())){};
            }

        }catch (NullPointerException npe){
            throw npe;
        }catch (Exception ex){
            appLogger.info("Exception: {}", ex.getMessage());
        }

        return (List<ResponseEntity<String>>) allResponse;
    }

    @Override
    public List<Map<String, Object>> dataToNotion(List<NotionSync> notionSync) {
        appLogger.info("enter");
        List<Map<String, Object>> allNotionSync = new ArrayList<>();



        try{

            for (NotionSync sync: notionSync) {
                Map<String, Object> mappings = new HashMap<>();
                Map<String, Object> properties = new HashMap<>();

                //check if these variables has been registered or not
                appLogger.info("url: " + ADD_ROW_URL);
                appLogger.info("token: " + NOTION_TOKEN);
                appLogger.info("notion database: " + NOTION_DATABASE_ID);
                if ((Objects.isNull(NOTION_DATABASE_ID) || NOTION_DATABASE_ID == "")
                        || (Objects.isNull(NOTION_TOKEN) || NOTION_TOKEN == "")
                        || (Objects.isNull(ADD_ROW_URL) || ADD_ROW_URL == "")){
                    throw new NullPointerException();
                }

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

        }catch(NullPointerException e){
            String errMsg = "";
            if (Objects.isNull(NOTION_DATABASE_ID) || NOTION_DATABASE_ID == ""){
                errMsg += "The notion database id has not been registered in env variable\n";
            }

            if (Objects.isNull(NOTION_TOKEN) || NOTION_TOKEN == ""){
                errMsg += "The notion's token has not been registered in env variable\n";
            }

            if (Objects.isNull(ADD_ROW_URL) || ADD_ROW_URL == ""){
                errMsg += "The url has not been registered in env variable\n";
            }

            throw new NullPointerException(errMsg);

        }

        return allNotionSync;
    }

    @Override
    public NotionSync convertMessage(GitlabWebhook gitlabWebhook){

        try{
            NotionSync sync = new NotionSync();

            if (gitlabWebhook == null || gitlabWebhook.getCommit() == null || gitlabWebhook.getCommit().isEmpty()) {
                throw new EmptyCommitMessageException("Empty Commit Message");
            }

            String input = gitlabWebhook.getCommit();
            long time = System.currentTimeMillis();

            String regex = "(.*?)\\s*\\[(.*?)\\]\\[(.*?)\\]\\[(.*?)\\]\\[(.*?)\\]";

            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(input);

            appLogger.info("matcher.matches(): {}", matcher.matches());

            if (matcher.matches() && matcher.groupCount() == 5){
                appLogger.info("looping");
                appLogger.info("{}",matcher.group(1));
                sync.setTitle(matcher.group(1));
                sync.setTopic(matcher.group(2));
                sync.setPlatform(matcher.group(3));
                sync.setDifficulty(matcher.group(4));
                sync.setPath(matcher.group(5));
            }else{
                throw new WrongCharacterPositionException("Wrong character position");
            }

            //set time

            ZonedDateTime dateTime = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"));
            sync.setCreatedAt(dateTime.toInstant());

            return sync;
        }catch(NullPointerException | IllegalArgumentException ex){
            appLogger.error("ERROR->",ex);
            //check a wrong,missing, or missplaced character posit between desc and topic section
            throw new WrongCharacterPositionException("Wrong character position");
        }catch (Exception e){
            appLogger.error("ERROR->",e);
            throw e;
        }
    }

    @Override
    public Long countSyncedToNotionIsTrue() {
        Long succeed = commitDao.countTrueValueInSyncedToNotion();
        return succeed;
    }

    @Override
    public Long countSyncedToNotionIsFalse() {
        Long fail = commitDao.countFalseValueInSyncedToNotion();

        return fail;
    }

}
