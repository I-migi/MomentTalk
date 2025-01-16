package personal_project.moment_talk.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    PARTICIPANT_LIMIT(411, "노래 맞추기 게임에 빈 자리가 없습니다");

    private final int httpStatusCode;
    private final String description;

}
