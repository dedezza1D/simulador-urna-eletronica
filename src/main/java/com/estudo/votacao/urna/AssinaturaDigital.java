package com.estudo.votacao.urna;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

/** Ed25519 para assinar o BU e SHA-256 para o resumo. Só APIs do JDK. */
public final class AssinaturaDigital {

    private static final String ALGORITMO = "Ed25519";

    private AssinaturaDigital() {
    }

    public static KeyPair gerarPar() {
        try {
            return KeyPairGenerator.getInstance(ALGORITMO).generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível gerar o par de chaves", e);
        }
    }

    public static String codificar(PublicKey chave) {
        return Base64.getEncoder().encodeToString(chave.getEncoded());
    }

    public static String assinar(PrivateKey chave, String conteudo) {
        try {
            Signature assinatura = Signature.getInstance(ALGORITMO);
            assinatura.initSign(chave);
            assinatura.update(conteudo.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(assinatura.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao assinar", e);
        }
    }

    /** Qualquer falha (chave malformada, Base64 inválido, assinatura errada) resulta em {@code false}. */
    public static boolean verificar(String chavePublicaBase64, String conteudo, String assinaturaBase64) {
        try {
            PublicKey chave = KeyFactory.getInstance(ALGORITMO)
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(chavePublicaBase64)));
            Signature verificador = Signature.getInstance(ALGORITMO);
            verificador.initVerify(chave);
            verificador.update(conteudo.getBytes(StandardCharsets.UTF_8));
            return verificador.verify(Base64.getDecoder().decode(assinaturaBase64));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    public static String sha256(String conteudo) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(conteudo.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
