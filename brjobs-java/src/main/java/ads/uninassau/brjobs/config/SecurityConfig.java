package ads.uninassau.brjobs.config;

import ads.uninassau.brjobs.security.CustomUserDetailsService;
import ads.uninassau.brjobs.security.JwtAuthenticationFilter;
import ads.uninassau.brjobs.security.TenantFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    @Autowired private TenantFilter tenantFilter;
    @Autowired private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Autowired private CustomUserDetailsService userDetailsService;

    /**
     * Configura o provedor de autenticação usando o UserDetailsService e o PasswordEncoder.
     * Este é o provedor principal para login via usuário/senha.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Expõe o AuthenticationManager para ser usado no serviço de autenticação (AuthService).
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        // IMPORTANTE: O AuthenticationConfiguration já integra o authenticationProvider()
        // que foi adicionado via http.authenticationProvider() na SecurityFilterChain
        return config.getAuthenticationManager();
    }

    /**
     * Define o encoder de senha (BCrypt) para criptografar e verificar senhas.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "https://brjobs.com.br",
                "https://www.brjobs.com.br",
                "https://brjobs-angular.pages.dev"
        ));

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-XSRF-TOKEN"
        ));

        configuration.setExposedHeaders(List.of(
                "Authorization"
        ));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * Configura a cadeia de filtros de segurança HTTP.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(request -> {
                            String path = request.getRequestURI();
                            if (path.startsWith("/api/auth/")) {
                                return true;
                            }
                            return !path.startsWith("/api/v1/auth/")
                                    || "/api/v1/auth/csrf".equals(path)
                                    || "/api/v1/auth/login".equals(path)
                                    || "/api/v1/auth/social/google".equals(path)
                                    || "/api/v1/auth/social/facebook".equals(path);
                        })
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Configura a política de criação de sessão como stateless (sem sessão HTTP)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Adiciona o provedor de autenticação customizado
        http.authenticationProvider(authenticationProvider());

        // Configuração de autorização das requisições
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Libera todas as rotas do Swagger/OpenAPI para que a documentação possa ser acessada
                .requestMatchers(
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                    "/webjars/**",
                    "/error"
                ).permitAll()

                // Libera as rotas de autenticação (login, registro, social-login)
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/social/google").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/publicacoes/minhas").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/publicacoes/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/highlight/plans").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/webhook/stripe").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/usuarios/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/prestadores/usuario/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/avaliacoes/prestador/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/avaliacoes/usuario/*/recebidas").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/avaliacoes/v1/prestador/*/stats").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/avaliacoes/v1/usuario/*/stats").permitAll()

                // Libera as rotas de registro de novos usuários
                .requestMatchers("/api/usuarios/contratante", "/api/usuarios/prestador").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/usuarios/email/**").permitAll()

                // Todas as outras requisições devem ser autenticadas
                .anyRequest().authenticated()
        );

            // Logs detalhados para erros de autenticação/autorização (401/403)
            http.exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    log.warn("SECURITY 401: method={} path={} origin={} authHeaderPresent={} message={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        request.getHeader("Origin"),
                        request.getHeader("Authorization") != null,
                        authException.getMessage());
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    log.warn("SECURITY 403: method={} path={} origin={} authHeaderPresent={} tenantAttr={} message={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        request.getHeader("Origin"),
                        request.getHeader("Authorization") != null,
                        request.getAttribute("tenant_id"),
                        accessDeniedException.getMessage());
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
                })
            );

        // Adiciona o filtro de tenant ANTES do filtro JWT customizado
        // TenantFilter: extrai tenant_id do JWT e armazena no request
        // JwtAuthenticationFilter: autentica o usuário
        http.addFilterBefore(tenantFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
