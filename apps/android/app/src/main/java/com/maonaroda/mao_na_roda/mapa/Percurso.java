package com.maonaroda.mao_na_roda.mapa;

import com.maonaroda.mao_na_roda.data.DadosRotas.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Estado em memória de um percurso. Distâncias internas seguem a geometria exibida. */
public final class Percurso {
    public static final float PRECISAO_MAXIMA = 20;
    private static final double CORREDOR = 30;
    private static final double APROXIMACAO = 40;
    private static final double CHEGADA = 15;
    private static final int CONFIRMACOES = 3;
    private static final double METROS_POR_GRAU = 111_195;

    public final Rota rota;
    public final String perfil;
    public final LocalRota destino;
    public boolean ativo;
    public Ponto posicao;
    private final double[] acumulado;
    private final double[] fimEtapa;
    private final Set<Evidencia> avisadas = new HashSet<>();
    private double progresso;
    private long instante;
    private int fora, perto, etapaAtual, leiturasAposEtapa;
    private boolean iniciou;
    public String alerta = "";

    public Percurso(Rota rota, String perfil, LocalRota destino) {
        this.rota = rota;
        this.perfil = perfil;
        this.destino = destino;
        acumulado = new double[rota.pontos().size()];
        for (int i = 1; i < acumulado.length; i++)
            acumulado[i] = acumulado[i - 1] + distancia(rota.pontos().get(i - 1), rota.pontos().get(i));
        fimEtapa = new double[rota.etapas().size()];
        double inicio = 0;
        for (int i = 0; i < fimEtapa.length; i++) {
            var pontos = rota.etapas().get(i).pontos();
            double comprimento = 0;
            for (int j = 1; j < pontos.size(); j++) comprimento += distancia(pontos.get(j - 1), pontos.get(j));
            var fim = projetar(pontos.get(pontos.size() - 1), inicio, inicio + comprimento + CORREDOR);
            fimEtapa[i] = fim.distancia <= CORREDOR ? fim.progresso : Math.min(total(), inicio + comprimento);
            inicio = fimEtapa[i];
        }
    }

    public void interromper() {
        ativo = false;
        invalidarLeitura();
        instante = 0;
    }

    public void invalidarLeitura() {
        posicao = null;
        fora = perto = leiturasAposEtapa = 0;
    }

    public void atualizar(Ponto ponto, float precisao, long tempoMillis) {
        if (!Float.isFinite(precisao) || precisao <= 0 || precisao > PRECISAO_MAXIMA) {
            invalidarLeitura();
            return;
        }
        if (tempoMillis <= instante) return;
        double segundos = instante == 0 ? 0 : (tempoMillis - instante) / 1000.0;
        instante = tempoMillis;
        posicao = ponto;
        // Restringe saltos para trechos distantes em cruzamentos, retornos e vias paralelas.
        var leitura = projetar(ponto, Math.max(0, progresso - CORREDOR),
                progresso + CORREDOR + Math.min(segundos, 10) * 3);
        if (leitura.distancia > CORREDOR + precisao) {
            fora++;
            perto = leiturasAposEtapa = 0;
            return;
        }
        fora = 0;
        if (leitura.distancia > Math.min(15, precisao + 5)) {
            perto = leiturasAposEtapa = 0;
            return;
        }
        iniciou = true;
        if (leitura.progresso > progresso + Math.max(3, precisao / 2.0)) progresso = leitura.progresso;
        if (etapaAtual + 1 < fimEtapa.length) {
            boolean passou = leitura.progresso - precisao > fimEtapa[etapaAtual];
            leiturasAposEtapa = passou ? leiturasAposEtapa + 1 : 0;
            if (leiturasAposEtapa >= 2) {
                etapaAtual++;
                leiturasAposEtapa = 0;
            }
        }
        Ponto destinoRota = rota.pontos().get(acumulado.length - 1);
        perto = total() - progresso <= CHEGADA && distancia(ponto, destinoRota) + precisao <= CHEGADA ? perto + 1 : 0;
        var novos = new ArrayList<String>();
        for (var evidencia : rota.evidencias()) {
            if (evidencia.fim() * total() >= progresso
                    && evidencia.inicio() * total() - progresso <= APROXIMACAO && avisadas.add(evidencia))
                novos.add(evidencia.descricao());
        }
        if (!novos.isEmpty()) alerta = "Evidências próximas: " + String.join(", ", novos);
    }

    public boolean desviou() { return fora >= CONFIRMACOES; }
    public boolean chegou() { return perto >= CONFIRMACOES; }
    public double restante() { return total() <= 0 ? 0 : rota.distancia() * (1 - progresso / total()); }
    public String instrucao() {
        if (!iniciou) return "Aproxime-se do início da rota.";
        String instrucao = rota.etapas().isEmpty() ? "" : rota.etapas().get(etapaAtual).instrucao();
        return instrucao.isBlank() ? "Siga o trajeto no mapa." : instrucao;
    }

    public String proximaInstrucao() {
        if (!iniciou || etapaAtual + 1 >= fimEtapa.length
                || fimEtapa[etapaAtual] - progresso > APROXIMACAO) return "";
        return rota.etapas().get(etapaAtual + 1).instrucao();
    }

    private double total() { return acumulado[acumulado.length - 1]; }

    private record Projecao(double progresso, double distancia) { }

    private Projecao projetar(Ponto ponto, double minimo, double maximo) {
        var melhor = new Projecao(minimo, Double.POSITIVE_INFINITY);
        List<Ponto> linha = rota.pontos();
        for (int i = 1; i < acumulado.length && acumulado[i - 1] <= maximo; i++) {
            double comprimento = acumulado[i] - acumulado[i - 1];
            if (acumulado[i] < minimo || comprimento == 0) continue;
            var a = linha.get(i - 1);
            var b = linha.get(i);
            double escala = Math.cos(Math.toRadians(a.latitude())) * METROS_POR_GRAU;
            double x = deltaLongitude(b.longitude() - a.longitude()) * escala;
            double y = (b.latitude() - a.latitude()) * METROS_POR_GRAU;
            double px = deltaLongitude(ponto.longitude() - a.longitude()) * escala;
            double py = (ponto.latitude() - a.latitude()) * METROS_POR_GRAU;
            double t = (px * x + py * y) / (x * x + y * y);
            double inicio = Math.max(0, (minimo - acumulado[i - 1]) / comprimento);
            double fim = Math.min(1, (maximo - acumulado[i - 1]) / comprimento);
            t = Math.max(inicio, Math.min(fim, t));
            double distancia = Math.hypot(px - t * x, py - t * y);
            if (distancia < melhor.distancia) melhor = new Projecao(acumulado[i - 1] + t * comprimento, distancia);
        }
        return melhor;
    }

    private static double deltaLongitude(double graus) { return Math.IEEEremainder(graus, 360); }

    private static double distancia(Ponto a, Ponto b) {
        return METROS_POR_GRAU * Math.hypot(b.latitude() - a.latitude(),
                deltaLongitude(b.longitude() - a.longitude()) * Math.cos(Math.toRadians((a.latitude() + b.latitude()) / 2)));
    }
}
