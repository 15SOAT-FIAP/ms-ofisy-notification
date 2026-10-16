package br.com.ofisy.notifications.config;
import br.com.ofisy.notifications.application.notification.exceptions.NotificationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice
public class NotificationExceptionHandler {
    @ExceptionHandler(NotificationNotFoundException.class)
    public ProblemDetail handleNotificationNotFound(NotificationNotFoundException ex) {
        var problem=ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,ex.getMessage());
        problem.setTitle("Notificação não encontrada");
        return problem;
    }
}
