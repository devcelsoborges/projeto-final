package ads.uninassau.brjobs;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Usamos o atributo 'properties' para forçar o Spring a desativar as conexões
// com o banco de dados durante a execução do teste.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb", // Força o uso do H2 em memória
        "spring.jpa.hibernate.ddl-auto=none" // Não tenta criar/atualizar o schema
})
class BrjobsApplicationTests {

    @Test
    void contextLoads() {
        // O teste agora usa um banco de dados H2 temporário em memória e não dependerá do PostgreSQL.
    }
}