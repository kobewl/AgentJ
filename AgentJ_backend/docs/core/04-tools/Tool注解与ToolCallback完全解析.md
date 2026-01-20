# @Tool 注解 vs ToolCallback 接口：Spring AI 工具定义完全解析

> 从简单注解到底层接口的完整学习指南（基于 Spring AI 官方文档）

## 📋 核心问题

**Q1**：我以前只使用过 `@Tool` 注解来给 AI 提供工具，`ToolCallback` 是什么？两者有什么关系？

**Q2**：官方文档说有 MethodToolCallback，为什么 AgentJ 代码中没有？

**Q3**：Functions as Tools 是什么意思？

**A**：
- `@Tool` 是**语法糖**，`ToolCallback` 是**本质**
- `@Tool` 注解会被 Spring AI 自动转换为 `MethodToolCallback`
- MethodToolCallback 是 `@Tool` 的底层实现
- AgentJ 使用的是 `FunctionToolCallback`（更适合函数式接口）
- Functions as Tools 指的是将 Java 函数式接口（Function, Supplier, Consumer, BiFunction）包装为工具

---

## 🎯 Spring AI 官方的三种工具定义方式

根据 Spring AI 官方文档，工具定义有三种主要方式：

| 方式 | 英文名称 | 中文名称 | 复杂度 | 灵活性 | 适用场景 |
|------|---------|---------|-------|-------|---------|
| **方式1** | Declarative Specification: @Tool | 声明式注解 | ⭐ 简单 | ⭐⭐ 低 | 快速原型开发 |
| **方式2** | Programmatic Specification: MethodToolCallback | 编程式方法 | ⭐⭐ 中等 | ⭐⭐⭐ 高 | 需要精细控制方法 |
| **方式3** | Functions as Tools: FunctionToolCallback | 函数式工具 | ⭐⭐ 中等 | ⭐⭐⭐ 高 | 函数式接口 |
| **方式4** | Dynamic Specification: @Bean | 动态 Bean | ⭐⭐ 中等 | ⭐⭐⭐ 高 | 运行时动态解析 |

**核心关系**：
```
@Tool 注解
    ↓ (Spring AI 自动扫描)
MethodToolCallback
    ↓ (实现)
ToolCallback 接口 ← 统一接口

Function<Request, Response>
    ↓ (包装为)
FunctionToolCallback
    ↓ (实现)
ToolCallback 接口
```

---

## 📚 方式1：声明式规范 - @Tool 注解（Declarative Specification）

### 1.1 基本用法

**官方文档示例**：

```java
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;
import java.time.LocalDateTime;

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

### 1.2 使用方式

```java
ChatModel chatModel = ...;

// 方式1：传递工具类实例
String response = ChatClient.create(chatModel)
    .prompt("What day is tomorrow?")
    .tools(new DateTimeTools())  // ← 传递工具类
    .call()
    .content();
```

### 1.3 @Tool 注解的属性

根据官方文档，`@Tool` 注解提供以下关键属性：

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | String | 否 | 工具名称。如果不提供，使用方法名。必须在同一请求的所有工具中唯一。 |
| `description` | String | 否 | 工具描述，帮助 AI 理解何时和如何调用工具。强烈建议提供详细描述！ |
| `returnDirect` | boolean | 否 | 是否直接返回结果给调用者，而不是传回给模型。默认 false。 |
| `resultConverter` | Class<? extends ToolCallResultConverter> | 否 | 结果转换器，用于将工具调用结果转换为 String 对象。 |

### 1.4 @ToolParam 参数注解

**官方文档示例**：

```java
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class DateTimeTools {

    @Tool(description = "Set a user alarm for the given time")
    void setAlarm(@ToolParam(description = "Time in ISO-8601 format") String time) {
        LocalDateTime alarmTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME);
        System.out.println("Alarm set for " + alarmTime);
    }
}
```

**@ToolParam 注解的属性**：

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `description` | String | 否 | 参数描述，帮助 AI 理解参数格式和允许的值。 |
| `required` | boolean | 否 | 参数是否必需。默认所有参数都是必需的。 |

**其他支持的注解**（按优先级排序）：
1. `@ToolParam(required = false)` from Spring AI
2. `@JsonProperty(required = false)` from Jackson
3. `@Schema(required = false)` from Swagger
4. `@Nullable` from Spring Framework

### 1.5 底层发生了什么？

```
你写的代码：
┌────────────────────────────────────────┐
│ @Tool(description = "...")            │
│ String getCurrentDateTime() { ... }      │
└────────────────────────────────────────┘
                ↓
Spring AI 自动扫描：
┌────────────────────────────────────────┐
│ 1. 扫描 @Tool 注解                    │
│ 2. 发现 getCurrentDateTime() 方法       │
│ 3. 自动生成 ToolDefinition             │
│ 4. 自动创建 MethodToolCallback  ← 关键！│
│ 5. 包装为 ToolCallback 接口实现          │
└────────────────────────────────────────┘
                ↓
