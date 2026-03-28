# 检索增强生成（RAG）——Spring AI 参考文档中文翻译

---

## 检索增强生成

检索增强生成（Retrieval Augmented Generation，RAG）是一种很有用的技术，用于克服大语言模型在以下方面的局限性：长文本内容处理能力不足、事实准确性不稳定，以及上下文感知能力有限。

Spring AI 通过提供一种模块化架构来支持 RAG：你既可以自行构建自定义的 RAG 流程，也可以使用基于 Advisor API 的开箱即用 RAG 流程。

你可以在 Concepts（概念）章节中进一步了解 Retrieval Augmented Generation。

---

## Advisors

Spring AI 通过 Advisor API 为常见的 RAG 流程提供了开箱即用的支持。

如果你想使用 `QuestionAnswerAdvisor` 或 `VectorStoreChatMemoryAdvisor`，需要在项目中加入 `spring-ai-advisors-vector-store` 依赖：

```xml
<dependency>
 <groupId>org.springframework.ai</groupId>
 <artifactId>spring-ai-advisors-vector-store</artifactId>
</dependency>
```

---

## QuestionAnswerAdvisor

向量数据库用于存储 AI 模型本身并不了解的数据。当用户的问题发送给 AI 模型时，`QuestionAnswerAdvisor` 会查询向量数据库，检索与该问题相关的文档。

来自向量数据库的响应会被追加到用户输入文本中，为 AI 模型生成答案提供上下文。

假设你已经将数据加载到了 `VectorStore` 中，那么你可以通过向 `ChatClient` 提供一个 `QuestionAnswerAdvisor` 实例来实现 Retrieval Augmented Generation（RAG）：

```java
ChatResponse response = ChatClient.builder(chatModel)
 .build().prompt()
 .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
 .user(userText)
 .call()
 .chatResponse();
```

在这个例子中，`QuestionAnswerAdvisor` 会对向量数据库中的所有文档执行相似度搜索。  
如果你希望限制被搜索文档的类型，`SearchRequest` 支持一种类似 SQL 的过滤表达式，并且该表达式可在所有 `VectorStore` 实现之间移植使用。

这种过滤表达式既可以在创建 `QuestionAnswerAdvisor` 时进行配置，使其始终对所有 `ChatClient` 请求生效；也可以在运行时按请求动态传入。

下面的示例展示了如何创建一个 `QuestionAnswerAdvisor` 实例：相似度阈值为 `0.8`，返回前 `6` 条结果。

```java
var qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
 .searchRequest(SearchRequest.builder().similarityThreshold(0.8d).topK(6).build())
 .build();
```

### 动态过滤表达式

你可以使用 Advisor 上下文参数 `FILTER_EXPRESSION`，在运行时更新 `SearchRequest` 的过滤表达式：

```java
ChatClient chatClient = ChatClient.builder(chatModel)
 .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore)
 .searchRequest(SearchRequest.builder().build())
 .build())
 .build();

// Update filter expression at runtime
String content = this.chatClient.prompt()
 .user("Please answer my question XYZ")
 .advisors(a -> a.param(QuestionAnswerAdvisor.FILTER_EXPRESSION, "type == 'Spring'"))
 .call()
 .content();
```

`FILTER_EXPRESSION` 参数允许你根据提供的表达式动态过滤搜索结果。

### 自定义模板

`QuestionAnswerAdvisor` 会使用一个默认模板，把检索到的文档内容合并进用户问题中。你可以通过 `.promptTemplate()` 构建器方法传入自定义 `PromptTemplate`，以覆盖这一行为。

这里传入的 `PromptTemplate`，用于自定义 Advisor 如何将检索到的上下文与用户查询融合。  
这与在 `ChatClient` 自身上配置 `TemplateRenderer`（通过 `.templateRenderer()`）不同：后者影响的是 Advisor 执行前，初始用户提示词 / 系统提示词的渲染方式。更多细节可参考 ChatClient Prompt Templates。

这里使用的自定义 `PromptTemplate` 可以基于任意 `TemplateRenderer` 实现（默认使用基于 StringTemplate 引擎的 `StPromptTemplate`）。  
但有一个重要要求：模板中必须包含以下两个占位符：

