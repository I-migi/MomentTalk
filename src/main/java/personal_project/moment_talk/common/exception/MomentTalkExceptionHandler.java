package personal_project.moment_talk.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import personal_project.moment_talk.chat.controller.ChatController;
import personal_project.moment_talk.chat.controller.GroupChatController;
import personal_project.moment_talk.chat.controller.MusicGameController;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice(basePackageClasses = {ChatController.class, GroupChatController.class, MusicGameController.class})
public class MomentTalkExceptionHandler {

    @ExceptionHandler(value = MomentTalkException.class)
    public ResponseEntity<Map<String, String>> handleMomentTalkException(MomentTalkException momentTalkException) {
        ErrorCode errorCode = momentTalkException.getErrorCode();
        String errorMessage = momentTalkException.getErrorMessage();

        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("errorCode",errorCode.name());
        errorResponse.put("errorMessage",errorMessage);

        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(errorResponse);


    }
}