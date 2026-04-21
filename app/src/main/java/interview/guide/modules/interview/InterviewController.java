package interview.guide.modules.interview;

import cn.hutool.core.bean.BeanUtil;
import interview.guide.common.annotation.RateLimit;
import interview.guide.common.result.Result;
import interview.guide.infrastructure.file.FileStorageService;
import interview.guide.modules.interview.model.*;
import interview.guide.modules.interview.pojo.*;
import interview.guide.modules.interview.pojo.dto.CalendarDTO;
import interview.guide.modules.interview.pojo.dto.CardRecordsDTO;
import interview.guide.modules.interview.pojo.vo.CalendarVO;
import interview.guide.modules.interview.repository.CalendarReposity;
import interview.guide.modules.interview.repository.CardRecordRepository;
import interview.guide.modules.interview.repository.InterviewSessionRepository;
import interview.guide.modules.interview.service.*;
import interview.guide.modules.knowledgebase.model.TestQuestionEntity;
import interview.guide.modules.knowledgebase.repository.TestQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;


import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 面试控制器
 * 提供模拟面试相关的API接口
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class InterviewController {
    
    private final InterviewSessionService sessionService;
    private final InterviewHistoryService historyService;
    private final InterviewPersistenceService persistenceService;
    private final TestQuestionRepository testQuestionRepository;
    private final InterviewSessionRepository sessionRepository;
    private final CardRecordRepository cardRecordRepository;
    private final CalendarReposity calendarReposity;

    /**
     * 用户日历计划表(查看)
     */
    @PostMapping("api/interview/sessions/getcalendar")
    public Result<List<CalendarVO>>getCalendar(@RequestBody CalendarDTO calendarDTO){
        int day=calendarDTO.getDay();
        if(day==0){
            List<Calendar>list=calendarReposity.findByUserIdAndYearAndMonthOrderByIdDesc(calendarDTO.getUserId(), calendarDTO.getYear(), calendarDTO.getMonth());
            if(list==null||list.size()==0)return Result.success(null);
            List<CalendarVO> calendarVOS1 = BeanUtil.copyToList(list, CalendarVO.class);
            return Result.success(calendarVOS1);
        }else {
            List<CalendarVO>calendarVOS=new ArrayList<>();
            Calendar calendar = calendarReposity.findByUserIdAndYearAndMonthAndDayOrderByIdDesc(calendarDTO.getUserId(), calendarDTO.getYear(), calendarDTO.getMonth(), calendarDTO.getDay());
            if(calendar==null)return Result.success(null);
            CalendarVO calendarVO = BeanUtil.copyProperties(calendar, CalendarVO.class);
            calendarVOS.add(calendarVO);
            return Result.success(calendarVOS);
        }
    }
    /**
     * 用户日历计划表(填写 或 修改)
     */
@PostMapping("api/interview/sessions/setcalendar")
public Result setCalendar(@RequestBody CalendarDTO calendarDTO){
    Calendar oldCalendar = calendarReposity.findByUserIdAndYearAndMonthAndDayOrderByIdDesc(calendarDTO.getUserId(), calendarDTO.getYear(), calendarDTO.getMonth(), calendarDTO.getDay());
    if(oldCalendar!=null){
        oldCalendar.setAssignment(calendarDTO.getAssignment());
        calendarReposity.save(oldCalendar);
        return Result.success();
    }
    Calendar calendar=Calendar.builder()
            .day(calendarDTO.getDay())
            .year(calendarDTO.getYear())
            .month(calendarDTO.getMonth())
            .assignment(calendarDTO.getAssignment())
            .userId(calendarDTO.getUserId())
            .build();
    calendarReposity.save(calendar);
    return Result.success();
}
     /**
     * 用户打卡
     */
    @PostMapping("api/interview/sessions/setcard")
    public Result setCardRecords(@RequestBody CardRecordsDTO cardRecordsDTO){
        CardRecord record = cardRecordRepository.findByUserIdAndYearAndMonthOrderByIdDesc(cardRecordsDTO.getUserId(), cardRecordsDTO.getYear(), cardRecordsDTO.getMonth());
        if (record == null) {
            // 真正没记录时，才 new 一个
            record = CardRecord.builder()
                    .year(cardRecordsDTO.getYear())
                    .month(cardRecordsDTO.getMonth())
                    .userId(cardRecordsDTO.getUserId())
                    .status(0) // 初始设为 0，统一后续逻辑
                    .build();
        }

// 统一更新 status
        int currentStatus = record.getStatus();
        record.setStatus(currentStatus | (1 << (cardRecordsDTO.getDay() - 1)));

// 保存（如果有 ID 则是 update，没 ID 则是 insert）
        cardRecordRepository.save(record);
        return Result.success();
    }
    /**
     * 用户当月打卡记录获取
     */
    @PostMapping("api/interview/sessions/getcards")
    public Result<int[]>getCardRecords(@RequestBody CardRecordsDTO cardRecordsDTO){
        CardRecord record = cardRecordRepository.findByUserIdAndYearAndMonthOrderByIdDesc(cardRecordsDTO.getUserId(), cardRecordsDTO.getYear(), cardRecordsDTO.getMonth());
        // 2. 判空：如果本月没记录，直接返回空数组
        if (record == null || record.getStatus() == 0) {
            return Result.success(new int[0]);
        }

        int status = record.getStatus();
        List<Integer> list = new ArrayList<>();

        // 3. 提取打卡日期 (更优雅的位运算)
        // 循环 31 次（一个月最多 31 天）
        for (int i = 1; i <= 31; i++) {
            // 使用位与运算：检查 status 的第 (i-1) 位是否为 1
            // 1 << (i-1) 是将 1 左移，比如 i=3，就是检查 00000100
            if (((status >> (i - 1)) & 1) == 1) {
                list.add(i);
            }
        }

        // 4. 转换返回类型 (将 List<Integer> 转为 int[])
        int[] result = list.stream().mapToInt(Integer::intValue).toArray();
        return Result.success(result);

    }

    /**
     * 获取面试分数统计
     */
    @GetMapping("api/interview/sessions/getscores/{resumeId}")
    public Result<List<ScoreEntity>> getSomeScores(@PathVariable Long resumeId) {
        List<InterviewSessionEntity> byResumeIdOrderByCreatedAtDesc = sessionRepository.findByResumeIdOrderByCreatedAtDesc(resumeId);
        List<ScoreEntity> list = new ArrayList<>();
        for (InterviewSessionEntity session : byResumeIdOrderByCreatedAtDesc) {
            ScoreEntity s = new ScoreEntity();
            s.setOverallScore(session.getOverallScore());
            List<InterviewAnswerEntity> ans = session.getAnswers();
            List<ScoreQuestionEntity> scoreQuestionEntityList = new ArrayList<>();
            for (InterviewAnswerEntity a : ans) {
                ScoreQuestionEntity scoreQuestionEntity = new ScoreQuestionEntity();
                scoreQuestionEntity.setCategory(a.getCategory());
                scoreQuestionEntity.setSocre(a.getScore());
                scoreQuestionEntityList.add(scoreQuestionEntity);
            }
            s.setScoreQuestionEntity(scoreQuestionEntityList);
            list.add(s);
        }
        return Result.success(list);
    }


    /**
     * 每日一题
     */
    @GetMapping("api/interview/sessions/getquestion/")
    public Result<List<TestQuestionEntity>> getQuestionDay(){
        Long id1 = (long) (LocalDateTime.now().getDayOfMonth()%20);
        Long id2=(id1+5)%20;
        Long id3=(id2+5)%20;
        List<TestQuestionEntity> list = List.of(
                testQuestionRepository.getTestQuestionEntitiesById(id1),
                testQuestionRepository.getTestQuestionEntitiesById(id2),
                testQuestionRepository.getTestQuestionEntitiesById(id3)
        );
        return Result.success(list);
    }
    /**
     * 创建面试会话
     */
    @PostMapping("/api/interview/sessions")
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 5)
    public Result<InterviewSessionDTO> createSession(@RequestBody CreateInterviewRequest request) {
        log.info("创建面试会话，题目数量: {}", request.questionCount());
        InterviewSessionDTO session = sessionService.createSession(request);
        return Result.success(session);
    }
    
    /**
     * 获取会话信息
     */
    @GetMapping("/api/interview/sessions/{sessionId}")
    public Result<InterviewSessionDTO> getSession(@PathVariable String sessionId) {
        InterviewSessionDTO session = sessionService.getSession(sessionId);
        return Result.success(session);
    }
    
    /**
     * 获取当前问题
     */
    @GetMapping("/api/interview/sessions/{sessionId}/question")
    public Result<Map<String, Object>> getCurrentQuestion(@PathVariable String sessionId) {
        return Result.success(sessionService.getCurrentQuestionResponse(sessionId));
    }
    
    /**
     * 提交任何答案
     */
    @PostMapping("/api/interview/sessions/{sessionId}/answers")
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL}, count = 10)
    public Result<SubmitAnswerResponse> submitAnswer(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> body) {
        Integer questionIndex = (Integer) body.get("questionIndex");
        String answer = (String) body.get("answer");
        Integer addQuestionIndex = (Integer) body.get("addQuestionIndex");
        int safeAddQuestionIndex = addQuestionIndex == null ? 0 : addQuestionIndex;
        log.info("提交答案: 会话{}, 问题{}", sessionId, questionIndex);
        log.info("addQuestionIndex:{}",safeAddQuestionIndex);
        // #region agent log
        try {
            Files.writeString(
                    Path.of("debug-bfb5dd.log"),
                    ("{\"sessionId\":\"bfb5dd\",\"runId\":\"pre-fix\",\"hypothesisId\":\"D_addQuestionIndex_null\",\"location\":\"app/src/main/java/interview/guide/modules/interview/InterviewController.java:submitAnswer\",\"message\":\"submitAnswer body parsed\",\"data\":{\"hasAddQuestionIndexKey\":" + body.containsKey("addQuestionIndex") + ",\"addQuestionIndexRaw\":" + (addQuestionIndex == null ? "null" : addQuestionIndex) + ",\"safeAddQuestionIndex\":" + safeAddQuestionIndex + ",\"questionIndex\":" + (questionIndex == null ? "null" : questionIndex) + ",\"answerLen\":" + (answer == null ? 0 : answer.length()) + "},\"timestamp\":" + System.currentTimeMillis() + "}\n"),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND
            );
        } catch (Exception ignored) {
        }
        // #endregion
        SubmitAnswerRequest request = new SubmitAnswerRequest(sessionId, questionIndex, answer, safeAddQuestionIndex);
        SubmitAnswerResponse response = sessionService.submitAnswer(request);
        return Result.success(response);
    }
