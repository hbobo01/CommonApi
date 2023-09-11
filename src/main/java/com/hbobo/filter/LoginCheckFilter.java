package com.hbobo.filter;

import com.alibaba.fastjson.JSON;
import com.hbobo.common.BaseContext;
import com.hbobo.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 检查用户是否已经完成登录
 */
@Slf4j
@WebFilter(filterName = "loginCheckFilter",urlPatterns = "/*")
public class LoginCheckFilter implements Filter {
    //路径匹配器，支持通配符(因为下面用到通配符，所以定义这个用来匹配路径)
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        log.info("拦截到请求：{}",request.getRequestURI());

        /**
         * 1. 获取请求URI
         * 2. 判断本次请求是否需要处理
         * 3. 如果不需要处理直接放行
         * 4. 判断登录状态，如果已经登录，放行
         * 5. 如果未登录则返回未登录结果
         */

        //1. 获取请求URI
        String requestURI = request.getRequestURI();
        log.info("拦截到请求：{}",requestURI);

        //定义不需要处理的路径
        String[] urls = new String[]{
                "/employee/login",
                "/emplyee/logout",
                "/backend/**",
                "/front/**",
                "/common/**",
                "/user/sendMsg", //移动端发送短信
                "/user/login", //移动端登录
                "/doc.html",
                "/webjars/**",
                "/swagger-resources",
                "/v2/api-docs"
        };

        //2. 判断本次请求是否需要处理
        boolean check = check(urls, requestURI);
        if (check){
            //3. 如果不需要处理直接放行
            filterChain.doFilter(servletRequest,servletResponse);
            log.info("请求{}不需要处理",requestURI);
            return;
        }

        //4-1. 判断登录状态，如果已经登录，放行
        if (request.getSession().getAttribute("employee")!=null) {
            log.info("当前为登录状态，用户id：{}",request.getSession().getAttribute("employee"));

            Long empId = (Long) request.getSession().getAttribute("employee");
            BaseContext.setCurrentId(empId);

            filterChain.doFilter(servletRequest,servletResponse);
            return;
        }

        //4-2. 判断移动端用户登录状态，如果已经登录，放行
        if (request.getSession().getAttribute("user")!=null) {
            log.info("当前为登录状态，用户id：{}",request.getSession().getAttribute("user"));

            Long userId = (Long) request.getSession().getAttribute("user");
            BaseContext.setCurrentId(userId);

            filterChain.doFilter(servletRequest,servletResponse);
            return;
        }

        //5. 如果未登录则返回未登录结果,通过输出流方式向客户端页面响应数据
        response.getWriter().write(JSON.toJSONString(Result.error("NOTLOGIN")));
        log.info("未登录，响应 NOTLOGIN");
        return;
    }

    //检查路径是否匹配
    public boolean check(String[] urls,String requestURI){
        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, requestURI);
            if (match){
                return true;
            }
        }
        return false;
    }
}
