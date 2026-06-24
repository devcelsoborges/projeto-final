package ads.uninassau.brjobs.service;

import ads.uninassau.brjobs.dto.UsuarioDTO;
import ads.uninassau.brjobs.exception.UserNotFoundException;
import ads.uninassau.brjobs.model.TipoUsuario;
import ads.uninassau.brjobs.model.Usuario;
import ads.uninassau.brjobs.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService Unit Tests")
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AccountActivationService accountActivationService;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuarioExistente;

    @BeforeEach
    void setUp() {
        usuarioExistente = Usuario.builder()
                .id(1L)
                .tipoUsuario(TipoUsuario.CONTRATANTE)
                .nome("Maria Silva")
                .email("maria@example.com")
                .senha("hash")
                .telefone("(81) 99999-0000")
                .cpf("111.222.333-44")
                .genero("Feminino")
                .dataNascimento(LocalDate.of(1990, 5, 20))
                .endereco("Endereço antigo")
                .cep("50000-000")
                .rua("Rua das Flores")
                .bairro("Boa Vista")
                .cidade("Recife")
                .estado("PE")
                .numero("123")
                .complemento("Apto 401")
                .bio("Bio original")
                .ativo(true)
                .build();
    }

    @Test
    @DisplayName("Update parcial deve preservar campos não enviados (null) no DTO")
    void atualizarDadosBasicosDevePreservarCamposNaoEnviados() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioExistente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        // DTO simulando um cliente que envia apenas o telefone
        UsuarioDTO dto = new UsuarioDTO();
        dto.setTelefone("(81) 98888-7777");

        UsuarioDTO resultado = usuarioService.atualizarDadosBasicos(1L, dto);

        assertEquals("(81) 98888-7777", resultado.getTelefone());
        assertEquals("Maria Silva", resultado.getNome());
        assertEquals("111.222.333-44", resultado.getCpf());
        assertEquals("Feminino", resultado.getGenero());
        assertEquals(LocalDate.of(1990, 5, 20), resultado.getDataNascimento());
        assertEquals("Rua das Flores", resultado.getRua());
        assertEquals("123", resultado.getNumero());
        assertEquals("Apto 401", resultado.getComplemento());
        assertEquals("50000-000", resultado.getCep());
        assertEquals("Boa Vista", resultado.getBairro());
        assertEquals("Recife", resultado.getCidade());
        assertEquals("PE", resultado.getEstado());
        assertEquals("Bio original", resultado.getBio());
        assertEquals("Endereço antigo", resultado.getEndereco());
    }

    @Test
    @DisplayName("String em branco deve limpar explicitamente campo opcional")
    void atualizarDadosBasicosComStringEmBrancoDeveLimparCampo() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioExistente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioDTO dto = new UsuarioDTO();
        dto.setComplemento("");

        UsuarioDTO resultado = usuarioService.atualizarDadosBasicos(1L, dto);

        assertNull(resultado.getComplemento());
        assertEquals("(81) 99999-0000", resultado.getTelefone());
        assertEquals("Rua das Flores", resultado.getRua());
    }

    @Test
    @DisplayName("Nome em branco ou ausente não deve apagar o nome existente")
    void atualizarDadosBasicosNaoDeveApagarNomeObrigatorio() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioExistente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioDTO dto = new UsuarioDTO();
        dto.setNome("   ");

        UsuarioDTO resultado = usuarioService.atualizarDadosBasicos(1L, dto);

        assertEquals("Maria Silva", resultado.getNome());
    }

    @Test
    @DisplayName("Update completo deve aplicar todos os valores enviados")
    void atualizarDadosBasicosComPayloadCompletoDeveAplicarTudo() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioExistente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        // DTO simulando o app Flutter, que envia o estado completo do perfil
        UsuarioDTO dto = new UsuarioDTO();
        dto.setNome("Maria Souza");
        dto.setTelefone("(81) 97777-1111");
        dto.setEndereco("Endereço novo");
        dto.setCep("51000-111");
        dto.setRua("Av. Central");
        dto.setBairro("Pina");
        dto.setCidade("Olinda");
        dto.setEstado("PE");
        dto.setNumero("456");
        dto.setComplemento("Casa B");
        dto.setBio("Bio atualizada");
        dto.setGenero("Feminino");
        dto.setDataNascimento(LocalDate.of(1991, 1, 15));

        UsuarioDTO resultado = usuarioService.atualizarDadosBasicos(1L, dto);

        assertEquals("Maria Souza", resultado.getNome());
        assertEquals("(81) 97777-1111", resultado.getTelefone());
        assertEquals("Endereço novo", resultado.getEndereco());
        assertEquals("51000-111", resultado.getCep());
        assertEquals("Av. Central", resultado.getRua());
        assertEquals("Pina", resultado.getBairro());
        assertEquals("Olinda", resultado.getCidade());
        assertEquals("PE", resultado.getEstado());
        assertEquals("456", resultado.getNumero());
        assertEquals("Casa B", resultado.getComplemento());
        assertEquals("Bio atualizada", resultado.getBio());
        assertEquals(LocalDate.of(1991, 1, 15), resultado.getDataNascimento());
    }

    @Test
    @DisplayName("Deve lançar UserNotFoundException quando usuário não existe")
    void atualizarDadosBasicosDeveLancarExcecaoQuandoUsuarioNaoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> usuarioService.atualizarDadosBasicos(99L, new UsuarioDTO()));
    }
}