1. `query`：用于接收用户问题  
2. `question_answer_context`：用于接收检索得到的上下文

```java
PromptTemplate customPromptTemplate = PromptTemplate.builder()
 .renderer(StTemplateRenderer.builder().startDelimiterToken('<').endDelimiterToken('>').build())
 .template("""
 <query>

 Context information is below.

 ---------------------
 <question_answer_context>
 ---------------------

 Given the context information and no prior knowledge, answer the query.

 Follow these rules:

 1. If the answer is not in the context, just say that you don't know.
 2. Avoid statements like "Based on the context..." or "The provided information...".
 """)
 .build();

 String question = "Where does the adventure of Anacletus and Birba take place?";

 QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
 .promptTemplate(customPromptTemplate)
 .build();

 String response = ChatClient.builder(chatModel).build()
 .prompt(question)
 .advisors(qaAdvisor)
 .call()
 .content();
```

`QuestionAnswerAdvisor.Builder.userTextAdvise()` 方法现已弃用，官方更推荐使用 `.promptTemplate()`，因为它提供了更灵活的自定义能力。

---

## RetrievalAugmentationAdvisor

Spring AI 提供了一组 RAG 模块库，你可以用它们来构建自己的 RAG 流程。  
`RetrievalAugmentationAdvisor` 是一个 Advisor，基于模块化架构，为最常见的 RAG 流程提供了开箱即用实现。

如果你想使用 `RetrievalAugmentationAdvisor`，需要添加 `spring-ai-rag` 依赖：

```xml
<dependency>
 <groupId>org.springframework.ai</groupId>
 <artifactId>spring-ai-rag</artifactId>
</dependency>
```

---

## 顺序式 RAG 流程

### 朴素 RAG（Naive RAG）

```java
Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
 .documentRetriever(VectorStoreDocumentRetriever.builder()
 .similarityThreshold(0.50)
 .vectorStore(vectorStore)
 .build())
 .build();

String answer = chatClient.prompt()
 .advisors(retrievalAugmentationAdvisor)
 .user(question)
 .call()
 .content();
```

默认情况下，`RetrievalAugmentationAdvisor` 不允许检索到的上下文为空。  
当上下文为空时，它会指示模型不要回答用户的问题。  
如果你希望允许空上下文，可以这样配置：

```java
Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
 .documentRetriever(VectorStoreDocumentRetriever.builder()
 .similarityThreshold(0.50)
 .vectorStore(vectorStore)
 .build())
 .queryAugmenter(ContextualQueryAugmenter.builder()
 .allowEmptyContext(true)
 .build())
 .build();

String answer = chatClient.prompt()
 .advisors(retrievalAugmentationAdvisor)
 .user(question)
 .call()
 .content();
```

`VectorStoreDocumentRetriever` 接受 `FilterExpression`，可基于元数据过滤搜索结果。  
你既可以在实例化 `VectorStoreDocumentRetriever` 时传入过滤表达式，也可以在运行时通过 `FILTER_EXPRESSION` Advisor 上下文参数动态传入。

```java
Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
 .documentRetriever(VectorStoreDocumentRetriever.builder()
 .similarityThreshold(0.50)
 .vectorStore(vectorStore)
 .build())
 .build();

String answer = chatClient.prompt()
 .advisors(retrievalAugmentationAdvisor)
 .advisors(a -> a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, "type == 'Spring'"))
 .user(question)
 .call()
 .content();
```

更多信息请参见 `VectorStoreDocumentRetriever`。

### 高级 RAG（Advanced RAG）

```java
Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
 .queryTransformers(RewriteQueryTransformer.builder()
 .chatClientBuilder(chatClientBuilder.build().mutate())
 .build())
 .documentRetriever(VectorStoreDocumentRetriever.builder()
 .similarityThreshold(0.50)
 .vectorStore(vectorStore)
 .build())
 .build();

String answer = chatClient.prompt()
 .advisors(retrievalAugmentationAdvisor)
 .user(question)
 .call()
 .content();
```

