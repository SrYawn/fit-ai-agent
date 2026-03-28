1.Retrieval Augmented Generation

Retrieval Augmented Generation 

Retrieval Augmented Generation (RAG) is a technique useful to overcome the limitations of large language models
that struggle with long-form content, factual accuracy, and context-awareness. 

Spring AI supports RAG by providing a modular architecture that allows you to build custom RAG flows yourself
or use out-of-the-box RAG flows using the Advisor API. 

Learn more about Retrieval Augmented Generation in the concepts section. 

Advisors 

Spring AI provides out-of-the-box support for common RAG flows using the Advisor API. 

To use the QuestionAnswerAdvisor or VectorStoreChatMemoryAdvisor , you need to add the spring-ai-advisors-vector-store dependency to your project: 

<dependency>
 <groupId>org.springframework.ai</groupId>
 <artifactId>spring-ai-advisors-vector-store</artifactId>
</dependency> 

QuestionAnswerAdvisor 

A vector database stores data that the AI model is unaware of. When a user question is sent to the AI model, a QuestionAnswerAdvisor queries the vector database for documents related to the user question. 

The response from the vector database is appended to the user text to provide context for the AI model to generate a response. 

Assuming you have already loaded data into a VectorStore , you can perform Retrieval Augmented Generation (RAG) by providing an instance of QuestionAnswerAdvisor to the ChatClient . 

ChatResponse response = ChatClient.builder(chatModel)
 .build().prompt()
 .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
 .user(userText)
 .call()
 .chatResponse(); 

In this example, the QuestionAnswerAdvisor will perform a similarity search over all documents in the Vector Database. To restrict the types of documents that are searched, the SearchRequest takes an SQL like filter expression that is portable across all VectorStores . 

This filter expression can be configured when creating the QuestionAnswerAdvisor and hence will always apply to all ChatClient requests, or it can be provided at runtime per request. 

Here is how to create an instance of QuestionAnswerAdvisor where the threshold is 0.8 and to return the top 6 results. 

var qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
 .searchRequest(SearchRequest.builder().similarityThreshold(0.8d).topK(6).build())
 .build(); 

Dynamic Filter Expressions 

Update the SearchRequest filter expression at runtime using the FILTER_EXPRESSION advisor context parameter: 

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

The FILTER_EXPRESSION parameter allows you to dynamically filter the search results based on the provided expression. 

Custom Template 

The QuestionAnswerAdvisor uses a default template to augment the user question with the retrieved documents. You can customize this behavior by providing your own PromptTemplate object via the .promptTemplate() builder method. 

The PromptTemplate provided here customizes how the advisor merges retrieved context with the user query. This is distinct from configuring a TemplateRenderer on the ChatClient itself (using .templateRenderer() ), which affects the rendering of the initial user/system prompt content before the advisor runs. See ChatClient Prompt Templates for more details on client-level template rendering. 

The custom PromptTemplate can use any TemplateRenderer implementation (by default, it uses StPromptTemplate based on the StringTemplate engine). The important requirement is that the template must contain the following two placeholders: 

a query placeholder to receive the user question. 

a question_answer_context placeholder to receive the retrieved context. 

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

The QuestionAnswerAdvisor.Builder.userTextAdvise() method is deprecated in favor of using .promptTemplate() for more flexible customization. 

RetrievalAugmentationAdvisor 

Spring AI includes a library of RAG modules that you can use to build your own RAG flows.
The RetrievalAugmentationAdvisor is an Advisor providing an out-of-the-box implementation for the most common RAG flows,
based on a modular architecture. 

To use the RetrievalAugmentationAdvisor , you need to add the spring-ai-rag dependency to your project: 

<dependency>
 <groupId>org.springframework.ai</groupId>
 <artifactId>spring-ai-rag</artifactId>
</dependency> 

Sequential RAG Flows 

Naive RAG 

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

By default, the RetrievalAugmentationAdvisor does not allow the retrieved context to be empty. When that happens,
it instructs the model not to answer the user query. You can allow empty context as follows. 

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

The VectorStoreDocumentRetriever accepts a FilterExpression to filter the search results based on metadata.
You can provide one when instantiating the VectorStoreDocumentRetriever or at runtime per request,
using the FILTER_EXPRESSION advisor context parameter. 

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

See VectorStoreDocumentRetriever for more information. 

Advanced RAG 

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

You can also use the DocumentPostProcessor API to post-process the retrieved documents before passing them to the model. For example, you can use such an interface to perform re-ranking of the retrieved documents based on their relevance to the query, remove irrelevant or redundant documents, or compress the content of each document to reduce noise and redundancy. 

Modules 

Spring AI implements a Modular RAG architecture inspired by the concept of modularity detailed in the paper
" Modular RAG: Transforming RAG Systems into LEGO-like Reconfigurable Frameworks ". 

Pre-Retrieval 

Pre-Retrieval modules are responsible for processing the user query to achieve the best possible retrieval results. 

Query Transformation 

A component for transforming the input query to make it more effective for retrieval tasks, addressing challenges
such as poorly formed queries, ambiguous terms, complex vocabulary, or unsupported languages. 

When using a QueryTransformer , it’s recommended to configure the ChatClient.Builder with a low temperature (e.g., 0.0) to ensure more deterministic and accurate results, improving retrieval quality. The default temperature for most chat models is typically too high for optimal query transformation, leading to reduced retrieval effectiveness. 

CompressionQueryTransformer 

A CompressionQueryTransformer uses a large language model to compress a conversation history and a follow-up query
into a standalone query that captures the essence of the conversation. 

This transformer is useful when the conversation history is long and the follow-up query is related
to the conversation context. 

Query query = Query.builder()
 .text("And what is its second largest city?")
 .history(new UserMessage("What is the capital of Denmark?"),
 new AssistantMessage("Copenhagen is the capital of Denmark."))
 .build();

QueryTransformer queryTransformer = CompressionQueryTransformer.builder()
 .chatClientBuilder(chatClientBuilder)
 .build();

Query transformedQuery = queryTransformer.transform(query); 

The prompt used by this component can be customized via the promptTemplate() method available in the builder. 

RewriteQueryTransformer 

A RewriteQueryTransformer uses a large language model to rewrite a user query to provide better results when
querying a target system, such as a vector store or a web search engine. 

This transformer is useful when the user query is verbose, ambiguous, or contains irrelevant information
that may affect the quality of the search results. 

Query query = new Query("I'm studying machine learning. What is an LLM?");

QueryTransformer queryTransformer = RewriteQueryTransformer.builder()
 .chatClientBuilder(chatClientBuilder)
 .build();

Query transformedQuery = queryTransformer.transform(query); 

The prompt used by this component can be customized via the promptTemplate() method available in the builder. 

TranslationQueryTransformer 

A TranslationQueryTransformer uses a large language model to translate a query to a target language that is supported
by the embedding model used to generate the document embeddings. If the query is already in the target language,
it is returned unchanged. If the language of the query is unknown, it is also returned unchanged. 

This transformer is useful when the embedding model is trained on a specific language and the user query
is in a different language. 

Query query = new Query("Hvad er Danmarks hovedstad?");

QueryTransformer queryTransformer = TranslationQueryTransformer.builder()
 .chatClientBuilder(chatClientBuilder)
 .targetLanguage("english")
 .build();

Query transformedQuery = queryTransformer.transform(query); 

The prompt used by this component can be customized via the promptTemplate() method available in the builder. 

Query Expansion 

A component for expanding the input query into a list of queries, addressing challenges such as poorly formed queries
by providing alternative query formulations, or by breaking down complex problems into simpler sub-queries. 

MultiQueryExpander 

A MultiQueryExpander uses a large language model to expand a query into multiple semantically diverse variations
to capture different perspectives, useful for retrieving additional contextual information and increasing the chances
of finding relevant results. 

MultiQueryExpander queryExpander = MultiQueryExpander.builder()
 .chatClientBuilder(chatClientBuilder)
 .numberOfQueries(3)
 .build();
List<Query> queries = queryExpander.expand(new Query("How to run a Spring Boot app?")); 

By default, the MultiQueryExpander includes the original query in the list of expanded queries. You can disable this behavior
via the includeOriginal method in the builder. 

MultiQueryExpander queryExpander = MultiQueryExpander.builder()
 .chatClientBuilder(chatClientBuilder)
 .includeOriginal(false)
 .build(); 

The prompt used by this component can be customized via the promptTemplate() method available in the builder. 

Retrieval 

Retrieval modules are responsible for querying data systems like vector store and retrieving the most relevant documents. 

Document Search 

Component responsible for retrieving Documents from an underlying data source, such as a search engine, a vector store,
a database, or a knowledge graph. 

VectorStoreDocumentRetriever 

A VectorStoreDocumentRetriever retrieves documents from a vector store that are semantically similar to the input
query. It supports filtering based on metadata, similarity threshold, and top-k results. 

DocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
 .vectorStore(vectorStore)
 .similarityThreshold(0.73)
 .topK(5)
 .filterExpression(new FilterExpressionBuilder()
 .eq("genre", "fairytale")
 .build())
 .build();
List<Document> documents = retriever.retrieve(new Query("What is the main character of the story?")); 

The filter expression can be static or dynamic. For dynamic filter expressions, you can pass a Supplier . 

DocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
 .vectorStore(vectorStore)
 .filterExpression(() -> new FilterExpressionBuilder()
 .eq("tenant", TenantContextHolder.getTenantIdentifier())
 .build())
 .build();
List<Document> documents = retriever.retrieve(new Query("What are the KPIs for the next semester?")); 

You can also provide a request-specific filter expression via the Query API, using the FILTER_EXPRESSION parameter.
If both the request-specific and the retriever-specific filter expressions are provided, the request-specific filter expression takes precedence. 

Query query = Query.builder()
 .text("Who is Anacletus?")
 .context(Map.of(VectorStoreDocumentRetriever.FILTER_EXPRESSION, "location == 'Whispering Woods'"))
 .build();
List<Document> retrievedDocuments = documentRetriever.retrieve(query); 

Document Join 

A component for combining documents retrieved based on multiple queries and from multiple data sources into
a single collection of documents. As part of the joining process, it can also handle duplicate documents and reciprocal
ranking strategies. 

ConcatenationDocumentJoiner 

A ConcatenationDocumentJoiner combines documents retrieved based on multiple queries and from multiple data sources
by concatenating them into a single collection of documents. In case of duplicate documents, the first occurrence is kept.
The score of each document is kept as is. 

Map<Query, List<List<Document>>> documentsForQuery = ...
DocumentJoiner documentJoiner = new ConcatenationDocumentJoiner();
List<Document> documents = documentJoiner.join(documentsForQuery); 

Post-Retrieval 

Post-Retrieval modules are responsible for processing the retrieved documents to achieve the best possible generation results. 

Document Post-Processing 

A component for post-processing retrieved documents based on a query, addressing challenges such as lost-in-the-middle , context length restrictions from the model, and the need to reduce noise and redundancy in the retrieved information. 

For example, it could rank documents based on their relevance to the query, remove irrelevant or redundant documents, or compress the content of each document to reduce noise and redundancy. 

Generation 

Generation modules are responsible for generating the final response based on the user query and retrieved documents. 

Query Augmentation 

A component for augmenting an input query with additional data, useful to provide a large language model
with the necessary context to answer the user query. 

ContextualQueryAugmenter 

The ContextualQueryAugmenter augments the user query with contextual data from the content of the provided documents. 

QueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder().build(); 

By default, the ContextualQueryAugmenter does not allow the retrieved context to be empty. When that happens,
it instructs the model not to answer the user query. 

You can enable the allowEmptyContext option to allow the model to generate a response even when the retrieved context is empty. 

QueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder()
 .allowEmptyContext(true)
 .build(); 

The prompts used by this component can be customized via the promptTemplate() and emptyContextPromptTemplate() methods
available in the builder. 

2. Tool Calling
# Tool Calling :: Spring AI Reference

Tool calling (also known as function calling ) is a common pattern in AI applications allowing a model to interact with a set of APIs, or tools , augmenting its capabilities. 

Tools are mainly used for: 

Information Retrieval . Tools in this category can be used to retrieve information from external sources, such as a database, a web service, a file system, or a web search engine. The goal is to augment the knowledge of the model, allowing it to answer questions that it would not be able to answer otherwise. As such, they can be used in Retrieval Augmented Generation (RAG) scenarios. For example, a tool can be used to retrieve the current weather for a given location, to retrieve the latest news articles, or to query a database for a specific record. 

Taking Action . Tools in this category can be used to take action in a software system, such as sending an email, creating a new record in a database, submitting a form, or triggering a workflow. The goal is to automate tasks that would otherwise require human intervention or explicit programming. For example, a tool can be used to book a flight for a customer interacting with a chatbot, to fill out a form on a web page, or to implement a Java class based on an automated test (TDD) in a code generation scenario. 

Even though we typically refer to tool calling as a model capability, it is actually up to the client application to provide the tool calling logic. The model can only request a tool call and provide the input arguments, whereas the application is responsible for executing the tool call from the input arguments and returning the result. The model never gets access to any of the APIs provided as tools, which is a critical security consideration. 

Spring AI provides convenient APIs to define tools, resolve tool call requests from a model, and execute the tool calls. The following sections provide an overview of the tool calling capabilities in Spring AI. 

Check the Chat Model Comparisons to see which AI models support tool calling invocation. 

Follow the guide to migrate from the deprecated FunctionCallback to ToolCallback API . 

Quick Start 

Let’s see how to start using tool calling in Spring AI. We’ll implement two simple tools: one for information retrieval and one for taking action. The information retrieval tool will be used to get the current date and time in the user’s time zone. The action tool will be used to set an alarm for a specified time. 

Information Retrieval 

AI models don’t have access to real-time information. Any question that assumes awareness of information such as the current date or weather forecast cannot be answered by the model. However, we can provide a tool that can retrieve this information, and let the model call this tool when access to real-time information is needed. 

Let’s implement a tool to get the current date and time in the user’s time zone in a DateTimeTools class. The tool will take no argument. The LocaleContextHolder from Spring Framework can provide the user’s time zone. The tool will be defined as a method annotated with @Tool . To help the model understand if and when to call this tool, we’ll provide a detailed description of what the tools does. 

import java.time.LocalDateTime;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder; class DateTimeTools {

 @Tool(description = "Get the current date and time in the user's timezone")
 String getCurrentDateTime() {
 return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
 }

} 

Next, let’s make the tool available to the model. In this example, we’ll use the ChatClient to interact with the model. We’ll provide the tool to the model by passing an instance of DateTimeTools via the tools() method. When the model needs to know the current date and time, it will request the tool to be called. Internally, the ChatClient will call the tool and return the result to the model, which will then use the tool call result to generate the final response to the original question. 

ChatModel chatModel = ...

String response = ChatClient.create(chatModel)
 .prompt("What day is tomorrow?")
 .tools(new DateTimeTools())
 .call()
 .content();

System.out.println(response); 

The output will be something like: 

Tomorrow is 2015-10-21. 

You can retry asking the same question again. This time, don’t provide the tool to the model. The output will be something like: 

I am an AI and do not have access to real-time information. Please provide the current date so I can accurately determine what day tomorrow will be. 

Without the tool, the model doesn’t know how to answer the question because it doesn’t have the ability to determine the current date and time. 

Taking Actions 

AI models can be used to generate plans for accomplishing certain goals. For example, a model can generate a plan for booking a trip to Denmark. However, the model doesn’t have the ability to execute the plan. That’s where tools come in: they can be used to execute the plan that a model generates. 

In the previous example, we used a tool to determine the current date and time. In this example, we’ll define a second tool for setting an alarm at a specific time. The goal is to set an alarm for 10 minutes from now, so we need to provide both tools to the model to accomplish this task. 

We’ll add the new tool to the same DateTimeTools class as before. The new tool will take a single parameter, which is the time in ISO-8601 format. The tool will then print a message to the console indicating that the alarm has been set for the given time. Like before, the tool is defined as a method annotated with @Tool , which we also use to provide a detailed description to help the model understand when and how to use the tool. 

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder; class DateTimeTools {

 @Tool(description = "Get the current date and time in the user's timezone")
 String getCurrentDateTime() {
 return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
 }

 @Tool(description = "Set a user alarm for the given time, provided in ISO-8601 format")
 void setAlarm(String time) {
 LocalDateTime alarmTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME);
 System.out.println("Alarm set for " + alarmTime);
 }

} 

Next, let’s make both tools available to the model. We’ll use the ChatClient to interact with the model. We’ll provide the tools to the model by passing an instance of DateTimeTools via the tools() method. When we ask to set up an alarm 10 minutes from now, the model will first need to know the current date and time. Then, it will use the current date and time to calculate the alarm time. Finally, it will use the alarm tool to set up the alarm. Internally, the ChatClient will handle any tool call request from the model and send back to it any tool call execution result, so that the model can generate the final response. 

ChatModel chatModel = ...

String response = ChatClient.create(chatModel)
 .prompt("Can you set an alarm 10 minutes from now?")
 .tools(new DateTimeTools())
 .call()
 .content();

System.out.println(response); 

In the application logs, you can check the alarm has been set at the correct time. 

Overview 

Spring AI supports tool calling through a set of flexible abstractions that allow you to define, resolve, and execute tools in a consistent way. This section provides an overview of the main concepts and components of tool calling in Spring AI. 

When we want to make a tool available to the model, we include its definition in the chat request. Each tool definition comprises of a name, a description, and the schema of the input parameters. 

When the model decides to call a tool, it sends a response with the tool name and the input parameters modeled after the defined schema. 

The application is responsible for using the tool name to identify and execute the tool with the provided input parameters. 

The result of the tool call is processed by the application. 

The application sends the tool call result back to the model. 

The model generates the final response using the tool call result as additional context. 

Tools are the building blocks of tool calling and they are modeled by the ToolCallback interface. Spring AI provides built-in support for specifying ToolCallback (s) from methods and functions, but you can always define your own ToolCallback implementations to support more use cases. 

ChatModel implementations transparently dispatch tool call requests to the corresponding ToolCallback implementations and will send the tool call results back to the model, which will ultimately generate the final response. They do so using the ToolCallingManager interface, which is responsible for managing the tool execution lifecycle. 

Both ChatClient and ChatModel accept a list of ToolCallback objects to make the tools available to the model and the ToolCallingManager that will eventually execute them. 

