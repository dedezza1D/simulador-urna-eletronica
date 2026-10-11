package com.estudo.votacao.eleicao;

/** Cargos majoritários. Proporcionais (deputados) ficam fora do escopo do treino. */
public enum Cargo {

    PRESIDENTE(2, true, 3),
    GOVERNADOR(2, true, 2),
    SENADOR(3, false, 1);

    private final int digitos;
    private final boolean temSegundoTurno;
    private final int ordemNaUrna;

    Cargo(int digitos, boolean temSegundoTurno, int ordemNaUrna) {
        this.digitos = digitos;
        this.temSegundoTurno = temSegundoTurno;
        this.ordemNaUrna = ordemNaUrna;
    }

    public int digitos() {
        return digitos;
    }

    /** Na eleição geral, a urna pede os votos de deputado, depois senador, governador e presidente. */
    public int ordemNaUrna() {
        return ordemNaUrna;
    }

    public boolean temSegundoTurno() {
        return temSegundoTurno;
    }

    public boolean aceitaNumero(String numero) {
        return numero != null
                && numero.length() == digitos
                && numero.chars().allMatch(Character::isDigit);
    }
}