你还可以使用 `DocumentPostProcessor` API，在文档传给模型之前对检索结果进行后处理。  
例如，你可以基于与查询的相关性，对检索到的文档重新排序（re-ranking）；也可以移除无关或冗余文档；或者压缩每个文档的内容，以减少噪声与重复信息。

---

## 模块（Modules）

Spring AI 实现了一种模块化 RAG 架构，其灵感来自论文 **“Modular RAG: Transforming RAG Systems into LEGO-like Reconfigurable Frameworks”** 中提出的模块化思想。

---

## 检索前阶段（Pre-Retrieval）

检索前模块负责对用户查询进行处理，以尽可能提高检索效果。

### 查询转换（Query Transformation）

这是一个用于转换输入查询的组件，目的是让查询更适合检索任务，解决如下问题：  
- 查询表达不规范  
- 术语歧义  
- 词汇复杂  
- 不支持的语言

当使用 `QueryTransformer` 时，建议将 `ChatClient.Builder` 的 temperature 设置得较低（例如 `0.0`），以获得更确定、更准确的结果，从而提升检索质量。  
大多数聊天模型的默认 temperature 往往偏高，不利于查询转换任务，可能会降低检索效果。

---

## CompressionQueryTransformer

`CompressionQueryTransformer` 使用大语言模型，将一段对话历史和一个后续问题压缩为一个独立查询，从而保留整段对话的核心语义。

当对话历史较长，且后续问题依赖前文上下文时，这个转换器非常有用。

```java
Query query = Query.builder()
 .text("And what is its second largest city?")
 .history(new UserMessage("What is the capital of Denmark?"),
 new AssistantMessage("Copenhagen is the capital of Denmark."))
 .build();

QueryTransformer queryTransformer = CompressionQueryTransformer.builder()
 .chatClientBuilder(chatClientBuilder)
 .build();

Query transformedQuery = queryTransformer.transform(query);
```

该组件使用的提示词可以通过构建器上的 `promptTemplate()` 方法进行自定义。

---

## RewriteQueryTransformer

`RewriteQueryTransformer` 使用大语言模型重写用户查询，以便在查询目标系统（如向量库或 Web 搜索引擎）时获得更好的结果。

当用户查询冗长、存在歧义，或者包含会影响搜索质量的无关信息时，这个转换器非常有用。

```java
Query query = new Query("I'm studying machine learning. What is an LLM?");

QueryTransformer queryTransformer = RewriteQueryTransformer.builder()
 .chatClientBuilder(chatClientBuilder)
 .build();

Query transformedQuery = queryTransformer.transform(query);
```

该组件使用的提示词同样可以通过构建器中的 `promptTemplate()` 方法自定义。

---

## TranslationQueryTransformer

`TranslationQueryTransformer` 使用大语言模型，将查询翻译成文档嵌入模型所支持的目标语言。  
如果查询本身已经是目标语言，则会原样返回；如果无法识别查询语言，也会原样返回。

当嵌入模型主要基于某一种语言训练，而用户查询使用的是另一种语言时，这个转换器非常有用。

```java
Query query = new Query("Hvad er Danmarks hovedstad?");

QueryTransformer queryTransformer = TranslationQueryTransformer.builder()
 .chatClientBuilder(chatClientBuilder)
 .targetLanguage("english")
 .build();

Query transformedQuery = queryTransformer.transform(query);
```

该组件使用的提示词也支持通过构建器中的 `promptTemplate()` 方法进行自定义。

---

## 查询扩展（Query Expansion）

查询扩展组件会把输入查询扩展成一个查询列表。  
它主要用于解决诸如查询表达不充分的问题：  
- 通过生成不同表述方式补充检索路径  
- 或把复杂问题拆成多个简单子查询

### MultiQueryExpander

`MultiQueryExpander` 使用大语言模型把一个查询扩展为多个语义上多样化的变体，以覆盖不同角度。  
这有助于检索到更多上下文信息，并提高找到相关结果的概率。

