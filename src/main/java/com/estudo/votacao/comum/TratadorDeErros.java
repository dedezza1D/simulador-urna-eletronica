package com.estudo.votacao.comum;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Converte exceções em respostas RFC 9457 (application/problem+json).
 * Estender {@link ResponseEntityExceptionHandler} mantém o tratamento padrão do Spring MVC
 * (JSON malformado, método não suportado, rota inexistente) em vez de virar 500.
 */
@RestControllerAdvice
public class TratadorDeErros extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    public record CampoInvalido(String campo, String mensagem) {
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<CampoInvalido> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new CampoInvalido(erro.getField(), erro.getDefaultMessage()))
                .toList();
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Requisição inválida", "Um ou mais campos são inválidos.");
        problema.setProperty("erros", erros);
        return handleExceptionInternal(ex, problema, headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ProblemDetail naoEncontrado(RecursoNaoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage());
    }

    @ExceptionHandler(ConflitoException.class)
    ProblemDetail conflito(ConflitoException ex) {
        return problema(HttpStatus.CONFLICT, "Conflito", ex.getMessage());
    }

    @ExceptionHandler(RegraNegocioException.class)
    ProblemDetail regraNegocio(RegraNegocioException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada", ex.getMessage());
    }

    // Última barreira contra corrida entre a verificação e o insert: a constraint UNIQUE do banco.
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade", ex);
        return problema(HttpStatus.CONFLICT, "Conflito", "O registro conflita com dados já existentes.");
    }

    // @Version: outra requisição alterou o mesmo registro entre a leitura e a gravação.
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail concorrencia(ObjectOptimisticLockingFailureException ex) {
        return problema(HttpStatus.CONFLICT, "Conflito", "O registro foi alterado por outra operação. Tente novamente.");
    }

    @ExceptionHandler(ParametroInvalidoException.class)
    ProblemDetail parametroInvalido(ParametroInvalidoException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Requisição inválida", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception ex) {
        log.error("Erro inesperado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Ocorreu um erro inesperado.");
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        return problema;
    }
}
