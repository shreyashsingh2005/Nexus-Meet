package Nexus_Meet.com;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // /signal (WebSocket endpoint), /dashboard, aur /room ko access dene ke liye matchers add kiye hain
                .requestMatchers("/", "/index.html", "/dashboard", "/room", "/signal", "/css/**", "/images/**", "/js/**").permitAll() 
                // Baaki sabhi requests ke liye authentication zaroori hogi
                .anyRequest().authenticated() 
            )
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl("/dashboard", true) // Login ke baad seedha dashboard par jayega
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            )
            .csrf(csrf -> csrf.disable());
            
        return http.build();
    }
}