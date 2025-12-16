import com.wangliang.agentj.rag.textsplitting.*;
import java.util.List;
import java.util.Map;

public class TestTokenSplitter {
    public static void main(String[] args) {
        try {
            System.out.println("Testing TokenBasedSplitter...");
            
            // Create splitter
            TokenBasedSplitter splitter = new TokenBasedSplitter();
            System.out.println("Splitter created: " + splitter.getName());
            
            // Test text
            String text = "这是一个测试文本。它包含多个句子。每个句子都应该被正确处理。" +
                         "这是另一个段落。它有更多的内容。我们可以测试分割效果。";
            
            // Split text
            List<TextChunk> chunks = splitter.split(text, "test-doc-1", null);
            
            System.out.println("Split completed. Number of chunks: " + chunks.size());
            
            // Print chunks
            for (int i = 0; i < chunks.size(); i++) {
                TextChunk chunk = chunks.get(i);
                System.out.println("Chunk " + i + ":");
                System.out.println("  Content: " + chunk.getContent());
                System.out.println("  Document ID: " + chunk.getDocumentId());
                System.out.println("  Chunk Index: " + chunk.getChunkIndex());
                System.out.println("  Metadata: " + chunk.getMetadata());
                System.out.println();
            }
            
            System.out.println("Test completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Test failed with error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}