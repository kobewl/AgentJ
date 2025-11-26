# 1 Java 语言概述

## 1 Java 简介

### 1.1 什么是 Java？

```ascii
Java 的特点：
┌──────────────────────────┐
│ Write Once, Run Anywhere │
└──────────────────────────┘
     ↓            ↓
  跨平台       一次编写
     ↓            ↓
[Windows/Linux/Mac OS...]

Java 程序运行过程：
源代码(.java) ──编译──→ 字节码(.class) ──运行──→ 程序结果
                javac              java
```

### 1.2 Java 的优势

1. **跨平台性**
   - JVM 提供统一运行环境
   - 不同系统使用相同代码

2. **面向对象**
   - 封装、继承、多态
   - 易维护、易复用

3. **安全性**
   - 强类型语言
   - 异常处理机制
   - 安全管理器

## 2 基本语法

### 2.1 数据类型

Java 的数据类型分为基本数据类型和引用数据类型。详细内容请参考：
- [[02-Java基本数据类型]]

### 2.2 变量与常量

```java
// 1. 变量声明和初始化
int number = 10;            // 声明并初始化
final double PI = 3.14159;  // 常量声明

// 2. 变量命名规则
studentName    // 驼峰命名法
MAX_VALUE      // 常量全大写
```

### 2.3 运算符

```ascii
运算符优先级：
高  ┌─────────────┐
    │ ()          │ 括号
    │ ++ -- !     │ 一元运算符
    │ * / %       │ 乘除
    │ + -         │ 加减
    │ > >= < <=   │ 比较
    │ == !=       │ 相等
    │ &&          │ 与
    │ ||          │ 或
低  │ = += -= etc │ 赋值
    └─────────────┘
```

## 3 流程控制

### 3.1 条件语句

```java
// 1. if-else
if (condition) {
    // 代码块
} else if (condition2) {
    // 代码块
} else {
    // 代码块
}

// 2. switch
switch (variable) {
    case value1:
        // 代码块
        break;
    case value2:
        // 代码块
        break;
    default:
        // 默认代码块
}
```

### 3.2 循环语句

```ascii
循环结构：
┌──────────────┐
│ for 循环     │──→ 知道循环次数
│ while 循环   │──→ 不知道循环次数
│ do-while 循环│──→ 至少执行一次
└──────────────┘
```

```java
// 1. for 循环
for (int i = 0; i < 10; i++) {
    System.out.println(i);
}

// 2. while 循环
while (condition) {
    // 代码块
}

// 3. do-while 循环
do {
    // 代码块
} while (condition);
```

## 4 数组

### 4.1 数组定义和使用

```java
// 1. 数组声明
int[] numbers = new int[5];
int[] numbers2 = {1, 2, 3, 4, 5};

// 2. 多维数组
int[][] matrix = new int[3][4];
int[][] matrix2 = {{1,2,3}, {4,5,6}};
```

### 4.2 数组操作

```ascii
数组常见操作：
┌─────────────┐
│ 遍历        │──→ for/foreach
│ 查找        │──→ Arrays.binarySearch()
│ 排序        │──→ Arrays.sort()
│ 复制        │──→ Arrays.copyOf()
└─────────────┘
```

## 5 核心概念深入

### 5.1 字符串处理
`String` 是 Java 中最常用的类之一，具有不可变性。关于字符串的详细操作、与 `StringBuilder`/`StringBuffer` 的对比，请参考：
- [[03-Java字符串处理]]

### 5.2 面向对象基础 (OOP)

```java
public class Student {
    // 属性 (Fields)
    private String name;
    private int age;

    // 构造方法 (Constructor)
    public Student(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // 方法 (Methods)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```

#### 5.2.1 三大特性

```ascii
面向对象三大特性：

1. 封装 (Encapsulation)
┌─────────────┐
│ private 属性 │
│ public 方法  │
└─────────────┘

2. 继承 (Inheritance)
┌─────────┐
│  父类   │
└────┬────┘
     │
┌────┴────┐
│  子类   │
└─────────┘

3. 多态 (Polymorphism)
方法重写和重载
父类引用指向子类对象
```

#### 5.2.2 访问修饰符

```ascii
访问修饰符：
┌─────────────┬───────┬───────┬───────┬───────┐
│ 修饰符       │ 类内部 │ 同包   │ 子类   │ 其他包│
├─────────────┼───────┼───────┼───────┼───────┤
│ private     │   √   │   ×   │   ×   │   ×   │
│ default     │   √   │   √   │   ×   │   ×   │
│ protected   │   √   │   √   │   √   │   ×   │
│ public      │   √   │   √   │   √   │   √   │
└─────────────┴───────┴───────┴───────┴───────┘
```

#### 5.2.3 方法重写 (Override) vs 重载 (Overload)

- **重写(Override)**
  - **前提**：发生在父子类之间。
  - **规则**：方法名、参数列表必须完全相同。返回值类型可以是子类类型。访问权限不能比父类更严格。
  - **核心**：运行时多态，子类提供自己的实现。

- **重载(Overload)**
  - **前提**：发生在同一个类中。
  - **规则**：方法名必须相同，但参数列表必须不同（类型、个数或顺序）。
  - **核心**：编译时多态，提供了功能相似但参数不同的多个方法。

### 5.3 异常处理
Java 使用 `try-catch-finally` 机制来处理程序运行时的异常情况，保证程序的健壮性。详细内容请参考：
- [[5 Java异常]]

## 6 编码规范

### 6.1 命名规范

```ascii
命名规范：
┌─────────────┐
│ 类名        │──→ 大驼峰命名 (UpperCamelCase)
│ 方法名      │──→ 小驼峰命名 (lowerCamelCase)
│ 变量名      │──→ 小驼峰命名 (lowerCamelCase)
│ 常量名      │──→ 全大写下划线 (CONSTANT_CASE)
│ 包名        │──→ 全小写 (lowercase)
└─────────────┘
```

### 6.2 代码格式

1. **缩进与空格**
   - 使用 4 个空格进行缩进。
   - 运算符两边应有空格。
   - 方法声明与方法体之间、不同逻辑块之间应有空行。

2. **注释规范**
   - **类注释**: 使用 Javadoc (`/** ... */`) 说明类的功能、作者、版本等。
   - **方法注释**: 使用 Javadoc 说明方法的功能、参数 (`@param`)、返回值 (`@return`) 和可能抛出的异常 (`@throws`)。
   - **行内注释**: 对关键或复杂的代码逻辑使用 `//` 进行说明。

## 7 关联知识
- 数据类型详解：[[02-Java基本数据类型#1.1 基本数据类型|Java基本数据类型]] —— 深入理解Java的8种基本数据类型和引用类型。
- 字符串处理：[[03-Java字符串处理#1.1 String类特性|Java字符串处理]] —— String、StringBuilder、StringBuffer的区别与使用场景。
- 版本特性：[[04-JDK版本新特性#1.1 Java 8新特性|JDK版本新特性]] —— 了解Java各版本的重要特性更新。
- 多线程基础：[[1 Java 多线程基础#1.1 线程概念|Java多线程基础]] —— Java并发编程的基础概念。
- 算法基础：[[01_计算机基础 (Fundamentals)/数据结构与算法/算法思想/时间和空间复杂度#1.1 时间复杂度|时间和空间复杂度]] —— 理解算法效率分析，为Java程序优化提供理论基础。
- 数据结构应用：[[1 数组和链表#1.1 数组特点|数组和链表]] —— Java集合框架的底层实现原理。
