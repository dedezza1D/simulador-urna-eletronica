package com.estudo.votacao.candidato;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.estudo.votacao.apuracao.ApuracaoRepository;
import com.estudo.votacao.comum.ConflitoException;
import com.estudo.votacao.comum.RecursoNaoEncontradoException;
import com.estudo.votacao.comum.RegraNegocioException;
import com.estudo.votacao.eleicao.Cargo;
import com.estudo.votacao.eleicao.Eleicao;
import com.estudo.votacao.eleicao.EleicaoRepository;

@ExtendWith(MockitoExtension.class)
class CandidatoServiceTest {

    @Mock
    private CandidatoRepository candidatoRepository;
    @Mock
    private EleicaoRepository eleicaoRepository;
    @Mock
    private ApuracaoRepository apuracaoRepository;

    @InjectMocks
    private CandidatoService service;

    private final Eleicao eleicao = comId(new Eleicao("Eleição 2026", 2026, 1), 1L);

    private static <T> T comId(T entidade, Long id) {
        ReflectionTestUtils.setField(entidade, "id", id);
        return entidade;
    }

    @Test
    void criaCandidatoNormalizandoNomeEPartido() {
        given(eleicaoRepository.findById(1L)).willReturn(Optional.of(eleicao));
        given(candidatoRepository.existsByEleicaoIdAndCargoAndNumero(1L, Cargo.PRESIDENTE, "10")).willReturn(false);
        given(candidatoRepository.save(any(Candidato.class))).willAnswer(invocacao -> comId(invocacao.getArgument(0), 99L));

        CandidatoResponse resposta = service.criar(new CandidatoRequest(1L, Cargo.PRESIDENTE, "10", "  Ana Ribeiro ", "pab"));

        ArgumentCaptor<Candidato> salvo = ArgumentCaptor.forClass(Candidato.class);
        verify(candidatoRepository).save(salvo.capture());
        assertThat(salvo.getValue().getNome()).isEqualTo("Ana Ribeiro");
        assertThat(salvo.getValue().getPartido()).isEqualTo("PAB");
        assertThat(resposta.id()).isEqualTo(99L);
        assertThat(resposta.eleicaoId()).isEqualTo(1L);
    }

    @Test
    void rejeitaEleicaoInexistente() {
        given(eleicaoRepository.findById(7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(new CandidatoRequest(7L, Cargo.PRESIDENTE, "10", "Ana", "PAB")))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Eleição 7");
        verify(candidatoRepository, never()).save(any());
    }

    @Test
    void rejeitaNumeroComQuantidadeDeDigitosErradaParaOCargo() {
        given(eleicaoRepository.findById(1L)).willReturn(Optional.of(eleicao));

        assertThatThrownBy(() -> service.criar(new CandidatoRequest(1L, Cargo.SENADOR, "10", "Igor", "PAB")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("3 dígitos");
        verify(candidatoRepository, never()).save(any());
    }

    @Test
    void rejeitaNumeroJaUsadoNoMesmoCargo() {
        given(eleicaoRepository.findById(1L)).willReturn(Optional.of(eleicao));
        given(candidatoRepository.existsByEleicaoIdAndCargoAndNumero(1L, Cargo.PRESIDENTE, "10")).willReturn(true);

        assertThatThrownBy(() -> service.criar(new CandidatoRequest(1L, Cargo.PRESIDENTE, "10", "Ana", "PAB")))
                .isInstanceOf(ConflitoException.class);
        verify(candidatoRepository, never()).save(any());
    }

    @Test
    void naoPermiteMudarNumeroDeCandidatoComVotos() {
        Candidato existente = comId(new Candidato(eleicao, Cargo.PRESIDENTE, "10", "Ana", "PAB"), 5L);
        given(candidatoRepository.findById(5L)).willReturn(Optional.of(existente));
        given(apuracaoRepository.existsByCandidatoId(5L)).willReturn(true);

        assertThatThrownBy(() -> service.atualizar(5L, new CandidatoRequest(1L, Cargo.PRESIDENTE, "11", "Ana", "PAB")))
                .isInstanceOf(ConflitoException.class);
        assertThat(existente.getNumero()).isEqualTo("10");
    }

    @Test
    void permiteCorrigirNomeDeCandidatoComVotos() {
        Candidato existente = comId(new Candidato(eleicao, Cargo.PRESIDENTE, "10", "Ana", "PAB"), 5L);
        given(candidatoRepository.findById(5L)).willReturn(Optional.of(existente));

        CandidatoResponse resposta = service.atualizar(5L,
                new CandidatoRequest(1L, Cargo.PRESIDENTE, "10", "Ana Ribeiro", "PAB"));

        assertThat(resposta.nome()).isEqualTo("Ana Ribeiro");
        verify(apuracaoRepository, never()).existsByCandidatoId(any());
    }

    @Test
    void naoRemoveCandidatoComVotos() {
        Candidato existente = comId(new Candidato(eleicao, Cargo.PRESIDENTE, "10", "Ana", "PAB"), 5L);
        given(candidatoRepository.findById(5L)).willReturn(Optional.of(existente));
        given(apuracaoRepository.existsByCandidatoId(5L)).willReturn(true);

        assertThatThrownBy(() -> service.remover(5L)).isInstanceOf(ConflitoException.class);
        verify(candidatoRepository, never()).delete(any());
    }
}
