package com.estudo.votacao.legado;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * EXERCÍCIO DE REFATORAÇÃO. Código propositalmente ruim, no estilo de um sistema legado.
 * Não é usado pela aplicação. Veja docs/03-refatoracao.md.
 */
public class ApuracaoLegada {

    public static final String SEP = ";";

    // l = lista de candidatos ("nome" -> String, "v" -> Integer), b = brancos, n = nulos, c = cargo
    public static List<String> proc(List<Map<String, Object>> l, int b, int n, String c) {
        List<String> r = new ArrayList<String>();
        int t = 0;
        for (int i = 0; i < l.size(); i++) {
            t = t + (Integer) l.get(i).get("v");
        }
        t = t + b; // soma brancos no total

        // ordena do maior pro menor
        for (int i = 0; i < l.size(); i++) {
            for (int j = 0; j < l.size() - 1; j++) {
                if ((Integer) l.get(j).get("v") < (Integer) l.get(j + 1).get("v")) {
                    Map<String, Object> tmp = l.get(j);
                    l.set(j, l.get(j + 1));
                    l.set(j + 1, tmp);
                }
            }
        }

        if (c.equals("SENADOR")) {
            for (int i = 0; i < l.size(); i++) {
                if (i == 0) {
                    r.add(l.get(i).get("nome") + SEP + "ELEITO");
                } else {
                    r.add(l.get(i).get("nome") + SEP + "NAO_ELEITO");
                }
            }
        } else {
            if (t > 0) {
                double p = ((Integer) l.get(0).get("v")).doubleValue() / t * 100;
                if (p >= 50) {
                    for (int i = 0; i < l.size(); i++) {
                        if (i == 0) {
                            r.add(l.get(i).get("nome") + SEP + "ELEITO");
                        } else {
                            r.add(l.get(i).get("nome") + SEP + "NAO_ELEITO");
                        }
                    }
                } else {
                    for (int i = 0; i < l.size(); i++) {
                        if (i == 0 || i == 1) {
                            r.add(l.get(i).get("nome") + SEP + "SEGUNDO_TURNO");
                        } else {
                            r.add(l.get(i).get("nome") + SEP + "NAO_ELEITO");
                        }
                    }
                }
            }
        }
        return r;
    }
}
