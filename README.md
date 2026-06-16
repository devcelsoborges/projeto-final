# 🛠️ brjobs - Plataforma de Contratação de Serviços

[![Java 17+](https://img.shields.io/badge/Java-17%2B-red?style=flat-square&logo=java)]()
[![Spring Boot 3.3.5](https://img.shields.io/badge/Spring%20Boot-3.3.5-green?style=flat-square&logo=spring)]()
[![Angular 20](https://img.shields.io/badge/Angular-20-red?style=flat-square&logo=angular)]()
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-13%2B-blue?style=flat-square&logo=postgresql)]()
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)]()

---

## 📋 Sumário

1. [Visão Geral](#visão-geral)
2. [Tecnologias](#tecnologias)
3. [Arquitetura](#arquitetura)
4. [Estrutura do Projeto](#estrutura-do-projeto)
5. [DTOs - Data Transfer Objects](#dtos---data-transfer-objects)
6. [Segurança](#segurança)
7. [Testes Automatizados](#testes-automatizados)
8. [Diagramas UML](#diagramas-uml)
9. [Endpoints da API](#endpoints-da-api)
10. [Últimas Alterações](#últimas-alterações)
11. [Como Rodar](#como-rodar)
12. [Contribuições](#contribuições)
13. [Licença e Contato](#licença-e-contato)

---

## 👥 Visão Geral

O **brjobs** é uma plataforma web desenvolvida para **facilitar a comunicação entre trabalhadores autônomos e contratantes** em áreas como:

- 🏠 Limpeza de casas (diaristas, faxineiras)
- 🔧 Manutenção (eletricistas, encanadores)
- 🏗️ Construção civil (pedreiros, pintores)

### Objetivo Principal
Reduzir a dificuldade na busca por profissionais confiáveis, oferecendo um canal **simples, acessível e seguro** para ambas as partes. O projeto tem **impacto social** direto, ajudando trabalhadores a terem mais visibilidade e contratantes a encontrarem serviços de qualidade de forma rápida.

### Funcionalidades Principais
✅ Cadastro e autenticação segura de usuários (trabalhadores e contratantes)  
✅ Perfil profissional com descrição de serviços, experiência e contatos  
✅ Sistema de busca por categoria de serviço  
✅ Agendamento e solicitação de serviços  
✅ Avaliação e feedback de profissionais  
✅ Gerenciamento de tipos de usuários com permissões específicas  

---

## 🌐 Tecnologias

### Backend
| Tecnologia | Versão | Descrição |
|-----------|--------|----------|
| **Java** | 17+ | Linguagem de programação principal |
| **Spring Boot** | 3.3.5 | Framework web e REST APIs |
| **Spring Security** | 6.x | Autenticação e autorização |
| **JWT (JJWT)** | 0.12.6 | Tokens de autenticação stateless |
| **Spring Data JPA** | 6.x | ORM e persistência de dados |
| **Lombok** | 1.18+ | Redução de boilerplate |
| **OpenAPI/Swagger** | 2.5.0 | Documentação automática de APIs |
| **Validation** | Jakarta | Validação de dados |

### Frontend
| Tecnologia | Versão | Descrição |
|-----------|--------|----------|
| **Angular** | 20.3.0 | Framework web SPA |
| **TypeScript** | 5.9.2 | Linguagem tipada |
| **RxJS** | 7.8.0 | Programação reativa |
| **Bootstrap** | Integrado | Componentes UI responsivos |

### Banco de Dados
| Tecnologia | Uso |
|-----------|-----|
| **PostgreSQL** | 13+ - Banco de dados principal em produção |
| **H2** | Banco em memória para testes |

---

## 🏗️ Arquitetura

### Arquitetura em Camadas (Layered Architecture)

```
┌─────────────────────────────────────────────────────────┐
│                   ANGULAR FRONTEND                       │
│         (Componentes, Serviços, Routing)                │
└──────────────┬──────────────────────────────────────────┘
               │ HTTP/REST
┌──────────────▼──────────────────────────────────────────┐
│                 CONTROLLERS                              │
│  (AuthController, PrestadorController, etc)             │
└──────────────┬──────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────┐
│                   SERVICES                               │
│  (AuthService, PrestadorService, UsuarioService)        │
│  - Lógica de negócio                                    │
│  - Validações                                           │
│  - Transformação de dados                               │
└──────────────┬──────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────┐
│                REPOSITORIES (Data Access)                │
│  (UsuarioRepository, PrestadorRepository, etc)          │
│  - Spring Data JPA                                      │
│  - Queries customizadas                                 │
└──────────────┬──────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────┐
│                PostgreSQL DATABASE                       │
│  (usuarios, prestadores, avaliacoes, servicos, etc)     │
└──────────────────────────────────────────────────────────┘
```

### Padrões de Design Utilizados

| Padrão | Descrição |
|--------|-----------|
| **DTO (Data Transfer Object)** | Transferência de dados entre camadas |
| **Repository** | Abstração de acesso a dados |
| **Service Layer** | Encapsulamento de lógica de negócio |
| **Dependency Injection** | Spring IoC Container |
| **Exception Handling** | GlobalExceptionHandler customizado |
| **JWT Bearer Token** | Autenticação stateless |

---

## 📂 Estrutura do Projeto

```
projeto-final/
│
├── brjobs-java/
│   ├── src/main/java/ads/uninassau/brjobs/
│   │   ├── BrjobsApplication.java
│   │   ├── config/                          # Configurações (Security, CORS, OpenAPI)
│   │   │   ├── SecurityConfig.java
│   │   │   ├── CorsConfig.java
│   │   │   └── OpenApiConfig.java
│   │   ├── controller/                      # REST Controllers
│   │   │   ├── AuthController.java
│   │   │   ├── UsuarioController.java
│   │   │   ├── PrestadorController.java
│   │   │   ├── ServicoController.java
│   │   │   ├── SolicitacaoServicoController.java
│   │   │   └── AvaliacaoController.java
│   │   ├── service/                         # Lógica de Negócio
│   │   │   ├── AuthService.java
│   │   │   ├── UsuarioService.java
│   │   │   ├── PrestadorService.java
│   │   │   ├── ServicoService.java
│   │   │   ├── SolicitacaoServicoService.java
│   │   │   ├── AvaliacaoService.java
│   │   │   └── FileService.java
│   │   ├── dto/                             # Data Transfer Objects
│   │   │   ├── LoginRequestDTO.java
│   │   │   ├── LoginResponseDTO.java
│   │   │   ├── UsuarioDTO.java
│   │   │   ├── PrestadorDTO.java
│   │   │   ├── ServicoDTO.java
│   │   │   ├── SolicitacaoServicoDTO.java
│   │   │   ├── AvaliacaoDTO.java
│   │   │   ├── CadastroPrestadorDTO.java
│   │   │   ├── CadastroContratanteDTO.java
│   │   │   └── FileUploadDTO.java
│   │   ├── model/                           # Entidades JPA
│   │   │   ├── Usuario.java
│   │   │   ├── Prestador.java
│   │   │   ├── Servico.java
│   │   │   ├── SolicitacaoServico.java
│   │   │   ├── Avaliacao.java
│   │   │   └── TipoUsuario.java
│   │   ├── repository/                      # Data Access Layer
│   │   │   ├── UsuarioRepository.java
│   │   │   ├── PrestadorRepository.java
│   │   │   ├── ServicoRepository.java
│   │   │   ├── SolicitacaoServicoRepository.java
│   │   │   └── AvaliacaoRepository.java
│   │   ├── security/                        # Autenticação e Autorização
│   │   │   ├── JwtTokenService.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   └── CustomUserDetailsService.java
│   │   ├── validator/                       # Validações customizadas
│   │   │   └── UsuarioValidator.java
│   │   └── exception/                       # Exceções customizadas
│   │       ├── GlobalExceptionHandler.java
│   │       ├── UserNotFoundException.java
│   │       ├── CPFAlreadyInUseException.java
│   │       ├── EmailAlreadyInUseException.java
│   │       ├── InvalidPasswordException.java
│   │       ├── InvalidUserTypeException.java
│   │       └── InvalidFileUploadException.java
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── db/migration/
│   ├── src/test/java/ads/uninassau/brjobs/
│   │   ├── BrjobsApplicationTests.java
│   │   ├── controller/
│   │   │   ├── UsuarioControllerUnitTest.java
│   │   │   ├── UsuarioControllerTest.java
│   │   │   ├── PrestadorControllerUnitTest.java
│   │   │   └── AvaliacaoControllerUnitTest.java
│   │   ├── service/
│   │   │   ├── UsuarioServiceTest.java
│   │   │   ├── AuthServiceTest.java
│   │   │   └── AvaliacaoServiceTest.java
│   │   └── validator/
│   ├── pom.xml
│   └── mvnw
│
├── brjobs-angular/
│   ├── src/
│   │   ├── app/
│   │   │   ├── app.ts                       # Root component
│   │   │   ├── app.routes.ts               # Roteamento principal
│   │   │   ├── components/
│   │   │   │   ├── header/
│   │   │   │   ├── footer/
│   │   │   │   ├── home/
│   │   │   │   ├── login/
│   │   │   │   ├── register/
│   │   │   │   ├── about/
│   │   │   │   ├── accessibility/
│   │   │   │   ├── search/
│   │   │   │   └── forgot-password.component/
│   │   │   └── service/
│   │   │       └── register.service.ts
│   │   ├── main.ts
│   │   ├── index.html
│   │   └── styles.css
│   ├── angular.json
│   ├── tsconfig.json
│   └── package.json
│
└── README.md
```

---

## 📦 DTOs - Data Transfer Objects

Os DTOs são responsáveis pela transferência segura de dados entre o frontend e backend, validando e limitando os campos expostos.

### Diagrama de DTOs

```
┌──────────────────┐
│ LoginRequestDTO  │
├──────────────────┤
│ - email: String  │
│ - senha: String  │
└──────────────────┘

┌──────────────────────┐
│ LoginResponseDTO     │
├──────────────────────┤
│ - token: String      │
│ - mensagem: String   │
└──────────────────────┘

┌─────────────────────────────┐
│ UsuarioDTO                  │
├─────────────────────────────┤
│ - id: Long                  │
│ - nome: String              │
│ - email: String             │
│ - telefone: String          │
│ - cpf: String               │
│ - tipo: TipoUsuario         │
│ - genero: String            │
│ - endereco: String          │
│ - dataNascimento: LocalDate │
│ - ativo: Boolean            │
└─────────────────────────────┘

┌──────────────────────────────┐
│ CadastroPrestadorDTO         │
├──────────────────────────────┤
│ - usuarioDTO: UsuarioDTO     │
│ - funcao: String             │
│ - experiencia: String        │
│ - especialidades: String     │
│ - descricao: String          │
│ - curriculo: MultipartFile   │
└──────────────────────────────┘

┌──────────────────────────────┐
│ PrestadorDTO                 │
├──────────────────────────────┤
│ - id: Long                   │
│ - usuario: UsuarioDTO        │
│ - funcao: String             │
│ - experiencia: String        │
│ - especialidades: String     │
│ - descricao: String          │
│ - rating: Double             │
└──────────────────────────────┘

┌──────────────────────────────┐
│ ServicoDTO                   │
├──────────────────────────────┤
│ - id: Long                   │
│ - nome: String               │
│ - descricao: String          │
│ - categoria: String          │
│ - preco: BigDecimal          │
└──────────────────────────────┘

┌───────────────────────────────────┐
│ SolicitacaoServicoDTO             │
├───────────────────────────────────┤
│ - id: Long                        │
│ - prestador: PrestadorDTO         │
│ - contratante: UsuarioDTO         │
│ - servico: ServicoDTO             │
│ - dataSolicitacao: LocalDateTime  │
│ - status: String                  │
│ - descricao: String               │
└───────────────────────────────────┘

┌──────────────────────────────┐
│ AvaliacaoDTO                 │
├──────────────────────────────┤
│ - id: Long                   │
│ - prestador: PrestadorDTO    │
│ - solicitacao: SolicitacaoDTO│
│ - nota: Integer (1-5)        │
│ - comentario: String         │
│ - dataAvaliacao: LocalDate   │
└──────────────────────────────┘
```

### Detalhes dos DTOs

| DTO | Campos Principais | Validações | Uso |
|-----|------------------|-----------|-----|
| **LoginRequestDTO** | email, senha | @NotBlank | Login de usuários |
| **LoginResponseDTO** | token, mensagem | - | Resposta de autenticação |
| **UsuarioDTO** | nome, email, cpf, telefone, tipo | @NotBlank, @Valid | Operações de usuário |
| **CadastroPrestadorDTO** | usuarioDTO, funcao, experiencia | @Valid | Registro de prestadores |
| **CadastroContratanteDTO** | usuarioDTO | @Valid | Registro de contratantes |
| **PrestadorDTO** | usuario, funcao, especialidades | @Valid | Leitura de prestadores |
| **PrestadorResponseDTO** | id, funcao, rating, especialidades | - | Resposta de busca de prestadores |
| **ServicoDTO** | nome, descricao, categoria, preco | @NotBlank, @NotNull | Operações de serviço |
| **SolicitacaoServicoDTO** | prestador, contratante, servico, status | @Valid | Solicitações de serviço |
| **AvaliacaoDTO** | prestador, nota, comentario | @Min(1), @Max(5) | Avaliações de prestadores |
| **FileUploadDTO** | arquivo, tipo | @NotNull | Upload de arquivos |

---

## 🔐 Segurança

### Estratégia de Segurança Implementada

O projeto utiliza uma abordagem **multi-camada** para segurança:

#### 1. **Autenticação com JWT (JSON Web Token)**

- **Tecnologia**: JJWT 0.12.6
- **Algoritmo**: HS512 (HMAC com SHA-512)
- **Validade**: Configurável (padrão: 1 hora = 3600000ms)

**Fluxo de Autenticação:**
```
1. Usuário faz POST /api/auth/login com email e senha
   ↓
2. Spring Security valida credenciais com AuthenticationManager
   ↓
3. Se válido, JwtTokenService gera token JWT
   ↓
4. Token é retornado ao cliente no LoginResponseDTO
   ↓
5. Cliente armazena token e envia em todas as requisições (Authorization: Bearer <token>)
   ↓
6. JwtAuthenticationFilter intercepta requisições e valida token
   ↓
7. Se válido, SecurityContext é preenchido com usuário autenticado
```

#### 2. **Codificação de Senha com BCrypt**

- **Encoder**: BCryptPasswordEncoder
- **Força**: 10 (padrão Spring Security)
- **Localização**: `SecurityConfig.java`

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

#### 3. **Spring Security Configuration**

**Arquivo**: `config/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    // DaoAuthenticationProvider com UserDetailsService customizado
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    // Filtro JWT integrado
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) 
            throws Exception {
        http
            .csrf().disable()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests()
                .requestMatchers("/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**")
                    .permitAll()
                .anyRequest()
                    .authenticated()
            .and()
            .addFilterBefore(jwtAuthenticationFilter, 
                            UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

#### 4. **JWT Token Service**

**Arquivo**: `security/JwtTokenService.java`

```java
@Service
public class JwtTokenService {
    
    @Value("${brjobs.jwt.secret}")
    private String secret;
    
    @Value("${brjobs.jwt.expiration}")
    private long expirationTime;
    
    // Gera token JWT
    public String generateToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);
        
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }
    
    // Valida token
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(authToken);
            return true;
        } catch (ExpiredJwtException e) {
            return false; // Token expirado
        } catch (MalformedJwtException e) {
            return false; // Token malformado
        }
    }
    
    // Extrai email do token
    public String getEmailFromToken(String authToken) {
        Claims claims = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(authToken)
                .getBody();
        return claims.getSubject();
    }
}
```

#### 5. **JWT Authentication Filter**

**Arquivo**: `security/JwtAuthenticationFilter.java`

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) 
                                   throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            
            if (jwt != null && jwtTokenService.validateToken(jwt)) {
                String email = jwtTokenService.getEmailFromToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            // Log erro
        }
        
        filterChain.doFilter(request, response);
    }
}
```

#### 6. **CORS Configuration**

**Arquivo**: `config/CorsConfig.java`

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:4200")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

#### 7. **Custom User Details Service**

**Arquivo**: `security/CustomUserDetailsService.java`

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Override
    public UserDetails loadUserByUsername(String email) 
            throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> 
                new UsernameNotFoundException("Usuário não encontrado: " + email));
        
        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getSenha())
                .authorities(SimpleGrantedAuthority(
                    "ROLE_" + usuario.getTipo().toString()))
                .accountNonExpired(true)
                .accountNonLocked(usuario.isAtivo())
                .credentialsNonExpired(true)
                .enabled(usuario.isAtivo())
                .build();
    }
}
```

#### 8. **Tratamento de Exceções Customizado**

**Arquivo**: `exception/GlobalExceptionHandler.java`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("USUARIO_NAO_ENCONTRADO", e.getMessage()));
    }
    
    @ExceptionHandler(CPFAlreadyInUseException.class)
    public ResponseEntity<ErrorResponse> handleCPFInUse(CPFAlreadyInUseException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("CPF_DUPLICADO", e.getMessage()));
    }
    
    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPassword(InvalidPasswordException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("SENHA_INVALIDA", e.getMessage()));
    }
}
```

### Checklist de Segurança

✅ Autenticação com JWT  
✅ Senha criptografada com BCrypt  
✅ Spring Security habilitado  
✅ CSRF desabilitado (API stateless)  
✅ Session policy STATELESS  
✅ CORS configurado  
✅ Validação de entrada com Jakarta Validation  
✅ Tratamento centralizado de exceções  
✅ Permissões por tipo de usuário  
✅ Filtro JWT em todas as requisições autenticadas  

---

## 🧪 Testes Automatizados

O projeto implementa testes em múltiplas camadas usando **JUnit 5** e **Mockito**.

### Estrutura de Testes

```
src/test/java/ads/uninassau/brjobs/
├── BrjobsApplicationTests.java         # Teste de contexto
├── controller/
│   ├── UsuarioControllerUnitTest.java      # Unit tests com Mockito
│   ├── UsuarioControllerTest.java          # Integration tests
│   ├── PrestadorControllerUnitTest.java
│   └── AvaliacaoControllerUnitTest.java
├── service/
│   ├── UsuarioServiceTest.java         # Testes de negócio
│   ├── AuthServiceTest.java
│   └── AvaliacaoServiceTest.java
└── validator/
```

### Tipos de Testes Implementados

#### 1. **Testes de Contexto**

**Arquivo**: `BrjobsApplicationTests.java`

```java
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=none"
})
class BrjobsApplicationTests {
    
    @Test
    void contextLoads() {
        // Usa banco de dados H2 em memória
    }
}
```

#### 2. **Unit Tests - Controller**

**Arquivo**: `controller/UsuarioControllerUnitTest.java`

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioController Unit Tests")
class UsuarioControllerUnitTest {
    
    @Mock
    private UsuarioService usuarioService;
    
    @InjectMocks
    private UsuarioController usuarioController;
    
    @Test
    @DisplayName("Deve retornar lista de usuários com sucesso")
    void testListarUsuariosComSucesso() {
        // Arrange
        List<Usuario> usuarios = Arrays.asList(usuario);
        when(usuarioService.listar()).thenReturn(usuarios);
        
        // Act
        ResponseEntity<?> response = usuarioController.listar();
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
```

#### 3. **Unit Tests - Service**

**Arquivo**: `service/UsuarioServiceTest.java`

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService Tests")
class UsuarioServiceTest {
    
    @Mock
    private UsuarioRepository usuarioRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UsuarioService usuarioService;
    
    @Test
    @DisplayName("Deve validar email único")
    void testEmailUnico() {
        // Testa lógica de negócio de validação de email
    }
}
```

### Cobertura de Testes

| Camada | Tipo | Abordagem | Status |
|--------|------|-----------|--------|
| **Controller** | Unit | Mockito + Assertions | ✅ Implementado |
| **Controller** | Integration | @SpringBootTest | ✅ Implementado |
| **Service** | Unit | Mockito + Assertions | ✅ Implementado |
| **Repository** | Integration | H2 In-Memory | ⏳ Pendente |
| **Validator** | Unit | Mockito | ⏳ Pendente |
| **Security** | Integration | Security Context | ⏳ Pendente |

### Executar Testes

```bash
# Todos os testes
mvn test

# Apenas testes de controller
mvn test -Dtest=**ControllerTest

# Apenas testes de service
mvn test -Dtest=**ServiceTest

# Com cobertura
mvn test jacoco:report
```

---

## 📊 Diagramas UML

### 1. Diagrama de Casos de Uso

```
                         ┌─────────────────┐
                         │   BRJOBS API    │
                         └─────────────────┘

        ┌─────────────────┐                     ┌──────────────────┐
        │  Contratante    │                     │   Prestador      │
        │  (User)         │                     │   (Provider)     │
        └────────┬────────┘                     └────────┬─────────┘
                 │                                       │
        ┌────────┴────────┐                     ┌────────┴─────────┐
        │                 │                     │                  │
        │                 │                     │                  │
   ┌────▼────┐  ┌────────▼──────┐      ┌──────▼────────┐  ┌──────▼────┐
   │ Autenticar│  │ Buscar Serviços│     │ Registrar     │  │ Visualizar│
   │ (Login)  │  │ (Listar/Search)│     │ Perfil        │  │ Solicitações│
   └────┬────┘  └────────┬──────┘      └──────┬────────┘  └──────┬────┘
        │                │                     │                 │
        │           ┌────▼────────────────────┴──────┐            │
        │           │   Solicitações de Serviço      │            │
        │           └────┬────────────────────┬──────┘            │
        │                │                    │                  │
        │           ┌────▼────────┐    ┌─────▼──────────┐       │
        │           │ Avaliar      │    │ Aceitar/Recusar│       │
        │           │ Prestador    │    │ Solicitação    │       │
        │           └─────────────┘    └────────────────┘       │
        │                                                         │
        └─────────────────────────────────────────────────────────┘
```

### 2. Diagrama de Classes (Modelo de Dados)

```
                            ┌──────────────────┐
                            │     Usuario      │
                            ├──────────────────┤
                            │ - id: Long       │
                            │ - nome: String   │
                            │ - email: String  │
                            │ - senha: String  │
                            │ - cpf: String    │
                            │ - telefone: String│
                            │ - tipo: Enum     │
                            │ - ativo: Boolean │
                            │ - dataCadastro   │
                            └────────┬─────────┘
                                     │
                    ┌────────────────┼────────────────┐
                    │ (tipo: PRESTADOR)               │
                    ▼                                 │
            ┌──────────────────┐                      │
            │   Prestador      │              (tipo: CONTRATANTE)
            ├──────────────────┤                      │
            │ - id: Long       │                      │
            │ - funcao: String │                      │
            │ - experiencia    │                      │
            │ - especialidades │                      ▼
            │ - descricao      │         (Contratante não tem entidade)
            │ - curriculo: byte│
            │ - rating: Double │
            └────────┬─────────┘
                     │ 1
                     │
                     │ N
            ┌────────▼─────────┐
            │  SolicitacaoServico│
            ├────────────────────┤
            │ - id: Long         │
            │ - prestador_id     │
            │ - contratante_id   │
            │ - servico_id       │
            │ - status: String   │
            │ - descricao        │
            │ - dataSolicitacao  │
            └────────┬───────────┘
                     │ 1
                     │
                     │ N
            ┌────────▼──────────┐
            │   Avaliacao      │
            ├──────────────────┤
            │ - id: Long       │
            │ - prestador_id   │
            │ - nota: Integer  │ (1-5)
            │ - comentario     │
            │ - dataAvaliacao  │
            └──────────────────┘

        ┌──────────────────┐
        │    Servico      │
        ├──────────────────┤
        │ - id: Long       │
        │ - nome: String   │
        │ - descricao      │
        │ - categoria      │
        │ - preco          │
        └──────────────────┘
```

### 3. Diagrama de Fluxo de Autenticação

```
┌──────────────┐
│   Cliente    │
└──────┬───────┘
       │
       │ POST /api/auth/login
       │ { email, senha }
       ▼
┌─────────────────────────────┐
│   AuthController            │
└──────┬──────────────────────┘
       │
       │ authService.authenticateAndGetToken()
       ▼
┌─────────────────────────────┐
│   AuthService               │
│  - AuthenticationManager    │
│  - JwtTokenService          │
└──────┬──────────────────────┘
       │
       │ Valida credenciais
       ▼
┌─────────────────────────────┐
│   DaoAuthenticationProvider │
│  - CustomUserDetailsService │
│  - BCryptPasswordEncoder    │
└──────┬──────────────────────┘
       │
       │ Consulta usuário no BD
       │ Compara senha com hash
       ▼
┌─────────────────────────────┐
│   BD (PostgreSQL)           │
│   Table: usuarios           │
└──────┬──────────────────────┘
       │
       │ Retorna sucesso
       ▼
┌─────────────────────────────┐
│   JwtTokenService           │
│   generateToken()           │
└──────┬──────────────────────┘
       │
       │ Cria JWT com:
       │ - subject (email)
       │ - issuedAt (agora)
       │ - expiration (1h)
       │ - signature HS512
       ▼
┌──────────────┐
│   Cliente    │
│ Token JWT    │
│ (Bearer)     │
└──────────────┘
```

### 4. Diagrama de Sequência - Requisição Autenticada

```
Cliente            JwtAuthenticationFilter    SecurityContext    Controller    Service
   │                       │                        │               │           │
   │ GET /api/usuarios     │                        │               │           │
   │ Authorization: Bearer │                        │               │           │
   ├──────────────────────>│                        │               │           │
   │                       │ getJwtFromRequest()    │               │           │
   │                       │ validateToken()        │               │           │
   │                       │ getEmailFromToken()    │               │           │
   │                       │ loadUserByUsername()   │               │           │
   │                       │                        │               │           │
   │                       │ setAuthentication()    │               │           │
   │                       │ ─────────────────────> │               │           │
   │                       │                        │               │           │
   │                       │                        │ doFilter()    │           │
   │                       │                        ├──────────────>│           │
   │                       │                        │               │ listar()  │
   │                       │                        │               ├──────────>│
   │                       │                        │               │           │
   │                       │                        │               │           │
   │                       │                        │               │<──────────┤
   │                       │                        │<──────────────┤           │
   │<──────────────────────┼────────────────────────┼───────────────┤           │
   │ 200 OK + JSON Array   │                        │               │           │
```

---

## 🔌 Endpoints da API

A documentação completa dos endpoints está disponível em **Swagger UI**: `http://localhost:8080/swagger-ui.html`

### Authentication (`/api/auth`)

| Método | Endpoint | Descrição | Autenticado |
|--------|----------|-----------|-------------|
| POST | `/api/auth/login` | Realizar login | ❌ Não |
| GET | `/api/auth/me` | Obter usuário autenticado | ✅ Sim |
| POST | `/api/auth/logout` | Realizar logout | ✅ Sim |

### Usuários (`/api/usuarios`)

| Método | Endpoint | Descrição | Autenticado |
|--------|----------|-----------|-------------|
| GET | `/api/usuarios` | Listar todos os usuários | ✅ Sim |
| GET | `/api/usuarios/{id}` | Obter usuário por ID | ✅ Sim |
| POST | `/api/usuarios` | Criar novo usuário | ❌ Não |
| PUT | `/api/usuarios/{id}` | Atualizar usuário | ✅ Sim |
| DELETE | `/api/usuarios/{id}` | Deletar usuário (soft delete) | ✅ Sim |

### Prestadores (`/api/prestadores`)

| Método | Endpoint | Descrição | Autenticado |
|--------|----------|-----------|-------------|
| GET | `/api/prestadores` | Listar prestadores | ❌ Não |
| GET | `/api/prestadores/{id}` | Obter prestador por ID | ❌ Não |
| POST | `/api/prestadores` | Registrar como prestador | ❌ Não |
| PUT | `/api/prestadores/{id}` | Atualizar perfil | ✅ Sim |
| DELETE | `/api/prestadores/{id}` | Remover prestador | ✅ Sim |
| GET | `/api/prestadores/search?nome=` | Buscar prestadores | ❌ Não |

### Serviços (`/api/servicos`)

| Método | Endpoint | Descrição | Autenticado |
|--------|----------|-----------|-------------|
| GET | `/api/servicos` | Listar serviços | ❌ Não |
| GET | `/api/servicos/{id}` | Obter serviço por ID | ❌ Não |
| POST | `/api/servicos` | Criar serviço | ✅ Sim (Admin) |
| PUT | `/api/servicos/{id}` | Atualizar serviço | ✅ Sim (Admin) |
| DELETE | `/api/servicos/{id}` | Deletar serviço | ✅ Sim (Admin) |

### Solicitações (`/api/solicitacoes`)

| Método | Endpoint | Descrição | Autenticado |
|--------|----------|-----------|-------------|
| GET | `/api/solicitacoes` | Listar solicitações | ✅ Sim |
| GET | `/api/solicitacoes/{id}` | Obter solicitação | ✅ Sim |
| POST | `/api/solicitacoes` | Criar solicitação | ✅ Sim (Contratante) |
| PUT | `/api/solicitacoes/{id}` | Atualizar solicitação | ✅ Sim |
| DELETE | `/api/solicitacoes/{id}` | Cancelar solicitação | ✅ Sim |

### Avaliações (`/api/avaliacoes`)

| Método | Endpoint | Descrição | Autenticado |
|--------|----------|-----------|-------------|
| GET | `/api/avaliacoes/prestador/{id}` | Listar avaliações | ❌ Não |
| POST | `/api/avaliacoes` | Criar avaliação | ✅ Sim (Contratante) |
| PUT | `/api/avaliacoes/{id}` | Atualizar avaliação | ✅ Sim |
| DELETE | `/api/avaliacoes/{id}` | Deletar avaliação | ✅ Sim |

### Exemplo de Requisição

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"usuario@example.com","senha":"senha123"}'

# Resposta
{
  "token": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
  "mensagem": "Login realizado com sucesso"
}

# Usar token
curl -X GET http://localhost:8080/api/usuarios \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9..."
```

---

## 🚀 Últimas Alterações

### Versão 0.0.1-SNAPSHOT (Atual)

#### 🔄 Melhorias Recentes

**Backend (Java/Spring Boot)**
- ✅ Atualização Spring Boot 3.3.5 com Java 17
- ✅ Implementação de autenticação JWT com JJWT 0.12.6
- ✅ BCrypt para criptografia de senha
- ✅ Spring Security configurado com filtros customizados
- ✅ CORS habilitado para localhost:4200 (Angular)
- ✅ Swagger/OpenAPI 2.5.0 para documentação automática
- ✅ Exception handling centralizado com GlobalExceptionHandler
- ✅ Validação com Jakarta Validation
- ✅ 7 Services implementados (Auth, Usuario, Prestador, Servico, SolicitacaoServico, Avaliacao, FileService)
- ✅ DTOs para transfer de dados seguro
- ✅ Tests unitários e de integração (JUnit 5 + Mockito)
- ✅ Lombok para redução de boilerplate

**Frontend (Angular)**
- ✅ Migração para Angular 20.3.0
- ✅ TypeScript 5.9.2
- ✅ RxJS 7.8.0 para programação reativa
- ✅ Componentes standalone (app.ts)
- ✅ Roteamento moderno (app.routes.ts)
- ✅ 8 Componentes principais (Header, Footer, Home, Login, Register, About, Accessibility, Search)
- ✅ Services de integração com API

**Banco de Dados**
- ✅ PostgreSQL configurado como principal
- ✅ H2 em memória para testes
- ✅ Relacionamentos JPA entre entidades
- ✅ Soft delete com campo 'ativo'

**Segurança**
- ✅ Autenticação stateless com JWT
- ✅ Token com expiração de 1 hora
- ✅ Permissões por TipoUsuario (PRESTADOR, CONTRATANTE)
- ✅ CORS whitelist configurado

**Testes**
- ✅ BrjobsApplicationTests (contexto)
- ✅ UsuarioControllerUnitTest
- ✅ PrestadorControllerUnitTest
- ✅ AvaliacaoControllerUnitTest
- ✅ UsuarioServiceTest
- ✅ AuthServiceTest
- ✅ AvaliacaoServiceTest

#### 🗓️ Roadmap Futuro

- ⏳ Testes de integração com banco H2
- ⏳ Testes de segurança (JWT, CORS)
- ⏳ Documentação Swagger mais detalhada
- ⏳ Upload de arquivos (currículo)
- ⏳ Paginação em listagens
- ⏳ Filtros avançados de busca
- ⏳ Notificações em tempo real (WebSocket)
- ⏳ Suporte a múltiplos idiomas (i18n)
- ⏳ Rate limiting
- ⏳ Logging estruturado (SLF4J + Logback)

---

## 🔧 Como Rodar o Projeto

### Pré-requisitos

✅ **Java 17+** - [Download](https://www.oracle.com/java/technologies/downloads/)  
✅ **Maven 3.8+** - [Download](https://maven.apache.org/download.cgi)  
✅ **Node.js 18+** - [Download](https://nodejs.org/)  
✅ **Angular CLI** - `npm install -g @angular/cli`  
✅ **PostgreSQL 13+** - [Download](https://www.postgresql.org/download/)  

### Configuração Inicial

#### 1. Clonar o Repositório

```bash
git clone https://github.com/devcelsoborges/projeto-final.git
cd projeto-final
```

#### 2. Configurar Banco de Dados PostgreSQL

```bash
# Criar banco de dados
createdb brjobsdb

# Criar usuário (se necessário)
createuser postgres

# Conectar e configurar
psql -U postgres

-- No prompt do psql:
CREATE DATABASE brjobsdb;
\c brjobsdb
```

Atualizar `brjobs-java/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/brjobsdb
spring.datasource.username=postgres
spring.datasource.password=SEU_PASSWORD
```

#### 3. Configurar JWT Secret

Gerar uma chave segura e adicionar ao `application.properties`:

```properties
brjobs.jwt.secret=sua_chave_secreta_super_segura_aqui_minimo_32_caracteres
brjobs.jwt.expiration=3600000
```

#### 4. Backend - Rodar Spring Boot

```bash
cd brjobs-java

# Baixar dependências
mvn clean install

# Executar aplicação
mvn spring-boot:run

# Ou com seu IDE favorito (IntelliJ IDEA, VS Code com Extension Pack for Java)
```

A API estará disponível em: `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui.html`

#### 5. Frontend - Rodar Angular

```bash
cd brjobs-angular

# Instalar dependências
npm install

# Executar servidor de desenvolvimento
ng serve

# Ou
npm start
```

A aplicação estará disponível em: `http://localhost:4200`

### Execução de Testes

```bash
# Backend - Todos os testes
cd brjobs-java
mvn test

# Backend - Específico
mvn test -Dtest=UsuarioControllerUnitTest

# Backend - Com cobertura
mvn test jacoco:report

# Frontend - Todos os testes
cd brjobs-angular
npm test

# Frontend - Watch mode
npm test -- --watch
```

### Build para Produção

```bash
# Backend
cd brjobs-java
mvn clean package -DskipTests
# JAR gerado em: target/brjobs-0.0.1-SNAPSHOT.jar

# Frontend
cd brjobs-angular
ng build --configuration production
# Build em: dist/brjobs-angular/
```

---

## 🤝 Contribuições

Agradecemos por considerar contribuir para o projeto! Siga as diretrizes abaixo:

### Processo de Contribuição

1. **Fork** o repositório
2. Crie uma **branch** para sua feature: `git checkout -b feature/sua-feature`
3. **Commit** suas mudanças: `git commit -m "feat: adicionar nova funcionalidade"`
4. **Push** para a branch: `git push origin feature/sua-feature`
5. Abra um **Pull Request** descrevendo suas mudanças

### Boas Práticas

- ✅ Usar **branches semânticas**: `feature/xyz`, `fix/xyz`, `docs/xyz`
- ✅ Fazer **commits atômicos** com mensagens descritivas
- ✅ Escrever **testes** para novas features
- ✅ Seguir **conventions** do projeto (Java/Angular)
- ✅ Documentar **mudanças significativas**
- ✅ Validar **entradas** no frontend e backend
- ✅ Respeitar o **código de conduta**

### Convenção de Commits

```
feat: adicionar nova funcionalidade
fix: corrigir bug
docs: atualizar documentação
style: formatação, sem mudança de lógica
refactor: refatorar código sem mudança de comportamento
perf: melhorias de performance
test: adicionar ou atualizar testes
chore: atualizações de build, dependências, etc.
```

---

## 📄 Licença e Contato

### Licença

Este projeto está licenciado sob **MIT License** — verifique o arquivo `LICENSE` para mais detalhes.

### Desenvolvedores

| Nome | Email | Role |
|------|-------|------|
| **Celso Sitônio Borges Neto** | borgesnetocs@gmail.com | Backend Lead |
| **Anthonny Caio Lima de Oliveira Alves** | anthonnycaiolima@gmail.com | Full Stack |

### Contato & Suporte

📧 Email: borgesnetocs@gmail.com  
🐙 GitHub: [@devcelsoborges](https://github.com/devcelsoborges)  
📱 LinkedIn: [Celso Borges](https://linkedin.com)  

### Agradecimentos

- Universidade UNINASSAU - Educação em Tecnologia
- Spring Boot Community
- Angular Team
- Comunidade Open Source

---

## 📚 Recursos Adicionais

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Angular Documentation](https://angular.io/docs)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [JWT.io](https://jwt.io/)
- [Spring Security](https://spring.io/projects/spring-security)
- [RESTful API Best Practices](https://restfulapi.net/)

---

**Última atualização**: 26 de Novembro de 2025  
**Versão**: 0.0.1-SNAPSHOT


## Deploy temporario AWS EC2

O backend `brjobs-java` pode ser publicado como container Docker em uma EC2 usando Amazon ECR. Consulte `brjobs-java/README_DEPLOY_AWS.md`.

Os arquivos `.env.prod.example` e `.env.test-ec2.example` incluem credenciais de PostgreSQL apenas para teste/MVP temporario:

```env
DB_HOST=brjobs-postgres
DB_NAME=brjobs
DB_USER=brjobs_user
DB_PASSWORD=brjobs_senha_forte_123
```

Essa senha deve ser trocada antes de qualquer producao real. O arquivo `.env.prod` nao deve ser versionado.
