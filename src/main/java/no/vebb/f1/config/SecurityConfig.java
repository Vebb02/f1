package no.vebb.f1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http.authorizeHttpRequests(auth -> {
			auth.requestMatchers("/", "/favicon.ico", "/**.css", "/user/**", "/score/**").permitAll();
			auth.anyRequest().authenticated();	
		})
		.oauth2Login(Customizer.withDefaults())
		.logout(logout -> logout.logoutSuccessUrl("/"))
		.build();
	}


}