Besides passing the ToolCallback objects directly, you can also pass a list of tool names, that will be resolved dynamically using the ToolCallbackResolver interface. 

The following sections will go into more details about all these concepts and APIs, including how to customize and extend them to support more use cases. 

Methods as Tools 

Spring AI provides built-in support for specifying tools (i.e. ToolCallback (s)) from methods in two ways: 

declaratively, using the @Tool annotation 

programmatically, using the low-level MethodToolCallback implementation. 

Declarative Specification: @Tool 

You can turn a method into a tool by annotating it with @Tool . 

class DateTimeTools {

 @Tool(description = "Get the current date and time in the user's timezone")
 String getCurrentDateTime() {
 return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
 }

} 

The @Tool annotation allows you to provide key information about the tool: 

name : The name of the tool. If not provided, the method name will be used. AI models use this name to identify the tool when calling it. Therefore, it’s not allowed to have two tools with the same name in the same class. The name must be unique across all the tools available to the model for a specific chat request. 

description : The description for the tool, which can be used by the model to understand when and how to call the tool. If not provided, the method name will be used as the tool description. However, it’s strongly recommended to provide a detailed description because that’s paramount for the model to understand the tool’s purpose and how to use it. Failing in providing a good description can lead to the model not using the tool when it should or using it incorrectly. 

returnDirect : Whether the tool result should be returned directly to the client or passed back to the model. See Return Direct for more details. 

resultConverter : The ToolCallResultConverter implementation to use for converting the result of a tool call to a String object to send back to the AI model. See Result Conversion for more details. 

The method can be either static or instance, and it can have any visibility (public, protected, package-private, or private). The class that contains the method can be either a top-level class or a nested class, and it can also have any visibility (as long as it’s accessible where you’re planning to instantiate it). 

Spring AI provides built-in support for AOT compilation of the @Tool -annotated methods as long as the class containing the methods is a Spring bean (e.g. @Component ). Otherwise, you’ll need to provide the necessary configuration to the GraalVM compiler. For example, by annotating the class with @RegisterReflection(memberCategories = MemberCategory.INVOKE_DECLARED_METHODS) . 

You can define any number of arguments for the method (including no argument) with most types (primitives, POJOs, enums, lists, arrays, maps, and so on). Similarly, the method can return most types, including void . If the method returns a value, the return type must be a serializable type, as the result will be serialized and sent back to the model. 

Some types are not supported. See Method Tool Limitations for more details. 

Spring AI will generate the JSON schema for the input parameters of the @Tool -annotated method automatically. The schema is used by the model to understand how to call the tool and prepare the tool request. The @ToolParam annotation can be used to provide additional information about the input parameters, such as a description or whether the parameter is required or optional. By default, all input parameters are considered required. 

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam; class DateTimeTools {

 @Tool(description = "Set a user alarm for the given time")
 void setAlarm(@ToolParam(description = "Time in ISO-8601 format") String time) {
 LocalDateTime alarmTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME);
 System.out.println("Alarm set for " + alarmTime);
 }

} 

The @ToolParam annotation allows you to provide key information about a tool parameter: 

description : The description for the parameter, which can be used by the model to understand better how to use it. For example, what format the parameter should be in, what values are allowed, and so on. 

required : Whether the parameter is required or optional. By default, all parameters are considered required. 

If a parameter is annotated as @Nullable , it will be considered optional unless explicitly marked as required using the @ToolParam annotation. 

Besides the @ToolParam annotation, you can also use the @Schema annotation from Swagger or @JsonProperty from Jackson. See JSON Schema for more details. 

Adding Tools to ChatClient 

When using the declarative specification approach, you can pass the tool class instance to the tools() method when invoking a ChatClient . Such tools will only be available for the specific chat request they are added to. 

ChatClient.create(chatModel)
 .prompt("What day is tomorrow?")
 .tools(new DateTimeTools())
 .call()
 .content(); 

Under the hood, the ChatClient will generate a ToolCallback from each @Tool -annotated method in the tool class instance and pass them to the model. In case you prefer to generate the ToolCallback (s) yourself, you can use the ToolCallbacks utility class. 

ToolCallback[] dateTimeTools = ToolCallbacks.from(new DateTimeTools()); 

Adding Default Tools to ChatClient 

When using the declarative specification approach, you can add default tools to a ChatClient.Builder by passing the tool class instance to the defaultTools() method.
If both default and runtime tools are provided, the runtime tools will completely override the default tools. 

Default tools are shared across all the chat requests performed by all the ChatClient instances built from the same ChatClient.Builder . They are useful for tools that are commonly used across different chat requests, but they can also be dangerous if not used carefully, risking to make them available when they shouldn’t. 

ChatModel chatModel = ...
ChatClient chatClient = ChatClient.builder(chatModel)
 .defaultTools(new DateTimeTools())
 .build(); 

Adding Tools to ChatModel 

When using the declarative specification approach, you can pass the tool class instance to the toolCallbacks() method of the ToolCallingChatOptions you use to call a ChatModel . Such tools will only be available for the specific chat request they are added to. 

ChatModel chatModel = ...
ToolCallback[] dateTimeTools = ToolCallbacks.from(new DateTimeTools());
ChatOptions chatOptions = ToolCallingChatOptions.builder()
 .toolCallbacks(dateTimeTools)
 .build();
Prompt prompt = new Prompt("What day is tomorrow?", chatOptions);
chatModel.call(prompt); 

Adding Default Tools to ChatModel 

When using the declarative specification approach, you can add default tools to ChatModel at construction time by passing the tool class instance to the toolCallbacks() method of the ToolCallingChatOptions instance used to create the ChatModel .
If both default and runtime tools are provided, the runtime tools will completely override the default tools. 

Default tools are shared across all the chat requests performed by that ChatModel instance. They are useful for tools that are commonly used across different chat requests, but they can also be dangerous if not used carefully, risking to make them available when they shouldn’t. 

ToolCallback[] dateTimeTools = ToolCallbacks.from(new DateTimeTools());
ChatModel chatModel = OllamaChatModel.builder()
 .ollamaApi(OllamaApi.builder().build())
 .defaultOptions(ToolCallingChatOptions.builder()
 .toolCallbacks(dateTimeTools)
 .build())
 .build(); 

Programmatic Specification: MethodToolCallback 

You can turn a method into a tool by building a MethodToolCallback programmatically. 

class DateTimeTools {

 String getCurrentDateTime() {
 return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
 }

} 

The MethodToolCallback.Builder allows you to build a MethodToolCallback instance and provide key information about the tool: 

toolDefinition : The ToolDefinition instance that defines the tool name, description, and input schema. You can build it using the ToolDefinition.Builder class. Required. 

toolMetadata : The ToolMetadata instance that defines additional settings such as whether the result should be returned directly to the client, and the result converter to use. You can build it using the ToolMetadata.Builder class. 

toolMethod : The Method instance that represents the tool method. Required. 

toolObject : The object instance that contains the tool method. If the method is static, you can omit this parameter. 

toolCallResultConverter : The ToolCallResultConverter instance to use for converting the result of a tool call to a String object to send back to the AI model. If not provided, the default converter will be used ( DefaultToolCallResultConverter ). 

The ToolDefinition.Builder allows you to build a ToolDefinition instance and define the tool name, description, and input schema: 

name : The name of the tool. If not provided, the method name will be used. AI models use this name to identify the tool when calling it. Therefore, it’s not allowed to have two tools with the same name in the same class. The name must be unique across all the tools available to the model for a specific chat request. 

description : The description for the tool, which can be used by the model to understand when and how to call the tool. If not provided, the method name will be used as the tool description. However, it’s strongly recommended to provide a detailed description because that’s paramount for the model to understand the tool’s purpose and how to use it. Failing in providing a good description can lead to the model not using the tool when it should or using it incorrectly. 

inputSchema : The JSON schema for the input parameters of the tool. If not provided, the schema will be generated automatically based on the method parameters. You can use the @ToolParam annotation to provide additional information about the input parameters, such as a description or whether the parameter is required or optional. By default, all input parameters are considered required. See JSON Schema for more details. 

The ToolMetadata.Builder allows you to build a ToolMetadata instance and define additional settings for the tool: 

returnDirect : Whether the tool result should be returned directly to the client or passed back to the model. See Return Direct for more details. 

Method method = ReflectionUtils.findMethod(DateTimeTools.class, "getCurrentDateTime");
ToolCallback toolCallback = MethodToolCallback.builder()
 .toolDefinition(ToolDefinitions.builder(method)
 .description("Get the current date and time in the user's timezone")
 .build())
 .toolMethod(method)
 .toolObject(new DateTimeTools())
 .build(); 

The method can be either static or instance, and it can have any visibility (public, protected, package-private, or private). The class that contains the method can be either a top-level class or a nested class, and it can also have any visibility (as long as it’s accessible where you’re planning to instantiate it). 

Spring AI provides built-in support for AOT compilation of the tool methods as long as the class containing the methods is a Spring bean (e.g. @Component ). Otherwise, you’ll need to provide the necessary configuration to the GraalVM compiler. For example, by annotating the class with @RegisterReflection(memberCategories = MemberCategory.INVOKE_DECLARED_METHODS) . 

You can define any number of arguments for the method (including no argument) with most types (primitives, POJOs, enums, lists, arrays, maps, and so on). Similarly, the method can return most types, including void . If the method returns a value, the return type must be a serializable type, as the result will be serialized and sent back to the model. 

