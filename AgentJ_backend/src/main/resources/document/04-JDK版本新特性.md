# JDK 重要版本新特性概览

## Java 8 (LTS)

Java 8 是一个里程碑式的版本，引入了大量革命性特性，极大地提升了开发效率和代码简洁性。

### 1.1 Lambda 表达式

### 1.1.1 基本语法

```ascii
Lambda 表达式结构：
┌─────────────────────────┐
│ (参数) -> { 表达式 }    │
└─────────────────────────┘

简化形式：
1. 单参数无括号：  x -> x * x
2. 无参数空括号：  () -> 42
3. 多参数要括号：  (x, y) -> x + y
```

### 1.1.2 函数式接口

```java
// 1. 内置函数式接口
Predicate<Integer> isPositive = x -> x > 0;
Consumer<String> printer = x -> System.out.println(x);
Function<String, Integer> lengthFunc = x -> x.length();
Supplier<Double> random = () -> Math.random();

// 2. 自定义函数式接口
@FunctionalInterface
interface Calculator {
    int calculate(int x, int y);
}

Calculator add = (x, y) -> x + y;
Calculator multiply = (x, y) -> x * y;
```

## 1.2 Stream API

### 1.2.1 创建流

```java
// 1. 从集合创建
List<String> list = Arrays.asList("a", "b", "c");
Stream<String> stream1 = list.stream();

// 2. 从数组创建
String[] array = {"a", "b", "c"};
Stream<String> stream2 = Arrays.stream(array);

// 3. 使用 Stream.of
Stream<Integer> stream3 = Stream.of(1, 2, 3);

// 4. 无限流
Stream<Integer> infiniteStream = Stream.iterate(0, n -> n + 2);
```

### 1.2.2 中间操作

```ascii
Stream 操作流程：
源数据 ──→ [过滤] ──→ [转换] ──→ [排序] ──→ 结果
         filter    map     sorted   collect

┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ 原始数据      │ ──→│ 中间操作     │ ──→│ 终止操作     │
└─────────────┘    └─────────────┘    └─────────────┘
```

```java
List<String> result = list.stream()
    .filter(s -> s.length() > 3)    // 过滤
    .map(String::toUpperCase)       // 转换
    .sorted()                       // 排序
    .collect(Collectors.toList());  // 收集
```

### 1.2.3 终止操作

```java
// 1. 收集
Map<Boolean, List<Integer>> groups = numbers.stream()
    .collect(Collectors.partitioningBy(n -> n > 0));

// 2. 归约
int sum = numbers.stream()
    .reduce(0, Integer::sum);

// 3. 匹配
boolean allPositive = numbers.stream()
    .allMatch(n -> n > 0);

// 4. 查找
Optional<Integer> first = numbers.stream()
    .findFirst();
```

## 1.3 Optional 类

### 1.3.1 创建 Optional

```java
// 1. 空的 Optional
Optional<String> empty = Optional.empty();

// 2. 非空值的 Optional
Optional<String> opt = Optional.of("Hello");

// 3. 可能为空的 Optional
Optional<String> nullable = Optional.ofNullable(null);
```

### 1.3.2 使用 Optional

```java
// 1. 安全获取值
Optional<String> name = Optional.ofNullable(getName());
String result = name.orElse("Unknown");

// 2. 条件执行
name.ifPresent(System.out::println);

// 3. 转换值
Optional<Integer> length = name.map(String::length);

// 4. 链式调用
String value = Optional.ofNullable(user)
    .map(User::getAddress)
    .map(Address::getCity)
    .orElse("Unknown");
```

## 1.4 新的日期时间 API

### 1.4.1 主要类

```ascii
日期时间类关系：
┌─────────────┐
│ LocalDate   │──→ 日期（2024-01-01）
├─────────────┤
│ LocalTime   │──→ 时间（12:30:45）
├─────────────┤
│ LocalDateTime│──→ 日期时间（2024-01-01T12:30:45）
├─────────────┤
│ ZonedDateTime│──→ 带时区的日期时间
└─────────────┘
```

### 1.4.2 使用示例

```java
// 1. 创建日期时间
LocalDate date = LocalDate.now();
LocalTime time = LocalTime.now();
LocalDateTime dateTime = LocalDateTime.now();

// 2. 日期计算
LocalDate tomorrow = date.plusDays(1);
LocalDate nextMonth = date.plusMonths(1);

// 3. 日期比较
boolean isBefore = date1.isBefore(date2);
Period period = Period.between(date1, date2);

// 4. 格式化
DateTimeFormatter formatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
String formatted = dateTime.format(formatter);
```

## 1.5 接口默认方法

### 1.5.1 默认方法

```java
interface Vehicle {
    // 默认方法
    default void start() {
        System.out.println("Vehicle starting");
    }

    // 静态方法
    static int getWheels() {
        return 4;
    }
}

class Car implements Vehicle {
    // 可以选择重写默认方法
    @Override
    public void start() {
        System.out.println("Car starting");
    }
}
```

