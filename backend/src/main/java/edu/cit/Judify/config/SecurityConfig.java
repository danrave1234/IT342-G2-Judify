package edu.cit.Judify.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.core.Authentication;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // Disable CSRF for development
//            .cors(cors -> cors.configure(http)) // Enable CORS
            .authorizeHttpRequests(auth -> auth
                // Swagger UI endpoints
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll()
                // Authentication endpoints
                .requestMatchers("/api/users/authenticate", "/api/users/addUser", "/api/users/register").permitAll()
                // OAuth2 endpoints
                .requestMatchers("/oauth2/**", "/login/oauth2/**", "/api/users/oauth2-success", "/api/users/oauth2-failure").permitAll()
                .anyRequest().permitAll()  // Allow all requests during development
                // For production, replace the line above with something like:
                // .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                // This must match the redirect-uri in application.properties
                .loginProcessingUrl("/login/oauth2/code/google")
                .successHandler(oauth2AuthenticationSuccessHandler())
                .failureUrl("/api/users/oauth2-failure")
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * Create a custom success handler for OAuth2 login
     */
    public AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, 
                                                Authentication authentication) throws IOException, ServletException {
                response.sendRedirect("/api/users/oauth2-success");
            }
        };
    }
} 
