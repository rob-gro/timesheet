package dev.robgro.timesheet.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class ViewExceptionHandler {

    @ExceptionHandler(BaseApplicationException.class)
    public String handleViewException(BaseApplicationException ex,
                                      HttpServletRequest request,
                                      RedirectAttributes redirectAttributes) {
        log.error("Application exception in view: {}", ex.getMessage(), ex);

        redirectAttributes.addFlashAttribute("error", ex.getMessage());

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public String handleValidationException(Exception ex,
                                            HttpServletRequest request,
                                            RedirectAttributes redirectAttributes) {
        log.error("Validation exception in view: {}", ex.getMessage(), ex);

        redirectAttributes.addFlashAttribute("error", "Validation failed. Please check your input.");

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex,
                                         HttpServletRequest request,
                                         RedirectAttributes redirectAttributes) {
        log.error("Unhandled exception in view: {}", ex.getMessage(), ex);

        redirectAttributes.addFlashAttribute("error", "An unexpected error occurred. Please try again later.");

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }
}
