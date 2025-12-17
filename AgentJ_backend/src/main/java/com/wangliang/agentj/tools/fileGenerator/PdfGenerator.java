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
import com.wangliang.agentj.tools.filesystem.UploadedFileLoaderTool;
import lombok.extern.slf4j.Slf4j;

/**
 * PDF generator tool - jmanus style: simple and direct
 * <p>
 * Based on user requests, the AI calls this tool to generate PDF files.
 */
@Slf4j
public class PdfGenerator extends AbstractBaseTool<PdfGenerator.PdfGeneratorInput> {

    private static final String TOOL_NAME = "pdf_generator";

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
        String fileDir = "/pdf";
        String filePath = fileDir + "/" + fileName;

        try {
            // 创建目录
            FileUtil.createDirectories(fileDir);
            // 创建 pdfWriter 和 pdfDocument 对象
            try (PdfWriter pdfWriter = new PdfWriter(filePath);
                 PdfDocument pdfDocument = new PdfDocument(pdfWriter);
                 Document document = new Document(pdfDocument)) {
                PdfFont font = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
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
        return "";
    }

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public String getParameters() {
        return "";
    }

    @Override
    public Class<PdfGeneratorInput> getInputType() {
        return null;
    }

    @Override
    public boolean isSelectable() {
        return false;
    }

    @Override
    public String getCurrentToolStateString() {
        return "";
    }

    @Override
    public void cleanup(String planId) {

    }

    @Override
    public ToolExecuteResult run(PdfGeneratorInput input) {
        return null;
    }

}
