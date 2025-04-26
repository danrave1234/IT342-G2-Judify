package edu.cit.Judify.config;

import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

/**
 * This class registers the springSecurityFilterChain filter for the application.
 * It ensures that Spring Security is properly initialized and will intercept requests
 * to apply security filters.
 */
public class SecurityWebInitializer extends AbstractSecurityWebApplicationInitializer {
    // No code needed here - extends AbstractSecurityWebApplicationInitializer
    // to register the springSecurityFilterChain
} 