Some types are not supported. See Method Tool Limitations for more details. 

If the method is static, you can omit the toolObject() method, as it’s not needed. 

class DateTimeTools {

 static String getCurrentDateTime() {
 return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
 }

} 

Method method = ReflectionUtils.findMethod(DateTimeTools.class, "getCurrentDateTime");
ToolCallback toolCallback = MethodToolCallback.builder()
 .toolDefinition(ToolDefinitions.builder(method)
 .description("Get the current date and time in the user's timezone")
 .build())
 .toolMethod(method)
 .build(); 

Spring AI will generate the JSON schema for the input parameters of the method automatically. The schema is used by the model to understand how to call the tool and prepare the tool request. The @ToolParam annotation can be used to provide additional information about the input parameters, such as a description or whether the parameter is required or optional. By default, all input parameters are considered required. 

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.ai.tool.annotation.ToolParam; class DateTimeTools {

 void setAlarm(@ToolParam(description = "Time in ISO-8601 format") String time) {
 LocalDateTime alarmTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME);
 System.out.println("Alarm set for " + alarmTime);
 }

} 

The @ToolParam annotation allows you to provide key information about a tool parameter: 

description : The description for the parameter, which can be used by the model to understand better how to use it. For example, what format the parameter should be in, what values are allowed, and so on. 

required : Whether the parameter is required or optional. By default, all parameters are considered required. 

If a parameter is annotated as @Nullable , it will be considered optional unless explicitly marked as required using the @ToolParam annotation. 

Besides the @ToolParam annotation, you can also use the @Schema annotation from Swagger or @JsonProperty from Jackson. See JSON Schema for more details. 

Adding Tools to ChatClient and ChatModel 

When using the programmatic specification approach, you can pass the MethodToolCallback instance to the toolCallbacks() method of ChatClient .
The tool will only be available for the specific chat request it’s added to. 

ToolCallback toolCallback = ...
ChatClient.create(chatModel)
 .prompt("What day is tomorrow?")
 .toolCallbacks(toolCallback)
 .call()
 .content(); 

Adding Default Tools to ChatClient 

When using the programmatic specification approach, you can add default tools to a ChatClient.Builder by passing the MethodToolCallback instance to the defaultToolCallbacks() method.
If both default and runtime tools are provided, the runtime tools will completely override the default tools. 

Default tools are shared across all the chat requests performed by all the ChatClient instances built from the same ChatClient.Builder . They are useful for tools that are commonly used across different chat requests, but they can also be dangerous if not used carefully, risking to make them available when they shouldn’t. 

ChatModel chatModel = ...
ToolCallback toolCallback = ...
ChatClient chatClient = ChatClient.builder(chatModel)
 .defaultToolCallbacks(toolCallback)
 .build(); 

Adding Tools to ChatModel 

When using the programmatic specification approach, you can pass the MethodToolCallback instance to the toolCallbacks() method of the ToolCallingChatOptions you use to call a ChatModel . The tool will only be available for the specific chat request it’s added to. 

ChatModel chatModel = ...
ToolCallback toolCallback = ...
ChatOptions chatOptions = ToolCallingChatOptions.builder()
 .toolCallbacks(toolCallback)
 .build():
Prompt prompt = new Prompt("What day is tomorrow?", chatOptions);
chatModel.call(prompt); 

Adding Default Tools to ChatModel 

When using the programmatic specification approach, you can add default tools to a ChatModel at construction time by passing the MethodToolCallback instance to the toolCallbacks() method of the ToolCallingChatOptions instance used to create the ChatModel .
If both default and runtime tools are provided, the runtime tools will completely override the default tools. 

Default tools are shared across all the chat requests performed by that ChatModel instance. They are useful for tools that are commonly used across different chat requests, but they can also be dangerous if not used carefully, risking to make them available when they shouldn’t. 

ToolCallback toolCallback = ...
ChatModel chatModel = OllamaChatModel.builder()
 .ollamaApi(OllamaApi.builder().build())
 .defaultOptions(ToolCallingChatOptions.builder()
 .toolCallbacks(toolCallback)
 .build())
 .build(); 

Method Tool Limitations 

The following types are not currently supported as parameters or return types for methods used as tools: 

Optional 

Asynchronous types (e.g. CompletableFuture , Future ) 

Reactive types (e.g. Flow , Mono , Flux ) 

Functional types (e.g. Function , Supplier , Consumer ). 

Functional types are supported using the function-based tool specification approach. See Functions as Tools for more details. 

Functions as Tools 

Spring AI provides built-in support for specifying tools from functions, either programmatically using the low-level FunctionToolCallback implementation or dynamically as @Bean (s) resolved at runtime. 

Programmatic Specification: FunctionToolCallback 

You can turn a functional type ( Function , Supplier , Consumer , or BiFunction ) into a tool by building a FunctionToolCallback programmatically. 

public class WeatherService implements Function<WeatherRequest, WeatherResponse> {
 public WeatherResponse apply(WeatherRequest request) {
 return new WeatherResponse(30.0, Unit.C);
 }
}

public enum Unit { C, F }
public record WeatherRequest(String location, Unit unit) {}
public record WeatherResponse(double temp, Unit unit) {} 

The FunctionToolCallback.Builder allows you to build a FunctionToolCallback instance and provide key information about the tool: 

name : The name of the tool. AI models use this name to identify the tool when calling it. Therefore, it’s not allowed to have two tools with the same name in the same context. The name must be unique across all the tools available to the model for a specific chat request. Required. 

toolFunction : The functional object that represents the tool method ( Function , Supplier , Consumer , or BiFunction ). Required. 

description : The description for the tool, which can be used by the model to understand when and how to call the tool. If not provided, the method name will be used as the tool description. However, it’s strongly recommended to provide a detailed description because that’s paramount for the model to understand the tool’s purpose and how to use it. Failing in providing a good description can lead to the model not using the tool when it should or using it incorrectly. 

inputType : The type of the function input. Required. 

inputSchema : The JSON schema for the input parameters of the tool. If not provided, the schema will be generated automatically based on the inputType . You can use the @ToolParam annotation to provide additional information about the input parameters, such as a description or whether the parameter is required or optional. By default, all input parameters are considered required. See JSON Schema for more details. 

toolMetadata : The ToolMetadata instance that defines additional settings such as whether the result should be returned directly to the client, and the result converter to use. You can build it using the ToolMetadata.Builder class. 

toolCallResultConverter : The ToolCallResultConverter instance to use for converting the result of a tool call to a String object to send back to the AI model. If not provided, the default converter will be used ( DefaultToolCallResultConverter ). 

The ToolMetadata.Builder allows you to build a ToolMetadata instance and define additional settings for the tool: 

returnDirect : Whether the tool result should be returned directly to the client or passed back to the model. See Return Direct for more details. 

ToolCallback toolCallback = FunctionToolCallback
 .builder("currentWeather", new WeatherService())
 .description("Get the weather in location")
 .inputType(WeatherRequest.class)
 .build(); 

The function inputs and outputs can be either Void or POJOs. The input and output POJOs must be serializable, as the result will be serialized and sent back to the model. The function as well as the input and output types must be public. 

Some types are not supported. See Function Tool Limitations for more details. 

Adding Tools to ChatClient 

When using the programmatic specification approach, you can pass the FunctionToolCallback instance to the toolCallbacks() method of ChatClient . The tool will only be available for the specific chat request it’s added to. 

ToolCallback toolCallback = ...
ChatClient.create(chatModel)
 .prompt("What's the weather like in Copenhagen?")
 .toolCallbacks(toolCallback)
 .call()
 .content(); 

Adding Default Tools to ChatClient 

When using the programmatic specification approach, you can add default tools to a ChatClient.Builder by passing the FunctionToolCallback instance to the defaultToolCallbacks() method.
If both default and runtime tools are provided, the runtime tools will completely override the default tools. 

Default tools are shared across all the chat requests performed by all the ChatClient instances built from the same ChatClient.Builder . They are useful for tools that are commonly used across different chat requests, but they can also be dangerous if not used carefully, risking to make them available when they shouldn’t. 

ChatModel chatModel = ...
ToolCallback toolCallback = ...
ChatClient chatClient = ChatClient.builder(chatModel)
 .defaultToolCallbacks(toolCallback)
 .build(); 

Adding Tools to ChatModel 

When using the programmatic specification approach, you can pass the FunctionToolCallback instance to the toolCallbacks() method of ToolCallingChatOptions . The tool will only be available for the specific chat request it’s added to. 

ChatModel chatModel = ...
ToolCallback toolCallback = ...
ChatOptions chatOptions = ToolCallingChatOptions.builder()
 .toolCallbacks(toolCallback)
 .build():
Prompt prompt = new Prompt("What's the weather like in Copenhagen?", chatOptions);
chatModel.call(prompt); 

Adding Default Tools to ChatModel 

When using the programmatic specification approach, you can add default tools to a ChatModel at construction time by passing the FunctionToolCallback instance to the toolCallbacks() method of the ToolCallingChatOptions instance used to create the ChatModel .
If both default and runtime tools are provided, the runtime tools will completely override the default tools. 

Default tools are shared across all the chat requests performed by that ChatModel instance. They are useful for tools that are commonly used across different chat requests, but they can also be dangerous if not used carefully, risking to make them available when they shouldn’t. 

