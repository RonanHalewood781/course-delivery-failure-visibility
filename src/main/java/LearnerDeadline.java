import java.time.LocalDate;

/** The teacher-facing decision: an undelivered lesson becomes urgent near a learner deadline. */
public record LearnerDeadline(String learnerId, String courseId, LocalDate dueOn, boolean lessonDelivered) {
    public String educatorStatus(LocalDate today) {
        if (lessonDelivered) return "DELIVERED";
        if (!dueOn.isAfter(today)) return "ESCALATE_TO_EDUCATOR";
        return "WATCH_DELIVERY";
    }
}
