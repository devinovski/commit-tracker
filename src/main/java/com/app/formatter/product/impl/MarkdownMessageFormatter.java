package com.app.formatter.product.impl;

import com.app.formatter.product.MessageFormatter;
import com.app.model.NotionSync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.*;

@Component
public class MarkdownMessageFormatter implements MessageFormatter {

    private List<String> headers = new ArrayList<>();

    private List<NotionSync> body;

    private static Logger appLogger = LoggerFactory.getLogger("all");

    public MarkdownMessageFormatter(List<String> headers, List<NotionSync> body) {
        this.headers = headers;
        this.body = body;
    }

    @Override
    public StringBuilder buildMessage() {
        StringBuilder sb = new StringBuilder();

        appLogger.info("building message...");

        //column
        sb.append(String.join(",",
                new ArrayList<>(Arrays.asList(
                        headers.get(0),
                        headers.get(1),
                        headers.get(2),
                        headers.get(3),
                        headers.get(4),
                        headers.get(5))
                )));

        appLogger.info("success");

        sb.append("\n\n");

        //row
        for (int i = 0; i < body.size(); i++) {
            sb.append(String.join(",",
                    new ArrayList<>(Arrays.asList(
                            String.valueOf(i + 1),
                            body.get(i).getTitle(),
                            body.get(i).getTopic(),
                            body.get(i).getPlatform(),
                            body.get(i).getDifficulty(),
                            body.get(i).getCreatedAt().atZone(ZoneId.of("Asia/Jakarta")).toLocalDate().toString(),
                            "\n")
                    )));

        }

        return sb;
    }


}
