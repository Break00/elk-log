package com.lee.jason.logaspect;

import cn.hutool.json.JSONUtil;
import com.lee.jason.util.IpUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.ExtendedServletRequestDataBinder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponseWrapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * @author huanli9
 * @description
 * @date 2021/9/6 22:58
 */
@Aspect
@Component
@Slf4j
public class LogAspect {
    /**
     * 换行符
     */
    private static final String LINE_SEPARATOR = System.lineSeparator();

    private static final String UNIQUE_ID = "traceId";


    /**
     * 以自定义 @WebLog 注解为切点
     */
    @Pointcut("execution(public * com.lee.jason.controller..*.*(..))")
    public void webLog() {
        // do nothing
    }

    /**
     * 在切点之前织入
     *
     * @param joinPoint 默认参数
     */
    @Before("webLog()")
    public void doBefore(JoinPoint joinPoint) {
        // 开始打印请求日志
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = Objects.requireNonNull(attributes).getRequest();
        // 添加日志追踪-traceid
        MDC.put(UNIQUE_ID, UUID.randomUUID().toString().replace("-", ""));

        // 打印请求相关参数
        log.info("========================================== 接口请求信息开始 ==========================================");
        // 打印请求 url
        log.info("请求URL            : {}", request.getRequestURL().toString());
        // 打印 Http method
        log.info("接口HTTP请求方式    : {}", request.getMethod());
        // 打印调用 controller 的全路径以及执行方法
        log.info("类名路径   : {}.{}", joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());
        // 打印请求的 IP
        log.info("请求IP地址             : {}", IpUtils.getIpAddress(request));
        // 打印请求入参
        log.info("请求参数：" + getMethodInfo(joinPoint));
    }


    /**
     * 在切点之后
     */
    @AfterReturning("webLog()")
    public void doAfterReturning() {
        // 接口结束后换行，方便分割查看
        log.info("=========================================== 接口请求信息结束 ===========================================" + LINE_SEPARATOR);
        // 删除此次调用日志追踪-traceid
        MDC.remove(UNIQUE_ID);
    }

    /**
     * 环绕
     *
     * @param proceedingJoinPoint 默认
     * @return 接口结果result
     */
    @Around("webLog()")
    public Object doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = proceedingJoinPoint.proceed();
        //打印出参

        log.info("响应数据  : {}", JSONUtil.toJsonStr(result));
        // 执行耗时
        log.info("接口响应时间 : {} ms", System.currentTimeMillis() - startTime);
        return result;
    }


    /**
     * @param point 实例传入
     * @return 处理完成的结果String
     */
    private String getMethodInfo(JoinPoint point) {
        // 下面两个数组中，参数值和参数名的个数和位置是一一对应的。
        Object[] objs = point.getArgs();
        // 参数名
        String[] argNames = ((MethodSignature) point.getSignature()).getParameterNames();
        Map<String, Object> paramMap = new HashMap<>(objs.length);
        for (int i = 0; i < objs.length; i++) {
            if (!(objs[i] instanceof ExtendedServletRequestDataBinder) && !(objs[i] instanceof HttpServletResponseWrapper)) {
                paramMap.put(argNames[i], objs[i]);
            }
        }
        try {
            return JSONUtil.toJsonStr(paramMap);
        } catch (Exception e) {
            log.error("转化为json字符串错误！", e);
            return "转化为json字符串错误！";
        }
    }
}