```java
MultiQueryExpander queryExpander = MultiQueryExpander.builder()
 .chatClientBuilder(chatClientBuilder)
 .numberOfQueries(3)
 .build();
List<Query> queries = queryExpander.expand(new Query("How to run a Spring Boot app?"));
```

默认情况下，`MultiQueryExpander` 会把原始查询也包含在扩展结果列表中。  
如果你不想保留原始查询，可以通过构建器中的 `includeOriginal(false)` 进行关闭：

```java
MultiQueryExpander queryExpander = MultiQueryExpander.builder()
 .chatClientBuilder(chatClientBuilder)
 .includeOriginal(false)
 .build();
```

该组件使用的提示词同样可以通过构建器中的 `promptTemplate()` 方法进行自定义。

---

## 检索阶段（Retrieval）

检索模块负责查询各类数据系统（如向量存储），并取回最相关的文档。

### 文档搜索（Document Search）

这是一个负责从底层数据源中获取 `Document` 的组件。  
这些底层数据源可以是搜索引擎、向量库、数据库，或者知识图谱。

---

## VectorStoreDocumentRetriever

`VectorStoreDocumentRetriever` 会从向量存储中检索与输入查询在语义上相似的文档。  
它支持以下能力：  
- 基于元数据进行过滤  
- 相似度阈值控制  
- top-k 结果数量控制

```java
DocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
 .vectorStore(vectorStore)
 .similarityThreshold(0.73)
 .topK(5)
 .filterExpression(new FilterExpressionBuilder()
 .eq("genre", "fairytale")
 .build())
 .build();
List<Document> documents = retriever.retrieve(new Query("What is the main character of the story?"));
```

过滤表达式既可以是静态的，也可以是动态的。  
对于动态过滤表达式，你可以传入一个 `Supplier`：

```java
DocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
 .vectorStore(vectorStore)
 .filterExpression(() -> new FilterExpressionBuilder()
 .eq("tenant", TenantContextHolder.getTenantIdentifier())
 .build())
 .build();
List<Document> documents = retriever.retrieve(new Query("What are the KPIs for the next semester?"));
```

你还可以通过 `Query` API 使用 `FILTER_EXPRESSION` 参数，按请求级别传入过滤表达式。  
如果同时提供了请求级别过滤表达式和 Retriever 自身配置的过滤表达式，则请求级别的表达式优先。

```java
Query query = Query.builder()
 .text("Who is Anacletus?")
 .context(Map.of(VectorStoreDocumentRetriever.FILTER_EXPRESSION, "location == 'Whispering Woods'"))
 .build();
List<Document> retrievedDocuments = documentRetriever.retrieve(query);
```

---

## 文档合并（Document Join）

文档合并组件用于把多个查询、多个数据源检索得到的文档整合成一个统一集合。  
在合并过程中，它还可以处理文档去重以及 reciprocal ranking（互惠排序）等策略。

### ConcatenationDocumentJoiner

`ConcatenationDocumentJoiner` 会把基于多个查询、多个数据源检索出的文档直接拼接成一个集合。  
如果出现重复文档，则保留第一次出现的那一份。  
每个文档的分数（score）保持不变。

```java
Map<Query, List<List<Document>>> documentsForQuery = ...
DocumentJoiner documentJoiner = new ConcatenationDocumentJoiner();
List<Document> documents = documentJoiner.join(documentsForQuery);
```

---

## 检索后阶段（Post-Retrieval）

检索后模块负责对已检索出的文档进一步处理，以尽可能提高最终生成结果的质量。

### 文档后处理（Document Post-Processing）

这是一个基于查询对检索文档进行后处理的组件，用于解决以下问题：  
- lost-in-the-middle（中间信息遗失）  
- 模型上下文长度限制  
- 检索信息中噪声与冗余过多

例如，它可以：  
- 根据与查询的相关性对文档排序  
- 删除无关或冗余文档  
- 压缩每个文档内容，减少噪声与重复

---

## 生成阶段（Generation）

生成模块负责基于用户查询和检索到的文档生成最终响应。

### 查询增强（Query Augmentation）

查询增强组件会用额外数据增强输入查询，以便为大语言模型提供回答用户问题所需的上下文。

