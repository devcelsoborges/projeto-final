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

    /**
     * Configura o provedor de autenticação usando o UserDetailsService e o PasswordEncoder.
     * Este é o provedor principal para login via usuário/senha.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Expõe o AuthenticationManager para ser usado no serviço de autenticação (AuthService).
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Define o encoder de senha (BCrypt) para criptografar e verificar senhas.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configura a cadeia de filtros de segurança HTTP.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Desabilita CSRF (Cross-Site Request Forgery) pois estamos usando JWT (stateless)
                .csrf(csrf -> csrf.disable())
                // Configura a política de criação de sessão como stateless (sem sessão HTTP)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Adiciona o provedor de autenticação customizado
        http.authenticationProvider(authenticationProvider());

        // Configuração de autorização das requisições
        http.authorizeHttpRequests(authorize -> authorize
                // Libera todas as rotas do Swagger/OpenAPI para que a documentação possa ser acessada
                .requestMatchers(
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**"
                ).permitAll()

                // Libera as rotas de autenticação (login, registro)
                .requestMatchers("/api/auth/**").permitAll()

                // Todas as outras requisições devem ser autenticadas
                .anyRequest().authenticated()
        );

        // Adiciona o filtro JWT customizado ANTES do filtro padrão de autenticação de usuário/senha
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}