# Java 字符串处理

## 1. String 类的核心特性

### 1.1 不可变性 (Immutability)

`String` 对象一旦被创建，其内容就无法被修改。任何看似修改 `String` 的操作（如 `concat`, `substring`, `replace`）实际上都会创建一个全新的 `String` 对象。

-   **实现原理**:
    -   `String` 类被 `final` 修饰，不能被继承。
    -   其内部用于存储字符的数组（在Java 9之前是 `char[]`，之后是 `byte[]`）被 `final` 修饰，并且是私有的，外部无法访问。
    -   类中没有提供任何可以修改内部字符数组的方法。

-   **不可变性的优点**:
    1.  **线程安全**: 由于无法被修改，`String` 对象可以被多个线程安全地共享，无需任何同步措施。
    2.  **支持字符串常量池**: 只有当字符串是不可变的，字符串常量池的实现才有意义，可以节省大量内存。
    3.  **可作为 `HashMap` 的键**: 因为字符串的 `hashCode()` 是根据其内容计算的，不可变性保证了 `hashCode` 在对象生命周期内不会改变，满足了 `HashMap` 键的要求。

### 1.2 字符串常量池 (String Constant Pool)

为了减少内存消耗和提高性能，Java 在内存中（自 JDK 7 起在堆内存中）维护了一个特殊的区域——字符串常量池。

-   **工作原理**:
    -   当使用字面量（如 `String s = "hello";`）创建字符串时，JVM 会首先检查常量池中是否存在内容为 "hello" 的字符串。
    -   如果存在，则直接返回池中对象的引用。
    -   如果不存在，则在池中创建一个新的 "hello" 对象，并返回其引用。
    -   使用 `new String("hello")` 会在堆上创建一个新对象，即使常量池中已存在 "hello"。

```java
String s1 = "Hello"; // 在常量池中创建 "Hello"
String s2 = "Hello"; // 直接引用常量池中的 "Hello"
String s3 = new String("Hello"); // 在堆上创建一个新对象

System.out.println(s1 == s2); // true, 因为 s1 和 s2 指向同一个池中对象
System.out.println(s1 == s3); // false, 因为 s3 是堆上的一个新对象
```

## 2. 字符串比较

### 2.1 `==` vs `equals()`

-   **`==`**: 比较的是两个引用的内存地址是否相同。
    -   对于基本数据类型，比较的是值。
    -   对于引用类型，比较的是它们是否指向同一个对象。

-   **`.equals()`**: `Object` 类的 `equals` 方法默认行为与 `==` 相同。但 `String` 类重写了此方法，用于比较两个字符串的**内容**是否完全相同。

```java
String s1 = "Hello";
String s2 = new String("Hello");
String s3 = "hello";

System.out.println(s1 == s2);              // false, 地址不同
System.out.println(s1.equals(s2));         // true, 内容相同
System.out.println(s1.equals(s3));         // false, 内容不同（大小写敏感）
System.out.println(s1.equalsIgnoreCase(s3)); // true, 忽略大小写比较内容
```
**最佳实践**: 比较字符串内容时，**必须**使用 `.equals()` 方法。

### 2.2 `compareTo()`

`compareTo()` 方法用于按字典序比较两个字符串的大小，常用于排序。

-   **返回值**:
    -   `0`: 两个字符串相等。
    -   负数: 当前字符串小于参数字符串。
    -   正数: 当前字符串大于参数字符串。

```java
String str1 = "apple";
String str2 = "banana";
System.out.println(str1.compareTo(str2)); // 输出负数，因为 'a' < 'b'
```

## 3. `StringBuilder` 与 `StringBuffer`

由于 `String` 的不可变性，在循环中拼接字符串会产生大量临时对象，效率低下。为了解决这个问题，Java 提供了 `StringBuilder` 和 `StringBuffer`。

| 特性       | `String`     | `StringBuilder` | `StringBuffer`  |
| :----------- | :----------- | :-------------- | :-------------- |
| **可变性**   | 不可变       | 可变            | 可变            |
| **线程安全** | 线程安全     | 非线程安全      | 线程安全        |
| **性能**     | 拼接时最低   | 最高            | 中等            |

-   **`StringBuilder`**: 可变字符串，提供了 `append`, `insert`, `delete` 等方法来修改字符串内容。由于没有同步开销，性能最高。**推荐在单线程环境下使用**。

-   **`StringBuffer`**: `StringBuilder` 的线程安全版本。其所有公开方法都由 `synchronized` 修饰，保证了在多线程环境下的数据一致性，但带来了性能开销。**适用于多线程环境**。

### 3.1 性能对比与选择

```java
// 错误示例：在循环中使用 `+` 拼接
String result = "";
for (int i = 0; i < 1000; i++) {
    result += i; // 每次循环都会创建一个新的 String 对象
}

// 正确示例：使用 StringBuilder
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 1000; i++) {
    sb.append(i); // 在同一个对象上操作，效率高
}
String finalResult = sb.toString();
```

**选择原则**:
1.  **字符串不经常变化**: 使用 `String`。
2.  **单线程环境，字符串频繁变化**: 使用 `StringBuilder`。
3.  **多线程环境，字符串频繁变化**: 使用 `StringBuffer`。

## 4. 常用方法

```java
String str = "  Hello, World!  ";

// --- 长度和访问 ---
str.length();          // 17, 获取字符串长度
str.isEmpty();         // false, 判断是否为空字符串 ""
str.isBlank();         // false, 判断是否为空白字符串 (Java 11+)
str.charAt(2);         // 'H', 获取指定索引的字符

// --- 查找和匹配 ---
str.contains("World"); // true, 是否包含子串
str.indexOf("o");      // 5, 查找子串首次出现的位置
str.lastIndexOf("o");  // 9, 查找子串最后一次出现的位置
str.startsWith("  H"); // true, 是否以指定前缀开头
str.endsWith("!  ");   // true, 是否以指定后缀结尾

// --- 截取和转换 ---
str.substring(7);      // "World!  ", 从指定索引截取到末尾
str.substring(7, 12);  // "World", 截取指定范围的子串
str.toUpperCase();     // "  HELLO, WORLD!  "
str.toLowerCase();     // "  hello, world!  "

// --- 清理和替换 ---
str.trim();            // "Hello, World!", 去除两端的传统空白符
str.strip();           // "Hello, World!", 去除两端的Unicode空白符 (Java 11+)
str.replace('o', 'a'); // "  Hella, Warld!  "
str.replaceAll("[,!]", ""); // "  Hello World  ", 使用正则替换

// --- 分割和连接 ---
String data = "apple,banana,orange";
String[] fruits = data.split(","); // ["apple", "banana", "orange"]

String joined = String.join("-", fruits); // "apple-banana-orange" (Java 8+)

// --- 格式化 ---
String formatted = String.format("User: %s, Age: %d", "Alice", 30);
// "User: Alice, Age: 30"