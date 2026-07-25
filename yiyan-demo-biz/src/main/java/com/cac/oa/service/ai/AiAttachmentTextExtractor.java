package com.cac.oa.service.ai;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Extracts readable text directly from a request-scoped AI attachment. */
@Component
public class AiAttachmentTextExtractor {
    public static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;

    public ExtractedAttachment extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择一个附件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("附件不能超过 10 MB");
        }
        String fileName = normalizeFileName(file.getOriginalFilename());
        String extension = extensionOf(fileName);
        if (extension == null) {
            throw new IllegalArgumentException("仅支持 PDF、Word、Excel 和 TXT 文件");
        }
        try {
            String text;
            if ("pdf".equals(extension)) {
                text = extractPdf(file);
            } else if ("docx".equals(extension)) {
                text = extractDocx(file);
            } else if ("doc".equals(extension)) {
                text = extractDoc(file);
            } else if ("xlsx".equals(extension) || "xls".equals(extension)) {
                text = extractWorkbook(file);
            } else if ("txt".equals(extension)) {
                text = extractText(file);
            } else {
                throw new IllegalArgumentException("仅支持 PDF、Word、Excel 和 TXT 文件");
            }
            String normalized = normalizeText(text);
            if (normalized.isEmpty()) {
                if ("pdf".equals(extension)) {
                    throw new IllegalArgumentException("未识别到可读取文本，请上传可编辑文件");
                }
                throw new IllegalArgumentException("附件中未识别到可读取文本");
            }
            return new ExtractedAttachment(fileName, extension, normalized);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (EncryptedDocumentException exception) {
            throw new IllegalArgumentException("附件已加密，无法解析");
        } catch (InvalidPasswordException exception) {
            throw new IllegalArgumentException("附件已加密，无法解析");
        } catch (Exception exception) {
            throw new IllegalArgumentException("附件解析失败，请检查文件是否完整且未加密");
        }
    }

    private String extractPdf(MultipartFile file) throws IOException {
        try (InputStream input = file.getInputStream(); PDDocument document = PDDocument.load(input)) {
            if (document.isEncrypted()) {
                throw new IllegalArgumentException("附件已加密，无法解析");
            }
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractDocx(MultipartFile file) throws IOException {
        try (InputStream input = file.getInputStream(); XWPFDocument document = new XWPFDocument(input);
                XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractDoc(MultipartFile file) throws IOException {
        try (InputStream input = file.getInputStream(); HWPFDocument document = new HWPFDocument(input);
                WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractWorkbook(MultipartFile file) throws Exception {
        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            DataFormatter formatter = new DataFormatter();
            StringBuilder result = new StringBuilder();
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                result.append("[工作表] ").append(sheet.getSheetName()).append('\n');
                for (Row row : sheet) {
                    short lastCell = row.getLastCellNum();
                    if (lastCell < 0) {
                        continue;
                    }
                    for (int cellIndex = 0; cellIndex < lastCell; cellIndex++) {
                        if (cellIndex > 0) {
                            result.append('\t');
                        }
                        Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        if (cell != null) {
                            result.append(formatter.formatCellValue(cell).replace('\t', ' ').replace('\n', ' '));
                        }
                    }
                    result.append('\n');
                }
                result.append('\n');
            }
            return result.toString();
        }
    }

    private String extractText(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder result = new StringBuilder();
            char[] buffer = new char[4096];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                result.append(buffer, 0, count);
            }
            return result.toString();
        }
    }

    private String normalizeFileName(String value) {
        String fileName = value == null ? "附件" : value.replace('\\', '/');
        int slash = fileName.lastIndexOf('/');
        String normalized = (slash >= 0 ? fileName.substring(slash + 1) : fileName)
                .replaceAll("[\\p{Cntrl}]", " ").trim();
        return normalized.isEmpty() ? "附件" : normalized;
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0 || dot == fileName.length() - 1) {
            return null;
        }
        String extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        return "pdf".equals(extension) || "doc".equals(extension) || "docx".equals(extension)
                || "xls".equals(extension) || "xlsx".equals(extension) || "txt".equals(extension)
                        ? extension : null;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.replace("\u0000", "").trim();
    }

    public static class ExtractedAttachment {
        private final String fileName;
        private final String extension;
        private final String text;

        public ExtractedAttachment(String fileName, String extension, String text) {
            this.fileName = fileName;
            this.extension = extension;
            this.text = text;
        }

        public String getFileName() { return fileName; }
        public String getExtension() { return extension; }
        public String getText() { return text; }
    }
}
