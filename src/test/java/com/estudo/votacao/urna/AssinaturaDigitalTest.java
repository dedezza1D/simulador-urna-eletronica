package com.estudo.votacao.urna;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;

import org.junit.jupiter.api.Test;

class AssinaturaDigitalTest {

    private final KeyPair urna = AssinaturaDigital.gerarPar();
    private final String chavePublica = AssinaturaDigital.codificar(urna.getPublic());

    @Test
    void assinaturaValidaConfere() {
        String assinatura = AssinaturaDigital.assinar(urna.getPrivate(), "{\"comparecimento\":3}");

        assertThat(AssinaturaDigital.verificar(chavePublica, "{\"comparecimento\":3}", assinatura)).isTrue();
    }

    @Test
    void qualquerAlteracaoNoConteudoInvalidaAAssinatura() {
        String assinatura = AssinaturaDigital.assinar(urna.getPrivate(), "{\"comparecimento\":3}");

        assertThat(AssinaturaDigital.verificar(chavePublica, "{\"comparecimento\":4}", assinatura)).isFalse();
        assertThat(AssinaturaDigital.verificar(chavePublica, "{\"comparecimento\":3} ", assinatura)).isFalse();
    }

    @Test
    void assinaturaDeOutraUrnaNaoConfere() {
        KeyPair outraUrna = AssinaturaDigital.gerarPar();
        String assinatura = AssinaturaDigital.assinar(outraUrna.getPrivate(), "{\"comparecimento\":3}");

        assertThat(AssinaturaDigital.verificar(chavePublica, "{\"comparecimento\":3}", assinatura)).isFalse();
    }

    @Test
    void entradaMalformadaNaoLancaExcecao() {
        assertThat(AssinaturaDigital.verificar("não é base64", "x", "também não")).isFalse();
    }

    @Test
    void sha256EmHexadecimal() {
        assertThat(AssinaturaDigital.sha256("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