### 1.5.2 多重继承问题

```java
interface A {
    default void foo() { System.out.println("A"); }
}

interface B {
    default void foo() { System.out.println("B"); }
}

// 必须解决冲突
class C implements A, B {
    @Override
    public void foo() {
        A.super.foo();  // 选择调用 A 的实现
    }
}
```

## 1.6 方法引用

### 1.6.1 四种类型

```ascii
方法引用类型：
┌─────────────────┐
│ 静态方法引用    │ Class::staticMethod
├─────────────────┤
│ 实例方法引用    │ object::instanceMethod
├─────────────────┤
│ 类方法引用      │ Class::instanceMethod
├─────────────────┤
│ 构造方法引用    │ Class::new
└─────────────────┘
```

### 1.6.2 使用示例

```java
// 1. 静态方法引用
Function<String, Integer> parser = Integer::parseInt;

// 2. 实例方法引用
String str = "Hello";
Supplier<Integer> lengthSupplier = str::length;

// 3. 类方法引用
Function<String, String> upper = String::toUpperCase;

// 4. 构造方法引用
Supplier<ArrayList<String>> listCreator = ArrayList::new;
```

## 1.7 并行流

### 1.7.1 创建并行流

```java
// 1. 从集合创建
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
Stream<Integer> parallelStream = numbers.parallelStream();

// 2. 将顺序流转换为并行流
Stream<Integer> parallel = numbers.stream().parallel();
```

### 1.7.2 性能考虑

```ascii
适合并行的场景：
┌─────────────────┐
│ 数据量大        │
│ 计算密集型      │
│ 元素独立        │
└─────────────────┘

不适合并行的场景：
┌─────────────────┐
│ 数据量小        │
│ IO 密集型       │
│ 元素有依赖      │
└─────────────────┘
```

## Java 9

Java 9 引入了模块化系统（Project Jigsaw），并对 API 进行了多项改进。

-   **模块化系统 (Jigsaw)**: 将 JDK 划分为多个模块，允许开发者创建模块化的应用程序，提高了安全性和性能。
-   **JShell (REPL)**: 提供了交互式的编程环境，方便快速测试代码片段。
-   **集合工厂方法**: 引入了 `List.of()`, `Set.of()`, `Map.of()` 等静态方法，方便创建不可变集合。
-   **接口私有方法**: 允许在接口中定义私有方法，以重用代码并隐藏实现细节。
-   **Stream API 增强**: 添加了 `takeWhile`, `dropWhile`, `ofNullable` 等方法。

## Java 10

-   **局部变量类型推断 (`var`)**: 引入 `var` 关键字，允许编译器推断局部变量的类型，简化了代码。
    ```java
    var list = new ArrayList<String>(); // 推断为 ArrayList<String>
    var stream = list.stream();         // 推断为 Stream<String>
    ```

## Java 11 (LTS)

Java 11 是继 Java 8 之后的又一个长期支持版本，带来了许多实用的新特性。

-   **标准化 `HttpClient`**: 提供了全新的、支持 HTTP/2 和 WebSocket 的客户端 API。
-   **`String` 类增强**: 添加了 `isBlank()`, `lines()`, `strip()`, `repeat()` 等实用方法。
-   **单文件源码程序**: 允许直接使用 `java` 命令运行单个 `.java` 文件，无需显式编译。
-   **`Epsilon` 垃圾收集器**: 一个无操作的垃圾收集器，用于性能分析。

## Java 17 (LTS)

Java 17 带来了性能改进和语言增强。

-   **Sealed Classes (密封类)**: 限制一个类或接口可以被哪些类继承或实现，提供了更强的封装性。
-   **Pattern Matching for `instanceof` (正式)**: 简化了 `instanceof` 类型检查和转换。
    ```java
    // 旧写法
    if (obj instanceof String) {
        String s = (String) obj;
        // ... use s
    }

    // 新写法
    if (obj instanceof String s) {
        // ... use s directly
    }
    ```
-   **`switch` 表达式增强 (正式)**: 提供了更简洁、更安全的 `switch` 语法。

## Java 21 (LTS)

Java 21 是最新的长期支持版本，带来了虚拟线程等重大更新。

-   **虚拟线程 (Virtual Threads)**: Project Loom 的核心成果，极大地简化了高并发应用的编写，让传统的“一个请求一个线程”模型重新变得高效可行。
-   **记录模式 (Record Patterns)**: 解构 `record` 对象，简化数据提取。
-   **`switch` 的模式匹配**: 进一步增强 `switch`，支持对类型和 `record` 进行模式匹配。
-   **序列化集合 (Sequenced Collections)**: 提供了统一的接口来访问集合的首尾元素，并支持逆序遍历。
