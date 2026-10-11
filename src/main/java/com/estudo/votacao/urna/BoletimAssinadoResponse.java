package com.estudo.votacao.urna;

/**
 * Equivalente à mídia de resultado: o BU em JSON, a assinatura Ed25519 sobre esses bytes
 * exatos e o SHA-256 do conteúdo (o que seria impresso no BU para conferência).
 */
public record BoletimAssinadoResponse(Long eleicaoId, int zona, int secao, String conteudo, String assinatura, String hash) {
}