ToolCallback toolCallback = ...
ChatModel chatModel = OllamaChatModel.builder()
 .ollamaApi(OllamaApi.builder().build())
 .defaultOptions(ToolCallingChatOptions.builder()
 .toolCallbacks(toolCallback)
 .build())
 .build(); 

Dynamic Specification: @Bean 

Instead of specifying tools programmatically, you can define tools as Spring beans and let Spring AI resolve them dynamically at runtime using the ToolCallbackResolver interface (via the SpringBeanToolCallbackResolver implementation). This option gives you the possibility to use any Function , Supplier , Consumer , or BiFunction bean as a tool. The bean name will be used as the tool name, and the @Description annotation from Spring Framework can be used to provide a description for the tool, used by the model to understand when and how to call the tool. If you don’t provide a description, the method name will be used as the tool description. However, it’s strongly recommended to provide a detailed description because that’s paramount for the model to understand the tool’s purpose and how to use it. Failing in providing a good description can lead to the model not using the tool when it should or using it incorrectly. 

@Configuration(proxyBeanMethods = false)
class WeatherTools {

 WeatherService weatherService = new WeatherService();

 @Bean
 @Description("Get the weather in location")
 Function<WeatherRequest, WeatherResponse> currentWeather() {
 return weatherService;
 }

} 

Some types are not supported. See Function Tool Limitations for more details. 

The JSON schema for the input parameters of the tool will be generated automatically. You can use the @ToolParam annotation to provide additional information about the input parameters, such as a description or whether the parameter is required or optional. By default, all input parameters are considered required. See JSON Schema for more details. 

record WeatherRequest(@ToolParam(description = "The name of a city or a country") String location, Unit unit) {} 

This tool specification approach has the drawback of not guaranteeing type safety, as the tool resolution is done at runtime. To mitigate this, you can specify the tool name explicitly using the @Bean annotation and storing the value in a constant, so that you can use it in a chat request instead of hard-coding the tool name. 

@Configuration(proxyBeanMethods = false)
class WeatherTools {

 public static final String CURRENT_WEATHER_TOOL = "currentWeather";

 @Bean(CURRENT_WEATHER_TOOL)
 @Description("Get the weather in location")
 Function<WeatherRequest, WeatherResponse> currentWeather() {
 ...
 }

} 

Adding Tools to ChatClient 

When using the dynamic specification approach, you can pass the tool name (i.e. the function bean name) to the toolNames() method of ChatClient .
The tool will only be available for the specific chat request it’s added to. 

ChatClient.create(chatModel)
 .prompt("What's the weather like in Copenhagen?")
 .toolNames("currentWeather")
 .call()
 .content(); 

Adding Default Tools to ChatClient 

When using the dynamic specification approach, you can add default tools to a ChatClient.Builder by passing the tool name to the defaultToolNames() method.
If both default and runtime tools are provided, the runtime tools will completely override the default tools. 

Default tools are shared across all the chat requests performed by all the ChatClient instances built from the same ChatClient.Builder . They are useful for tools that are commonly used across different chat requests, but they can also be dangerous if not used carefully, risking to make them available when they shouldn’t. 

ChatModel chatModel = ...
ChatClient chatClient = ChatClient.builder(chatModel)
 .defaultToolNames("currentWeather")
 .build(); 

Adding Tools to ChatModel 

When using the dynamic specification approach, you can pass the tool name to the toolNames() method of the ToolCallingChatOptions you use to call the ChatModel . The tool will only be available for the specific chat request it’s added to. 

ChatModel chatModel = ...
ChatOptions chatOptions = ToolCallingChatOptions.builder()
 .toolNames("currentWeather")
 .build();
Prompt prompt = new Prompt("What's the weather like in Copenhagen?", chatOptions);
chatModel.call(prompt); 

Adding Default Tools to ChatModel 

When using the dynamic specification approach, you can add default tools to ChatModel at construction time by passing the tool name to the toolNames() method of the ToolCallingChatOptions instance used to create the ChatModel .
If both default and runtime tools are provided, the runtime tools will completely override the default tools. 

Default tools are shared across all the chat requests performed by that ChatModel instance. They are useful for tools that are commonly used across different chat requests, but they can also be dangerous if not used carefully, risking to make them available when they shouldn’t. 

ChatModel chatModel = OllamaChatModel.builder()
 .ollamaApi(OllamaApi.builder().build())
 .defaultOptions(ToolCallingChatOptions.builder()
 .toolNames("currentWeather")
 .build())
 .build(); 

Function Tool Limitations 

The following types are not currently supported as input or output types for functions used as tools: 

Primitive types 

Optional 

Collection types (e.g. List , Map , Array , Set ) 

Asynchronous types (e.g. CompletableFuture , Future ) 

Reactive types (e.g. Flow , Mono , Flux ). 

Primitive types and collections are supported using the method-based tool specification approach. See Methods as Tools for more details. 

Tool Specification 

In Spring AI, tools are modeled via the ToolCallback interface. In the previous sections, we’ve seen how to define tools from methods and functions using the built-in support provided by Spring AI (see Methods as Tools and Functions as Tools ). This section will dive deeper into the tool specification and how to customize and extend it to support more use cases. 

Tool Callback 

The ToolCallback interface provides a way to define a tool that can be called by the AI model, including both definition and execution logic. It’s the main interface to implement when you want to define a tool from scratch. For example, you can define a ToolCallback from an MCP Client (using the Model Context Protocol) or a ChatClient (to build a modular agentic application). 

The interface provides the following methods: 

public interface ToolCallback {

 /**
 * Definition used by the AI model to determine when and how to call the tool.
 */
 ToolDefinition getToolDefinition();

 /**
 * Metadata providing additional information on how to handle the tool.
 */
 ToolMetadata getToolMetadata();

 /**
 * Execute tool with the given input and return the result to send back to the AI model.
 */
 String call(String toolInput);

 /**
 * Execute tool with the given input and context, and return the result to send back to the AI model.
 */
 String call(String toolInput, ToolContext tooContext);

} 

Spring AI provides built-in implementations for tool methods ( MethodToolCallback ) and tool functions ( FunctionToolCallback ). 

Tool Definition 

The ToolDefinition interface provides the required information for the AI model to know about the availability of the tool, including the tool name, description, and input schema. Each ToolCallback implementation must provide a ToolDefinition instance to define the tool. 

The interface provides the following methods: 

public interface ToolDefinition {

 /**
 * The tool name. Unique within the tool set provided to a model.
 */
 String name();

 /**
 * The tool description, used by the AI model to determine what the tool does.
 */
 String description();

 /**
 * The schema of the parameters used to call the tool.
 */
 String inputSchema();

} 

See JSON Schema for more details on the input schema. 

The ToolDefinition.Builder lets you build a ToolDefinition instance using the default implementation ( DefaultToolDefinition ). 

ToolDefinition toolDefinition = ToolDefinition.builder()
 .name("currentWeather")
 .description("Get the weather in location")
 .inputSchema("""
 {
 "type": "object",
 "properties": {
 "location": {
 "type": "string"
 },
 "unit": {
 "type": "string",
 "enum": ["C", "F"]
 }
 },
 "required": ["location", "unit"]
 }
 """)
 .build(); 

Method Tool Definition 

When building tools from a method, the ToolDefinition is automatically generated for you. In case you prefer to generate the ToolDefinition yourself, you can use this convenient builder. 

Method method = ReflectionUtils.findMethod(DateTimeTools.class, "getCurrentDateTime");
ToolDefinition toolDefinition = ToolDefinitions.from(method); 

The ToolDefinition generated from a method includes the method name as the tool name, the method name as the tool description, and the JSON schema of the method input parameters. If the method is annotated with @Tool , the tool name and description will be taken from the annotation, if set. 

See Methods as Tools for more details. 

If you’d rather provide some or all of the attributes explicitly, you can use the ToolDefinition.Builder to build a custom ToolDefinition instance. 

Method method = ReflectionUtils.findMethod(DateTimeTools.class, "getCurrentDateTime");
ToolDefinition toolDefinition = ToolDefinitions.builder(method)
 .name("currentDateTime")
 .description("Get the current date and time in the user's timezone")
 .inputSchema(JsonSchemaGenerator.generateForMethodInput(method))
 .build(); 

Function Tool Definition 

When building tools from a function, the ToolDefinition is automatically generated for you. When you use the FunctionToolCallback.Builder to build a FunctionToolCallback instance, you can provide the tool name, description, and input schema that will be used to generate the ToolDefinition . See Functions as Tools for more details. 

JSON Schema 

When providing a tool to the AI model, the model needs to know the schema of the input type for calling the tool. The schema is used to understand how to call the tool and prepare the tool request. Spring AI provides built-in support for generating the JSON Schema of the input type for a tool via the JsonSchemaGenerator class. The schema is provided as part of the ToolDefinition . 

See Tool Definition for more details on the ToolDefinition and how to pass the input schema to it. 

The JsonSchemaGenerator class is used under the hood to generate the JSON schema for the input parameters of a method or a function, using any of the strategies described in Methods as Tools and Functions as Tools . The JSON schema generation logic supports a series of annotations that you can use on the input parameters for methods and functions to customize the resulting schema. 

