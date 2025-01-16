package personal_project.moment_talk.common.exception;

public class ParticipantLimitException extends MomentTalkException {

    public ParticipantLimitException() {
        super(ErrorCode.PARTICIPANT_LIMIT, "빈 자리가 없습니다");
    }
}
