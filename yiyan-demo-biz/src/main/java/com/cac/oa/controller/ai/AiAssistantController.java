package com.cac.oa.controller.ai;

import com.cac.oa.service.ai.AiChatService;
import com.cac.oa.service.ai.AiChatStreamContext;
import com.cac.oa.service.ai.AiConfigService;
import com.cac.oa.vo.ai.AiChatRequest;
import com.cac.oa.vo.ai.AiChatVO;
import com.cac.oa.vo.ai.AiChatSessionDetailVO;
import com.cac.oa.vo.ai.AiChatSessionVO;
import com.cac.oa.vo.ai.AiConfigSaveRequest;
import com.cac.oa.vo.ai.AiConfigVO;
import com.cac.oa.vo.ai.AiGatewayDiscoverRequest;
import com.cac.oa.vo.ai.AiModelVO;
import com.cac.yiyan.common.vo.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "OA-个人 AI 助手")
@RestController
@RequestMapping("/api/240/oa/ai")
@RequiredArgsConstructor
public class AiAssistantController {
    private final AiConfigService configService;
    private final AiChatService chatService;
    private final ObjectMapper objectMapper;

    @GetMapping("/config")
    @ApiOperation("读取当前用户的 AI 配置摘要")
    public Result<AiConfigVO> getConfig() {
        return Result.ok(configService.getMyConfig());
    }

    @PutMapping("/config")
    @ApiOperation("保存当前用户的 AI 网关与密钥")
    public Result<AiConfigVO> saveConfig(@RequestBody AiConfigSaveRequest request) {
        return Result.ok(configService.saveMyConfig(request));
    }

    @PostMapping("/models/discover")
    @ApiOperation("使用填写的网关地址和密钥读取模型，不保存配置")
    public Result<List<AiModelVO>> discoverModels(@RequestBody AiGatewayDiscoverRequest request) {
        return Result.ok(configService.discoverModels(request));
    }

    @GetMapping("/models")
    @ApiOperation("读取当前用户已缓存的模型列表；首次读取时自动建立缓存")
    public Result<List<AiModelVO>> getModels() {
        return Result.ok(configService.listMyModels());
    }

    @PostMapping("/models/refresh")
    @ApiOperation("从当前用户的网关刷新并保存模型列表")
    public Result<List<AiModelVO>> refreshModels() {
        return Result.ok(configService.refreshMyModels());
    }

    @PostMapping("/chat")
    @ApiOperation("使用当前用户的网关发起对话")
    public Result<AiChatVO> chat(@RequestBody AiChatRequest request) {
        return Result.ok(chatService.chat(request));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation("使用当前用户的网关流式发起对话")
    public ResponseEntity<StreamingResponseBody> streamChat(@RequestBody AiChatRequest request) {
        AiChatStreamContext context = chatService.startStream(request);
        return streamResponse(context);
    }

    @PostMapping(value = "/chat/stream/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation("携带单个附件流式发起 AI 对话")
    public ResponseEntity<StreamingResponseBody> streamChatWithAttachment(
            @RequestPart("request") AiChatRequest request, @RequestPart("file") List<MultipartFile> files) {
        AiChatStreamContext context = chatService.startStreamWithAttachment(request, requireSingleAttachment(files));
        return streamResponse(context);
    }

    static MultipartFile requireSingleAttachment(List<MultipartFile> files) {
        if (files == null || files.size() != 1) {
            throw new IllegalArgumentException("一次只能上传 1 个附件");
        }
        return files.get(0);
    }

    private ResponseEntity<StreamingResponseBody> streamResponse(AiChatStreamContext context) {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl("no-cache");
        headers.add("X-Accel-Buffering", "no");
        headers.add("Connection", "keep-alive");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(output -> writeStream(context, output));
    }

    @GetMapping("/chat/sessions")
    @ApiOperation("读取当前用户的 AI 对话历史")
    public Result<List<AiChatSessionVO>> listChatSessions() {
        return Result.ok(chatService.listMySessions());
    }

    @GetMapping("/chat/sessions/{sessionId}")
    @ApiOperation("读取一条 AI 对话历史")
    public Result<AiChatSessionDetailVO> getChatSession(@PathVariable String sessionId) {
        return Result.ok(chatService.getMySession(sessionId));
    }

    @DeleteMapping("/chat/sessions/{sessionId}")
    @ApiOperation("删除一条 AI 对话历史")
    public Result<Boolean> deleteChatSession(@PathVariable String sessionId) {
        chatService.deleteMySession(sessionId);
        return Result.ok(true);
    }

    private void writeStream(AiChatStreamContext context, OutputStream output) throws IOException {
        StringBuilder assistantContent = new StringBuilder();
        AiChatVO response = null;
        Exception failure = null;
        try {
            Map<String, String> meta = new HashMap<String, String>();
            meta.put("sessionId", String.valueOf(context.getSessionId()));
            meta.put("model", context.getModel());
            writeSse(output, "meta", meta);
            response = chatService.streamChat(context, delta -> {
                assistantContent.append(delta);
                Map<String, String> event = new HashMap<String, String>();
                event.put("content", delta);
                writeSse(output, "delta", event);
            });
        } catch (Exception exception) {
            failure = exception;
        } finally {
            try {
                chatService.completeStream(context, assistantContent.toString());
            } catch (Exception exception) {
                if (failure == null) {
                    failure = exception;
                }
            }
        }
        if (failure != null) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("message", "AI 回复已中断，已保留当前内容。请稍后重试。");
            error.put("retryable", !context.isAttachmentRequest());
            try {
                writeSse(output, "error", error);
            } catch (IOException ignored) {
                // The browser intentionally stopped or closed the stream.
            }
            return;
        }
        Map<String, String> done = new HashMap<String, String>();
        done.put("id", response == null ? "" : response.getId());
        done.put("sessionId", String.valueOf(context.getSessionId()));
        done.put("model", response == null ? context.getModel() : response.getModel());
        done.put("finishReason", response == null ? "" : response.getFinishReason());
        writeSse(output, "done", done);
    }

    private void writeSse(OutputStream output, String event, Object data) throws IOException {
        String payload = objectMapper.writeValueAsString(data);
        String message = "event: " + event + "\n" + "data: " + payload + "\n\n";
        output.write(message.getBytes(StandardCharsets.UTF_8));
        output.flush();
    }
}
