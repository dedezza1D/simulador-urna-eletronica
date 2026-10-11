package com.estudo.votacao;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class VotacaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(VotacaoApplication.class, args);
    }

    // Injetar o relógio permite fixar o horário nos testes.
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
