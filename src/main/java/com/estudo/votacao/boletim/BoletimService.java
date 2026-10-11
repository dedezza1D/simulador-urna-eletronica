package com.estudo.votacao.boletim;

import java.time.Clock;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.estudo.votacao.boletim.RegistrarBoletimRequest.VotacaoCargo;
import com.estudo.votacao.boletim.RegistrarBoletimRequest.Voto;
import com.estudo.votacao.candidato.Candidato;
import com.estudo.votacao.candidato.CandidatoRepository;
import com.estudo.votacao.comum.ConflitoException;
import com.estudo.votacao.comum.RecursoNaoEncontradoException;
import com.estudo.votacao.comum.RegraNegocioException;
import com.estudo.votacao.eleicao.Cargo;
import com.estudo.votacao.eleicao.Eleicao;
import com.estudo.votacao.eleicao.EleicaoRepository;
import com.estudo.votacao.secao.Secao;
import com.estudo.votacao.secao.SecaoRepository;

@Service
public class BoletimService {

    private record ChaveCandidato(Cargo cargo, String numero) {
    }

    private final BoletimRepository boletimRepository;
    private final EleicaoRepository eleicaoRepository;
    private final SecaoRepository secaoRepository;
    private final CandidatoRepository candidatoRepository;
    private final Clock clock;

    public BoletimService(BoletimRepository boletimRepository,
                          EleicaoRepository eleicaoRepository,
                          SecaoRepository secaoRepository,
                          CandidatoRepository candidatoRepository,
                          Clock clock) {
        this.boletimRepository = boletimRepository;
        this.eleicaoRepository = eleicaoRepository;
        this.secaoRepository = secaoRepository;
        this.candidatoRepository = candidatoRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public BoletimResponse buscar(Long id) {
        return boletimRepository.findById(id)
                .map(BoletimResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Boletim " + id + " não encontrado."));
    }

    @Transactional
    public BoletimResponse registrar(RegistrarBoletimRequest request) {
        return registrar(request, null);
    }

    /** @param hashConteudo hash do BU assinado pela urna, ou {@code null} se não houver assinatura */
    @Transactional
    public BoletimResponse registrar(RegistrarBoletimRequest request, String hashConteudo) {
        Eleicao eleicao = eleicaoRepository.findById(request.eleicaoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Eleição " + request.eleicaoId() + " não encontrada."));
        Secao secao = secaoRepository.findByZonaAndNumero(request.zona(), request.secao())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Seção " + request.secao() + " da zona " + request.zona() + " não encontrada."));

        if (boletimRepository.existsByEleicaoIdAndSecaoId(eleicao.getId(), secao.getId())) {
            throw new ConflitoException(
                    "Boletim da seção " + secao.getNumero() + " da zona " + secao.getZona() + " já foi recebido.");
        }
        if (request.comparecimento() > secao.getEleitoresAptos()) {
            throw new RegraNegocioException("Comparecimento (" + request.comparecimento()
                    + ") maior que o número de eleitores aptos da seção (" + secao.getEleitoresAptos() + ").");
        }

        // Uma consulta para todos os candidatos, em vez de uma por voto (evita N+1).
        Map<ChaveCandidato, Candidato> candidatos = candidatoRepository.findByEleicaoId(eleicao.getId()).stream()
                .collect(Collectors.toMap(c -> new ChaveCandidato(c.getCargo(), c.getNumero()), Function.identity()));

        BoletimUrna boletim = new BoletimUrna(eleicao, secao, request.comparecimento(), clock.instant(), hashConteudo);
        Set<Cargo> cargosInformados = EnumSet.noneOf(Cargo.class);
        for (VotacaoCargo votacao : request.cargos()) {
            if (!cargosInformados.add(votacao.cargo())) {
                throw new RegraNegocioException("Cargo " + votacao.cargo() + " informado mais de uma vez.");
            }
            registrarVotacao(boletim, votacao, candidatos, request.comparecimento());
        }

        return BoletimResponse.de(boletimRepository.save(boletim));
    }

    private static void registrarVotacao(BoletimUrna boletim, VotacaoCargo votacao,
                                         Map<ChaveCandidato, Candidato> candidatos, int comparecimento) {
        Cargo cargo = votacao.cargo();
        Set<String> numerosInformados = new HashSet<>();
        int nominais = 0;
        for (Voto voto : votacao.votos()) {
            if (!numerosInformados.add(voto.numero())) {
                throw new RegraNegocioException("Número " + voto.numero() + " repetido para " + cargo + ".");
            }
            Candidato candidato = candidatos.get(new ChaveCandidato(cargo, voto.numero()));
            if (candidato == null) {
                throw new RegraNegocioException("Número " + voto.numero() + " não corresponde a candidato a " + cargo + ".");
            }
            boletim.registrarVoto(candidato, voto.quantidade());
            nominais += voto.quantidade();
        }

        int total = nominais + votacao.brancos() + votacao.nulos();
        if (total != comparecimento) {
            throw new RegraNegocioException("Para " + cargo + ", nominais + brancos + nulos (" + total
                    + ") deve ser igual ao comparecimento (" + comparecimento + ").");
        }
        boletim.registrarCargo(cargo, votacao.brancos(), votacao.nulos());
    }
}
