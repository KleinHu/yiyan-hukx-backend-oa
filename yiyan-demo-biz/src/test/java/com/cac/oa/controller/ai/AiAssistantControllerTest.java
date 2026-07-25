package com.cac.oa.controller.ai;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiAssistantControllerTest {
    @Test
    void rejectsMultipleAttachmentParts() {
        MockMultipartFile first = new MockMultipartFile("file", "first.txt", "text/plain", "one".getBytes());
        MockMultipartFile second = new MockMultipartFile("file", "second.txt", "text/plain", "two".getBytes());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> AiAssistantController.requireSingleAttachment(Arrays.asList(first, second)));

        assertEquals("一次只能上传 1 个附件", error.getMessage());
    }
}
