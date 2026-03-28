
# Spring AI Reference：Tool Calling（工具调用）中文完整翻译

## Tool Calling（工具调用）

工具调用（也称为 function calling，函数调用）是 AI 应用中的一种常见模式，它允许模型与一组 API 或工具（tools）交互，从而增强模型的能力。

工具主要用于以下两类场景：

### 1. 信息检索（Information Retrieval）

这一类工具可用于从外部来源检索信息，例如数据库、Web 服务、文件系统或 Web 搜索引擎。其目标是增强模型的知识，使其能够回答原本无法回答的问题。因此，它们可以用于检索增强生成（RAG）场景。

例如，可以使用工具来获取某个位置的当前天气、检索最新新闻文章，或查询数据库中的某条特定记录。

### 2. 执行动作（Taking Action）

这一类工具可用于在软件系统中执行动作，例如发送邮件、在数据库中新建记录、提交表单或触发某个工作流。其目标是自动化那些原本需要人工干预或显式编程才能完成的任务。

例如，可以使用工具为一个与聊天机器人交互的客户预订航班、填写网页表单，或在代码生成场景中根据自动化测试（TDD）来实现 Java 类。

尽管我们通常把工具调用视为模型的一项能力，但实际上，真正提供工具调用逻辑的是客户端应用程序。模型只能“请求调用某个工具”并给出输入参数，而应用程序负责根据这些输入参数执行真正的工具调用并返回结果。模型本身永远不会直接访问这些作为工具暴露出来的 API，这一点是一个非常关键的安全设计考虑。

Spring AI 提供了便捷的 API，用于定义工具、解析模型发出的工具调用请求，并执行工具调用。下面的章节将概述 Spring AI 中的工具调用能力。

你可以查看 **Chat Model Comparisons**，了解哪些 AI 模型支持工具调用。

你也可以参考迁移指南，从已废弃的 **FunctionCallback** 迁移到 **ToolCallback API**。

---

## Quick Start（快速开始）

让我们看一下如何在 Spring AI 中开始使用工具调用。我们将实现两个简单的工具：一个用于信息检索，另一个用于执行操作。

信息检索工具将用于获取用户时区下的当前日期和时间。动作工具将用于为指定时间设置一个闹钟。

---

## Information Retrieval（信息检索）

AI 模型无法访问实时信息。任何假定模型知道当前日期、当前天气预报之类实时信息的问题，模型本身都无法回答。不过，我们可以提供一个用于检索这些信息的工具，并在模型需要获取实时信息时让它调用这个工具。

下面在 `DateTimeTools` 类中实现一个工具，用于获取用户时区下的当前日期和时间。这个工具不接收任何参数。Spring Framework 中的 `LocaleContextHolder` 可以提供用户时区。该工具通过 `@Tool` 注解定义为一个方法。为了帮助模型理解何时以及为什么要调用该工具，我们会提供一段较为详细的工具描述。

```java
import java.time.LocalDateTime;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;

class DateTimeTools {

    @Tool(description = "Get the current date and time in the user's timezone")
    String getCurrentDateTime() {
        return LocalDateTime.now()
                .atZone(LocaleContextHolder.getTimeZone().toZoneId())
                .toString();
    }

}
```

接下来，让我们把这个工具提供给模型。在本例中，我们使用 `ChatClient` 与模型进行交互。通过 `tools()` 方法传入一个 `DateTimeTools` 实例，即可把工具提供给模型。当模型需要知道当前日期和时间时，它会请求调用这个工具。

在内部，`ChatClient` 会调用该工具，并把结果返回给模型；随后模型会利用这个工具调用结果，对原始问题生成最终回答。

```java
ChatModel chatModel = ...

String response = ChatClient.create(chatModel)
    .prompt("What day is tomorrow?")
    .tools(new DateTimeTools())
    .call()
    .content();

System.out.println(response);
```

输出结果大致如下：

```text
Tomorrow is 2015-10-21.
```

你可以再次尝试问同样的问题。这一次，不向模型提供该工具。输出可能类似于：

```text
I am an AI and do not have access to real-time information. Please provide the current date so I can accurately determine what day tomorrow will be.
```

如果没有这个工具，模型就不知道该如何回答这个问题，因为它无法自行确定当前日期和时间。

> **补充说明**：这一类工具本质上属于“给模型补充外部知识或实时信息”的工具，与 RAG 的目标高度一致。区别在于，RAG 更常见的是从知识库或向量库中召回内容，而工具调用则可以进一步连接天气、搜索、数据库、时间服务等外部能力。

---

## Taking Actions（执行操作）

AI 模型可以用来生成完成某项任务的计划。例如，模型可以生成一个“如何预订去丹麦旅行”的计划。但模型本身并不具备执行这个计划的能力。这正是工具发挥作用的地方：工具可以用来执行模型所生成的计划。

在前面的示例中，我们使用一个工具来确定当前日期和时间。在这个示例中，我们将定义第二个工具，用于在指定时间设置一个闹钟。目标是“设置一个从当前时间起 10 分钟后的闹钟”，因此我们需要把两个工具都提供给模型，才能完成这个任务。

我们把新工具加入到前面的 `DateTimeTools` 类中。这个新工具接收一个参数，即 ISO-8601 格式的时间。然后工具会向控制台打印一条消息，表示闹钟已被设置到给定时间。和之前一样，这个工具也通过 `@Tool` 注解定义，并通过详细描述帮助模型理解何时以及如何使用该工具。

```java
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;

class DateTimeTools {

    @Tool(description = "Get the current date and time in the user's timezone")
    String getCurrentDateTime() {
        return LocalDateTime.now()
                .atZone(LocaleContextHolder.getTimeZone().toZoneId())
                .toString();
    }

    @Tool(description = "Set a user alarm for the given time, provided in ISO-8601 format")
    void setAlarm(String time) {
        LocalDateTime alarmTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME);
        System.out.println("Alarm set for " + alarmTime);
    }

}
```

接下来，把这两个工具都提供给模型。我们使用 `ChatClient` 与模型交互。当我们请求“从现在起 10 分钟后设置闹钟”时，模型首先需要知道当前日期和时间；然后，它会利用当前时间计算出闹钟时间；最后，它会调用闹钟工具完成设置。

在内部，`ChatClient` 会处理模型发出的任意工具调用请求，并把工具执行结果发回给模型，以便模型生成最终响应。

```java
ChatModel chatModel = ...

String response = ChatClient.create(chatModel)
    .prompt("Can you set an alarm 10 minutes from now?")
    .tools(new DateTimeTools())
    .call()
    .content();

System.out.println(response);
```

你可以在应用日志中检查，闹钟是否已在正确的时间被设置成功。

> **补充说明**：Spring AI Alibaba 的工具教程也强调，工具不仅能“查信息”，还可以“执行动作”。这使得智能体能够从“会回答问题”进一步演进到“能驱动系统完成任务”。

---

## Overview（概览）

Spring AI 通过一组灵活的抽象来支持工具调用，这些抽象允许你以统一方式定义、解析并执行工具。本节概述 Spring AI 工具调用中的主要概念和核心组件。

