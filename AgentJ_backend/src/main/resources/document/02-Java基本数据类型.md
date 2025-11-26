# Java 基本数据类型

## 1. 基本数据类型分类

### 1.1 整数类型

```ascii
整数类型范围：
┌─────────────┬────────────┬─────────────────────┐
│ 类型        │ 字节数     │ 取值范围            │
├─────────────┼────────────┼─────────────────────┤
│ byte        │ 1字节      │ -128 到 127         │
│ short       │ 2字节      │ -32768 到 32767     │
│ int         │ 4字节      │ -2^31 到 2^31-1     │
│ long        │ 8字节      │ -2^63 到 2^63-1     │
└─────────────┴────────────┴─────────────────────┘
```
**使用场景**：
- `byte`：处理二进制数据流，如文件IO、网络字节流。
- `short`：在内存敏感的大型数组中，可以节省空间。
- `int`：最常用的整数类型，用于计数、索引等绝大部分场景。
- `long`：需要表示大数值的场景，如时间戳、数据库主键。


### 1.2 浮点类型

````ascii
浮点类型特点：
┌─────────────┬────────────┬─────────────────────┐
│ 类型        │ 字节数     │ 精度                │
├─────────────┼────────────┼─────────────────────┤
│ float       │ 4字节      │ 约 7位有效数字      │
│ double      │ 8字节      │ 约 15位有效数字     │
└─────────────┴────────────┴─────────────────────┘
````

**精度问题与 `BigDecimal`**：
由于二进制无法精确表示某些十进制小数，直接使用 `float` 和 `double` 进行运算（尤其是金融计算）会导致精度丢失。

```java
// 错误的比较方式
double a = 0.1 + 0.2;
double b = 0.3;
System.out.println(a == b);  // 输出: false，因为 a 的实际值约等于 0.30000000000000004

// 正确的处理方式：使用 BigDecimal
BigDecimal aDecimal = new BigDecimal("0.1");
BigDecimal bDecimal = new BigDecimal("0.2");
BigDecimal cDecimal = aDecimal.add(bDecimal);
System.out.println(cDecimal.toString()); // 输出: "0.3"，精确计算
```
**最佳实践**：凡是涉及精确计算（如货币、科学计算），都应使用 `BigDecimal`。

### 1.3 字符类型

- `char`: 2 字节，用于存储单个 Unicode 字符。
- 取值范围：`\u0000` 到 `\uffff`（即 0 到 65,535）。
```java
// Unicode 编码示例
char c1 = 'A';           // 字符字面量
char c2 = '\u0041';      // 使用16进制Unicode值
char c3 = 65;            // 使用十进制编码

// 常见的转义字符
char newLine = '\n';     // 换行符
char tab = '\t';         // 制表符
char quote = '\'';       // 单引号
char backslash = '\\';   // 反斜杠
```

### 1.4 布尔类型

- `boolean`: 只有 `true` 和 `false` 两个值。
- JVM 规范没有明确其占用大小，可能是1个比特或1个字节，具体取决于虚拟机实现。
```java
// 条件判断
boolean isReady = true;
if (isReady) {
    System.out.println("Ready to go!");
}

// 逻辑运算
boolean hasPermission = true;
boolean isExpired = false;
if (hasPermission && !isExpired) { // 短路与 和 取反
    System.out.println("Access granted.");
}
```

## 2. 类型转换

### 2.1 自动类型转换（隐式转换）
当一个“小”类型的数据赋值给一个“大”类型的变量时，Java 会自动进行类型转换。

```ascii
自动类型转换方向（安全转换）：
byte → short → int → long → float → double
              ↗
            char
```
**注意事项**：
1.  小范围类型向大范围类型转换是安全的。
2.  `char` 可以安全地转换为 `int` 及更大的整数类型。
3.  整型转换为浮点型时，可能会损失精度（例如 `long` 转 `float`）。

### 2.2 强制类型转换（显式转换）
当需要将“大”类型的数据赋值给“小”类型的变量时，必须使用强制类型转换。这可能导致数据溢出或精度损失。

```java
// 基本强制转换
double d = 99.98;
int i = (int) d;    // i 的值为 99，小数部分被直接截断

// 溢出示例
int largeInt = 130;
byte b = (byte) largeInt; // b 的值为 -126，因为超出了byte的范围(-128 to 127)

// 处理精度损失
double price = 100.123;
// 推荐使用 Math.round 进行四舍五入，而不是直接强转
long cents = Math.round(price * 100); // cents 的值为 10012
```

## 3. 包装类

包装类（Wrapper Class）是将Java的基本数据类型封装成对象的类，从而让基本类型也能参与到面向对象编程中。

| 基本类型 | 包装类    |
|----------|-----------|
| `byte`   | `Byte`    |
| `short`  | `Short`   |
| `int`    | `Integer` |
| `long`   | `Long`    |
| `float`  | `Float`   |
| `double` | `Double`  |
| `char`   | `Character`|
| `boolean`| `Boolean` |

