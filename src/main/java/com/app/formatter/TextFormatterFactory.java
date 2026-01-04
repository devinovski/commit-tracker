package com.app.formatter;

import com.app.formatter.product.MessageFormatter;
import com.app.formatter.product.impl.MarkdownMessageFormatter;
import com.app.model.NotionSync;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TextFormatterFactory implements FormatterFactory{

    @Override
    public MessageFormatter createMessage(List<String> headers, List<NotionSync> body) {

        return new MarkdownMessageFormatter(headers, body);

    }
}