当我们希望把某个工具提供给模型时，需要在聊天请求中包含该工具的定义。每个工具定义由以下内容构成：

- 名称（name）
- 描述（description）
- 输入参数的 Schema（schema of input parameters）

当模型决定调用某个工具时，它会返回一个响应，其中包含工具名称以及符合已定义 Schema 的输入参数。

应用程序负责根据工具名称定位对应工具，并使用提供的输入参数执行它。

工具调用的结果会由应用程序处理。

应用程序再把工具调用结果发送回模型。

模型会把该工具调用结果作为额外上下文，用于生成最终回答。

工具是工具调用的基础构件，在 Spring AI 中它们由 `ToolCallback` 接口来建模。Spring AI 内置支持从“方法（methods）”和“函数（functions）”来声明 `ToolCallback`，当然你也可以定义自己的 `ToolCallback` 实现，以覆盖更多使用场景。

各个 `ChatModel` 实现会透明地把模型发出的工具调用请求分派给相应的 `ToolCallback` 实现，并把工具调用结果再发送回模型，最终由模型生成最后的回答。这一过程是通过 `ToolCallingManager` 接口完成的，它负责管理工具执行生命周期。

`ChatClient` 和 `ChatModel` 都可以接受一组 `ToolCallback` 对象，以便把工具提供给模型；同时，它们也会接受最终执行这些工具的 `ToolCallingManager`。

除了直接传入 `ToolCallback` 对象之外，你也可以传入一组工具名称，然后通过 `ToolCallbackResolver` 接口在运行时动态解析这些工具。

接下来的章节会更详细地介绍上述概念和 API，包括如何对它们进行自定义与扩展，以支持更多用例。

---

## Methods as Tools（将方法作为工具）

Spring AI 内置支持通过两种方式把“方法”定义为工具（即 `ToolCallback`）：

1. **声明式方式**：使用 `@Tool` 注解  
2. **编程式方式**：使用底层的 `MethodToolCallback` 实现

---

## Declarative Specification: @Tool（声明式定义：@Tool）

你可以通过给一个方法添加 `@Tool` 注解，把它变成一个工具。

```java
class DateTimeTools {

    @Tool(description = "Get the current date and time in the user's timezone")
    String getCurrentDateTime() {
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }

}
```

`@Tool` 注解允许你提供以下关键信息：

- `name`：工具名称。如果未提供，则默认使用方法名。AI 模型会用该名称来识别工具。因此，在同一个类中不允许有两个同名工具；而且对于某一个具体的聊天请求而言，提供给模型的所有工具名称必须全局唯一。
- `description`：工具描述，模型可借此理解何时以及如何调用该工具。如果不提供，则默认使用方法名作为描述。不过，**强烈建议提供详细描述**，因为这对模型正确理解工具用途以及正确使用工具至关重要。如果描述不充分，模型可能在应该调用时不调用，或者错误调用。
- `returnDirect`：是否将工具结果直接返回给客户端，而不是回传给模型。详见后文 **Return Direct**。
- `resultConverter`：指定一个 `ToolCallResultConverter` 实现，用于把工具调用结果转换为字符串，再发送给 AI 模型。详见后文 **Result Conversion**。

该方法既可以是静态方法，也可以是实例方法；其可见性也没有限制（`public`、`protected`、包级私有、`private` 均可）。包含该方法的类既可以是顶层类，也可以是嵌套类，可见性同样不受限制（只要你实际实例化它时是可访问的即可）。

Spring AI 内置支持对 `@Tool` 标注的方法进行 AOT 编译，但前提是这些方法所在类必须是 Spring Bean（例如使用 `@Component`）。否则，你需要为 GraalVM 编译器提供所需配置，例如通过给类添加 `@RegisterReflection(memberCategories = MemberCategory.INVOKE_DECLARED_METHODS)` 注解。

你可以为该方法定义任意数量的参数（包括零参数），并且大多数类型都支持，例如基本类型、POJO、枚举、列表、数组、Map 等。同样，该方法也可以返回大多数类型，包括 `void`。如果方法有返回值，则其返回类型必须是可序列化的，因为返回结果最终会被序列化并发送回模型。

某些类型目前不受支持。详见 **Method Tool Limitations（方法工具限制）**。

Spring AI 会自动为 `@Tool` 注解方法的输入参数生成 JSON Schema。模型需要借助该 Schema 来理解如何调用该工具并构造工具请求。`@ToolParam` 注解可以用于为输入参数提供额外信息，例如参数描述，或指明它是必填还是可选。默认情况下，所有输入参数都被视为必填项。

```java
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

class DateTimeTools {

    @Tool(description = "Set a user alarm for the given time")
    void setAlarm(@ToolParam(description = "Time in ISO-8601 format") String time) {
        LocalDateTime alarmTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME);
        System.out.println("Alarm set for " + alarmTime);
    }

}
```

`@ToolParam` 注解允许你提供以下关键信息：

- `description`：参数描述，帮助模型更准确地理解如何使用该参数。例如参数应采用什么格式、允许哪些值等。
- `required`：参数是必填还是可选。默认情况下，所有参数都被视为必填。

如果某个参数被标注为 `@Nullable`，则它默认会被视为可选参数，除非你又通过 `@ToolParam` 显式将其标记为必填。

除了 `@ToolParam`，你还可以使用 Swagger 的 `@Schema` 或 Jackson 的 `@JsonProperty`。详见后文 **JSON Schema**。

---

## Adding Tools to ChatClient（向 ChatClient 添加工具）

在采用声明式定义方式时，你可以在调用 `ChatClient` 时，通过 `tools()` 方法传入工具类实例。这些工具只在当前这个聊天请求中可用。

```java
ChatClient.create(chatModel)
    .prompt("What day is tomorrow?")
    .tools(new DateTimeTools())
    .call()
    .content();
```

在底层，`ChatClient` 会从你传入的工具类实例中，把每个 `@Tool` 标注的方法都转换成一个 `ToolCallback`，并将其传给模型。如果你更喜欢自己生成 `ToolCallback`，可以使用 `ToolCallbacks` 工具类。

```java
ToolCallback[] dateTimeTools = ToolCallbacks.from(new DateTimeTools());
```

---

## Adding Default Tools to ChatClient（为 ChatClient 添加默认工具）

在声明式定义方式下，你也可以在 `ChatClient.Builder` 上通过 `defaultTools()` 方法添加默认工具。

如果同时提供了默认工具和运行时工具，那么**运行时工具会完全覆盖默认工具**。

默认工具会被同一个 `ChatClient.Builder` 构建出的所有 `ChatClient` 实例共享，并作用于这些实例发起的所有聊天请求。它们适合那些在多个聊天请求中都会频繁使用的工具，但如果使用不慎，也可能带来风险——例如把本不应该暴露的工具暴露给了模型。

```java
ChatModel chatModel = ...
ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultTools(new DateTimeTools())
    .build();
```

---