///**
//    * 提交追问答案
// */
//    @PostMapping("/api/interview/sessions/{sessionId}/addAnswers")
//    public Result<AddQuestionVO>submitAddAnswers(@PathVariable String sessionId,
//                                                 @RequestBody Map<String,Object>body){
//        Integer questionIndex =(Integer) body.get("questionIndex");
//        String answer = (String) body.get("answer");
//        Integer addQuestionIndex=(Integer)body.get("addQuestionIndex");
//        log.info("提交：会话{}，第{}次追问答案，问题：{}",sessionId,addQuestionIndex,answer);
//                AddQuestionVO addQuestionVO=sessionService.submitAddAnswers(questionIndex,answer,addQuestionIndex,sessionId);
//                return Result.success(addQuestionVO);
//    }
    /**
     * 生成面试报告
     */
    @GetMapping("/api/interview/sessions/{sessionId}/report")
    public Result<InterviewReportDTO> getReport(@PathVariable String sessionId) {
        log.info("生成面试报告: {}", sessionId);
        InterviewReportDTO report = sessionService.generateReport(sessionId);
        return Result.success(report);
    }
    
    /**
     * 查找未完成的面试会话
     * GET /api/interview/sessions/unfinished/{resumeId}
     */
    @GetMapping("/api/interview/sessions/unfinished/{resumeId}")
    public Result<InterviewSessionDTO> findUnfinishedSession(@PathVariable Long resumeId) {
        return Result.success(sessionService.findUnfinishedSessionOrThrow(resumeId));
    }
    
    /**
     * 暂存答案（不进入下一题）
     */
    @PutMapping("/api/interview/sessions/{sessionId}/answers")
    public Result<Void> saveAnswer(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> body) {
        Integer questionIndex = (Integer) body.get("questionIndex");
        String answer = (String) body.get("answer");
        Integer addQuestionIndex=(Integer)body.get("addQuestionIndex");
        log.info("暂存答案: 会话{}, 问题{}", sessionId, questionIndex);
        SubmitAnswerRequest request = new SubmitAnswerRequest(sessionId, questionIndex, answer,addQuestionIndex);
        sessionService.saveAnswer(request);
        return Result.success(null);
    }
    
    /**
     * 提前交卷
     */
    @PostMapping("/api/interview/sessions/{sessionId}/complete")
    public Result<Void> completeInterview(@PathVariable String sessionId) {
        log.info("提前交卷: {}", sessionId);
        sessionService.completeInterview(sessionId);
        return Result.success(null);
    }
    
    /**
     * 获取面试会话详情
     * GET /api/interview/sessions/{sessionId}/details
     */
    @GetMapping("/api/interview/sessions/{sessionId}/details")
    public Result<InterviewDetailDTO> getInterviewDetail(@PathVariable String sessionId) {
        InterviewDetailDTO detail = historyService.getInterviewDetail(sessionId);
        return Result.success(detail);
    }
    
    /**
     * 导出面试报告为PDF
     */
    @GetMapping("/api/interview/sessions/{sessionId}/export")
    public ResponseEntity<byte[]> exportInterviewPdf(@PathVariable String sessionId) {
        try {
            byte[] pdfBytes = historyService.exportInterviewPdf(sessionId);
            String filename = URLEncoder.encode("模拟面试报告_" + sessionId + ".pdf", 
                StandardCharsets.UTF_8);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
        } catch (Exception e) {
            log.error("导出PDF失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 删除面试会话
     */
    @DeleteMapping("/api/interview/sessions/{sessionId}")
    public Result<Void> deleteInterview(@PathVariable String sessionId) {
        log.info("删除面试会话: {}", sessionId);
        persistenceService.deleteSessionBySessionId(sessionId);
        return Result.success(null);
    }

    private final TTSService textToSpeechService;
    /*
     * 获取 TTS 语音
     */
    @PostMapping("/api/interview/sessions/tts")
    public ResponseEntity<Resource> getTtsMp3(@RequestBody TtsDTO req) {
        try {
            log.info("收到 TTS 请求，文本长度：{}", req.getText().length());

            // 调用 TTS 服务生成语音
            byte[] mp3Bytes = textToSpeechService.generateSpeech(
                    req.getText(),
                    req.getVoice(),
                    req.getRate()
            );

            ByteArrayResource resource = new ByteArrayResource(mp3Bytes);

            // 返回标准 MP3
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            String fileName = URLEncoder.encode("voice.mp3", StandardCharsets.UTF_8);
            headers.add("Content-Disposition", "inline;filename*=UTF-8''" + fileName);

            return ResponseEntity.ok().headers(headers).body(resource);

        } catch (Exception e) {
            log.error("TTS 语音生成失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    /**
     * 流式 TTS（SSE）- 实时生成并播放语音
     */
    @PostMapping(value = "/api/interview/sessions/tts/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<byte[]>> getTtsStream(@RequestBody TtsDTO req) {
        log.info("收到流式 TTS 请求，文本长度：{}", req.getText().length());

        return textToSpeechService.generateSpeechStream(req.getText(), req.getVoice(), req.getRate())
                .map(chunk -> ServerSentEvent.builder(chunk).build())
                .onErrorResume(e -> {
                    log.error("流式 TTS 失败", e);
                    return Flux.empty();
                });
    }

    private final FileStorageService ossFileUploadUtil;
    private final AsrAnalysisService asrAnalysisService;

    /**
     * 上传音频文件并分析
     * @param file 前端上传的音频文件（MP3/WAV）
     */
    /*@PostMapping("/api/interview/sessions/asr")
    public ResponseEntity<String> analyzeAudioFile(@RequestParam("file") MultipartFile file) {
        String ossFileUrl = null;
        try {
            // 1. 上传文件到 OSS
            String fileKey = ossFileUploadUtil.uploadFile(file, "interview-audio");
            // 2. 生成完整的 HTTP URL
            ossFileUrl = ossFileUploadUtil.getFileUrl(fileKey);
            log.info("OSS 文件 URL: {}", ossFileUrl);
            // 3. 调用ASR分析
            String result = asrAnalysisService.analyzeAudioByUrl(ossFileUrl);
            // 4. 可选：删除OSS临时文件（若不需要保留）
            ossFileUploadUtil.deleteFile(fileKey);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // 失败时清理OSS文件
            if (ossFileUrl != null) {
                ossFileUploadUtil.deleteFile(ossFileUrl);
            }
            log.error("音频分析失败", e);
            return ResponseEntity.badRequest().body(null);
        }*/
    // ... existing code ...
    @PostMapping("/api/interview/sessions/asr")
    public ResponseEntity<AsrAnalysisResult> analyzeAudioFile(@RequestParam("file") MultipartFile file) {
        String ossFileUrl = null;
        try {
            // 1. 上传文件到 OSS
            String fileKey = ossFileUploadUtil.uploadFile(file, "interview-audio");
            // 2. 生成完整的 HTTP URL
            ossFileUrl = ossFileUploadUtil.getFileUrl(fileKey);
            log.info("OSS 文件 URL: {}", ossFileUrl);

            // 3. 调用 ASR 分析
            AsrAnalysisResult result = asrAnalysisService.analyzeAudioByUrl(ossFileUrl);

            // 4. 删除 OSS 临时文件
            ossFileUploadUtil.deleteFile(fileKey);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // 失败时清理 OSS 文件
            if (ossFileUrl != null) {
                ossFileUploadUtil.deleteFile(ossFileUrl);
            }
            log.error("音频分析失败", e);
            return ResponseEntity.badRequest().body(null);
        }
    }
    /**
     * 实时 ASR 识别 - 快速版本
     * 直接处理音频文件，不经过 OSS 存储
     */
    @PostMapping("/api/interview/sessions/asr/realtime")
    public ResponseEntity<AsrAnalysisResult> realTimeAsr(@RequestParam("file") MultipartFile file) throws Exception {
        log.info("收到实时 ASR 请求，文件大小：{} bytes", file.getSize());

        AsrAnalysisResult result = asrAnalysisService.recognizeRealTime(file).get();
        return ResponseEntity.ok(result);
    }

    /**
     * 面试结束后语音分析（可选自动交卷）
     * 场景：用户通过语音完成面试后，上传整段录音，返回自信度/清晰度/语速等分析结果。
     */
    @PostMapping("/api/interview/sessions/{sessionId}/voice-analysis")
    public ResponseEntity<AsrAnalysisResult> analyzeInterviewVoiceAfterComplete(
            @PathVariable String sessionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "autoComplete", defaultValue = "true") boolean autoComplete) {
        try {
            if (autoComplete) {
                sessionService.completeInterview(sessionId);
            }
            AsrAnalysisResult result = asrAnalysisService.recognizeRealTime(file).get();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("面试结束语音分析失败: sessionId={}", sessionId, e);
            return ResponseEntity.badRequest().body(null);
        }
    }
}

