package com.wky.controller;

import cn.hutool.json.JSONUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.wky.dto.LocationIn;
import com.wky.entity.Location;
import com.wky.dto.LocationDTO;
import com.wky.mapper.LocationMapper;
import com.wky.tools.DateTimeTools;
import com.wky.tools.MapTools;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class LocationController {

    private final LocationMapper locationMapper;
    private final ChatModel chatModel;
    private final MapTools mapTools;

    private final ToolCallingManager toolCallingManager = DefaultToolCallingManager.builder().build();
    private final ChatMemory chatMemory = MessageWindowChatMemory.builder().build();
    private static final Cache<String, Boolean> requestCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES) // 1分钟后自动过期
            .concurrencyLevel(Runtime.getRuntime().availableProcessors())
            .build();


    /**
     * 检查是否在同一分钟内已经处理过相同标识的请求
     *
     * @return true-可以处理(未重复), false-重复请求
     */
    public boolean shouldProcess(LocalDateTime time) {
        String minute = getMinute(time);

        // 如果key不存在，则放入缓存并返回true；如果已存在，返回false
        Boolean result = requestCache.asMap().putIfAbsent(minute, true);
        return result == null;
    }

    private String getMinute(LocalDateTime time) {
        return time.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
    }

    @PostMapping("/location")
    public ResponseEntity<Void> postLocation(@Valid @RequestBody LocationIn in) {
        String traceId = UUID.randomUUID().toString();
        log.info("request: {}, traceId: {}", in, traceId);
        LocalDateTime time = in.getTime().truncatedTo(ChronoUnit.MINUTES);
        if (!shouldProcess(time)) {
            return ResponseEntity.noContent().build();
        }
        Location loc = new Location();
        loc.setDeviceId(in.getDeviceId());
        loc.setLat(in.getLat());
        loc.setLng(in.getLng());
        loc.setAccuracy(in.getAccuracy());
        loc.setProvider(in.getProvider());
        loc.setCreatedAt(time);
        loc.setDeleted(0);
        locationMapper.insert(loc);
        return ResponseEntity.ok().build();
    }

    // 新增：查询所有设备ID（去重）
    @GetMapping("/devices")
    public List<String> listDevices() {
        return locationMapper.selectDistinctDeviceIds();
    }

    // 新增：按天查询某设备的轨迹
    @GetMapping("/device-day")
    public List<Location> listDeviceDay(@RequestParam("deviceId") String deviceId,
                                        @RequestParam("date") String date) {
        LocalDate day = LocalDate.parse(date); // 格式: YYYY-MM-DD
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();
        return locationMapper.selectByDeviceAndDate(deviceId, start, end);
    }


    PromptTemplate promptTemplate = new PromptTemplate(
            """
                    # 角色
                    你是人生足迹APP的智能助手，你的任务是分析用户的轨迹数据，并给出一个总结报告。
                    
                    # 任务
                    以下是用户{currentDate}的轨迹数据，用户所在时区为东八区。这个数据是一个list，每个list的元素item包含经纬度和时间，你需要根据以下数据来进行分析，总结用户的生活习惯，为用户提供更加智能的服务。
                    
                    {locations}
                    
                    # 步骤
                    1. 调用工具获取每个item中的经纬度所在的具体位置
                    2. 根据这些具体位置以及时间生成一个总结报告
                    
                    # 结果
                    输出结果使用markdown格式即可
                    """
    );

    @PostMapping("/agent-summary")
    public ResponseEntity<String> agentSummary(@RequestParam("deviceId") String deviceId,
                                               @RequestParam("date") String date) {
        // 1. 获取今天的经纬度数据
        List<Location> locations = listDeviceDay(deviceId, date);
        if (CollectionUtils.isEmpty(locations)) {
            return ResponseEntity.ok("今天没有轨迹数据");
        }
        List<LocationDTO> locationDTOS = locations.stream()
                .map(loc -> new LocationDTO()
                        .setLat(loc.getLat())
                        .setLng(loc.getLng())
                        .setTime(loc.getCreatedAt()))
                .toList();

        String conversationId = UUID.randomUUID().toString();

        ChatOptions chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(ToolCallbacks.from(mapTools))
                .internalToolExecutionEnabled(false)
                .build();
        String systemMessageContent = promptTemplate.render(Map.of(
                "currentDate", LocalDate.now().toString(),
                "locations", JSONUtil.toJsonStr(locationDTOS)
        ));
        SystemMessage systemMessage = new SystemMessage(systemMessageContent);
        Prompt prompt = new Prompt(systemMessage, chatOptions);
        chatMemory.add(conversationId, prompt.getInstructions());

        Prompt promptWithMemory = new Prompt(chatMemory.get(conversationId), chatOptions);
        ChatResponse chatResponse = chatModel.call(promptWithMemory);
        chatMemory.add(conversationId, chatResponse.getResult().getOutput());

        while (chatResponse.hasToolCalls()) {
            ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(promptWithMemory,
                    chatResponse);
            chatMemory.add(conversationId, toolExecutionResult.conversationHistory()
                    .getLast());
            promptWithMemory = new Prompt(chatMemory.get(conversationId), chatOptions);
            chatResponse = chatModel.call(promptWithMemory);
            chatMemory.add(conversationId, chatResponse.getResult().getOutput());
        }

        String result = chatMemory.get(conversationId).getLast().getText();
        return ResponseEntity.ok(result);
    }
}