## Adding Tools to ChatModel（向 ChatModel 添加工具）

在声明式定义方式下，你可以把工具类实例转换成 `ToolCallback` 后，通过 `ToolCallingChatOptions` 的 `toolCallbacks()` 方法，在调用 `ChatModel` 时传入。此类工具只对当前这一次聊天请求生效。

```java
ChatModel chatModel = ...
ToolCallback[] dateTimeTools = ToolCallbacks.from(new DateTimeTools());
ChatOptions chatOptions = ToolCallingChatOptions.builder()
    .toolCallbacks(dateTimeTools)
    .build();
Prompt prompt = new Prompt("What day is tomorrow?", chatOptions);
chatModel.call(prompt);
```

---

## Adding Default Tools to ChatModel（为 ChatModel 添加默认工具）

在声明式定义方式下，你也可以在创建 `ChatModel` 时，通过其默认选项里的 `toolCallbacks()` 方法传入默认工具。

如果同时提供了默认工具和运行时工具，那么**运行时工具会完全覆盖默认工具**。

默认工具会被该 `ChatModel` 实例发起的所有聊天请求共享。它们适合通用工具，但如果不谨慎使用，也可能在不应该暴露工具的时候暴露出去。

```java
ToolCallback[] dateTimeTools = ToolCallbacks.from(new DateTimeTools());
ChatModel chatModel = OllamaChatModel.builder()
    .ollamaApi(OllamaApi.builder().build())
    .defaultOptions(ToolCallingChatOptions.builder()
        .toolCallbacks(dateTimeTools)
        .build())
    .build();
```

---

## Programmatic Specification: MethodToolCallback（编程式定义：MethodToolCallback）

你也可以通过编程方式构建 `MethodToolCallback`，从而把一个方法变成工具。

```java
class DateTimeTools {

    String getCurrentDateTime() {
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }

}
```

`MethodToolCallback.Builder` 允许你构建一个 `MethodToolCallback` 实例，并指定如下关键信息：

- `toolDefinition`：`ToolDefinition` 实例，用来定义工具名称、描述和输入 Schema。可通过 `ToolDefinition.Builder` 构建。**必填**
- `toolMetadata`：`ToolMetadata` 实例，用来定义额外设置，例如结果是否直接返回给客户端、使用哪个结果转换器等。可通过 `ToolMetadata.Builder` 构建
- `toolMethod`：表示工具方法的 `Method` 实例。**必填**
- `toolObject`：包含该工具方法的对象实例。如果方法是静态方法，可以省略该参数
- `toolCallResultConverter`：用于把工具调用结果转换为字符串并发送给 AI 模型的 `ToolCallResultConverter`。如果不提供，将使用默认转换器（`DefaultToolCallResultConverter`）

`ToolDefinition.Builder` 用于构建 `ToolDefinition`，定义工具名称、描述和输入 Schema：

- `name`：工具名称。如果不提供，则使用方法名。该名称在单次请求可用的工具集合中必须唯一。
- `description`：工具描述。如果不提供，则使用方法名作为描述。仍然强烈建议你显式提供详细描述。
- `inputSchema`：工具输入参数的 JSON Schema。如果不提供，则根据方法参数自动生成。你仍可以使用 `@ToolParam` 为参数补充描述、必填/可选信息。

`ToolMetadata.Builder` 用于构建 `ToolMetadata`，定义工具的额外设置：

- `returnDirect`：工具结果是否直接返回给调用方，而不是发回模型。详见后文 **Return Direct**。

```java
Method method = ReflectionUtils.findMethod(DateTimeTools.class, "getCurrentDateTime");
ToolCallback toolCallback = MethodToolCallback.builder()
    .toolDefinition(ToolDefinitions.builder(method)
        .description("Get the current date and time in the user's timezone")
        .build())
    .toolMethod(method)
    .toolObject(new DateTimeTools())
    .build();
```

该方法可以是静态或实例方法，可见性不限。包含方法的类可以是顶层类或嵌套类，可见性也不限。

Spring AI 对工具方法提供内置的 AOT 支持，前提仍然是其所在类是 Spring Bean；否则你需要为 GraalVM 提供反射配置，例如使用 `@RegisterReflection(memberCategories = MemberCategory.INVOKE_DECLARED_METHODS)`。

你可以为该方法定义任意数量参数（含零参数），支持大多数类型；返回值也支持大多数类型（含 `void`）。如果方法有返回值，则必须是可序列化类型，因为结果会被序列化并发送给模型。

某些类型不受支持。详见 **Method Tool Limitations**。

如果方法是静态方法，那么你可以省略 `toolObject()`。

```java
class DateTimeTools {

    static String getCurrentDateTime() {
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }

}
```

```java
Method method = ReflectionUtils.findMethod(DateTimeTools.class, "getCurrentDateTime");
ToolCallback toolCallback = MethodToolCallback.builder()
    .toolDefinition(ToolDefinitions.builder(method)
        .description("Get the current date and time in the user's timezone")
        .build())
    .toolMethod(method)
    .build();
```

Spring AI 会自动为该方法的输入参数生成 JSON Schema。你可以通过 `@ToolParam` 为参数提供描述及必填/可选信息。默认所有参数都是必填。

```java
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.ai.tool.annotation.ToolParam;

class DateTimeTools {

    void setAlarm(@ToolParam(description = "Time in ISO-8601 format") String time) {
        LocalDateTime alarmTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME);
        System.out.println("Alarm set for " + alarmTime);
    }

}
```

`@ToolParam` 的含义与前文一致：

- `description`：参数描述
- `required`：参数是否必填

若参数标注了 `@Nullable`，则默认视为可选，除非通过 `@ToolParam` 明确标为必填。

此外，还可以配合 Swagger 的 `@Schema` 或 Jackson 的 `@JsonProperty` 使用。详见 **JSON Schema**。

---

## Adding Tools to ChatClient and ChatModel（向 ChatClient / ChatModel 添加工具）

在使用编程式定义方式时，你可以把 `MethodToolCallback` 实例传给 `ChatClient` 的 `toolCallbacks()` 方法。该工具仅对当前聊天请求生效。

```java
ToolCallback toolCallback = ...
ChatClient.create(chatModel)
    .prompt("What day is tomorrow?")
    .toolCallbacks(toolCallback)
    .call()
    .content();
```

---

## Adding Default Tools to ChatClient（为 ChatClient 添加默认工具）

在编程式定义方式下，你可以把 `MethodToolCallback` 实例通过 `defaultToolCallbacks()` 方法设置为 `ChatClient.Builder` 的默认工具。

如果同时提供默认工具与运行时工具，那么**运行时工具会完全覆盖默认工具**。

默认工具会共享给同一个 `ChatClient.Builder` 构建出的所有 `ChatClient` 实例，对它们发起的所有聊天请求都有效。它们适合跨请求复用，但也可能在不应暴露时被暴露。

```java
ChatModel chatModel = ...
ToolCallback toolCallback = ...
ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultToolCallbacks(toolCallback)
    .build();
```

---