---

## ContextualQueryAugmenter

`ContextualQueryAugmenter` 会使用给定文档中的内容来增强用户查询。

```java
QueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder().build();
```

默认情况下，`ContextualQueryAugmenter` 不允许检索上下文为空。  
一旦上下文为空，它就会指示模型不要回答用户的问题。

如果你希望即使在没有检索到上下文的情况下，也允许模型继续生成响应，可以启用 `allowEmptyContext` 选项：

```java
QueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder()
 .allowEmptyContext(true)
 .build();
```

该组件所使用的提示词，可以通过构建器中的 `promptTemplate()` 与 `emptyContextPromptTemplate()` 方法进行自定义。

---

## 补充说明：结合 Spring AI Alibaba 文档理解 RAG 工程实现

以下内容属于辅助理解，不是对原文的改写。

### 1. RAG 的核心目标

Spring AI Alibaba 中文文档指出，RAG 的根本价值在于解决大模型的两个核心短板：

- **上下文有限**：模型无法一次性读取整个知识库  
- **知识静态**：模型训练完成后，内部知识不会自动更新

因此，RAG 的工程思想并不是“让模型记住更多”，而是“在提问时动态检索外部知识，再把这些知识与问题一起交给模型生成答案”。

### 2. 典型工程链路

在实际项目中，一个完整的 RAG 流程通常包括：

1. **文档加载**：从 Markdown、PDF、数据库、知识库等来源读入数据  
2. **文本切分**：把长文档切成适合嵌入与检索的小块  
3. **向量化**：通过 Embedding 模型生成向量  
4. **向量存储**：写入 Milvus、Elasticsearch、Redis、Pinecone 等向量数据库  
5. **查询处理**：对用户问题做改写、压缩、翻译或扩展  
6. **检索召回**：找出与问题最相关的若干文档片段  
7. **上下文增强**：将检索结果拼接进 Prompt  
8. **答案生成**：交由大语言模型生成最终回答

这与 Spring AI 官方文档中的模块划分是对齐的：Pre-Retrieval、Retrieval、Post-Retrieval、Generation。

### 3. 两步 RAG 与 Agentic RAG 的区别

中文文档还强调了两种常见架构：

- **Two-Step RAG（两步 RAG）**：先检索，再生成；流程固定，延迟更可控，适合 FAQ、企业知识库问答、文档机器人  
- **Agentic RAG**：由 Agent 在推理过程中自行决定是否检索、检索什么、何时调用工具；灵活性更高，但链路复杂、延迟波动也更大

而 Spring AI 官方本页主要聚焦的是更标准、可组合、可控的模块化 RAG 流程。

### 4. 为什么模块化很重要

对工程项目来说，模块化的价值非常大，因为它允许你单独替换每个阶段的实现，而不用重写整个系统。例如：

- 把 `RewriteQueryTransformer` 换成 `TranslationQueryTransformer`
- 把 `VectorStoreDocumentRetriever` 接到不同的向量库
- 给检索结果增加 re-rank 或压缩逻辑
- 修改 `PromptTemplate` 来适配你的业务问答规范

这种“像乐高一样拼装”的方式，非常适合毕业设计、企业知识库、智能客服、智能助手等实际系统开发。

---

## 小结

Spring AI 的 RAG 支持并不是一个单一组件，而是一套完整的模块化能力体系。  
从简单的 `QuestionAnswerAdvisor` 到更灵活的 `RetrievalAugmentationAdvisor`，再到查询转换、查询扩展、文档检索、文档合并、文档后处理和上下文增强等模块，开发者可以根据业务场景自由组合，构建从朴素 RAG 到高级 RAG 的多种实现方案。

对于工程开发项目而言，这种设计非常适合做系统化落地，因为它同时兼顾了：

- 可扩展性  
- 可替换性  
- 可维护性  
- 与实际业务知识库的对接能力

如果你的项目是基于 Spring AI / Spring AI Alibaba 构建智能问答、知识库助手、健身助理、教学辅助系统等应用，那么本页介绍的这些模块就是实现 RAG 能力的核心技术基础。