This section describes two main options you can customize when generating the JSON schema for the input parameters of a tool: description and required status. 

Description 

Besides providing a description for the tool itself, you can also provide a description for the input parameters of a tool. The description can be used to provide key information about the input parameters, such as what format the parameter should be in, what values are allowed, and so on. This is useful to help the model understand the input schema and how to use it. Spring AI provides built-in support for generating the description for an input parameter using one of the following annotations: 

@ToolParam(description = "…​") from Spring AI 

@JsonClassDescription(description = "…​") from Jackson 

@JsonPropertyDescription(description = "…​") from Jackson 

@Schema(description = "…​") from Swagger. 

This approach works for both methods and functions, and you can use it recursively for nested types. 

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.i18n.LocaleContextHolder; class DateTimeTools {

 @Tool(description = "Set a user alarm for the given time")
 void setAlarm(@ToolParam(description = "Time in ISO-8601 format") String time) {
 LocalDateTime alarmTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME);
 System.out.println("Alarm set for " + alarmTime);
 }

} 

Required/Optional 

By default, each input parameter is considered required, which forces the AI model to provide a value for it when calling the tool. However, you can make an input parameter optional by using one of the following annotations, in this order of precedence: 

@ToolParam(required = false) from Spring AI 

@JsonProperty(required = false) from Jackson 

@Schema(required = false) from Swagger 

@Nullable from Spring Framework. 

This approach works for both methods and functions, and you can use it recursively for nested types. 

class CustomerTools {

 @Tool(description = "Update customer information")
 void updateCustomerInfo(Long id, String name, @ToolParam(required = false) String email) {
 System.out.println("Updated info for customer with id: " + id);
 }

} 

Defining the correct required status for the input parameter is crucial to mitigate the risk of hallucinations and ensure the model provides the right input when calling the tool. In the previous example, the email parameter is optional, which means the model can call the tool without providing a value for it. If the parameter was required, the model would have to provide a value for it when calling the tool. And if no value existed, the model would probably make one up, leading to hallucinations. 

Result Conversion 

The result of a tool call is serialized using a ToolCallResultConverter and then sent back to the AI model. The ToolCallResultConverter interface provides a way to convert the result of a tool call to a String object. 

The interface provides the following method: 

@FunctionalInterface
public interface ToolCallResultConverter {

 /**
 * Given an Object returned by a tool, convert it to a String compatible with the
 * given class type.
 */
 String convert(@Nullable Object result, @Nullable Type returnType);

} 

The result must be a serializable type. By default, the result is serialized to JSON using Jackson ( DefaultToolCallResultConverter ), but you can customize the serialization process by providing your own ToolCallResultConverter implementation. 

Spring AI relies on the ToolCallResultConverter in both method and function tools. 

Method Tool Call Result Conversion 

When building tools from a method with the declarative approach, you can provide a custom ToolCallResultConverter to use for the tool by setting the resultConverter() attribute of the @Tool annotation. 

class CustomerTools {

 @Tool(description = "Retrieve customer information", resultConverter = CustomToolCallResultConverter.class)
 Customer getCustomerInfo(Long id) {
 return customerRepository.findById(id);
 }

} 

If using the programmatic approach, you can provide a custom ToolCallResultConverter to use for the tool by setting the resultConverter() attribute of the MethodToolCallback.Builder . 

See Methods as Tools for more details. 

Function Tool Call Result Conversion 

When building tools from a function using the programmatic approach, you can provide a custom ToolCallResultConverter to use for the tool by setting the resultConverter() attribute of the FunctionToolCallback.Builder . 

See Functions as Tools for more details. 

Tool Context 

Spring AI supports passing additional contextual information to tools through the ToolContext API. This feature allows you to provide extra, user-provided data that can be used within the tool execution along with the tool arguments passed by the AI model. 

class CustomerTools {

 @Tool(description = "Retrieve customer information")
 Customer getCustomerInfo(Long id, ToolContext toolContext) {
 return customerRepository.findById(id, toolContext.getContext().get("tenantId"));
 }

} 

The ToolContext is populated with the data provided by the user when invoking ChatClient . 

ChatModel chatModel = ...

String response = ChatClient.create(chatModel)
 .prompt("Tell me more about the customer with ID 42")
 .tools(new CustomerTools())
 .toolContext(Map.of("tenantId", "acme"))
 .call()
 .content();

System.out.println(response); 

None of the data provided in the ToolContext is sent to the AI model. 

Similarly, you can define tool context data when invoking the ChatModel directly. 

ChatModel chatModel = ...
ToolCallback[] customerTools = ToolCallbacks.from(new CustomerTools());
ChatOptions chatOptions = ToolCallingChatOptions.builder()
 .toolCallbacks(customerTools)
 .toolContext(Map.of("tenantId", "acme"))
 .build();
Prompt prompt = new Prompt("Tell me more about the customer with ID 42", chatOptions);
chatModel.call(prompt); 

If the toolContext option is set both in the default options and in the runtime options, the resulting ToolContext will be the merge of the two,
where the runtime options take precedence over the default options. 

Return Direct 

By default, the result of a tool call is sent back to the model as a response. Then, the model can use the result to continue the conversation. 

There are cases where you’d rather return the result directly to the caller instead of sending it back to the model. For example, if you build an agent that relies on a RAG tool, you might want to return the result directly to the caller instead of sending it back to the model for unnecessary post-processing. Or perhaps you have certain tools that should end the reasoning loop of the agent. 

Each ToolCallback implementation can define whether the result of a tool call should be returned directly to the caller or sent back to the model. By default, the result is sent back to the model. But you can change this behavior per tool. 

The ToolCallingManager , responsible for managing the tool execution lifecycle, is in charge of handling the returnDirect attribute associated with the tool. If the attribute is set to true , the result of the tool call is returned directly to the caller. Otherwise, the result is sent back to the model. 

If multiple tool calls are requested at once, the returnDirect attribute must be set to true for all the tools to return the results directly to the caller. Otherwise, the results will be sent back to the model. 

When we want to make a tool available to the model, we include its definition in the chat request. If we want the result of the tool execution to be returned directly to the caller, we set the returnDirect attribute to true . 

When the model decides to call a tool, it sends a response with the tool name and the input parameters modeled after the defined schema. 

The application is responsible for using the tool name to identify and execute the tool with the provided input parameters. 

The result of the tool call is processed by the application. 

The application sends the tool call result directly to the caller, instead of sending it back to the model. 

Method Return Direct 

When building tools from a method with the declarative approach, you can mark a tool to return the result directly to the caller by setting the returnDirect attribute of the @Tool annotation to true . 

class CustomerTools {

 @Tool(description = "Retrieve customer information", returnDirect = true)
 Customer getCustomerInfo(Long id) {
 return customerRepository.findById(id);
 }

} 

If using the programmatic approach, you can set the returnDirect attribute via the ToolMetadata interface and pass it to the MethodToolCallback.Builder . 

ToolMetadata toolMetadata = ToolMetadata.builder()
 .returnDirect(true)
 .build(); 

See Methods as Tools for more details. 

Function Return Direct 

When building tools from a function with the programmatic approach, you can set the returnDirect attribute via the ToolMetadata interface and pass it to the FunctionToolCallback.Builder . 

ToolMetadata toolMetadata = ToolMetadata.builder()
 .returnDirect(true)
 .build(); 

See Functions as Tools for more details. 

Tool Execution 

The tool execution is the process of calling the tool with the provided input arguments and returning the result. The tool execution is handled by the ToolCallingManager interface, which is responsible for managing the tool execution lifecycle. 

public interface ToolCallingManager {

 /**
 * Resolve the tool definitions from the model's tool calling options.
 */
 List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions);

 /**
 * Execute the tool calls requested by the model.
 */
 ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse);

} 

If you’re using any of the Spring AI Spring Boot Starters, DefaultToolCallingManager is the autoconfigured implementation of the ToolCallingManager interface. You can customize the tool execution behavior by providing your own ToolCallingManager bean. 

@Bean
ToolCallingManager toolCallingManager() {
 return ToolCallingManager.builder().build();
} 

By default, Spring AI manages the tool execution lifecycle transparently for you from within each ChatModel implementation. But you have the possibility to opt-out of this behavior and control the tool execution yourself. This section describes these two scenarios. 

Framework-Controlled Tool Execution 

When using the default behavior, Spring AI will automatically intercept any tool call request from the model, call the tool and return the result to the model. All of this is done transparently for you by each ChatModel implementation using a ToolCallingManager . 

When we want to make a tool available to the model, we include its definition in the chat request ( Prompt ) and invoke the ChatModel API which sends the request to the AI model. 

When the model decides to call a tool, it sends a response ( ChatResponse ) with the tool name and the input parameters modeled after the defined schema. 

The ChatModel sends the tool call request to the ToolCallingManager API. 

The ToolCallingManager is responsible for identifying the tool to call and executing it with the provided input parameters. 

The result of the tool call is returned to the ToolCallingManager . 

The ToolCallingManager returns the tool execution result back to the ChatModel . 

The ChatModel sends the tool execution result back to the AI model ( ToolResponseMessage ). 

The AI model generates the final response using the tool call result as additional context and sends it back to the caller ( ChatResponse ) via the ChatClient . 

Currently, the internal messages exchanged with the model regarding the tool execution are not exposed to the user. If you need to access these messages, you should use the user-controlled tool execution approach. 