## Adding Tools to ChatModel（向 ChatModel 添加工具）

在编程式定义方式下，你可以把 `MethodToolCallback` 实例通过 `ToolCallingChatOptions` 的 `toolCallbacks()` 方法传给 `ChatModel`。该工具只在当前请求中可用。

```java
ChatModel chatModel = ...
ToolCallback toolCallback = ...
ChatOptions chatOptions = ToolCallingChatOptions.builder()
    .toolCallbacks(toolCallback)
    .build();
Prompt prompt = new Prompt("What day is tomorrow?", chatOptions);
chatModel.call(prompt);
```

---

## Adding Default Tools to ChatModel（为 ChatModel 添加默认工具）

在编程式定义方式下，你可以在创建 `ChatModel` 时，把 `MethodToolCallback` 实例放进默认选项中的 `toolCallbacks()` 方法里，作为默认工具。

如果同时提供默认工具与运行时工具，那么**运行时工具会完全覆盖默认工具**。

默认工具会被该 `ChatModel` 实例的所有聊天请求共享。

```java
ToolCallback toolCallback = ...
ChatModel chatModel = OllamaChatModel.builder()
    .ollamaApi(OllamaApi.builder().build())
    .defaultOptions(ToolCallingChatOptions.builder()
        .toolCallbacks(toolCallback)
        .build())
    .build();
```

---

## Method Tool Limitations（方法工具限制）

当前，以下类型**不支持**作为“方法型工具”的参数类型或返回类型：

- `Optional`
- 异步类型（例如 `CompletableFuture`、`Future`）
- 响应式类型（例如 `Flow`、`Mono`、`Flux`）
- 函数式类型（例如 `Function`、`Supplier`、`Consumer`）

函数式类型可以通过“基于函数的工具定义方式”来支持。详见后文 **Functions as Tools**。

---

## Functions as Tools（将函数作为工具）

Spring AI 内置支持从函数来定义工具。你既可以使用底层的 `FunctionToolCallback` 进行编程式定义，也可以把函数定义为 `@Bean`，在运行时动态解析。

---

## Programmatic Specification: FunctionToolCallback（编程式定义：FunctionToolCallback）

你可以通过编程方式构建 `FunctionToolCallback`，把一个函数式对象（`Function`、`Supplier`、`Consumer` 或 `BiFunction`）转换为工具。

```java
public class WeatherService implements Function<WeatherRequest, WeatherResponse> {
    public WeatherResponse apply(WeatherRequest request) {
        return new WeatherResponse(30.0, Unit.C);
    }
}

public enum Unit { C, F }
public record WeatherRequest(String location, Unit unit) {}
public record WeatherResponse(double temp, Unit unit) {}
```

`FunctionToolCallback.Builder` 允许你构建 `FunctionToolCallback` 实例，并提供以下关键信息：

- `name`：工具名称。AI 模型使用它识别工具。名称在同一上下文中必须唯一，在同一次聊天请求可用的所有工具中也必须唯一。**必填**
- `toolFunction`：表示工具逻辑的函数对象（`Function`、`Supplier`、`Consumer` 或 `BiFunction`）。**必填**
- `description`：工具描述。虽然可省略，但强烈建议提供详细描述
- `inputType`：函数输入的类型。**必填**
- `inputSchema`：输入参数的 JSON Schema。如不提供，会根据 `inputType` 自动生成
- `toolMetadata`：额外元数据，例如是否直接返回结果、使用哪个结果转换器等
- `toolCallResultConverter`：把工具结果转换成字符串后发给 AI 模型的转换器。如不提供，将使用默认转换器 `DefaultToolCallResultConverter`

`ToolMetadata.Builder` 用于设置额外元数据，例如：

- `returnDirect`：结果是否直接返回给调用方

```java
ToolCallback toolCallback = FunctionToolCallback
    .builder("currentWeather", new WeatherService())
    .description("Get the weather in location")
    .inputType(WeatherRequest.class)
    .build();
```

函数的输入与输出都可以是 `Void` 或 POJO。POJO 输入输出都必须是可序列化的，因为结果会被序列化后发送回模型。函数本身及其输入、输出类型都必须是 `public` 的。

某些类型不受支持。详见后文 **Function Tool Limitations**。

---

## Adding Tools to ChatClient（向 ChatClient 添加函数工具）

在使用编程式函数工具定义方式时，你可以把 `FunctionToolCallback` 实例传给 `ChatClient` 的 `toolCallbacks()` 方法。该工具仅对当前聊天请求有效。

```java
ToolCallback toolCallback = ...
ChatClient.create(chatModel)
    .prompt("What's the weather like in Copenhagen?")
    .toolCallbacks(toolCallback)
    .call()
    .content();
```

---

## Adding Default Tools to ChatClient（为 ChatClient 添加默认函数工具）

在函数工具的编程式定义方式下，你可以通过 `defaultToolCallbacks()` 把 `FunctionToolCallback` 设置为 `ChatClient.Builder` 的默认工具。

如果同时提供默认工具和运行时工具，则**运行时工具会完全覆盖默认工具**。

```java
ChatModel chatModel = ...
ToolCallback toolCallback = ...
ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultToolCallbacks(toolCallback)
    .build();
```

---

## Adding Tools to ChatModel（向 ChatModel 添加函数工具）

在函数工具的编程式定义方式下，你可以通过 `ToolCallingChatOptions` 的 `toolCallbacks()` 方法把 `FunctionToolCallback` 传给 `ChatModel`。该工具只在当前请求有效。

```java
ChatModel chatModel = ...
ToolCallback toolCallback = ...
ChatOptions chatOptions = ToolCallingChatOptions.builder()
    .toolCallbacks(toolCallback)
    .build();
Prompt prompt = new Prompt("What's the weather like in Copenhagen?", chatOptions);
chatModel.call(prompt);
```

---

## Adding Default Tools to ChatModel（为 ChatModel 添加默认函数工具）

在函数工具的编程式定义方式下，你也可以在创建 `ChatModel` 时把 `FunctionToolCallback` 作为默认工具传入。

如果同时提供默认工具和运行时工具，则**运行时工具会完全覆盖默认工具**。

```java
ToolCallback toolCallback = ...
ChatModel chatModel = OllamaChatModel.builder()
    .ollamaApi(OllamaApi.builder().build())
    .defaultOptions(ToolCallingChatOptions.builder()
        .toolCallbacks(toolCallback)
        .build())
    .build();
```

---

## Dynamic Specification: @Bean（动态定义：@Bean）

你也可以不通过编程方式显式指定工具，而是把工具定义成 Spring Bean，再由 Spring AI 在运行时借助 `ToolCallbackResolver` 接口（具体实现是 `SpringBeanToolCallbackResolver`）动态解析。

这种方式允许你把任意 `Function`、`Supplier`、`Consumer` 或 `BiFunction` Bean 作为工具使用。Bean 名称会作为工具名称；而 Spring Framework 的 `@Description` 注解可以为工具提供描述，以便模型理解工具用途和使用方式。