最终得到：
MethodToolCallback (实现了 ToolCallback 接口)
```

**关键发现**：`@Tool` 注解 → `MethodToolCallback` → `ToolCallback` 接口

---

## 🔧 方式2：编程式规范 - MethodToolCallback（Programmatic Specification）

### 2.1 什么是 MethodToolCallback？

**定义**：`MethodToolCallback` 是 Spring AI 提供的编程式工具定义方式，用于将一个**普通方法**包装为工具。

**与 @Tool 的关系**：
- `@Tool` 注解 → Spring AI **自动**创建 `MethodToolCallback`
- 手动编程 → 你**自己**创建 `MethodToolCallback`

**本质**：两者最终都是 `MethodToolCallback`，只是创建方式不同。

### 2.2 MethodToolCallback.Builder 属性

根据官方文档，`MethodToolCallback.Builder` 提供以下配置项：

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `toolDefinition` | ToolDefinition | 是 | 工具定义（名称、描述、输入Schema） |
| `toolMetadata` | ToolMetadata | 否 | 工具元数据（returnDirect、resultConverter） |
| `toolMethod` | Method | 是 | 方法对象（通过反射获取） |
| `toolObject` | Object | 否* | 方法所属对象。如果是静态方法，可以省略。 |
| `toolCallResultConverter` | ToolCallResultConverter | 否 | 结果转换器。默认使用 `DefaultToolCallResultConverter`。 |

### 2.3 使用示例

**官方文档示例**：

```java
import org.springframework.ai.tool.MethodToolCallback;
import org.springframework.ai.tool.ToolDefinition;
import org.springframework.ai.tool.ToolDefinitions;
import org.springframework.util.ReflectionUtils;

class DateTimeTools {
    // 注意：方法没有 @Tool 注解
    String getCurrentDateTime() {
        return LocalDateTime.now()
            .atZone(LocaleContextHolder.getTimeZone().toZoneId())
            .toString();
    }
}

// 手动创建 MethodToolCallback
Method method = ReflectionUtils.findMethod(DateTimeTools.class, "getCurrentDateTime");

ToolCallback toolCallback = MethodToolCallback.builder()
    .toolDefinition(ToolDefinitions.builder(method)
            .description("Get the current date and time in the user's timezone")
            .build())
    .toolMethod(method)              // ← 方法对象
    .toolObject(new DateTimeTools()) // ← 方法所属对象
    .build();

// 使用
ChatClient.create(chatModel)
    .prompt("What day is tomorrow?")
    .toolCallbacks(toolCallback)
    .call()
    .content();
```

### 2.4 静态方法示例

**官方文档示例**：

```java
class DateTimeTools {
    // 静态方法
    static String getCurrentDateTime() {
        return LocalDateTime.now()
            .atZone(LocaleContextHolder.getTimeZone().toZoneId())
            .toString();
    }
}

// 静态方法不需要 toolObject
Method method = ReflectionUtils.findMethod(DateTimeTools.class, "getCurrentDateTime");

ToolCallback toolCallback = MethodToolCallback.builder()
    .toolDefinition(ToolDefinitions.builder(method)
            .description("Get the current date and time")
            .build())
    .toolMethod(method)
    // .toolObject(...) ← 静态方法不需要
    .build();
```

### 2.5 MethodToolCallback vs @Tool

| 维度 | @Tool 注解 | MethodToolCallback |
|------|-----------|-------------------|
| **创建方式** | 自动（Spring AI 扫描） | 手动（编程式构建） |
| **代码量** | 3-5 行 | 10-15 行 |
| **灵活性** | 低（受注解限制） | 高（完全控制） |
| **适用场景** | 快速开发 | 需要精细控制 |
| **底层实现** | 也是 MethodToolCallback | MethodToolCallback |

**结论**：`@Tool` 是 `MethodToolCallback` 的语法糖，两者本质相同。

---

## 🚀 方式3：Functions as Tools - FunctionToolCallback

### 3.1 什么是 Functions as Tools？

**定义**：将 Java 函数式接口（`Function`, `Supplier`, `Consumer`, `BiFunction`）包装为 AI 工具。

**官方定义**：
> Spring AI provides built-in support for specifying tools from functions, either programmatically using the low-level `FunctionToolCallback` implementation or dynamically as `@Bean`(s) resolved at runtime.

**支持的函数式接口**：
- `Function<I, O>`：接受一个输入，返回一个输出
- `Supplier<O>`：无输入，返回一个输出
- `Consumer<I>`：接受一个输入，无返回值
- `BiFunction<I, C, O>`：接受两个输入（I + ToolContext），返回一个输出

### 3.2 FunctionToolCallback.Builder 属性

根据官方文档，`FunctionToolCallback.Builder` 提供以下配置项：

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | String | 是 | 工具名称。必须在同一请求的所有工具中唯一。 |
| `toolFunction` | Function/Supplier/Consumer/BiFunction | 是 | 函数式对象（实现了函数式接口的对象）。 |
| `description` | String | 否 | 工具描述。强烈建议提供详细描述！ |
| `inputType` | Class<?> | 是 | 函数输入类型（Request 类型）。 |
| `inputSchema` | String | 否 | JSON Schema。如果不提供，会自动根据 inputType 生成。 |
| `toolMetadata` | ToolMetadata | 否 | 工具元数据（returnDirect、resultConverter）。 |
| `toolCallResultConverter` | ToolCallResultConverter | 否 | 结果转换器。默认使用 `DefaultToolCallResultConverter`。 |

### 3.3 官方示例：天气查询工具

**定义函数式接口实现**：

```java
// 1. 定义输入输出类型（POJO）
public enum Unit { C, F }

public record WeatherRequest(
    String location,
    Unit unit
) {}

public record WeatherResponse(
    double temp,
    Unit unit
) {}

