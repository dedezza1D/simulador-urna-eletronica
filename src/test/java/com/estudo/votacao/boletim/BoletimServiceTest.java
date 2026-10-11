package com.estudo.votacao.boletim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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

@ExtendWith(MockitoExtension.class)
class BoletimServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-04T20:30:00Z");

    @Mock
    private BoletimRepository boletimRepository;
    @Mock
    private EleicaoRepository eleicaoRepository;
    @Mock
    private SecaoRepository secaoRepository;
    @Mock
    private CandidatoRepository candidatoRepository;

    private BoletimService service;

    private final Eleicao eleicao = comId(new Eleicao("Eleição 2026", 2026, 1), 1L);
    private final Secao secao = comId(new Secao(1, 1, "Brasília", "DF", 100), 10L);
    private final Candidato ana = comId(new Candidato(eleicao, Cargo.PRESIDENTE, "10", "Ana", "PAB"), 100L);
    private final Candidato bruno = comId(new Candidato(eleicao, Cargo.PRESIDENTE, "20", "Bruno", "PCD"), 200L);

    private static <T> T comId(T entidade, Long id) {
        ReflectionTestUtils.setField(entidade, "id", id);
        return entidade;
    }

    @BeforeEach
    void setUp() {
        // Construção manual em vez de @InjectMocks: o Clock é um valor real, não um mock.
        Clock relogio = Clock.fixed(AGORA, ZoneOffset.UTC);
        service = new BoletimService(boletimRepository, eleicaoRepository, secaoRepository, candidatoRepository, relogio);
    }

    private void cenarioValido() {
        given(eleicaoRepository.findById(1L)).willReturn(Optional.of(eleicao));
        given(secaoRepository.findByZonaAndNumero(1, 1)).willReturn(Optional.of(secao));
        given(boletimRepository.existsByEleicaoIdAndSecaoId(1L, 10L)).willReturn(false);
    }

    private static RegistrarBoletimRequest boletim(int comparecimento, VotacaoCargo... cargos) {
        return new RegistrarBoletimRequest(1L, 1, 1, comparecimento, List.of(cargos));
    }

    private static VotacaoCargo presidente(int brancos, int nulos, Voto... votos) {
        return new VotacaoCargo(Cargo.PRESIDENTE, brancos, nulos, List.of(votos));
    }

    @Test
    void registraBoletimValido() {
        cenarioValido();
        given(candidatoRepository.findByEleicaoId(1L)).willReturn(List.of(ana, bruno));
        given(boletimRepository.save(any(BoletimUrna.class))).willAnswer(invocacao -> comId(invocacao.getArgument(0), 500L));

        BoletimResponse resposta = service.registrar(
                boletim(80, presidente(5, 5, new Voto("10", 40), new Voto("20", 30))));

        ArgumentCaptor<BoletimUrna> salvo = ArgumentCaptor.forClass(BoletimUrna.class);
        verify(boletimRepository).save(salvo.capture());
        BoletimUrna boletim = salvo.getValue();
        assertThat(boletim.getVotos()).extracting(v -> v.getCandidato().getNumero(), VotoCandidato::getVotos)
                .containsExactly(tuple("10", 40), tuple("20", 30));
        assertThat(boletim.getCargos()).singleElement()
                .satisfies(c -> {
                    assertThat(c.getCargo()).isEqualTo(Cargo.PRESIDENTE);
                    assertThat(c.getBrancos()).isEqualTo(5);
                    assertThat(c.getNulos()).isEqualTo(5);
                });
        assertThat(resposta.id()).isEqualTo(500L);
        assertThat(resposta.recebidoEm()).isEqualTo(AGORA);
    }

    @Test
    void rejeitaSecaoInexistente() {
        given(eleicaoRepository.findById(1L)).willReturn(Optional.of(eleicao));
        given(secaoRepository.findByZonaAndNumero(1, 1)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(boletim(10, presidente(10, 0))))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void rejeitaBoletimDuplicado() {
        given(eleicaoRepository.findById(1L)).willReturn(Optional.of(eleicao));
        given(secaoRepository.findByZonaAndNumero(1, 1)).willReturn(Optional.of(secao));
        given(boletimRepository.existsByEleicaoIdAndSecaoId(1L, 10L)).willReturn(true);

        assertThatThrownBy(() -> service.registrar(boletim(10, presidente(10, 0))))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("já foi recebido");
        verify(boletimRepository, never()).save(any());
    }

    @Test
    void rejeitaComparecimentoMaiorQueEleitoresAptos() {
        cenarioValido();

        assertThatThrownBy(() -> service.registrar(boletim(101, presidente(101, 0))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("eleitores aptos");
        verify(boletimRepository, never()).save(any());
    }

    @Test
    void rejeitaSomaDeVotosDiferenteDoComparecimento() {
        cenarioValido();
        given(candidatoRepository.findByEleicaoId(1L)).willReturn(List.of(ana, bruno));

        assertThatThrownBy(() -> service.registrar(boletim(80, presidente(5, 5, new Voto("10", 40), new Voto("20", 29)))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("(79)")
                .hasMessageContaining("(80)");
        verify(boletimRepository, never()).save(any());
    }

    @Test
    void rejeitaNumeroSemCandidatoNoCargo() {
        cenarioValido();
        given(candidatoRepository.findByEleicaoId(1L)).willReturn(List.of(ana, bruno));

        assertThatThrownBy(() -> service.registrar(boletim(80, presidente(5, 5, new Voto("99", 70)))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("99");
    }

    @Test
    void rejeitaCargoRepetido() {
        cenarioValido();
        given(candidatoRepository.findByEleicaoId(1L)).willReturn(List.of(ana, bruno));

        assertThatThrownBy(() -> service.registrar(boletim(10, presidente(10, 0), presidente(10, 0))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("mais de uma vez");
    }

    @Test
    void rejeitaNumeroRepetidoNoMesmoCargo() {
        cenarioValido();
        given(candidatoRepository.findByEleicaoId(1L)).willReturn(List.of(ana, bruno));

        assertThatThrownBy(() -> service.registrar(boletim(80, presidente(0, 0, new Voto("10", 40), new Voto("10", 40)))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("repetido");
    }
}
