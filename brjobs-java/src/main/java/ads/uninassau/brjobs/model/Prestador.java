package ads.uninassau.brjobs.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade que representa um prestador de serviço no sistema.
 * Cada Prestador está associado a um usuário com tipoUsuario == PRESTADOR.
 *
 * Responsabilidades:
 * - Armazenar dados profissionais (função, experiência, especialidades)
 * - Manter relacionamento bidirecional com Usuario
 * - Rastrear avaliações recebidas
 * - Suportar soft delete (ativo/inativo)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "prestadores")
public class Prestador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relacionamento bidirecional com Usuario (obrigatório)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", unique = true, nullable = false)
    private Usuario usuario;

    @Column(name = "funcao", length = 100, nullable = false)
    private String funcao;

    @Column(name = "experiencia_profissional", length = 2000)
    private String experienciaProfissional;

    @Column(name = "especialidades", length = 500)
    private String especialidades;

    @Column(name = "curriculo")
    private byte[] curriculo;

    @Column(name = "descricao", length = 2000)
    private String descricao;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "nota_media")
    private Double notaMedia;

    @Column(name = "quantidade_avaliacoes")
    private Integer quantidadeAvaliacoes = 0;

    @CreationTimestamp
    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    // Relacionamento com avaliações recebidas
    @OneToMany(mappedBy = "prestador", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Avaliacao> avaliacoes = new ArrayList<>();

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }

    /**
     * Ativa o prestador (soft delete)
     */
    public void ativar() {
        this.ativo = true;
    }

    /**
     * Desativa o prestador (soft delete)
     */
    public void desativar() {
        this.ativo = false;
    }

    /**
     * Atualiza a nota média baseado nas avaliações
     */
    public void atualizarNotaMedia() {
        if (this.avaliacoes == null || this.avaliacoes.isEmpty()) {
            this.notaMedia = null;
            this.quantidadeAvaliacoes = 0;
        } else {
            double soma = this.avaliacoes.stream()
                    .mapToDouble(Avaliacao::getNota)
                    .sum();
            this.notaMedia = soma / this.avaliacoes.size();
            this.quantidadeAvaliacoes = this.avaliacoes.size();
        }
    }

    /**
     * Adiciona uma avaliação ao prestador
     */
    public void adicionarAvaliacao(Avaliacao avaliacao) {
        if (this.avaliacoes == null) {
            this.avaliacoes = new ArrayList<>();
        }
        this.avaliacoes.add(avaliacao);
        avaliacao.setPrestador(this);
        atualizarNotaMedia();
    }

    /**
     * Remove uma avaliação do prestador
     */
    public void removerAvaliacao(Avaliacao avaliacao) {
        if (this.avaliacoes != null) {
            this.avaliacoes.remove(avaliacao);
            avaliacao.setPrestador(null);
            atualizarNotaMedia();
        }
    }

    /**
     * Verifica se o prestador está ativo
     */
    public boolean estaAtivo() {
        return this.ativo && this.usuario != null && this.usuario.isAtivo();
    }
}
