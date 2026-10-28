import java.time.LocalDate;

/** Focused business test. Run with: javac -d out src/main/java/*.java src/test/java/*.java && java -ea -cp out LearnerDeadlineTest */
public final class LearnerDeadlineTest {
    public static void main(String[] args) {
        LearnerDeadline blockedLesson = new LearnerDeadline(
                "learner-204", "algebra-foundations", LocalDate.of(2026, 9, 14), false);
        assert "ESCALATE_TO_EDUCATOR".equals(blockedLesson.educatorStatus(LocalDate.of(2026, 9, 14)));

        LearnerDeadline deliveredLesson = new LearnerDeadline(
                "learner-204", "algebra-foundations", LocalDate.of(2026, 9, 14), true);
        assert "DELIVERED".equals(deliveredLesson.educatorStatus(LocalDate.of(2026, 9, 14)));
    }
}
