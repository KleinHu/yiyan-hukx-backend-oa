package com.cac.oa.service.ai;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiAttachmentTextExtractorTest {
    private final AiAttachmentTextExtractor extractor = new AiAttachmentTextExtractor();

    @Test
    void extractsTxtAndRejectsUnsupportedExtension() {
        MockMultipartFile text = new MockMultipartFile("file", "memo.txt", "text/plain",
                "附件正文".getBytes());
        assertEquals("附件正文", extractor.extract(text).getText());

        MockMultipartFile csv = new MockMultipartFile("file", "data.csv", "text/csv", "a,b".getBytes());
        assertThrows(IllegalArgumentException.class, () -> extractor.extract(csv));
    }

    @Test
    void extractsTextPdf() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            try (PDPageContentStream stream = new PDPageContentStream(document, document.getPage(0))) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(72, 720);
                stream.showText("PDF attachment text");
                stream.endText();
            }
            document.save(output);
        }
        MockMultipartFile file = new MockMultipartFile("file", "memo.pdf", "application/pdf", output.toByteArray());
        assertTrue(extractor.extract(file).getText().contains("PDF attachment text"));
    }

    @Test
    void rejectsTextlessAndEncryptedPdfBeforeGatewayUse() throws Exception {
        ByteArrayOutputStream blankOutput = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(blankOutput);
        }
        MockMultipartFile blank = new MockMultipartFile("file", "scan.pdf", "application/pdf",
                blankOutput.toByteArray());
        IllegalArgumentException blankError = assertThrows(IllegalArgumentException.class,
                () -> extractor.extract(blank));
        assertEquals("未识别到可读取文本，请上传可编辑文件", blankError.getMessage());

        ByteArrayOutputStream encryptedOutput = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.protect(new StandardProtectionPolicy("owner-password", "user-password",
                    new AccessPermission()));
            document.save(encryptedOutput);
        }
        MockMultipartFile encrypted = new MockMultipartFile("file", "locked.pdf", "application/pdf",
                encryptedOutput.toByteArray());
        IllegalArgumentException encryptedError = assertThrows(IllegalArgumentException.class,
                () -> extractor.extract(encrypted));
        assertEquals("附件已加密，无法解析", encryptedError.getMessage());
    }

    @Test
    void extractsDocxAndWorkbookStructure() throws Exception {
        ByteArrayOutputStream wordOutput = new ByteArrayOutputStream();
        try (XWPFDocument document = new XWPFDocument()) {
            document.createParagraph().createRun().setText("Word attachment text");
            document.write(wordOutput);
        }
        MockMultipartFile word = new MockMultipartFile("file", "memo.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", wordOutput.toByteArray());
        assertTrue(extractor.extract(word).getText().contains("Word attachment text"));

        ByteArrayOutputStream workbookOutput = new ByteArrayOutputStream();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("统计");
            sheet.createRow(0).createCell(0).setCellValue("姓名");
            sheet.getRow(0).createCell(1).setCellValue("数量");
            sheet.createRow(1).createCell(0).setCellValue("张三");
            sheet.getRow(1).createCell(1).setCellValue(3);
            workbook.write(workbookOutput);
        }
        MockMultipartFile workbook = new MockMultipartFile("file", "report.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", workbookOutput.toByteArray());
        String text = extractor.extract(workbook).getText();
        assertTrue(text.contains("[工作表] 统计"));

        ByteArrayOutputStream legacyWorkbookOutput = new ByteArrayOutputStream();
        try (HSSFWorkbook legacyXls = new HSSFWorkbook()) {
            legacyXls.createSheet("旧表").createRow(0).createCell(0).setCellValue("旧格式内容");
            legacyXls.write(legacyWorkbookOutput);
        }
        MockMultipartFile legacyWorkbook = new MockMultipartFile("file", "legacy.xls",
                "application/vnd.ms-excel", legacyWorkbookOutput.toByteArray());
        String legacyText = extractor.extract(legacyWorkbook).getText();
        assertTrue(legacyText.contains("[工作表] 旧表"));
        assertTrue(legacyText.contains("旧格式内容"));
        assertTrue(text.contains("姓名\t数量"));
        assertTrue(text.contains("张三\t3"));
    }

    @Test
    void rejectsOversizedAttachment() {
        MockMultipartFile file = new MockMultipartFile("file", "large.txt", "text/plain",
                new byte[(int) AiAttachmentTextExtractor.MAX_FILE_SIZE + 1]);
        assertThrows(IllegalArgumentException.class, () -> extractor.extract(file));
    }

    @Test
    void rejectsDamagedFilesAndNormalizesAttachmentFileNames() {
        MockMultipartFile damaged = new MockMultipartFile("file", "broken.pdf", "application/pdf",
                "not-a-pdf".getBytes());
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> extractor.extract(damaged));
        assertEquals("附件解析失败，请检查文件是否完整且未加密", error.getMessage());

        MockMultipartFile namedText = new MockMultipartFile("file", "C:\\\\tmp\\\\report\\r\\n.txt", "text/plain",
                "正文".getBytes());
        assertEquals("n.txt", extractor.extract(namedText).getFileName());

        MockMultipartFile controlNameText = new MockMultipartFile("file", "report\r\n.txt", "text/plain",
                "正文".getBytes());
        assertEquals("report  .txt", extractor.extract(controlNameText).getFileName());
    }
}