如果不提供描述，则会使用方法名作为工具描述；但仍然强烈建议你提供详细描述。

```java
@Configuration(proxyBeanMethods = false)
class WeatherTools {

    WeatherService weatherService = new WeatherService();

    @Bean
    @Description("Get the weather in location")
    Function<WeatherRequest, WeatherResponse> currentWeather() {
        return weatherService;
    }

}
```

某些类型仍不支持。详见 **Function Tool Limitations**。

工具输入参数的 JSON Schema 会自动生成。你可以使用 `@ToolParam` 为参数补充描述或必填/可选信息。默认所有参数都是必填。详见 **JSON Schema**。

```java
record WeatherRequest(
    @ToolParam(description = "The name of a city or a country") String location,
    Unit unit
) {}
```

这种工具定义方式的缺点在于：由于工具解析发生在运行时，它**无法保证类型安全**。为了降低这个问题，可以通过 `@Bean` 显式指定 Bean 名，并把它存入常量中，以便在聊天请求里引用，而不是把工具名硬编码为字符串。

```java
@Configuration(proxyBeanMethods = false)
class WeatherTools {

    public static final String CURRENT_WEATHER_TOOL = "currentWeather";

    @Bean(CURRENT_WEATHER_TOOL)
    @Description("Get the weather in location")
    Function<WeatherRequest, WeatherResponse> currentWeather() {
        ...
    }

}
```

---

## Adding Tools to ChatClient（向 ChatClient 添加动态工具）

在动态定义方式下，你可以把工具名称（也就是函数 Bean 的名称）通过 `toolNames()` 方法传给 `ChatClient`。该工具只对当前聊天请求有效。

```java
ChatClient.create(chatModel)
    .prompt("What's the weather like in Copenhagen?")
    .toolNames("currentWeather")
    .call()
    .content();
```

---

## Adding Default Tools to ChatClient（为 ChatClient 添加默认动态工具）

在动态定义方式下，你可以把工具名通过 `defaultToolNames()` 添加到 `ChatClient.Builder` 中作为默认工具。

如果同时提供默认工具与运行时工具，则**运行时工具会完全覆盖默认工具**。

```java
ChatModel chatModel = ...
ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultToolNames("currentWeather")
    .build();
```

---

## Adding Tools to ChatModel（向 ChatModel 添加动态工具）

在动态定义方式下，你可以通过 `ToolCallingChatOptions` 的 `toolNames()` 方法把工具名传给 `ChatModel`。该工具只在当前请求生效。

```java
ChatModel chatModel = ...
ChatOptions chatOptions = ToolCallingChatOptions.builder()
    .toolNames("currentWeather")
    .build();
Prompt prompt = new Prompt("What's the weather like in Copenhagen?", chatOptions);
chatModel.call(prompt);
```

---

## Adding Default Tools to ChatModel（为 ChatModel 添加默认动态工具）

在动态定义方式下，你也可以在创建 `ChatModel` 时，把工具名通过默认选项里的 `toolNames()` 方法设置为默认工具。

如果同时提供默认工具与运行时工具，则**运行时工具会完全覆盖默认工具**。

```java
ChatModel chatModel = OllamaChatModel.builder()
    .ollamaApi(OllamaApi.builder().build())
    .defaultOptions(ToolCallingChatOptions.builder()
        .toolNames("currentWeather")
        .build())
    .build();
```

---

## Function Tool Limitations（函数工具限制）

当前，以下类型**不支持**作为“函数型工具”的输入或输出类型：

- 基本类型（Primitive types）
- `Optional`
- 集合类型（例如 `List`、`Map`、数组、`Set`）
- 异步类型（例如 `CompletableFuture`、`Future`）
- 响应式类型（例如 `Flow`、`Mono`、`Flux`）

如果你需要支持基本类型或集合类型，可以使用“基于方法的工具定义方式”。详见 **Methods as Tools**。

---

## Tool Specification（工具定义规范）

在 Spring AI 中，工具通过 `ToolCallback` 接口来建模。前文中我们已经看到，Spring AI 内置支持从方法和函数出发定义工具（见 **Methods as Tools** 与 **Functions as Tools**）。本节将进一步深入介绍工具定义本身，以及如何对其进行自定义和扩展，以支持更多用例。

---

## Tool Callback

`ToolCallback` 接口为定义一个可由 AI 模型调用的工具提供了统一方式，它同时包含“工具定义”和“执行逻辑”两个方面。当你想从零开始自定义工具时，这是最核心的接口。例如，你可以基于一个 MCP Client（Model Context Protocol）来定义 `ToolCallback`，也可以基于 `ChatClient` 来定义 `ToolCallback`，以构建模块化的智能体应用。

该接口包含以下方法：

```java
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
```

Spring AI 内置提供了两类主要实现：

- `MethodToolCallback`：基于方法
- `FunctionToolCallback`：基于函数

---

## Tool Definition（工具定义）

`ToolDefinition` 接口为 AI 模型提供它所需要了解的工具可用性信息，包括工具名称、描述和输入 Schema。每个 `ToolCallback` 实现都必须提供一个 `ToolDefinition` 实例来定义工具。

该接口包含以下方法：

```java
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
```

关于输入 Schema 的更多内容，详见 **JSON Schema**。

`ToolDefinition.Builder` 可用于使用默认实现类 `DefaultToolDefinition` 构建一个 `ToolDefinition` 实例。

```java
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
```

---

## Method Tool Definition（方法工具定义）

当你从某个方法构建工具时，`ToolDefinition` 会自动为你生成。如果你更希望自己生成 `ToolDefinition`，也可以使用方便的构建器。

```java
Method method = ReflectionUtils.findMethod(DateTimeTools.class, "getCurrentDateTime");
ToolDefinition toolDefinition = ToolDefinitions.from(method);
```

从方法自动生成的 `ToolDefinition` 中：

- 工具名称默认使用方法名
- 工具描述默认使用方法名
- 输入 Schema 则根据方法输入参数生成

如果该方法上标注了 `@Tool`，则会优先使用注解中显式给出的工具名和描述（如果设置了的话）。

详见 **Methods as Tools**。

如果你希望显式指定其中部分或全部属性，可以用 `ToolDefinition.Builder` 构建一个自定义 `ToolDefinition`。

```java
Method method = ReflectionUtils.findMethod(DateTimeTools.class, "getCurrentDateTime");
ToolDefinition toolDefinition = ToolDefinitions.builder(method)
    .name("currentDateTime")
    .description("Get the current date and time in the user's timezone")
    .inputSchema(JsonSchemaGenerator.generateForMethodInput(method))
    .build();
```

---

## Function Tool Definition（函数工具定义）

当你从函数构建工具时，`ToolDefinition` 同样会自动为你生成。当你使用 `FunctionToolCallback.Builder` 构建 `FunctionToolCallback` 时，可以提供工具名称、描述和输入 Schema，这些信息都会被用于生成 `ToolDefinition`。详见 **Functions as Tools**。

---

## JSON Schema

