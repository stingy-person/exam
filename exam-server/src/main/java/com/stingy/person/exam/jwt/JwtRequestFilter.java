package com.stingy.person.exam.jwt;


import com.stingy.person.exam.dao.mapper.LoginUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/***
 * JWT过滤器
 *
 * @author Administrator
 * @since 2022/1/23
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final String TOKEN_PREFIX = "Bearer ";

    private LoginUserDetailsService loginUserDetailsService;

    private JwtTokenUtil jwtTokenUtil;

    public JwtRequestFilter(LoginUserDetailsService loginUserDetailsService, JwtTokenUtil jwtTokenUtil) {
        this.loginUserDetailsService = loginUserDetailsService;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        final String requestTokenHeader = request.getHeader("Authorization");
        String username = null;
        String jwtToken = null;
        /*
         *JWT Token is in the form "Bearer token". Remove Bearer word and get
         */
        if (requestTokenHeader != null && requestTokenHeader.startsWith(TOKEN_PREFIX)) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                username = jwtTokenUtil.getUsernameFromToken(jwtToken);
            } catch (IllegalArgumentException e) {
                logger.error("Unable to get JWT Token");
            } catch (ExpiredJwtException e) {
                logger.error("JWT Token has expired");
            }
        } else {
            logger.debug("JWT Token does not begin with Bearer String");
        }
        /*
         * Once we get the token validate it.
         */
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = loginUserDetailsService.loadUserByUsername(username);

            /*
             * if token is valid configure Spring Security to manually set
             */
            if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                /*
                 * After setting the Authentication in the context, we specify
                 * that the current user is authenticated. So it passes the
                 * Spring Security Configurations successfully.
                 */
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            }

            request.setAttribute("X-USER-NAME", username);
        }
        chain.doFilter(request, response);
    }
}