The logic determining whether a tool call is eligible for execution is handled by the ToolExecutionEligibilityPredicate interface. By default, the tool execution eligibility is determined by checking if the internalToolExecutionEnabled attribute of ToolCallingChatOptions is set to true (the default value), and if the ChatResponse contains any tool calls. 

public class DefaultToolExecutionEligibilityPredicate implements ToolExecutionEligibilityPredicate {

 @Override
 public boolean test(ChatOptions promptOptions, ChatResponse chatResponse) {
 return ToolCallingChatOptions.isInternalToolExecutionEnabled(promptOptions) && chatResponse != null
 && chatResponse.hasToolCalls();
 }

} 

You can provide your custom implementation of ToolExecutionEligibilityPredicate when creating the ChatModel bean. 

User-Controlled Tool Execution 

There are cases where you’d rather control the tool execution lifecycle yourself. You can do so by setting the internalToolExecutionEnabled attribute of ToolCallingChatOptions to false . 

When you invoke a ChatModel with this option, the tool execution will be delegated to the caller, giving you full control over the tool execution lifecycle. It’s your responsibility checking for tool calls in the ChatResponse and executing them using the ToolCallingManager . 

The following example demonstrates a minimal implementation of the user-controlled tool execution approach: 

ChatModel chatModel = ...
ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();

ChatOptions chatOptions = ToolCallingChatOptions.builder()
 .toolCallbacks(new CustomerTools())
 .internalToolExecutionEnabled(false)
 .build();
Prompt prompt = new Prompt("Tell me more about the customer with ID 42", chatOptions);

ChatResponse chatResponse = chatModel.call(prompt);

while (chatResponse.hasToolCalls()) {
 ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, chatResponse);

 prompt = new Prompt(toolExecutionResult.conversationHistory(), chatOptions);

 chatResponse = chatModel.call(prompt);
}

System.out.println(chatResponse.getResult().getOutput().getText()); 

When choosing the user-controlled tool execution approach, we recommend using a ToolCallingManager to manage the tool calling operations. This way, you can benefit from the built-in support provided by Spring AI for tool execution. However, nothing prevents you from implementing your own tool execution logic. 

The next examples shows a minimal implementation of the user-controlled tool execution approach combined with the usage of the ChatMemory API: 

ToolCallingManager toolCallingManager = DefaultToolCallingManager.builder().build();
ChatMemory chatMemory = MessageWindowChatMemory.builder().build();
String conversationId = UUID.randomUUID().toString();

ChatOptions chatOptions = ToolCallingChatOptions.builder()
 .toolCallbacks(ToolCallbacks.from(new MathTools()))
 .internalToolExecutionEnabled(false)
 .build();
Prompt prompt = new Prompt(
 List.of(new SystemMessage("You are a helpful assistant."), new UserMessage("What is 6 * 8?")),
 chatOptions);
chatMemory.add(conversationId, prompt.getInstructions());

Prompt promptWithMemory = new Prompt(chatMemory.get(conversationId), chatOptions);
ChatResponse chatResponse = chatModel.call(promptWithMemory);
chatMemory.add(conversationId, chatResponse.getResult().getOutput());

while (chatResponse.hasToolCalls()) {
 ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(promptWithMemory,
 chatResponse);
 chatMemory.add(conversationId, toolExecutionResult.conversationHistory()
 .get(toolExecutionResult.conversationHistory().size() - 1));
 promptWithMemory = new Prompt(chatMemory.get(conversationId), chatOptions);
 chatResponse = chatModel.call(promptWithMemory);
 chatMemory.add(conversationId, chatResponse.getResult().getOutput());
}

UserMessage newUserMessage = new UserMessage("What did I ask you earlier?");
chatMemory.add(conversationId, newUserMessage);

ChatResponse newResponse = chatModel.call(new Prompt(chatMemory.get(conversationId))); 

Exception Handling 

When a tool call fails, the exception is propagated as a ToolExecutionException which can be caught to handle the error.
A ToolExecutionExceptionProcessor can be used to handle a ToolExecutionException with two outcomes: either producing an error message to be sent back to the AI model or throwing an exception to be handled by the caller. 

@FunctionalInterface
public interface ToolExecutionExceptionProcessor {

 /**
 * Convert an exception thrown by a tool to a String that can be sent back to the AI
 * model or throw an exception to be handled by the caller.
 */
 String process(ToolExecutionException exception);

} 

If you’re using any of the Spring AI Spring Boot Starters, DefaultToolExecutionExceptionProcessor is the autoconfigured implementation of the ToolExecutionExceptionProcessor interface. By default, the error message of RuntimeException is sent back to the model, while checked exceptions and Errors (e.g., IOException , OutOfMemoryError ) are always thrown. The DefaultToolExecutionExceptionProcessor constructor lets you set the alwaysThrow attribute to true or false . If true , an exception will be thrown instead of sending an error message back to the model. 

