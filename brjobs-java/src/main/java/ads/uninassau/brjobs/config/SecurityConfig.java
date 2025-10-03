package ads.uninassau.brjobs.config;

import ads.uninassau.brjobs.security.CustomUserDetailsService;
import ads.uninassau.brjobs.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Autowired private CustomUserDetailsService userDetailsService;

    // O WebSecurityCustomizer foi removido para resolver o WARN e o conflito.

    // 1. O MÉTODO authenticationProvider() RESTAURADO E ESSENCIAL PARA O LOGIN
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. O securityFilterChain agora tem o authenticationProvider E as rotas do Swagger liberadas.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Adiciona o provedor de autenticação (usa o método RESTAURADO)
        http.authenticationProvider(authenticationProvider());

        http.authorizeHttpRequests(authorize -> authorize
                // PRIORITY 1: Libera TODAS as rotas do Swagger (Solução final para o 403)
                .requestMatchers(
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**"
                ).permitAll()

                // PRIORITY 2: Libera as rotas de Auth
                .requestMatchers("/api/auth/**").permitAll()

                // PRIORITY 3: Protege todo o resto
                .anyRequest().authenticated()
        );

        // O filtro JWT volta ao seu lugar
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}