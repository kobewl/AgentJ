# JsonDocumentParser 使用示例

## 概述

JsonDocumentParser是一个用于解析JSON文档的组件，它可以将JSON内容转换为项目中的标准Document格式。

## 主要功能

1. **智能内容提取**：自动从JSON的常见字段中提取内容（content, text, body, description, summary, data）
2. **元数据提取**：自动提取JSON中的元数据信息
3. **错误处理**：完善的错误处理和日志记录
4. **多种输入方式**：支持字符串和InputStream输入

## 使用方法

### 1. 基本使用

```java
import com.wangliang.agentj.rag.documentloader.parsers.JsonDocumentParser;
import com.wangliang.agentj.rag.common.models.Document;

// 创建解析器实例
JsonDocumentParser parser = new JsonDocumentParser();

// 解析JSON字符串
String jsonContent = "{\"title\": \"My Article\", \"content\": \"This is the article content.\", \"author\": \"John Doe\"}";
Document document = parser.parse(jsonContent, "article.json");

// 使用解析后的文档
System.out.println("Document ID: " + document.getId());
System.out.println("Content: " + document.getContent());
System.out.println("Title: " + document.getTitle());
System.out.println("Metadata: " + document.getMetadata());
```

### 2. 从InputStream解析

```java
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

// 创建InputStream
String jsonContent = "{\"text\": \"Content from stream\", \"description\": \"Stream description\"}";
InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes(StandardCharsets.UTF_8));

// 解析
Document document = parser.parse(inputStream, "stream-file.json");
```

### 3. 支持的JSON格式示例

#### 格式1：包含content字段
```json
{
  "title": "文档标题",
  "content": "这是文档的主要内容",
  "author": "作者名",
  "date": "2024-01-15"
}
```

#### 格式2：包含text字段
```json
{
  "text": "这是文本内容",
  "description": "描述信息",
  "type": "article"
}
```

#### 格式3：包含body字段
```json
{
  "body": "这是正文内容",
  "title": "文章标题",
  "category": "技术"
}
```

#### 格式4：没有标准内容字段
```json
{
  "name": "配置项",
  "value": 42,
  "active": true,
  "tags": ["重要", "系统"]
}
```
这种情况下，整个JSON会被格式化为字符串作为内容。

### 4. 元数据提取

解析器会自动提取以下字段作为元数据：
- title
- author  
- date
- version
- type
- category

还会添加JSON结构信息：
- json_type: JSON节点类型
- has_children: 是否有子节点

### 5. 错误处理

```java
try {
    String invalidJson = "{invalid json";
    Document document = parser.parse(invalidJson, "invalid.json");
} catch (IOException e) {
    System.err.println("JSON解析错误: " + e.getMessage());
}
```

## 测试用例

### 测试JSON内容提取

```java
// 测试不同的内容字段
String[] testJsons = {
    "{\"content\": \"Content field\"}",
    "{\"text\": \"Text field\"}",
    "{\"body\": \"Body field\"}",
    "{\"description\": \"Description field\"}",
    "{\"summary\": \"Summary field\"}",
    "{\"data\": \"Data field\"}"
};

for (String json : testJsons) {
    Document doc = parser.parse(json, "test.json");
    System.out.println("Content: " + doc.getContent());
}
```

### 测试元数据提取

```java
String json = "{\"content\": \"Test\", \"title\": \"Title\", \"author\": \"Author\", \"date\": \"2024-01-01\", \"version\": \"1.0\"}";
Document doc = parser.parse(json, "metadata-test.json");

System.out.println("Title: " + doc.getMetadata().get("title"));
System.out.println("Author: " + doc.getMetadata().get("author"));
System.out.println("Date: " + doc.getMetadata().get("date"));
System.out.println("Version: " + doc.getMetadata().get("version"));
```

## 注意事项

1. **内容优先级**：解析器按照以下顺序查找内容字段：content → text → body → description → summary → data
2. **空内容处理**：如果所有内容字段都为空或不存在，整个JSON会被转换为格式化字符串
3. **编码**：使用UTF-8编码处理JSON内容
4. **性能**：对于大型JSON文件，建议使用InputStream方式解析

## 集成示例

```java
// 在Spring Boot应用中使用
@Service
public class DocumentService {
    
    @Autowired
    private JsonDocumentParser jsonParser;
    
    public Document parseJsonFile(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            return jsonParser.parse(inputStream, file.getOriginalFilename());
        }
    }
}
```

这个JsonDocumentParser提供了灵活且强大的JSON文档解析功能，可以处理各种格式的JSON数据并将其转换为统一的Document格式。