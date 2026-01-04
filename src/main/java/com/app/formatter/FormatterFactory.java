package com.app.formatter;

import com.app.formatter.product.MessageFormatter;
import com.app.model.NotionSync;

import java.util.List;

public interface FormatterFactory {
    MessageFormatter createMessage(List<String> headers, List<NotionSync> body);//it can be implemented using text and file-based message formats

}