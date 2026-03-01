package com.zsr.fitaiagent.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自定义基于 Token 的切词器
 */
@Component
public class MyTokenTextSplitter {
    public List<Document> splitDocuments(List<Document> documents) {
        // 每个片段 1024 tokens，重叠 200 tokens
        TokenTextSplitter splitter = new TokenTextSplitter(1024, 200, 10, 10000, true);
        return splitter.apply(documents);
    }

    public List<Document> splitCustomized(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter(200, 100, 10, 5000, true);
        return splitter.apply(documents);
    }
}