// 2. 实现 Function 接口
public class WeatherService implements Function<WeatherRequest, WeatherResponse> {
    @Override
    public WeatherResponse apply(WeatherRequest request) {
        // 调用天气 API
        return new WeatherResponse(30.0, Unit.C);
    }
}
```

**包装为 FunctionToolCallback**：

```java
import org.springframework.ai.tool.FunctionToolCallback;

ToolCallback toolCallback = FunctionToolCallback
    .builder("currentWeather", new WeatherService())  // ← 工具名称 + 函数对象
    .description("Get the weather in location")
    .inputType(WeatherRequest.class)  // ← 输入类型
    .build();
```

**使用工具**：

```java
ChatClient.create(chatModel)
    .prompt("What's the weather like in Copenhagen?")
    .toolCallbacks(toolCallback)
    .call()
    .content();
```

### 3.4 支持的四种函数式接口

#### Function<I, O> - 最常用

```java
// 定义
public class MyFunction implements Function<Input, Output> {
    @Override
    public Output apply(Input input) {
        return new Output();
    }
}

// 包装
ToolCallback toolCallback = FunctionToolCallback
    .builder("myTool", new MyFunction())
    .description("My tool description")
    .inputType(Input.class)
    .build();
```

#### Supplier<O> - 无输入

```java
// 定义
public class CurrentTimeSupplier implements Supplier<String> {
    @Override
    public String get() {
        return LocalDateTime.now().toString();
    }
}

// 包装
ToolCallback toolCallback = FunctionToolCallback
    .builder("currentTime", new CurrentTimeSupplier())
    .description("Get current time")
    .inputType(Void.class)  // ← 无输入
    .build();
```

#### Consumer<I> - 无输出

```java
// 定义
public class LogWriter implements Consumer<String> {
    @Override
    public void accept(String message) {
        System.out.println("LOG: " + message);
    }
}

// 包装
ToolCallback toolCallback = FunctionToolCallback
    .builder("logWriter", new LogWriter())
    .description("Write log message")
    .inputType(String.class)
    .build();
```

#### BiFunction<I, ToolContext, O> - 带上下文

```java
// 定义
public class ContextAwareFunction implements BiFunction<Input, ToolContext, Output> {
    @Override
    public Output apply(Input input, ToolContext context) {
        // 可以访问 context
        String userId = context.getContext().get("userId");
        return new Output(userId);
    }
}

// 包装
ToolCallback toolCallback = FunctionToolCallback
    .builder("contextAwareTool", new ContextAwareFunction())
    .description("Tool with context")
    .inputType(Input.class)
    .build();
```

### 3.5 FunctionToolCallback 限制

根据官方文档，以下类型**不支持**作为函数工具的输入或输出：

| 不支持的类型 | 说明 | 替代方案 |
|------------|------|---------|
| **原始类型**（int, long, double等） | 使用包装类 | Integer, Long, Double |
| **Optional** | 不支持 | 使用可空字段 + @Nullable |
| **集合类型**（List, Map, Array, Set） | 不支持 | 使用 Record 或 POJO |
| **异步类型**（CompletableFuture, Future） | 不支持 | 使用同步方法 |
| **响应式类型**（Flow, Mono, Flux） | 不支持 | 使用同步方法 |

**注意**：原始类型和集合类型可以使用 MethodToolCallback（方式2），不支持只是针对 FunctionToolCallback。

---

## 🔌 方式4：动态规范 - @Bean（Dynamic Specification）

### 4.1 什么是 Dynamic Specification？

**定义**：将函数定义为 Spring Bean，让 Spring AI 在运行时动态解析工具。

**特点**：
- 不需要手动创建 `FunctionToolCallback`
- 只需要定义 `@Bean` 方法
- Spring AI 通过 `ToolCallbackResolver` 自动解析
- 支持传递工具名称（`toolNames()`）而不是 `ToolCallback` 对象

### 4.2 官方示例

**定义工具 Bean**：

```java
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.chat.client.advisor.DefaultToolAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;

@Configuration(proxyBeanMethods = false)
class WeatherTools {

    WeatherService weatherService = new WeatherService();

    @Bean
    @Description("Get the weather in location")  // ← 工具描述
    Function<WeatherRequest, WeatherResponse> currentWeather() {
        return weatherService;  // ← 返回 Function 对象
    }
}
```

**使用工具名称**（而不是 ToolCallback 对象）：

```java
ChatClient.create(chatModel)
    .prompt("What's the weather like in Copenhagen?")
    .toolNames("currentWeather")  // ← 只传 Bean 名称
    .call()
    .content();
```

### 4.3 工具名称最佳实践

**问题**：直接使用字符串 "currentWeather" 容易出错

**解决方案**：使用常量存储工具名称

```java
@Configuration(proxyBeanMethods = false)
class WeatherTools {

    public static final String CURRENT_WEATHER_TOOL = "currentWeather";  // ← 常量

    @Bean(CURRENT_WEATHER_TOOL)  // ← 显式指定 Bean 名称
    @Description("Get the weather in location")
    Function<WeatherRequest, WeatherResponse> currentWeather() {
        return weatherService;
    }
}

// 使用常量（类型安全）
ChatClient.create(chatModel)
    .prompt("What's the weather like in Copenhagen?")
    .toolNames(WeatherTools.CURRENT_WEATHER_TOOL)  // ← 使用常量
    .call()
    .content();