### 3.1 为什么需要包装类

1.  **面向对象编程需求**：Java 是面向对象的语言，但在很多场景下（如集合、泛型），操作的必须是对象。
2.  **泛型和集合支持**：Java 的集合框架（如 `ArrayList<T>`）和泛型机制不支持基本数据类型，必须使用对应的包装类。例如，`ArrayList<int>` 是非法的，必须写成 `ArrayList<Integer>`。
3.  **null 值表示**：基本类型不能赋值为 `null`，而包装类型可以，这在表示“缺失”或“未定义”状态时非常有用（例如，数据库查询结果中的可选字段）。

### 3.2 自动装箱与拆箱

自动装箱（Autoboxing）和自动拆箱（Unboxing）是 JDK 5 引入的语法糖，极大地简化了基本类型和包装类型之间的转换代码。

```java
// 自动装箱：基本类型 → 包装类型
Integer num = 100;                  // 编译器自动转换为：Integer.valueOf(100)

// 自动拆箱：包装类型 → 基本类型
int value = num;                    // 编译器自动转换为：num.intValue()

// 性能陷阱：在循环中进行自动装箱/拆箱
Integer sum = 0;
for (int i = 0; i < 1000; i++) {
    sum += i;  // 每次循环都会创建一个新的Integer对象，造成不必要的开销
}

// 推荐写法：在循环体内部使用基本类型
int sumPrimitive = 0;
for (int i = 0; i < 1000; i++) {
    sumPrimitive += i;
}
Integer finalSum = sumPrimitive; // 仅在最后进行一次装箱
```

### 3.3 包装类缓存机制

为了提高性能和节省内存，Java 对部分包装类提供了缓存机制。当通过 `valueOf()` 方法或自动装箱创建包装类对象时，如果值在缓存范围内，会直接返回缓存中的对象。

-   **`Integer`**: 缓存范围为 **-128 到 127**。
-   **`Short`, `Long`, `Byte`**: 同样缓存 -128 到 127。
-   **`Character`**: 缓存范围为 0 到 127 (`\u0000` 到 `\u007F`)。
-   **`Boolean`**: 缓存了 `true` 和 `false` 两个实例。

```java
// Integer 缓存示例
Integer a = 127;
Integer b = 127;
System.out.println(a == b);      // true，因为从缓存中获取了同一个对象

Integer c = 128;
Integer d = 128;
System.out.println(c == d);      // false，因为超出了缓存范围，创建了新对象
```
**重要提示**：比较包装类对象的值时，应始终使用 `.equals()` 方法，而不是 `==`，以避免缓存机制带来的混淆。

### 3.4 常用方法

包装类提供了许多实用的静态方法，用于类型转换和数值操作。

```java
// 字符串与基本类型的转换
String numStr = "123";
int num = Integer.parseInt(numStr);        // 字符串转 int
double d = Double.parseDouble("3.14");     // 字符串转 double

// 基本类型转字符串
String strFromNum = String.valueOf(123);   // 通用方法
String strFromInt = Integer.toString(123); // 特定类型方法

// 进制转换
String binary = Integer.toBinaryString(10); // "1010"
String hex = Integer.toHexString(160);      // "a0"

// 获取常量
System.out.println(Integer.MAX_VALUE);     // int 类型的最大值
System.out.println(Double.MIN_VALUE);      // double 类型的最小正数值

## 关联知识
- 语言概述：[[02_编程语言与范式 (Programming)/Java/Java基础/01-Java语言概述#1.2.1 数据类型|Java语言概述]] —— 了解Java数据类型在整个语言体系中的位置和作用。
- 字符串处理：[[02_编程语言与范式 (Programming)/Java/Java基础/03-Java字符串处理#1.1 String类特性|Java字符串处理]] —— 字符串与基本数据类型的转换和处理方法。
- 内存模型：[[02_编程语言与范式 (Programming)/Java/Java基础/08-Java内存模型（JMM）#1.1 内存模型概述|Java内存模型]] —— 理解基本数据类型在JVM内存中的存储方式。
- 多线程安全：[[02_编程语言与范式 (Programming)/Java/Java 多线程/1 Java 多线程基础#2.6.5 原子类|原子类]] —— 多线程环境下基本数据类型的线程安全操作。
- 算法复杂度：[[01_计算机基础 (Fundamentals)/数据结构与算法/算法思想/时间和空间复杂度#1.2 空间复杂度|空间复杂度]] —— 不同数据类型对算法空间复杂度的影响。
- 数据结构基础：[[01_计算机基础 (Fundamentals)/数据结构与算法/基础数据结构/数组和链表#1.1 数组特点|数组和链表]] —— 基本数据类型在数据结构中的应用。
