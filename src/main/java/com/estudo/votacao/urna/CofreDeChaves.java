package com.estudo.votacao.urna;

import java.security.PrivateKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Simula o hardware de segurança da urna: a chave privada nunca vai para o banco nem sai daqui.
 * Fica em memória; como o H2 também é em memória, os dois se perdem juntos ao reiniciar.
 */
@Component
public class CofreDeChaves {

    private final Map<Long, PrivateKey> chaves = new ConcurrentHashMap<>();

    public void guardar(Long urnaId, PrivateKey chave) {
        chaves.put(urnaId, chave);
    }

    public String assinar(Long urnaId, String conteudo) {
        PrivateKey chave = chaves.get(urnaId);
        if (chave == null) {
            throw new IllegalStateException("Chave privada da urna " + urnaId + " indisponível.");
        }
        return AssinaturaDigital.assinar(chave, conteudo);
    }
}