```

### 4.4 动态解析原理

```
你的代码：
┌────────────────────────────────────────┐
│ @Bean("currentWeather")                │
│ @Description("Get the weather...")     │
│ Function<WeatherRequest, Response> ... │
└────────────────────────────────────────┘
                ↓
Spring AI 运行时解析：
┌────────────────────────────────────────┐
│ 1. 扫描 @Bean 方法                     │
│ 2. 发现 currentWeather() 返回 Function  │
│ 3. 通过 ToolCallbackResolver 解析       │
│ 4. 自动创建 FunctionToolCallback        │
│ 5. 包装为 ToolCallback 接口实现          │
└────────────────────────────────────────┘
                ↓
SpringBeanToolCallbackResolver:
    根据 Bean 名称查找对应的 Bean
    → 返回 ToolCallback
                ↓
最终得到：
FunctionToolCallback (实现了 ToolCallback 接口)
```

### 4.5 四种 ToolCallbackResolver

根据官方文档，Spring AI 使用 `DelegatingToolCallbackResolver` 委托给多个解析器：

| 解析器 | 作用 | 示例 |
|--------|------|------|
| **SpringBeanToolCallbackResolver** | 从 Spring Bean 解析 Function/Supplier/Consumer/BiFunction | @Bean + @Description |
| **StaticToolCallbackResolver** | 从静态列表解析 ToolCallback | 自动扫描所有 ToolCallback Bean |

**默认配置**：
- Spring Boot Autoconfiguration 自动配置 `DelegatingToolCallbackResolver`
- 自动扫描所有 `@Bean` 方法定义的函数
- 自动扫描所有 `ToolCallback` 类型的 Bean

---

## 🎯 核心概念对比

### 5.1 四种方式完整对比

| 维度 | @Tool | MethodToolCallback | FunctionToolCallback | @Bean (Dynamic) |
|------|-----------|-------------------|---------------------|----------------|
| **类型** | 声明式注解 | 编程式构建 | 编程式构建 | 动态解析 |
| **级别** | 高层 API | 低层 API | 低层 API | 高层 API |
| **复杂度** | ⭐ 简单 | ⭐⭐⭐ 中等 | ⭐⭐ 中等 | ⭐⭐ 中等 |
| **灵活性** | ⭐ 低 | ⭐⭐⭐⭐ 最高 | ⭐⭐⭐ 高 | ⭐⭐⭐ 高 |
| **代码量** | 3-5 行 | 10-15 行 | 10-15 行 | 5-8 行 |
| **自动生成** | 是（MethodToolCallback） | 否 | 否 | 是（FunctionToolCallback） |
| **适用场景** | 快速原型 | 精细控制方法 | 函数式接口 | 运行时动态 |

### 5.2 选择决策树

```
需要定义工具
    ↓
工具逻辑是否在普通方法中？
    ├─ 是 → 使用 @Tool（最简单）
    │         或 MethodToolCallback（需要精细控制）
    └─ 否 → 继续
         ↓
        是否实现了函数式接口？
        ├─ 是 → 使用 FunctionToolCallback
        │         或 @Bean（如果希望动态解析）
        └─ 否 → 继续
             ↓
            是否需要运行时动态解析？
            ├─ 是 → 使用 @Bean + toolNames()
            └─ 否 → 直接实现 ToolCallback 接口
```

---

## 🌉 AgentJ 的选择：为什么没有 MethodToolCallback？

### 6.1 代码搜索结果

**Grep 搜索结果**：
```
搜索 MethodToolCallback：
- AgentJ_backend\docs\PlanningFactory核心解析-为什么需要三种工具创建方式.md
- AgentJ_backend\docs\Tool注解与ToolCallback完全解析.md
```

**结论**：AgentJ 的 Java 代码中**没有使用** `MethodToolCallback`！

### 6.2 为什么 AgentJ 不使用 MethodToolCallback？

**原因1：AgentJ 使用的是函数式接口**

```java
// AgentJ 的工具接口
public interface ToolCallBiFunctionDef<I> extends BiFunction<I, ToolContext, ToolExecuteResult> {
    // extends BiFunction ← 这是函数式接口！
    ToolExecuteResult apply(I input, ToolContext context);
}
```

- `MethodToolCallback`：用于包装**普通方法**
- `FunctionToolCallback`：用于包装**函数式接口**（Function, Supplier, Consumer, BiFunction）
- AgentJ 的 `ToolCallBiFunctionDef` 继承了 `BiFunction`，所以应该用 `FunctionToolCallback`

**原因2：PlanningFactory 使用的是 FunctionToolCallback**

```java
// PlanningFactory.java (316-322行)
FunctionToolCallback<?, ToolExecuteResult> functionToolcallback =
    FunctionToolCallback.builder(qualifiedKey, dbTool)  // ← FunctionToolCallback
        .description(dbTool.getDescriptionWithServiceGroup())
        .inputSchema(dbTool.getParameters())
        .inputType(dbTool.getInputType())
        .toolMetadata(ToolMetadata.builder()
            .returnDirect(dbTool.isReturnDirect())
            .build())
        .build();
```

**原因3：MethodToolCallback 不适合 AgentJ 的需求**

| AgentJ 需求 | MethodToolCallback 能否满足 | FunctionToolCallback 能否满足 |
|------------|--------------------------|----------------------------|
| 动态上下文管理（planId） | ❌ | ✅ |
| 状态管理（lastCommand） | ❌ | ✅ |
| 资源清理（cleanup） | ❌ | ✅ |
| 环境数据注入（getCurrentToolStateString） | ❌ | ✅ |

**原因4：ToolCallBiFunctionDef 是自定义接口，不是普通方法**

```java
// MethodToolCallback 需要的是普通方法
class MyTools {
    String myMethod(String input) {  // ← 普通方法
        return "result";
    }
}

