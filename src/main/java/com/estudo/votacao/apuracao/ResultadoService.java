package com.estudo.votacao.apuracao;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.estudo.votacao.boletim.BoletimRepository;
import com.estudo.votacao.comum.RecursoNaoEncontradoException;
import com.estudo.votacao.eleicao.Cargo;
import com.estudo.votacao.eleicao.EleicaoRepository;
import com.estudo.votacao.secao.SecaoRepository;

@Service
public class ResultadoService {

    private final ApuracaoRepository apuracaoRepository;
    private final EleicaoRepository eleicaoRepository;
    private final BoletimRepository boletimRepository;
    private final SecaoRepository secaoRepository;

    public ResultadoService(ApuracaoRepository apuracaoRepository,
                            EleicaoRepository eleicaoRepository,
                            BoletimRepository boletimRepository,
                            SecaoRepository secaoRepository) {
        this.apuracaoRepository = apuracaoRepository;
        this.eleicaoRepository = eleicaoRepository;
        this.boletimRepository = boletimRepository;
        this.secaoRepository = secaoRepository;
    }

    @Transactional(readOnly = true)
    public ResultadoResponse apurar(Long eleicaoId, Cargo cargo) {
        if (!eleicaoRepository.existsById(eleicaoId)) {
            throw new RecursoNaoEncontradoException("Eleição " + eleicaoId + " não encontrada.");
        }

        TotaisNaoNominais naoNominais = apuracaoRepository.totalizarNaoNominais(eleicaoId, cargo);
        ResultadoCargo resultado = Apuracao.apurar(cargo,
                apuracaoRepository.totalizarPorCandidato(eleicaoId, cargo),
                naoNominais.brancos(), naoNominais.nulos());

        long totalizadas = boletimRepository.countByEleicaoId(eleicaoId);
        long total = secaoRepository.count();
        BigDecimal percentual = total == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(totalizadas * 100).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        return new ResultadoResponse(eleicaoId, totalizadas, total, percentual, totalizadas < total, resultado);
    }
}
