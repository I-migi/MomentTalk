package personal_project.moment_talk.common.exception;

import lombok.Getter;

@Getter
public class MomentTalkException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String errorMessage;

    public MomentTalkException(ErrorCode errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
