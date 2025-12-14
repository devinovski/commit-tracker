package com.app.controller;

import com.app.Exception.WrongCharacterPositionException;
import com.app.model.GitlabWebhook;
import com.app.service.NotionSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/gitlab-webhook")
public class CommitController {

    @Autowired
    private NotionSyncService notionSyncService;

    private static Logger statusLogger = LoggerFactory.getLogger("com.status");

    @PostMapping("/commit-parser")
    public ResponseEntity<?> handleWebhook(@RequestBody GitlabWebhook gitlabWebhook){

        try{
            notionSyncService.syncToNotion(gitlabWebhook);
            return ResponseEntity.ok("Success");
        }catch (DuplicateKeyException dke){
            return new ResponseEntity<>("Record already exists", HttpStatus.CONFLICT);
        }catch (DataAccessException dae){
            return new ResponseEntity<>(dae.getMessage().toString(), HttpStatus.CONFLICT);
        }catch (WrongCharacterPositionException | NestedRuntimeException e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }catch (Exception ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }finally {
            Long succeed = notionSyncService.countSyncedToNotionIsTrue();
            Long fail = notionSyncService.countSyncedToNotionIsFalse();
            statusLogger.info("succeed: {}, fail: {}", succeed, Objects.isNull(fail) ? 0 : fail);
        }
    }

}