// AgentJ 的工具实现了自定义接口
public class DatabaseReadTool extends AbstractBaseTool<DatabaseRequest> {
    @Override
    public ToolExecuteResult run(DatabaseRequest input) {  // ← 自定义接口方法
        // ...
    }

    @Override
    public ToolExecuteResult apply(DatabaseRequest input, ToolContext context) {
        // ← BiFunction 接口方法
    }
}
```

### 6.3 AgentJ 的工具定义方式

**完整流程**：

```java
// 1. 定义工具（实现 ToolCallBiFunctionDef）
public class DatabaseReadTool extends AbstractBaseTool<DatabaseRequest> {

    @Override
    public String getName() {
        return "database_read_use";
    }

    @Override
    public String getDescription() {
        return "Execute SQL queries";
    }

    @Override
    public ToolExecuteResult run(DatabaseRequest request) {
        // 执行逻辑
    }

    // BiFunction 接口方法
    @Override
    public ToolExecuteResult apply(DatabaseRequest input, ToolContext context) {
        return run(input);
    }
}

// 2. 在 PlanningFactory 中包装为 FunctionToolCallback
DatabaseReadTool dbTool = DatabaseReadTool.getInstance(...);

FunctionToolCallback<?, ToolExecuteResult> functionToolcallback =
    FunctionToolCallback.builder("database_read_use__0", dbTool)
        .description(dbTool.getDescriptionWithServiceGroup())
        .inputSchema(dbTool.getParameters())
        .inputType(DatabaseRequest.class)
        .build();

// 3. 现在是一个 ToolCallback 接口实现
// 可以传递给 ChatClient
ChatClient.create(chatModel)
    .toolCallbacks(functionToolcallback)
    .call();
```

### 6.4 总结：AgentJ 为什么选择 FunctionToolCallback

| 决策因素 | 说明 |
|---------|------|
| **接口类型** | `ToolCallBiFunctionDef` 继承了 `BiFunction`，是函数式接口 |
| **灵活性** | `FunctionToolCallback` 支持自定义接口实现 |
| **扩展性** | `ToolCallBiFunctionDef` 添加了 AgentJ 特有的方法（setCurrentPlanId, cleanup等） |
| **不使用 @Tool** | `@Tool` → `MethodToolCallback`，不适合函数式接口 |
| **不使用 MethodToolCallback** | 需要反射获取 Method 对象，不适合自定义接口 |

**结论**：AgentJ 选择 `FunctionToolCallback` 是因为工具实现了 `BiFunction` 接口，这是最自然的选择。

---

## 🔍 深度解析：为什么 AgentJ 选择函数式接口？

### 1.1 核心代码分析

**ToolCallBiFunctionDef 接口定义**：
```java
public interface ToolCallBiFunctionDef<I> extends BiFunction<I, ToolContext, ToolExecuteResult> {
    // BiFunction 接口方法（继承自 java.util.function.BiFunction）
    ToolExecuteResult apply(I input, ToolContext context);

    // AgentJ 的业务方法
    ToolExecuteResult run(I input);
}
```

**AbstractBaseTool 的实现**：
```java
public abstract class AbstractBaseTool<I> implements ToolCallBiFunctionDef<I> {

    protected String currentPlanId;   // ← 状态管理
    protected String rootPlanId;     // ← 状态管理

    // BiFunction 接口方法的实现
    @Override
    public ToolExecuteResult apply(I input, ToolContext toolContext) {
        return run(input);  // ← 委托给 run() 方法
    }

    // 子类需要实现的业务方法
    public abstract ToolExecuteResult run(I input);
}
```

**工具类的实现示例**：
```java
public class DatabaseReadTool extends AbstractBaseTool<DatabaseRequest> {

    @Override
    public ToolExecuteResult run(DatabaseRequest request) {
        // 执行数据库查询
        log.info("[{}] Executing query: {}", currentPlanId, request.getQuery());
        return executeQuery(request);
    }
}
```

---

### 1.2 使用函数式接口的五大原因

#### 原因1：完美匹配 Spring AI 的 FunctionToolCallback

**Spring AI 的 FunctionToolCallback 需要**：
```java
// FunctionToolCallback 支持函数式接口：
Function<I, O>              // 1个输入
Supplier<O>                 // 0个输入
Consumer<I>                 // 1个输入，无返回值
BiFunction<I, ToolContext, O>  // 2个输入（包括 ToolContext）← AgentJ 使用
```

**AgentJ 的 ToolCallBiFunctionDef 正好匹配**：
```java
public interface ToolCallBiFunctionDef<I> extends BiFunction<I, ToolContext, ToolExecuteResult> {
    ToolExecuteResult apply(I input, ToolContext context);
}
```

**包装为 ToolCallback**：
```java
// ✅ 函数式接口 - 代码简洁
FunctionToolCallback<?, ToolExecuteResult> functionToolCallback =
    FunctionToolCallback.builder(qualifiedKey, dbTool)  // ← dbTool 是 BiFunction
        .description(dbTool.getDescription())
        .inputType(DatabaseRequest.class)
        .build();
