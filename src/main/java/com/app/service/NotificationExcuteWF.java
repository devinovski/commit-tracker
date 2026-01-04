package com.app.service;

import com.app.dao.CommitDao;
import com.app.formatter.FormatterFactory;
import com.app.formatter.TextFormatterFactory;
import com.app.formatter.product.MessageFormatter;
import com.app.model.NotionSync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class NotificationExcuteWF extends TelegramLongPollingBot {

    private final CommitDao commitDao;

    private String botName;

    private String token;

    private static Logger appLogger = LoggerFactory.getLogger("all");

    public NotificationExcuteWF(DefaultBotOptions defaultBotOptions, CommitDao commitDao, @Value("${telegram.api.name}") String botName, @Value("${telegram.api.token}") String token) {
        super(defaultBotOptions);

        this.commitDao = commitDao;
        this.botName = botName;
        this.token = token;

        List<BotCommand> commands = new ArrayList<>(
                Arrays.asList(
                        new BotCommand("/hello", "to greets the user"),
                        new BotCommand("/generate_report", "to generate the markdown report")
                )
        );

        //first message when an user start the conversation with telegram API
        try {

            System.out.println("bot name: " + getBotUsername());
            System.out.println("bot token: " + getBotToken());

            execute(new SetMyCommands(commands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println("onUpdateReceived: " + update);

        String messageChat = update.getMessage().getText();
        String chatId = update.getMessage().getChatId().toString();

        try {

            if (messageChat.equals("/generate_report")){
                SendMessage message = SendMessage
                        .builder()
                        .chatId(chatId)
                        .text(sendMessage("MD").toString())
                        .build();

                //please add conditions if the type is other than MD, such as file


                appLogger.info(message.getText());
                execute(message);

            }else if (messageChat.equals("")){

            }else{

            }

        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }


    }

    public StringBuilder sendMessage(String type){
        FormatterFactory formatterFactory = new TextFormatterFactory();

        //headers
        List<String> headers = Arrays.asList("No", "Title", "Topic", "Platform", "Level", "Log Date");

        //body
        List<NotionSync> notionSyncList = commitDao.findBySyncedToNotionTrue();

        MessageFormatter messageFormatter = formatterFactory.createMessage(headers,notionSyncList);

        StringBuilder text = messageFormatter.buildMessage();

        return text;

    }

    public SendDocument sendDocument(String chatId, File file){
        return null;
    }


}
