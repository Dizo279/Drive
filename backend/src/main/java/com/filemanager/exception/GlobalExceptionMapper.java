package com.filemanager.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {
        // Ghi log lỗi ra console để dev dễ debug
        exception.printStackTrace(); 

        int statusCode = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        String message = "Đã xảy ra lỗi hệ thống!";

        // Nếu là lỗi chủ động do chúng ta ném ra (ví dụ: 404 Not Found, 400 Bad Request)
        if (exception instanceof WebApplicationException) {
            WebApplicationException webAppException = (WebApplicationException) exception;
            statusCode = webAppException.getResponse().getStatus();
            
            // Lấy body lỗi nếu có, không thì lấy message mặc định
            if (webAppException.getResponse().getEntity() != null) {
                message = webAppException.getResponse().getEntity().toString();
            } else {
                message = webAppException.getMessage();
            }
        }

        // Trả về JSON chuẩn có chứa key "message"
        return Response.status(statusCode)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", true, "message", message))
                .build();
    }
}