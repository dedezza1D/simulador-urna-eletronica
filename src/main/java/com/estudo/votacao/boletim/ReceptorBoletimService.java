package com.estudo.votacao.boletim;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.estudo.votacao.comum.ConflitoException;
import com.estudo.votacao.comum.RecursoNaoEncontradoException;
import com.estudo.votacao.comum.RegraNegocioException;
import com.estudo.votacao.urna.AssinaturaDigital;
import com.estudo.votacao.urna.Urna;
import com.estudo.votacao.urna.UrnaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * Recepção de BU assinado, no papel do totalizador. A ordem das verificações importa:
 * primeiro a assinatura (o conteúdo veio mesmo daquela urna e não foi alterado),
 * só depois a leitura do conteúdo e as regras de negócio.
 */
@Service
public class ReceptorBoletimService {

    public record Recebimento(BoletimResponse boletim, boolean novo) {
    }

    private final UrnaRepository urnaRepository;
    private final BoletimRepository boletimRepository;
    private final BoletimService boletimService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public ReceptorBoletimService(UrnaRepository urnaRepository, BoletimRepository boletimRepository,
                                  BoletimService boletimService, ObjectMapper objectMapper, Validator validator) {
        this.urnaRepository = urnaRepository;
        this.boletimRepository = boletimRepository;
        this.boletimService = boletimService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Transactional
    public Recebimento receber(BoletimAssinadoRequest envelope) {
        // A chave vem do cadastro da urna, nunca do próprio envelope.
        Urna urna = urnaRepository.findByEleicaoIdAndSecaoZonaAndSecaoNumero(
                        envelope.eleicaoId(), envelope.zona(), envelope.secao())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Nenhuma urna registrada para a zona " + envelope.zona() + ", seção " + envelope.secao() + "."));
        if (!AssinaturaDigital.verificar(urna.getChavePublica(), envelope.conteudo(), envelope.assinatura())) {
            throw new RegraNegocioException("Assinatura digital inválida. Boletim descartado.");
        }

        RegistrarBoletimRequest boletim = ler(envelope.conteudo());
        if (!boletim.eleicaoId().equals(envelope.eleicaoId())
                || !boletim.zona().equals(envelope.zona())
                || !boletim.secao().equals(envelope.secao())) {
            throw new RegraNegocioException("O envelope não corresponde à seção do conteúdo assinado.");
        }

        String hash = AssinaturaDigital.sha256(envelope.conteudo());
        Optional<BoletimUrna> existente = boletimRepository.findByEleicaoIdAndSecaoId(
                envelope.eleicaoId(), urna.getSecao().getId());
        if (existente.isPresent()) {
            // Retransmissão do mesmo BU é normal (rede instável): responde como sucesso, sem duplicar.
            if (hash.equals(existente.get().getHashConteudo())) {
                return new Recebimento(BoletimResponse.de(existente.get()), false);
            }
            throw new ConflitoException("Já existe um boletim diferente para esta seção.");
        }
        return new Recebimento(boletimService.registrar(boletim, hash), true);
    }

    private RegistrarBoletimRequest ler(String conteudo) {
        RegistrarBoletimRequest boletim;
        try {
            boletim = objectMapper.readValue(conteudo, RegistrarBoletimRequest.class);
        } catch (JsonProcessingException e) {
            throw new RegraNegocioException("Conteúdo do boletim ilegível.");
        }
        Set<ConstraintViolation<RegistrarBoletimRequest>> violacoes = validator.validate(boletim);
        if (!violacoes.isEmpty()) {
            throw new RegraNegocioException("Conteúdo do boletim inválido: " + violacoes.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; ")));
        }
        return boletim;
    }
}