当你把工具提供给 AI 模型时，模型需要知道该工具输入类型的 Schema，以便理解如何调用工具并构造工具请求。Spring AI 内置提供了通过 `JsonSchemaGenerator` 类来生成工具输入类型 JSON Schema 的能力。该 Schema 会作为 `ToolDefinition` 的一部分提供给模型。

关于 `ToolDefinition` 及如何向其中传递输入 Schema，详见 **Tool Definition**。

在底层，`JsonSchemaGenerator` 会被用于自动生成方法或函数输入参数的 JSON Schema；它支持一系列注解，允许你自定义生成后的 Schema。

本节主要介绍两个可自定义的方面：

1. **描述（description）**
2. **必填 / 可选（required / optional）**

---

## Description（参数描述）

除了给工具本身提供描述外，你还可以给工具的输入参数提供描述。这个描述可以帮助说明参数应采用何种格式、允许哪些值等关键信息，从而帮助模型更好地理解输入 Schema 并正确使用该工具。

Spring AI 内置支持通过以下任一注解为输入参数生成描述：

- Spring AI 的 `@ToolParam(description = "...")`
- Jackson 的 `@JsonClassDescription(description = "...")`
- Jackson 的 `@JsonPropertyDescription(description = "...")`
- Swagger 的 `@Schema(description = "...")`

这种方式同时适用于方法和函数，并且可以递归应用于嵌套类型。

```java
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.i18n.LocaleContextHolder;

class DateTimeTools {

    @Tool(description = "Set a user alarm for the given time")
    void setAlarm(@ToolParam(description = "Time in ISO-8601 format") String time) {
        LocalDateTime alarmTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME);
        System.out.println("Alarm set for " + alarmTime);
    }

}
```

---

## Required/Optional（必填 / 可选）

默认情况下，每个输入参数都被视为必填项，这意味着 AI 模型在调用工具时必须为它提供值。不过，你也可以通过以下注解把某个输入参数设为可选；其优先级从高到低如下：

1. Spring AI 的 `@ToolParam(required = false)`
2. Jackson 的 `@JsonProperty(required = false)`
3. Swagger 的 `@Schema(required = false)`
4. Spring Framework 的 `@Nullable`

这种方式同样适用于方法和函数，也可以递归用于嵌套类型。

```java
class CustomerTools {

    @Tool(description = "Update customer information")
    void updateCustomerInfo(Long id, String name, @ToolParam(required = false) String email) {
        System.out.println("Updated info for customer with id: " + id);
    }

}
```

为输入参数定义正确的“必填 / 可选”状态，对于减轻幻觉风险、确保模型在调用工具时提供正确输入非常重要。

以上面的例子为例，`email` 参数是可选的，这意味着模型在调用该工具时，可以不提供该值。如果它被定义为必填，而模型又拿不到真实值，那么模型很可能会“编造一个邮箱”，从而导致幻觉。

> **补充说明**：在工程实践中，参数定义越清晰，模型调用工具时越稳定。尤其是时间格式、枚举值、是否允许为空、是否允许缺省等信息，最好在 Schema 层面说清楚。

---

## Result Conversion（结果转换）

工具调用的结果会通过 `ToolCallResultConverter` 进行序列化，然后发送回 AI 模型。`ToolCallResultConverter` 接口用于把工具调用结果转换为一个 `String`。

该接口定义如下：

```java
@FunctionalInterface
public interface ToolCallResultConverter {

    /**
     * Given an Object returned by a tool, convert it to a String compatible with the
     * given class type.
     */
    String convert(@Nullable Object result, @Nullable Type returnType);

}
```

工具返回值必须是可序列化类型。默认情况下，Spring AI 使用 Jackson 把结果序列化成 JSON（默认实现为 `DefaultToolCallResultConverter`）。不过你也可以提供自己的 `ToolCallResultConverter` 实现，自定义序列化过程。

Spring AI 在方法工具和函数工具中都依赖 `ToolCallResultConverter` 来处理结果。

---

## Method Tool Call Result Conversion（方法工具的结果转换）

在采用声明式方式从方法构建工具时，你可以通过 `@Tool` 注解的 `resultConverter()` 属性，为该工具指定自定义的 `ToolCallResultConverter`。

```java
class CustomerTools {

    @Tool(
        description = "Retrieve customer information",
        resultConverter = CustomToolCallResultConverter.class
    )
    Customer getCustomerInfo(Long id) {
        return customerRepository.findById(id);
    }

}
```

如果采用编程式方式，则可以在 `MethodToolCallback.Builder` 中指定自定义的 `ToolCallResultConverter`。

详见 **Methods as Tools**。

---

## Function Tool Call Result Conversion（函数工具的结果转换）

在使用编程式方式从函数构建工具时，你可以在 `FunctionToolCallback.Builder` 中指定自定义的 `ToolCallResultConverter`。

详见 **Functions as Tools**。

---

## Tool Context（工具上下文）

Spring AI 支持通过 `ToolContext` API 向工具传递附加上下文信息。这个特性使你能够向工具执行逻辑提供额外的、由用户给出的上下文数据，并且这些数据会与 AI 模型传来的工具参数一起在工具内部使用。

```java
class CustomerTools {

    @Tool(description = "Retrieve customer information")
    Customer getCustomerInfo(Long id, ToolContext toolContext) {
        return customerRepository.findById(id, toolContext.getContext().get("tenantId"));
    }

}
```

`ToolContext` 会在调用 `ChatClient` 时，通过用户提供的数据进行填充。

```java
ChatModel chatModel = ...

String response = ChatClient.create(chatModel)
    .prompt("Tell me more about the customer with ID 42")
    .tools(new CustomerTools())
    .toolContext(Map.of("tenantId", "acme"))
    .call()
    .content();

System.out.println(response);
```

`ToolContext` 中提供的任何数据**都不会发送给 AI 模型**。

同样地，在直接调用 `ChatModel` 时，你也可以定义 `toolContext` 数据。

```java
ChatModel chatModel = ...
ToolCallback[] customerTools = ToolCallbacks.from(new CustomerTools());
ChatOptions chatOptions = ToolCallingChatOptions.builder()
    .toolCallbacks(customerTools)
    .toolContext(Map.of("tenantId", "acme"))
    .build();
Prompt prompt = new Prompt("Tell me more about the customer with ID 42", chatOptions);
chatModel.call(prompt);
```

如果 `toolContext` 同时在默认选项和运行时选项中设置，那么最终得到的 `ToolContext` 会是二者合并后的结果，并且**运行时选项优先级更高**。

> **补充说明**：这是工程里非常有用的一个能力。比如租户 ID、用户身份、会话态配置、权限范围等，都可以放进 `ToolContext`，这样工具执行时能拿到，但又不会暴露给模型。

---

## Return Direct（直接返回）

默认情况下，工具调用结果会被发送回模型，然后模型再利用这个结果继续生成后续对话内容。

但在某些情况下，你可能希望**不再把工具结果交回模型**，而是直接把结果返回给调用方。例如：

