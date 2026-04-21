package interview.guide.modules.interview.listener;

import interview.guide.common.async.AbstractStreamProducer;
import interview.guide.common.constant.AsyncTaskStreamConstants;
import interview.guide.common.model.AsyncTaskStatus;
import interview.guide.infrastructure.redis.RedisService;
import interview.guide.modules.interview.repository.InterviewSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 面试评估任务生产者
 * 负责发送评估任务到 Redis Stream
 */
@Slf4j
@Component
public class EvaluateStreamProducer extends AbstractStreamProducer<EvaluateStreamProducer.EvaluateTask> {

    private final InterviewSessionRepository sessionRepository;

    public record EvaluateTask(String sessionId, Long userId) {}

    public EvaluateStreamProducer(RedisService redisService, InterviewSessionRepository sessionRepository) {
        super(redisService);
        this.sessionRepository = sessionRepository;
    }

    /**
     * 发送评估任务到 Redis Stream
     *
     * @param sessionId 面试会话ID
     * @param userId 用户ID
     */
    public void sendEvaluateTask(String sessionId, Long userId) {
        sendTask(new EvaluateTask(sessionId, userId));
    }

    @Override
    protected String taskDisplayName() {
        return "评估";
    }

    @Override
    protected String streamKey() {
        return AsyncTaskStreamConstants.INTERVIEW_EVALUATE_STREAM_KEY;
    }

    @Override
    protected Map<String, String> buildMessage(EvaluateTask task) {
        Map<String, String> message = new HashMap<>();
        message.put(AsyncTaskStreamConstants.FIELD_SESSION_ID, task.sessionId());
        message.put(AsyncTaskStreamConstants.FIELD_USER_ID, String.valueOf(task.userId()));
        message.put(AsyncTaskStreamConstants.FIELD_RETRY_COUNT, "0");
        return message;
    }

    @Override
    protected String payloadIdentifier(EvaluateTask task) {
        return "sessionId=" + task.sessionId();
    }

    @Override
    protected void onSendFailed(EvaluateTask task, String error) {
        updateEvaluateStatus(task.sessionId(), AsyncTaskStatus.FAILED, truncateError(error));
    }

    /**
     * 更新评估状态
     */
    private void updateEvaluateStatus(String sessionId, AsyncTaskStatus status, String error) {
        sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            session.setEvaluateStatus(status);
            if (error != null) {
                session.setEvaluateError(error.length() > 500 ? error.substring(0, 500) : error);
            }
            sessionRepository.save(session);
        });
    }
}