You can use the `spring.ai.tools.throw-exception-on-error property to control the behavior of the DefaultToolExecutionExceptionProcessor bean: 

Property Description Default 
spring.ai.tools.throw-exception-on-error 

If true , tool calling errors are thrown as exceptions for the caller to handle. If false , errors are converted to messages and sent back to the AI model, allowing it to process and respond to the error. 

false 

@Bean
ToolExecutionExceptionProcessor toolExecutionExceptionProcessor() {
 return new DefaultToolExecutionExceptionProcessor(true);
} 

If you defined your own ToolCallback implementation, make sure to throw a ToolExecutionException when an error occurs as part of the tool execution logic in the call() method. 

The ToolExecutionExceptionProcessor is used internally by the default ToolCallingManager ( DefaultToolCallingManager ) to handle exceptions during tool execution. See Tool Execution for more details about the tool execution lifecycle. 

Tool Resolution 

The main approach for passing tools to a model is by providing the ToolCallback (s) when invoking the ChatClient or the ChatModel ,
using one of the strategies described in Methods as Tools and Functions as Tools . 

However, Spring AI also supports resolving tools dynamically at runtime using the ToolCallbackResolver interface. 

public interface ToolCallbackResolver {

 /**
 * Resolve the {@link ToolCallback} for the given tool name.
 */
 @Nullable
 ToolCallback resolve(String toolName);

} 

When using this approach: 

On the client-side, you provide the tool names to the ChatClient or the ChatModel instead of the ToolCallback (s). 

On the server-side, a ToolCallbackResolver implementation is responsible for resolving the tool names to the corresponding ToolCallback instances. 

By default, Spring AI relies on a DelegatingToolCallbackResolver that delegates the tool resolution to a list of ToolCallbackResolver instances: 

The SpringBeanToolCallbackResolver resolves tools from Spring beans of type Function , Supplier , Consumer , or BiFunction . See Dynamic Specification: @Bean for more details. 

The StaticToolCallbackResolver resolves tools from a static list of ToolCallback instances. When using the Spring Boot Autoconfiguration, this resolver is automatically configured with all the beans of type ToolCallback defined in the application context. 

If you rely on the Spring Boot Autoconfiguration, you can customize the resolution logic by providing a custom ToolCallbackResolver bean. 

@Bean
ToolCallbackResolver toolCallbackResolver(List<FunctionCallback> toolCallbacks) {
 StaticToolCallbackResolver staticToolCallbackResolver = new StaticToolCallbackResolver(toolCallbacks);
 return new DelegatingToolCallbackResolver(List.of(staticToolCallbackResolver));
} 

The ToolCallbackResolver is used internally by the ToolCallingManager to resolve tools dynamically at runtime, supporting both Framework-Controlled Tool Execution and User-Controlled Tool Execution . 

Observability 

Tool calling includes observability support with spring.ai.tool observations that measure completion time and propagate tracing information. See Tool Calling Observability . 

Optionally, Spring AI can export tool call arguments and results as span attributes, disabled by default for sensitivity reasons. Details: Tool Call Arguments and Result Data . 

Logging 

All the main operations of the tool calling features are logged at the DEBUG level. You can enable the logging by setting the log level to DEBUG for the org.springframework.ai package. 

Chat Memory Model Context Protocol (MCP)

3. Model Context Protocol (MCP)
# Model Context Protocol (MCP) :: Spring AI Reference

New to MCP? Start with our Getting Started with MCP guide for a quick introduction and hands-on examples. 

The Model Context Protocol (MCP) is a standardized protocol that enables AI models to interact with external tools and resources in a structured way.
Think of it as a bridge between your AI models and the real world - allowing them to access databases, APIs, file systems, and other external services through a consistent interface.
It supports multiple transport mechanisms to provide flexibility across different environments. 

The MCP Java SDK provides a Java implementation of the Model Context Protocol, enabling standardized interaction with AI models and tools through both synchronous and asynchronous communication patterns. 

Spring AI embraces MCP with comprehensive support through dedicated Boot Starters and MCP Java Annotations, making it easier than ever to build sophisticated AI-powered applications that can seamlessly connect to external systems.
This means Spring developers can participate in both sides of the MCP ecosystem - building AI applications that consume MCP servers and creating MCP servers that expose Spring-based services to the wider AI community.
Bootstrap your AI applications with MCP support using Spring Initializer . 

MCP Java SDK Architecture 

This section provides an overview for the MCP Java SDK architecture .
For the Spring AI MCP integration, refer to the Spring AI MCP Boot Starters documentation. 

The Java MCP implementation follows a three-layer architecture that separates concerns for maintainability and flexibility: 

Figure 1. MCP Stack Architecture 

Client/Server Layer (Top) 

The top layer handles the main application logic and protocol operations: 

McpClient - Manages client-side operations and server connections 

McpServer - Handles server-side protocol operations and client requests 

Both components utilize the session layer below for communication management 

Session Layer (Middle) 

The middle layer manages communication patterns and maintains connection state: 

McpSession - Core session management interface 

McpClientSession - Client-specific session implementation 

McpServerSession - Server-specific session implementation 

Transport Layer (Bottom) 

The bottom layer handles the actual message transport and serialization: 

McpTransport - Manages JSON-RPC message serialization and deserialization 

Supports multiple transport implementations (STDIO, HTTP/SSE, Streamable-HTTP, etc.) 

Provides the foundation for all higher-level communication 

MCP Client 

The MCP Client is a key component in the Model Context Protocol (MCP) architecture, responsible for establishing and managing connections with MCP servers. It implements the client-side of the protocol, handling: 

Protocol version negotiation to ensure compatibility with servers 

Capability negotiation to determine available features 

Message transport and JSON-RPC communication 

Tool discovery and execution 

Resource access and management 

Prompt system interactions 

Optional features: 

Roots management 

Sampling support 

Synchronous and asynchronous operations 

Transport options: 

Stdio-based transport for process-based communication 

Java HttpClient-based SSE client transport 

WebFlux SSE client transport for reactive HTTP streaming 

MCP Server 

The MCP Server is a foundational component in the Model Context Protocol (MCP) architecture that provides tools, resources, and capabilities to clients. It implements the server-side of the protocol, responsible for: 

Server-side protocol operations implementation 

Tool exposure and discovery 

Resource management with URI-based access 

Prompt template provision and handling 

Capability negotiation with clients 

Structured logging and notifications 

Concurrent client connection management 

Synchronous and Asynchronous API support 

Transport implementations: 

Stdio, Streamable-HTTP, Stateless Streamable-HTTP, SSE 

For detailed implementation guidance, using the low-level MCP Client/Server APIs, refer to the MCP Java SDK documentation .
For simplified setup using Spring Boot, use the MCP Boot Starters described below. 

Spring AI MCP Integration 

Spring AI provides MCP integration through the following Spring Boot starters: 

Client Starters 

spring-ai-starter-mcp-client - Core starter providing STDIO , Servlet-based Streamable-HTTP , Stateless Streamable-HTTP and SSE support 

spring-ai-starter-mcp-client-webflux - WebFlux-based Streamable-HTTP , Stateless Streamable-HTTP and SSE transport implementation 

Server Starters 

STDIO 
Server Type Dependency Property 
Standard Input/Output (STDIO) 

spring-ai-starter-mcp-server 

spring.ai.mcp.server.stdio=true 

WebMVC 

Server Type 

Dependency 

Property 

SSE WebMVC 

spring-ai-starter-mcp-server-webmvc 

spring.ai.mcp.server.protocol=SSE or empty 

Streamable-HTTP WebMVC 

spring-ai-starter-mcp-server-webmvc 

spring.ai.mcp.server.protocol=STREAMABLE 

Stateless Streamable-HTTP WebMVC 

spring-ai-starter-mcp-server-webmvc 

spring.ai.mcp.server.protocol=STATELESS 

WebMVC (Reactive) 

Server Type 

Dependency 

Property 

SSE WebFlux 

spring-ai-starter-mcp-server-webflux 

spring.ai.mcp.server.protocol=SSE or empty 

Streamable-HTTP WebFlux 

spring-ai-starter-mcp-server-webflux 

spring.ai.mcp.server.protocol=STREAMABLE 

Stateless Streamable-HTTP WebFlux 

spring-ai-starter-mcp-server-webflux 

spring.ai.mcp.server.protocol=STATELESS 

Spring AI MCP Annotations 

In addition to the programmatic MCP client & server configuration, Spring AI provides annotation-based method handling for MCP servers and clients through the MCP Annotations module.
This approach simplifies the creation and registration of MCP operations using a clean, declarative programming model with Java annotations. 

The MCP Annotations module enables developers to: 

Create MCP tools, resources, and prompts using simple annotations 

Handle client-side notifications and requests declaratively 

Reduce boilerplate code and improve maintainability 

Automatically generate JSON schemas for tool parameters 

Access special parameters and context information 

Key features include: 

Server Annotations : @McpTool , @McpResource , @McpPrompt , @McpComplete 

Client Annotations : @McpLogging , @McpSampling , @McpElicitation , @McpProgress 

Special Parameters : McpSyncServerExchange , McpAsyncServerExchange , McpTransportContext , McpMeta 

Automatic Discovery : Annotation scanning with configurable package inclusion/exclusion 

Spring Boot Integration : Seamless integration with MCP Boot Starters 

Additional Resources 

MCP Annotations Documentation 

MCP Client Boot Starters Documentation 

MCP Server Boot Starters Documentation 

MCP Utilities Documentation 

Model Context Protocol Specification 

Tool Calling MCP Client Boot Starters


4.Spring AI Alibaba Graph
# Spring AI Alibaba Graph

## What's Spring AI Alibaba Graph

Spring AI Alibaba Graph is a **workflow and multi-agent framework** for Java developers to build complex applications composed of multiple AI models or steps.

Spring AI Alibaba Graph serves as the underlying core engine of the Agent Framework. It provides atomic components for building intelligent agents with interruptible and orchestratable capabilities, offering high flexibility but also relatively high learning costs. In contrast, the Agent Framework is built atop Graph, abstracting away the underlying complexities through concepts like ReactAgent and SequentialAgent.

Please check [the documentation](https://java2ai.com/docs/frameworks/graph-core/quick-start) on official website for mote details

## Core Concepts & Classes

Graph is deeply integrated with the Spring Boot ecosystem, providing a declarative API to orchestrate workflows. This allows developers to abstract each step of an AI application as a node (Node) and connect these nodes in the form of a directed graph (Graph) to create a customizable execution flow. Compared to traditional single-agent (one-turn Q&A) solutions, Spring AI Alibaba Graph supports more complex multi-step task flows, helping to address the issue of a **single large model being insufficient for complex tasks**.

The core of the framework includes: **StateGraph** (the state graph for defining nodes and edges), **Node** (node, encapsulating a specific operation or model call), **Edge** (edge, representing transitions between nodes), and **OverAllState** (global state, carrying shared data throughout the flow). These designs make it convenient to manage state and control flow in the workflow.

1. StateGraph
   The main class for defining a workflow.
   Lets you add nodes (addNode) and edges (addEdge, addConditionalEdges).
   Supports conditional routing, subgraphs, and validation.
   Can be compiled into a CompiledGraph for execution.
2. Node
   Represents a single step in the workflow (e.g., a model call, a data transformation).
   Nodes can be asynchronous and can encapsulate LLM calls or custom logic.
3. Edge
   Represents transitions between nodes.
   Can be conditional, with logic to determine the next node based on the current state.
4. OverAllState
   A serializable, central state object that holds all workflow data.
   Supports key-based strategies for merging/updating state.
   Used for checkpointing, resuming, and passing data between nodes.
5. CompiledGraph
   The executable form of a StateGraph.
   Handles the actual execution, state transitions, and streaming of results.
   Supports interruption, parallel nodes, and checkpointing.
6. InterruptableAction
   Interface for actions that can interrupt graph execution.
   Provides two hook points: `interrupt()` (before execution) and `interruptAfter()` (after execution).
   Useful for human-in-the-loop scenarios, approval workflows, and multi-turn conversations.

## How It's Used (Typical Flow)
- Define StateGraph: In a Spring configuration, you define a StateGraph bean, add nodes (each encapsulating a model call or logic), and connect them with edges.
- Configure State: Use an OverAllStateFactory to define the initial state and key strategies.
- Execution: The graph is compiled and executed, with state flowing through nodes and edges, and conditional logic determining the path.
- Integration: Typically exposed via a REST controller or service in a Spring Boot app.

## Interruption Support

Spring AI Alibaba Graph supports interrupting workflow execution at specific points, enabling human-in-the-loop scenarios.

### InterruptableAction Interface

```java
public interface InterruptableAction {
    // Called BEFORE node execution - can prevent execution
    Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config);

    // Called AFTER node execution - can inspect results and interrupt
    default Optional<InterruptionMetadata> interruptAfter(String nodeId, OverAllState state,
            Map<String, Object> actionResult, RunnableConfig config) {
        return Optional.empty();
    }
}
```

### Example Usage

```java
public class ReviewAction implements AsyncNodeActionWithConfig, InterruptableAction {

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        return CompletableFuture.completedFuture(Map.of("result", "generated_content"));
    }

    @Override
    public Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config) {
        return Optional.empty(); // Don't interrupt before execution
    }

    @Override
    public Optional<InterruptionMetadata> interruptAfter(String nodeId, OverAllState state,
            Map<String, Object> actionResult, RunnableConfig config) {
        // Interrupt after execution for human review
        return Optional.of(InterruptionMetadata.builder(nodeId, state)
            .addMetadata("reason", "needs_review")
            .addMetadata("content", actionResult.get("result"))
            .build());
    }
}
```
