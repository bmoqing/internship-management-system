package fun.bmoqing.internship.config;

import fun.bmoqing.internship.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleBodyFormatError(HttpMessageNotReadableException ex) {
        return Result.validationError("请求体格式错误，请检查输入内容");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingParam(MissingServletRequestParameterException ex) {
        return Result.validationError("缺少必要参数: " + ex.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return Result.validationError("参数格式错误: " + ex.getName());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgument(IllegalArgumentException ex) {
        return Result.validationError(ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return Result.validationError("上传文件过大，最大支持20MB");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<?> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return Result.methodNotAllowed("请求方式不支持: " + ex.getMethod());
    }

    @ExceptionHandler(DataAccessException.class)
    public Result<?> handleDataAccessException(DataAccessException ex) {
        log.error("Database access exception", ex);
        String rootMessage = ex.getMostSpecificCause() == null
                ? ex.getMessage()
                : ex.getMostSpecificCause().getMessage();
        if (rootMessage != null) {
            String lower = rootMessage.toLowerCase();
            if (lower.contains("doesn't exist")
                    || lower.contains("unknown column")
                    || lower.contains("sqlsyntaxerrorexception")
                    || lower.contains("error code [1146]")
                    || lower.contains("error code [1054]")) {
                return Result.serverError("数据库结构与当前代码不一致，请重新导入最新 internship_db.sql 并执行 scripts/seed_stage4_data.sql");
            }
        }
        return Result.serverError("数据库访问异常，请稍后重试");
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleUnknownException(Exception ex) {
        log.error("Unhandled exception", ex);
        return Result.serverError("系统繁忙，请稍后重试");
    }
}
