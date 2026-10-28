import java.time.LocalDate;

/**
 * Runnable entry point for the educator report. The same client, key, and base URL
 * read job runs, dead letters, and captured exceptions without an intermediary service.
 */
public final class CourseDeliveryVisibilityApplication {
    public static void main(String[] args) throws Exception {
        String jobId = required("COURSE_DELIVERY_JOB_ID");
        String queue = required("COURSE_DELIVERY_QUEUE");
        InfraiLearningClient infrai = new InfraiLearningClient(ServiceSettings.load());

        LearnerDeadline deadline = new LearnerDeadline(
                "learner-204", "algebra-foundations", LocalDate.now(), false);
        String decision = deadline.educatorStatus(LocalDate.now());

        String runs = infrai.recentCourseRuns(jobId);
        String deadLetters = infrai.stalledDeliveryLetters(queue);
        String errors = infrai.capturedTeachingErrors();
        System.out.println("educator decision=" + decision);
        System.out.println("recent course runs=" + runs);
        System.out.println("undelivered lesson letters=" + deadLetters);
        System.out.println("captured course errors=" + errors);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Set " + name);
        return value;
    }
}