- 你构建了一个依赖 RAG 工具的智能体，希望检索结果直接返回，而不是再让模型做一层不必要的后处理
- 某些工具本身就意味着推理链结束，希望立即返回结果

每个 `ToolCallback` 实现都可以定义：工具结果是**直接返回给调用方**，还是**发送回模型**。默认行为是发送回模型；你可以按工具粒度修改这一行为。

负责管理工具执行生命周期的 `ToolCallingManager` 会处理工具关联的 `returnDirect` 属性：

- 如果 `returnDirect = true`，则工具结果会直接返回给调用方
- 否则，工具结果会发送回模型

如果一次请求中模型同时发起了多个工具调用，那么只有当**所有工具的 `returnDirect` 都设置为 `true`** 时，结果才会统一直接返回给调用方；否则，结果会发送回模型。

当我们希望把某个工具提供给模型时，需要在聊天请求中包含其定义。如果希望工具执行结果直接返回给调用方，就把 `returnDirect` 设置为 `true`。

当模型决定调用某个工具时，它会返回一个响应，里面包含工具名称和符合 Schema 的输入参数。

应用程序负责根据工具名定位工具，并使用提供的输入参数执行它。

工具调用结果由应用程序处理。

应用程序把工具调用结果**直接返回给调用方**，而不是发送回模型。

---

## Method Return Direct（方法工具的直接返回）

在使用声明式方式从方法构建工具时，你可以通过给 `@Tool` 注解设置 `returnDirect = true`，把该工具标记为“直接返回结果给调用方”。

```java
class CustomerTools {

    @Tool(description = "Retrieve customer information", returnDirect = true)
    Customer getCustomerInfo(Long id) {
        return customerRepository.findById(id);
    }

}
```

如果采用编程式方式，则可以通过 `ToolMetadata` 设置 `returnDirect`，并传给 `MethodToolCallback.Builder`。

```java
ToolMetadata toolMetadata = ToolMetadata.builder()
    .returnDirect(true)
    .build();
```

详见 **Methods as Tools**。

---

## Function Return Direct（函数工具的直接返回）

在使用编程式方式从函数构建工具时，你可以通过 `ToolMetadata` 设置 `returnDirect`，并传给 `FunctionToolCallback.Builder`。

```java
ToolMetadata toolMetadata = ToolMetadata.builder()
    .returnDirect(true)
    .build();
```

详见 **Functions as Tools**。

---

## Tool Execution（工具执行）

工具执行，是指使用给定输入参数调用工具，并返回结果的过程。这个过程由 `ToolCallingManager` 接口负责管理，它承担了工具执行生命周期管理的职责。

```java
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
```

如果你使用的是 Spring AI 的 Spring Boot Starter，那么 `DefaultToolCallingManager` 会作为 `ToolCallingManager` 接口的自动配置实现。

你可以通过提供自己的 `ToolCallingManager` Bean 来定制工具执行行为。

```java
@Bean
ToolCallingManager toolCallingManager() {
    return ToolCallingManager.builder().build();
}
```

默认情况下，Spring AI 会在各个 `ChatModel` 实现内部透明地帮你管理工具执行生命周期。但你也可以选择退出这种默认行为，自己接管工具执行。本节分别介绍这两种场景。

---

## Framework-Controlled Tool Execution（框架控制的工具执行）

在默认行为下，Spring AI 会自动拦截模型发出的工具调用请求，执行对应工具，并把结果返回给模型。整个过程对开发者是透明的，由各个 `ChatModel` 实现通过 `ToolCallingManager` 自动完成。

流程如下：

1. 当我们希望把某个工具提供给模型时，在聊天请求（`Prompt`）中包含该工具定义，并调用 `ChatModel` API，把请求发送给 AI 模型。
2. 当模型决定调用工具时，它会返回一个响应（`ChatResponse`），其中包含工具名称和符合已定义 Schema 的输入参数。
3. `ChatModel` 会把这个工具调用请求发送给 `ToolCallingManager`。
4. `ToolCallingManager` 负责定位要调用的工具，并使用给定参数执行它。
5. 工具调用结果返回给 `ToolCallingManager`。
6. `ToolCallingManager` 再把工具执行结果返回给 `ChatModel`。
7. `ChatModel` 把工具执行结果作为 `ToolResponseMessage` 发送回 AI 模型。
8. AI 模型利用工具调用结果作为额外上下文，生成最终响应，并通过 `ChatClient` 返回给调用方。

当前，工具执行过程中模型与系统之间交换的内部消息**不会暴露给用户**。如果你需要访问这些消息，应使用“用户控制的工具执行方式”。

用于判断“某个工具调用是否应当被执行”的逻辑，由 `ToolExecutionEligibilityPredicate` 接口处理。默认情况下，是否执行工具由以下条件决定：

- `ToolCallingChatOptions` 的 `internalToolExecutionEnabled` 属性是否为 `true`（默认就是 `true`）
- 当前 `ChatResponse` 是否包含工具调用

默认实现如下：

```java
public class DefaultToolExecutionEligibilityPredicate implements ToolExecutionEligibilityPredicate {

    @Override
    public boolean test(ChatOptions promptOptions, ChatResponse chatResponse) {
        return ToolCallingChatOptions.isInternalToolExecutionEnabled(promptOptions)
            && chatResponse != null
            && chatResponse.hasToolCalls();
    }

}
```

你也可以在创建 `ChatModel` Bean 时，提供自己的 `ToolExecutionEligibilityPredicate` 实现，以定制这一逻辑。

---

## User-Controlled Tool Execution（用户控制的工具执行）

有些情况下，你可能希望自行控制整个工具执行生命周期。要做到这一点，可以把 `ToolCallingChatOptions` 的 `internalToolExecutionEnabled` 属性设置为 `false`。

当你以这个选项调用 `ChatModel` 时，工具执行将交由调用方自己负责，你可以完全控制工具执行的全过程。此时，你需要自行检查 `ChatResponse` 中是否包含工具调用，并使用 `ToolCallingManager` 来执行它们。

下面是一个最小可运行示例，展示如何手动管理工具执行：

```java
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
```

当你选择用户控制工具执行时，官方建议仍然使用 `ToolCallingManager` 来管理工具调用相关操作。这样你仍可以复用 Spring AI 已经提供的工具执行能力。当然，你也完全可以自行实现一套工具执行逻辑。

下面是一个“用户控制工具执行 + ChatMemory API”的最小实现示例：

```java
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
    ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(promptWithMemory, chatResponse);
    chatMemory.add(conversationId, toolExecutionResult.conversationHistory()
        .get(toolExecutionResult.conversationHistory().size() - 1));
    promptWithMemory = new Prompt(chatMemory.get(conversationId), chatOptions);
    chatResponse = chatModel.call(promptWithMemory);
    chatMemory.add(conversationId, chatResponse.getResult().getOutput());
}

UserMessage newUserMessage = new UserMessage("What did I ask you earlier?");
chatMemory.add(conversationId, newUserMessage);

ChatResponse newResponse = chatModel.call(new Prompt(chatMemory.get(conversationId)));
```