```

**对比 MethodToolCallback（如果使用普通方法）**：
```java
// ❌ 普通方法 - 需要反射，代码复杂
Method method = ReflectionUtils.findMethod(DatabaseReadTool.class, "run");
ToolCallback toolCallback = MethodToolCallback.builder()
    .toolMethod(method)
    .toolObject(dbTool)
    .build();
```

---

#### 原因2：支持两个输入（输入参数 + ToolContext）

**BiFunction 的优势**：
```java
// BiFunction<I, ToolContext, O> 支持两个输入：
// 1. I input - 工具输入参数（AI 传入）
// 2. ToolContext context - 额外上下文（用户传入）

@Override
public ToolExecuteResult apply(I input, ToolContext toolContext) {
    // 可以使用 toolContext 获取额外信息
    String userId = toolContext.getContext().get("userId");
    String tenantId = toolContext.getContext().get("tenantId");

    // 使用上下文执行工具
    return runWithContext(input, userId, tenantId);
}
```

**ToolContext 的实际用途**：
```java
// ChatClient 调用时传递上下文
ChatClient.create(chatModel)
    .prompt("查询所有用户")
    .toolCallbacks(toolCallbacks)
    .toolContext(Map.of(
        "userId", "user123",
        "tenantId", "acme"
    ))
    .call();
```

**对比普通方法（@Tool 注解）**：
```java
// ❌ 普通方法（@Tool 注解）
@Tool(description = "Execute query")
String executeQuery(String sql) {
    // 无法获取 ToolContext
    // 无法获取 userId、tenantId 等额外信息
    return database.query(sql);
}
```

---

#### 原因3：状态管理能力

**函数式接口允许工具维护状态**：
```java
public abstract class AbstractBaseTool<I> implements ToolCallBiFunctionDef<I> {

    protected String currentPlanId;   // ← 状态
    protected String rootPlanId;     // ← 状态

    // 可以在执行前设置状态
    @Override
    public void setCurrentPlanId(String planId) {
        this.currentPlanId = planId;
    }

    // 可以在执行中使用状态
    @Override
    public ToolExecuteResult run(I input) {
        log.info("[{}] Executing tool", currentPlanId);
        // 执行逻辑...
    }
}
```

**实际示例：Bash 工具的状态管理**：
```java
public class Bash extends AbstractBaseTool<BashInput> {

    private String lastCommand = "";   // ← 记住上次命令
    private String lastResult = "";   // ← 记住上次结果

    @Override
    public ToolExecuteResult run(BashInput input) {
        this.lastCommand = input.getCommand();  // ← 更新状态
        List<String> result = executor.execute(input.getCommand());
        this.lastResult = String.join("\n", result);  // ← 更新状态
        return new ToolExecuteResult(objectMapper.writeValueAsString(result));
    }

    @Override
    public String getCurrentToolStateString() {
        return String.format("""
            Last Command: %s
            Last Result: %s
            """, lastCommand, lastResult);
    }
}
```

**对比普通方法（@Tool 注解）**：
```java
// ❌ 普通方法（@Tool 注解）
@Tool(description = "Execute bash command")
String executeCommand(String command) {
    // ❌ 无状态，无法记住上次执行
    return bash.execute(command);
}
```

---

#### 原因4：生命周期控制

**函数式接口支持生命周期管理**：
```java
public interface ToolCallBiFunctionDef<I> extends BiFunction<I, ToolContext, ToolExecuteResult> {

    // ========== 生命周期方法 ==========
    // 执行前：设置上下文
    void setCurrentPlanId(String planId);
    void setRootPlanId(String rootPlanId);

    // 执行后：清理资源
    void cleanup(String planId);

    // 状态查询
    String getCurrentToolStateString();
}
```

**实际示例：BrowserUseTool 的资源清理**：
```java
public class BrowserUseTool extends AbstractBaseTool<BrowserRequest> {

    private final ChromeDriverService chromeDriverService;

    @Override
    public ToolExecuteResult run(BrowserRequest request) {
        // 打开浏览器
        ChromeDriver driver = chromeDriverService.getDriver(currentPlanId);
        driver.get(request.getUrl());
        return new ToolExecuteResult("Browser opened");
    }

    @Override
    public void cleanup(String planId) {
        // ✅ 执行完成后清理浏览器资源
        log.info("Cleaning up Chrome resources for plan: {}", planId);
        this.chromeDriverService.closeDriverForPlan(planId);
    }
}
```

**DynamicAgent 自动调用 cleanup**：
```java
// DynamicAgent 执行完成后
public ChatResponse execute() {
    try {
        // 执行工具...
        return response;
    } finally {
        // 自动调用所有工具的 cleanup 方法
        for (ToolCallBiFunctionDef<?> tool : tools) {
            tool.cleanup(planId);
        }
    }
}
```

**对比普通方法（@Tool 注解）**：
```java
// ❌ 普通方法（@Tool 注解）
@Tool(description = "Automate browser")
String automateBrowser(String url) {
    Browser browser = new Browser();
    browser.open(url);
    // ❌ 无法在执行完成后清理资源
    return "Done";
}
```

---

#### 原因5：与 Spring AI 框架无缝集成

**完整的集成流程**：
```java
// 1. AgentJ 的工具（实现了 BiFunction）
public class DatabaseReadTool extends AbstractBaseTool<DatabaseRequest> {
    @Override
    public ToolExecuteResult run(DatabaseRequest request) {
        return executeQuery(request);
    }
}

