package com.estudo.votacao.comum;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Rotas sem extensão para as páginas estáticas do frontend (cada terminal numa página). */
@Configuration
public class RotasFrontend implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/mesario").setViewName("forward:/mesario.html");
        registry.addViewController("/urna").setViewName("forward:/urna.html");
        registry.addViewController("/transmissao").setViewName("forward:/transmissao.html");
        registry.addViewController("/resultados").setViewName("forward:/resultados.html");
        registry.addViewController("/candidatos").setViewName("forward:/candidatos.html");
    }
}
