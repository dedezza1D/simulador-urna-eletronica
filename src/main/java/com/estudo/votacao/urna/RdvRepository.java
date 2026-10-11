package com.estudo.votacao.urna;

import java.security.SecureRandom;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.estudo.votacao.comum.RegraNegocioException;
import com.estudo.votacao.eleicao.Cargo;

/**
 * Registro Digital do Voto. Cada cargo tem uma tabela de posições do tamanho do eleitorado
 * da seção; cada voto vai para uma posição livre sorteada. Assim, a ordem de gravação não
 * revela a ordem em que os eleitores votaram. JDBC puro porque não há entidade: o RDV é
 * só um conjunto de células.
 */
@Repository
public class RdvRepository {

    public record Contagem(Cargo cargo, TipoVoto tipo, String numero, long quantidade) {
    }

    public record Registro(Cargo cargo, int posicao, TipoVoto tipo, String numero) {
    }

    private final JdbcTemplate jdbc;
    private final SecureRandom sorteio = new SecureRandom();

    public RdvRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void registrar(Long urnaId, int capacidade, Cargo cargo, TipoVoto tipo, String numero) {
        Integer ocupadas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM rdv_voto WHERE urna_id = ? AND cargo = ?", Integer.class, urnaId, cargo.name());
        if (ocupadas != null && ocupadas >= capacidade) {
            throw new RegraNegocioException("RDV cheio para " + cargo + ": todos os eleitores aptos já votaram.");
        }
        while (true) {
            int posicao = sorteio.nextInt(capacidade);
            Integer usada = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM rdv_voto WHERE urna_id = ? AND cargo = ? AND posicao = ?",
                    Integer.class, urnaId, cargo.name(), posicao);
            if (usada != null && usada == 0) {
                jdbc.update("INSERT INTO rdv_voto (urna_id, cargo, posicao, tipo, numero) VALUES (?, ?, ?, ?, ?)",
                        urnaId, cargo.name(), posicao, tipo.name(), numero);
                return;
            }
        }
    }

    public List<Contagem> contar(Long urnaId) {
        return jdbc.query("""
                        SELECT cargo, tipo, numero, COUNT(*) AS quantidade
                        FROM rdv_voto
                        WHERE urna_id = ?
                        GROUP BY cargo, tipo, numero
                        """,
                (rs, i) -> new Contagem(Cargo.valueOf(rs.getString("cargo")), TipoVoto.valueOf(rs.getString("tipo")),
                        rs.getString("numero"), rs.getLong("quantidade")),
                urnaId);
    }

    // ORDER BY posicao: sem ele, a ordem física de inserção poderia vazar a sequência dos votos.
    public List<Registro> listar(Long urnaId) {
        return jdbc.query("""
                        SELECT cargo, posicao, tipo, numero
                        FROM rdv_voto
                        WHERE urna_id = ?
                        ORDER BY cargo, posicao
                        """,
                (rs, i) -> new Registro(Cargo.valueOf(rs.getString("cargo")), rs.getInt("posicao"),
                        TipoVoto.valueOf(rs.getString("tipo")), rs.getString("numero")),
                urnaId);
    }
}