// 2. PlanningFactory 包装为 FunctionToolCallback
FunctionToolCallback<?, ToolExecuteResult> functionToolCallback =
    FunctionToolCallback.builder("database_read_use__0", dbTool)  // ← dbTool 是 BiFunction
        .description(dbTool.getDescription())
        .inputSchema(dbTool.getParameters())
        .inputType(DatabaseRequest.class)
        .build();

// 3. 现在 functionToolCallback 是一个 ToolCallback
// 可以传递给 ChatClient
ChatClient.create(chatModel)
    .prompt("查询所有用户")
    .toolCallbacks(functionToolCallback)
    .call();
```

**Spring AI 的调用链**：
```
ChatClient.toolCallbacks(functionToolCallback)
    ↓
Spring AI 调用 functionToolCallback.apply(input, context)
    ↓
FunctionToolCallback 内部调用 toolFunction.apply(input, context)
    ↓
AgentJ 的 DatabaseReadTool.apply(input, context)
    ↓
AbstractBaseTool.apply(input, context) 调用 run(input)
    ↓
DatabaseReadTool.run(input) 执行业务逻辑
```

---

### 1.3 对比总结：函数式接口 vs 普通方法 vs MethodToolCallback

| 维度 | 函数式接口 (AgentJ) | 普通方法 (@Tool) | MethodToolCallback |
|------|-------------------|-----------------|-------------------|
| **输入灵活性** | ✅ 支持两个输入（I + ToolContext） | ❌ 只支持固定参数 | ❌ 只支持固定参数 |
| **状态管理** | ✅ 可以维护状态（字段） | ❌ 无状态 | ❌ 无状态 |
| **生命周期** | ✅ 支持 cleanup、setCurrentPlanId 等方法 | ❌ 无法控制 | ❌ 无法控制 |
| **Spring AI 集成** | ✅ 直接用 FunctionToolCallback | ✅ 自动转换（@Tool → MethodToolCallback） | ⚠️ 需要手动构建 |
| **代码简洁性** | ✅ 简洁（FunctionToolCallback.builder()） | ✅ 最简洁（@Tool） | ❌ 复杂（反射 + builder） |
| **扩展性** | ✅ 可以添加自定义方法 | ❌ 受注解限制 | ❌ 受反射限制 |
| **类型安全** | ✅ 编译时类型检查 | ✅ 编译时类型检查 | ⚠️ 运行时反射 |
| **适用场景** | 企业级应用（复杂需求） | 快速原型（简单需求） | 需要精细控制方法调用 |

---

### 1.4 设计哲学总结

**AgentJ 的选择**：
```
工具接口设计
    ↓
extends BiFunction<I, ToolContext, ToolExecuteResult>
    ↓
核心优势：
1. 支持两个输入（参数 + 上下文）
2. 支持状态管理（currentPlanId、lastQuery 等）
3. 支持生命周期控制（cleanup、setCurrentPlanId 等）
4. 完美匹配 Spring AI 的 FunctionToolCallback
    ↓
可以直接包装为 FunctionToolCallback
    ↓
传递给 ChatClient 使用
```

**为什么不使用普通方法（@Tool 注解）**：
```
普通方法（@Tool 注解）
    ↓
1. 只能有一个输入（固定参数）
2. 无状态，无法维护上下文
3. 无法控制生命周期
4. 无法访问 ToolContext
    ↓
无法满足 AgentJ 的复杂需求：
- 需要动态上下文（planId、rootPlanId）
- 需要状态管理（lastCommand、lastResult）
- 需要资源清理（cleanup）
- 需要环境数据注入（getCurrentToolStateString）
```

**为什么不使用 MethodToolCallback**：
```
MethodToolCallback（包装普通方法）
    ↓
1. 需要反射获取 Method 对象
2. 不适合自定义接口
3. 无法添加额外的生命周期方法
    ↓
AgentJ 的 ToolCallBiFunctionDef 是自定义接口
- 需要添加 setCurrentPlanId() 方法
- 需要添加 cleanup() 方法
- 需要添加 getCurrentToolStateString() 方法
    ↓
无法用 MethodToolCallback 包装
```

---

### 1.5 最终结论

**AgentJ 选择函数式接口（`BiFunction<I, ToolContext, ToolExecuteResult>`）的五大理由**：

1. **最灵活**：支持两个输入（参数 + ToolContext）
2. **最强大**：支持状态管理和生命周期控制
3. **最简洁**：直接用 `FunctionToolCallback` 包装，无需反射
4. **最完美**：与 Spring AI 框架无缝集成
5. **最实用**：满足企业级应用的所有复杂需求

**这是一个经过深思熟虑的架构设计选择！** 🎯

---

## 🎯 实战：ToolCallback 怎么使用？

### 使用场景1：将 AgentJ 工具传递给 ChatClient

```java
// 1. 从 PlanningFactory 获取工具
Map<String, ToolCallBackContext> toolCallbackMap =
    planningFactory.toolCallbackMap(planId, rootPlanId, expectedReturnInfo);

// 2. 提取 ToolCallback[]
ToolCallback[] toolCallbacks = toolCallbackMap.values().stream()
    .map(context -> context.getToolCallback())
    .toArray(ToolCallback[]::new);

// 3. 创建 ChatClient
ChatClient chatClient = ChatClient.create(chatModel)
    .prompt("查询所有用户")
    .toolCallbacks(toolCallbacks)
    .call();
