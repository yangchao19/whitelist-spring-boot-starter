package com.yang.midware.whitelist;

import com.alibaba.fastjson.JSON;
import com.yang.midware.whitelist.annotation.DoWhiteList;
import org.apache.commons.beanutils.BeanUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ForkJoinTask;

/**
 * @description:
 * @author：杨超
 * @date: 2023/7/6
 * @Copyright：
 */
@Aspect
@Component
public class DoJoinPoint {

    private Logger logger = LoggerFactory.getLogger(DoJoinPoint.class);

    @Resource
    private String whiteListConfig;

    @Pointcut("@annotation(com.yang.midware.whitelist.annotation.DoWhiteList)")
    public void aopPoint() {
    }

    @Around("aopPoint()")
    public Object doRouter(ProceedingJoinPoint jp) throws Throwable {

        //获取方法、注解
        Method method = getMethod(jp);
        DoWhiteList whiteList = method.getAnnotation(DoWhiteList.class);

        //获取字段值
        String keyValue = getFiledValue(whiteList.key(), jp.getArgs());
        logger.info("middleware whitelist handler method:{} value:{}",method.getName(),keyValue);

        //如果获取的字段为空，则方法不过滤
        if (null == keyValue || "".equals(keyValue)) {
            return jp.proceed();
        }

        //白名单过滤
        String[] split = whiteListConfig.split(",");
        for (String str : split) {
            if (keyValue.equals(str)) {
                return jp.proceed();
            }
        }

        //拦截
        return returnObject(whiteList,method);
    }

    /**
     * 该方法通过JoinPoint，先获取方法的签名对象
     * 将签名对象强转为 MethodSignature 以便获取更多方法的相关信息
     * 然后通过方法名，和参数类型列表获取 Method 对象
     * @param jp JoinPoint对象
     * @return 指定的方法
     * @throws NoSuchMethodException
     */
    private Method getMethod(JoinPoint jp) throws NoSuchMethodException {
        Signature signature = jp.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        return jp.getTarget().getClass().getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
    }

    /**
     *获取方法入参中对应字段的属性值
     * @param filed 目标字段
     * @param args 入参
     * @return 存在则返回目标字段，否则为空
     */
    private String getFiledValue(String filed, Object[] args) {
        String filedValue = null;
        for (Object arg : args) {
            try {
                if (null == filedValue || "".equals(filedValue)) {
                    filedValue = BeanUtils.getProperty(arg,filed);
                }else {
                    break;
                }
            } catch (Exception e) {
                //了在无法获取属性值时，返回一个默认值。如果参数 args 的长度为 1，
                // 表示只有一个参数，那么可能是期望这个参数作为默认值返回。
                // 这样可以确保在出现异常时，仍然能够返回一个有效的值，避免返回空值或抛出异常。
                if (args.length == 1) {
                    return args[0].toString();
                }
            }
        }
        return filedValue;
    }

    /**
     * 根据注解中的属性，构造方法返回类型的对象
     * @param whiteList 注解
     * @param method 方法
     * @return 方法返回对象
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private Object returnObject(DoWhiteList whiteList, Method method) throws IllegalAccessException, InstantiationException {
        Class<?> returnType = method.getReturnType();
        String returnJson = whiteList.returnJson();

        if ("".equals(returnJson)) {
            return returnType.newInstance();
        }

        return JSON.parseObject(returnJson,returnType);
    }
}
