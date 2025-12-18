package com.wangliang.agentj.tools.fileGenerator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.itextpdf.commons.utils.FileUtil;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.wangliang.agentj.tools.AbstractBaseTool;
import com.wangliang.agentj.tools.code.ToolExecuteResult;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * PDF generator tool - jmanus style: simple and direct
 * <p>
 * Based on user requests, the AI calls this tool to generate PDF files.
 */
@Slf4j
public class PdfGenerator extends AbstractBaseTool<PdfGenerator.PdfGeneratorInput> {

    private static final String TOOL_NAME = "pdf_generator";
    private static final String OUTPUT_DIR = "uploads/pdf_general";

    /**
     * Input class for uploaded file operations
     */
    public static class PdfGeneratorInput {

        @JsonProperty("file_name")
        private String fileName;

        @JsonProperty("content")
        private String content;

        // Getters and setters
        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

    }


    public String generatePdf(PdfGeneratorInput input) {

        String fileName = input.getFileName();
        Path fileDir = Paths.get(OUTPUT_DIR);
        String filePath = fileDir.resolve(fileName).toString();

        try {

            log.info("使用生成 pdf 工具……");

            // 创建目录
            FileUtil.createDirectories(fileDir.toString());
            // 创建 pdfWriter 和 pdfDocument 对象
            try (PdfWriter pdfWriter = new PdfWriter(filePath);
                 PdfDocument pdfDocument = new PdfDocument(pdfWriter);
                 Document document = new Document(pdfDocument)) {
                PdfFont font = PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H");
                document.setFont(font);
                // 创建段落
                Paragraph paragraph = new Paragraph(input.getContent());
                // 添加段落并关闭文档
                document.add(paragraph);
            }
            return "PDF generated successfully to: " + filePath;
        }catch (Exception e){
            return "Error generating PDF: " + e.getMessage();
        }
    }

    @Override
    public String getServiceGroup() {
        return "default-service-group";
    }

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return "Generate a PDF file from plain text content and save it under uploads/pdf_general/.";
    }

    @Override
    public String getParameters() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "file_name": {
                      "type": "string",
                      "description": "Output PDF file name",
                      "minLength": 1
                    },
                    "content": {
                      "type": "string",
                      "description": "Plain text content written into the PDF",
                      "minLength": 1
                    }
                  },
                  "required": ["file_name", "content"],
                  "additionalProperties": false
                }
                """;
    }

    @Override
    public Class<PdfGeneratorInput> getInputType() {
        return PdfGeneratorInput.class;
    }

    @Override
    public boolean isSelectable() {
        return true;
    }

    @Override
    public String getCurrentToolStateString() {
        return "PDF generator ready";
    }

    @Override
    public void cleanup(String planId) {
        if (planId != null) {
            log.info("Cleaning up PDF generator resources for plan: {}", planId);
        }
    }

    @Override
    public ToolExecuteResult run(PdfGeneratorInput input) {
        if (input == null) {
            return new ToolExecuteResult("Error: input is required");
        }
        String fileName = input.getFileName();
        if (fileName == null || fileName.trim().isEmpty()) {
            return new ToolExecuteResult("Error: file_name is required");
        }
        if (input.getContent() == null) {
            return new ToolExecuteResult("Error: content is required");
        }

        String normalizedFileName = fileName.trim();
        if (!normalizedFileName.toLowerCase().endsWith(".pdf")) {
            normalizedFileName = normalizedFileName + ".pdf";
        }
        input.setFileName(normalizedFileName);

        try {
            return new ToolExecuteResult(generatePdf(input));
        }
        catch (Exception e) {
            log.error("PDF generation failed", e);
            return new ToolExecuteResult("Error generating PDF: " + e.getMessage());
        }
    }

}