```

### 使用场景2：动态解析工具（@Bean）

```java
@Configuration
class WeatherTools {
    @Bean("currentWeather")
    @Description("Get the weather in location")
    Function<WeatherRequest, WeatherResponse> currentWeather() {
        return new WeatherService();
    }
}

// 使用：传递工具名称
ChatClient.create(chatModel)
    .prompt("What's the weather like in Copenhagen?")
    .toolNames("currentWeather")
    .call();
```

---

## 📝 学习路径

### 阶段1：理解 @Tool 注解（1-2天）
- @Tool 和 @ToolParam 注解
- 工具描述编写技巧
- ChatClient 集成

### 阶段2：理解 ToolCallback 接口（2-3天）
- ToolDefinition 和 ToolMetadata
- MethodToolCallback vs FunctionToolCallback
- 编程式工具定义

### 阶段3：深入 AgentJ 的 ToolCallBiFunctionDef（3-4天）
- ToolCallBiFunctionDef 接口设计
- AbstractBaseTool 基类实现
- 工具生命周期管理
- 环境数据收集机制

---

## 🚀 总结

### 核心关系图

```
┌─────────────────────────────────────────────────────────────┐
│                   Spring AI 工具定义全景图                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  @Tool 注解 (方式1)                                          │
│      ↓ (自动转换)                                            │
│  MethodToolCallback ────┐                                   │
│                           │                                  │
│  Function/Supplier/      │   实现                             │
│  Consumer/BiFunction ────┼──► ToolCallback 接口 ◄── 统一接口 │
│                           │          ▲                       │
│  AgentJ:                  │          │                        │
│  ToolCallBiFunctionDef ───┴── FunctionToolCallback          │
│      (包装为)                                                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 四种方式选择指南

| 场景 | 推荐方式 | 原因 |
|------|---------|------|
| **快速原型开发** | @Tool 注解 | 代码最少，最快上手 |
| **简单工具（无状态）** | @Tool 注解 | 不需要复杂功能 |
| **需要精细控制方法** | MethodToolCallback | 可以精确控制方法调用 |
| **函数式接口** | FunctionToolCallback | 支持 Function/Supplier/Consumer/BiFunction |
| **运行时动态解析** | @Bean + toolNames() | 支持动态工具注册 |
| **企业级应用** | AgentJ 方式 | 需要状态管理、资源清理、动态上下文 |

### 关键要点

1. **@Tool 是语法糖**：自动转换为 MethodToolCallback
2. **MethodToolCallback 包装方法**：通过反射调用普通方法
3. **FunctionToolCallback 包装函数**：包装函数式接口
4. **ToolCallback 是统一接口**：所有工具定义最终都实现这个接口
5. **AgentJ 选择 FunctionToolCallback**：因为 ToolCallBiFunctionDef 继承了 BiFunction

---

## 🎓 面试要点

### Q1：@Tool 和 ToolCallback 有什么区别？

**A**：
- `@Tool` 是**注解**（声明式），`ToolCallback` 是**接口**（编程式）
- `@Tool` 会被自动转换为 `MethodToolCallback`
- `MethodToolCallback` 实现了 `ToolCallback` 接口
- `@Tool` 简单但不够灵活，`ToolCallback` 复杂但功能强大

### Q2：MethodToolCallback 和 FunctionToolCallback 有什么区别？

**A**：
| 维度 | MethodToolCallback | FunctionToolCallback |
|------|-------------------|---------------------|
| **包装对象** | 普通方法 | 函数式接口（Function/Supplier/Consumer/BiFunction） |
| **创建方式** | 反射获取 Method 对象 | 直接传入函数对象 |
| **适用场景** | 普通类的方法 | 实现了函数式接口的类 |
| **支持类型** | 原始类型、集合类型 | 不支持原始类型和集合类型 |

### Q3：为什么 AgentJ 代码中没有 MethodToolCallback？

**A**：
1. **AgentJ 使用函数式接口**：`ToolCallBiFunctionDef extends BiFunction`
2. **应该用 FunctionToolCallback**：用于包装函数式接口，而不是普通方法
3. **MethodToolCallback 不适合**：需要反射获取 Method 对象，不适合自定义接口
4. **PlanningFactory 使用的是**：`FunctionToolCallback.builder(qualifiedKey, dbTool)`

### Q4：Functions as Tools 是什么意思？

**A**：
- **定义**：将 Java 函数式接口包装为 AI 工具
- **支持的接口**：Function、Supplier、Consumer、BiFunction
- **实现方式**：使用 `FunctionToolCallback.builder()`
- **动态方式**：使用 `@Bean` + `@Description`，通过 `toolNames()` 调用
- **限制**：不支持原始类型、集合类型、异步类型、响应式类型

### Q5：四种工具定义方式的优先级？

**A**：
1. **最简单**：@Tool 注解（快速开发）
2. **最灵活**：自定义 ToolCallback（完全控制）
3. **函数式**：FunctionToolCallback（函数式接口）
4. **动态解析**：@Bean + toolNames()（运行时解析）

---

**总结**：Spring AI 提供了四种工具定义方式，每种方式都有其适用场景。`@Tool` 注解最简单，`MethodToolCallback` 用于精细控制方法，`FunctionToolCallback` 用于函数式接口，`@Bean` 用于动态解析。AgentJ 选择 `FunctionToolCallback` 是因为工具实现了 `BiFunction` 接口，这是最自然的选择。理解这四种方式的区别和适用场景，是掌握 Spring AI 工具系统的关键！🎯
