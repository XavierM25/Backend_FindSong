package com.findsong.findsongapi.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Interceptor para medir el rendimiento de las solicitudes HTTP.
 * Registra el tiempo de ejecución de cada solicitud para análisis y
 * optimización.
 */
@Component
@Slf4j
public class PerformanceInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTRIBUTE = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
            ModelAndView modelAndView) throws Exception {
        // No necesitamos hacer nada aquí
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return;
        }

        long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
        long executionTime = System.currentTimeMillis() - startTime;

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        String controllerName = handlerMethod.getBeanType().getSimpleName();
        String methodName = handlerMethod.getMethod().getName();
        String path = request.getRequestURI();

        if (executionTime > 1000) {
            log.warn("Solicitud lenta [{}]: {} {} - {} ms", controllerName, methodName, path, executionTime);
        } else {
            log.debug("Solicitud [{}]: {} {} - {} ms", controllerName, methodName, path, executionTime);
        }
    }
}