> **补充说明**：如果你后面要做“多工具编排”“Agent 推理链可视化”“工具执行日志记录”“工具调用审计”等高级能力，通常会更偏向采用用户控制的执行方式，因为可观测性和可插拔性更强。

---

## Exception Handling（异常处理）

当工具调用失败时，异常会被包装并传播为 `ToolExecutionException`，你可以捕获该异常并进行处理。

`ToolExecutionExceptionProcessor` 可用于处理 `ToolExecutionException`，它有两种可能结果：

1. 生成一个错误消息，发送回 AI 模型  
2. 直接抛出异常，由调用方自行处理

其接口定义如下：

```java
@FunctionalInterface
public interface ToolExecutionExceptionProcessor {

    /**
     * Convert an exception thrown by a tool to a String that can be sent back to the AI
     * model or throw an exception to be handled by the caller.
     */
    String process(ToolExecutionException exception);

}
```

如果你使用 Spring AI Spring Boot Starters，那么 `DefaultToolExecutionExceptionProcessor` 会作为 `ToolExecutionExceptionProcessor` 的自动配置实现。

默认情况下：

- `RuntimeException` 的错误消息会被发送回模型
- 受检异常（checked exceptions）以及 `Error`（例如 `IOException`、`OutOfMemoryError`）则始终会被直接抛出

`DefaultToolExecutionExceptionProcessor` 的构造函数允许你设置 `alwaysThrow` 属性：

- 若为 `true`，则抛出异常，而不是把错误消息发送回模型
- 若为 `false`，则按默认策略处理

你还可以通过 `spring.ai.tools.throw-exception-on-error` 属性控制 `DefaultToolExecutionExceptionProcessor` Bean 的行为。

| 属性 | 说明 | 默认值 |
|---|---|---|
| `spring.ai.tools.throw-exception-on-error` | 若为 `true`，则工具调用错误会作为异常抛给调用方处理；若为 `false`，则错误会被转换成消息发送给 AI 模型，由模型继续处理和响应该错误 | `false` |

```java
@Bean
ToolExecutionExceptionProcessor toolExecutionExceptionProcessor() {
    return new DefaultToolExecutionExceptionProcessor(true);
}
```

如果你自定义了 `ToolCallback` 实现，请确保在工具执行逻辑的 `call()` 方法发生错误时抛出 `ToolExecutionException`。

`ToolExecutionExceptionProcessor` 会被默认的 `ToolCallingManager`（即 `DefaultToolCallingManager`）在内部使用，以处理工具执行期间产生的异常。关于工具执行生命周期，详见 **Tool Execution**。

---

## Tool Resolution（工具解析）

把工具提供给模型的主要方式，是在调用 `ChatClient` 或 `ChatModel` 时，直接传入 `ToolCallback`。前文在 **Methods as Tools** 和 **Functions as Tools** 中已经介绍了多种策略。

除此之外，Spring AI 还支持通过 `ToolCallbackResolver` 接口，在运行时动态解析工具。

```java
public interface ToolCallbackResolver {

    /**
     * Resolve the {@link ToolCallback} for the given tool name.
     */
    @Nullable
    ToolCallback resolve(String toolName);

}
```

在这种方式下：

- 在客户端侧，你向 `ChatClient` 或 `ChatModel` 提供的是**工具名称**
- 在服务端侧，由某个 `ToolCallbackResolver` 实现负责把工具名解析为对应的 `ToolCallback` 实例

默认情况下，Spring AI 使用 `DelegatingToolCallbackResolver`，它会把解析工作委托给一组 `ToolCallbackResolver`：

1. `SpringBeanToolCallbackResolver`：从 Spring Bean 中解析工具，支持 `Function`、`Supplier`、`Consumer`、`BiFunction` 类型的 Bean。详见前文 **Dynamic Specification: @Bean**
2. `StaticToolCallbackResolver`：从一组静态的 `ToolCallback` 列表中解析工具。若使用 Spring Boot 自动配置，那么应用上下文中所有 `ToolCallback` 类型的 Bean 都会自动配置进该解析器

如果你依赖 Spring Boot 自动配置，也可以通过提供自己的 `ToolCallbackResolver` Bean，自定义解析逻辑。

```java
@Bean
ToolCallbackResolver toolCallbackResolver(List<FunctionCallback> toolCallbacks) {
    StaticToolCallbackResolver staticToolCallbackResolver = new StaticToolCallbackResolver(toolCallbacks);
    return new DelegatingToolCallbackResolver(List.of(staticToolCallbackResolver));
}
```

`ToolCallbackResolver` 会在内部被 `ToolCallingManager` 用来进行运行时工具解析，无论是框架控制的工具执行还是用户控制的工具执行，都可以使用它。

---

## Observability（可观测性）

工具调用支持可观测性能力，其中包括 `spring.ai.tool` 相关观测指标，可用于衡量工具调用完成时间并传播链路追踪信息。详见 **Tool Calling Observability**。

此外，Spring AI 还可以选择性地把工具调用参数和结果导出为 span attribute；但由于这些数据可能具有敏感性，因此该功能默认关闭。详见 **Tool Call Arguments and Result Data**。

---

## Logging（日志）

工具调用功能中的主要操作都会以 `DEBUG` 级别进行日志记录。你可以把 `org.springframework.ai` 包的日志级别设置为 `DEBUG`，以启用这些日志。

---

## 译文补充总结（便于工程实现理解）

结合 Spring AI 官方文档与 Spring AI Alibaba 的教程页，可以把 Spring AI 中的 Tool Calling 理解为下面这条链路：

1. **定义工具**：可以来自方法、函数或 Spring Bean  
2. **暴露给模型**：把工具定义（名称、描述、参数 Schema）传给模型  
3. **模型决定是否调用**：模型返回工具名与参数  
4. **应用负责执行**：真正执行工具的是你的后端应用，而不是模型  
5. **结果回传或直返**：结果可继续发给模型，也可以直接返回给调用方  
6. **支持上下文、异常、日志、可观测性**：适合做生产级工程系统

对你的项目来说，这一机制特别适合实现如下能力：

- 查询类工具：天气、时间、知识库检索、用户数据读取、课程计划读取
- 动作类工具：创建训练计划、保存用户目标、记录饮食、调用外部服务
- 智能体编排：让模型自动选择“先查再答”或“先算再执行”的流程

如果你后面要做毕业设计或项目文档，通常可以把 Tool Calling 总结为：

> Tool Calling 是 Spring AI 智能体体系中连接“大模型推理能力”和“外部系统执行能力”的核心桥梁。它使模型不再局限于文本生成，而是能够以结构化方式驱动检索、查询与业务操作，从而构建真正可落地的工程化 AI 应用。

---

## 参考说明

- 主体翻译内容基于你上传的 Spring AI 官方文档：`docs.spring.io_spring-ai_reference_api_tools.html.md`
- 补充参考了 Spring AI Alibaba 官方中文教程页 “Tools 工具 / Tool Calling” 的相关说明，用于帮助理解工具分类、用途与工程实践方向

