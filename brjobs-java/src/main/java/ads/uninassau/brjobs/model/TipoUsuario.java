package ads.uninassau.brjobs.model;

/**
 * Enum que define os tipos de usuários no sistema.
 *
 * CONTRATANTE: Pessoa que contrata serviços
 * PRESTADOR: Pessoa que oferece serviços
 */
public enum TipoUsuario {
    CONTRATANTE("Contratante"),
    PRESTADOR("Prestador de Serviço");

    private final String descricao;

    TipoUsuario(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Verifica se o tipo é PRESTADOR
     */
    public boolean isPrestador() {
        return this == PRESTADOR;
    }

    /**
     * Verifica se o tipo é CONTRATANTE
     */
    public boolean isContratante() {
        return this == CONTRATANTE;
    }
}

