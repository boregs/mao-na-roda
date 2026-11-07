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
    private static final double MARGEM_RELATO_LOCAL = 15;

    public final Rota rota;
    public final String perfil;
    public final LocalRota destino;
    public boolean ativo;
    public Ponto posicao;
    private final double[] acumulado;
    private final double[] fimEtapa;
    private final Set<Evidencia> avisadas = new HashSet<>();
    private final Set<Evidencia> consultadas = new HashSet<>();
    private Evidencia bloqueioProximo;
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
        bloqueioProximo = null;
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
        bloqueioProximo = null;
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
        bloqueioProximo = rota.evidencias().stream()
                .filter(e -> e.impeditiva() && !consultadas.contains(e)
                        && e.inicio() * total() >= Math.max(progresso, leitura.progresso + precisao)
                        && e.inicio() * total() - leitura.progresso <= APROXIMACAO)
                .min(java.util.Comparator.comparingDouble(Evidencia::inicio)).orElse(null);
        if (!novos.isEmpty()) alerta = "Evidências próximas: " + String.join(", ", novos);
    }

    public Evidencia consumirBloqueioProximo() {
        if (!ativo || chegou() || bloqueioProximo == null) return null;
        var bloqueio = bloqueioProximo;
        bloqueioProximo = null;
        consultadas.add(bloqueio);
        return bloqueio;
    }

    public boolean bloqueioAindaAdiante(Evidencia evidencia) {
        return progresso < evidencia.inicio() * total();
    }

    public boolean evitaBloqueio(Rota alternativa, Evidencia evidencia) {
        if (alternativa.impeditivas() > 0 || alternativa.id().equals(rota.id())) return false;
        var linha = rota.pontos();
        // As frações dos trechos vêm de ST_LineSubstring sobre geometria EPSG:4326.
        double comprimento = 0;
        for (int i = 1; i < linha.size(); i++) comprimento += graus(linha.get(i - 1), linha.get(i));
        if (comprimento == 0) return false;
        double inicio = evidencia.inicio() * comprimento;
        double fim = evidencia.fim() * comprimento;
        double acumuladoGraus = 0;
        double sul = 90, norte = -90, oeste = 180, leste = -180;
        for (int i = 1; i < linha.size() && acumuladoGraus < fim; i++) {
            var a = linha.get(i - 1);
            var b = linha.get(i);
            double segmento = graus(a, b);
            double proximo = acumuladoGraus + segmento;
            if (segmento > 0 && proximo > inicio) {
                double de = Math.max(0, (inicio - acumuladoGraus) / segmento);
                double ate = Math.min(1, (fim - acumuladoGraus) / segmento);
                double latitudeDe = interpolar(a.latitude(), b.latitude(), de);
                double latitudeAte = interpolar(a.latitude(), b.latitude(), ate);
                double longitudeDe = interpolar(a.longitude(), b.longitude(), de);
                double longitudeAte = interpolar(a.longitude(), b.longitude(), ate);
                sul = Math.min(sul, Math.min(latitudeDe, latitudeAte));
                norte = Math.max(norte, Math.max(latitudeDe, latitudeAte));
                oeste = Math.min(oeste, Math.min(longitudeDe, longitudeAte));
                leste = Math.max(leste, Math.max(longitudeDe, longitudeAte));
            }
            acumuladoGraus = proximo;
        }
        double margem = 5 / METROS_POR_GRAU;
        double margemLongitude = margem / Math.cos(Math.toRadians(Math.max(Math.abs(sul), Math.abs(norte))));
        if (sul > norte || leste - oeste >= 180 || oeste - margemLongitude <= -180 || leste + margemLongitude >= 180)
            return false;
        var pontos = alternativa.pontos();
        for (int i = 1; i < pontos.size(); i++) {
            var a = pontos.get(i - 1);
            var b = pontos.get(i);
            if (Math.abs(a.longitude() - b.longitude()) > 180
                    || cruzaArea(a, b, sul - margem, norte + margem, oeste - margemLongitude, leste + margemLongitude))
                return false;
        }
        return pontos.size() >= 2;
    }

    private static boolean cruzaArea(Ponto a, Ponto b, double sul, double norte, double oeste, double leste) {
        double delta = b.latitude() - a.latitude();
        if (delta == 0) {
            return a.latitude() >= sul && a.latitude() <= norte
                    && Math.max(a.longitude(), b.longitude()) >= oeste && Math.min(a.longitude(), b.longitude()) <= leste;
        }
        double tSul = (sul - a.latitude()) / delta;
        double tNorte = (norte - a.latitude()) / delta;
        double de = Math.max(0, Math.min(tSul, tNorte));
        double ate = Math.min(1, Math.max(tSul, tNorte));
        if (de > ate) return false;
        double x1 = interpolar(a.longitude(), b.longitude(), de);
        double x2 = interpolar(a.longitude(), b.longitude(), ate);
        return Math.max(x1, x2) >= oeste && Math.min(x1, x2) <= leste;
    }

    private static double graus(Ponto a, Ponto b) {
        return Math.hypot(b.latitude() - a.latitude(), b.longitude() - a.longitude());
    }

    private static double interpolar(double a, double b, double t) {
        return a + (b - a) * t;
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

    public static boolean pertoDaRota(Ponto ponto, List<Ponto> linha) {
        for (int i = 1; i < linha.size(); i++) {
            if (projetarSegmento(ponto, linha.get(i - 1), linha.get(i), 0, 1).distancia <= MARGEM_RELATO_LOCAL)
                return true;
        }
        return false;
    }

    public static int recomendarAlternativa(List<Rota> rotas, List<Ponto> barreiras) {
        if (rotas.size() < 2 || barreiras.stream().noneMatch(ponto -> pertoDaRota(ponto, rotas.get(0).pontos()))) {
            return -1;
        }
        for (int i = 1; i < rotas.size(); i++) {
            Rota rota = rotas.get(i);
            if (rota.impeditivas() == 0 && barreiras.stream().noneMatch(ponto -> pertoDaRota(ponto, rota.pontos()))) {
                return i;
            }
        }
        return -1;
    }

    private Projecao projetar(Ponto ponto, double minimo, double maximo) {
        var melhor = new Projecao(minimo, Double.POSITIVE_INFINITY);
        List<Ponto> linha = rota.pontos();
        for (int i = 1; i < acumulado.length && acumulado[i - 1] <= maximo; i++) {
            double comprimento = acumulado[i] - acumulado[i - 1];
            if (acumulado[i] < minimo || comprimento == 0) continue;
            double inicio = Math.max(0, (minimo - acumulado[i - 1]) / comprimento);
            double fim = Math.min(1, (maximo - acumulado[i - 1]) / comprimento);
            var projecao = projetarSegmento(ponto, linha.get(i - 1), linha.get(i), inicio, fim);
            if (projecao.distancia < melhor.distancia) {
                melhor = new Projecao(acumulado[i - 1] + projecao.fracao * comprimento, projecao.distancia);
            }
        }
        return melhor;
    }

    private record ProjecaoSegmento(double fracao, double distancia) { }

    private static ProjecaoSegmento projetarSegmento(Ponto ponto, Ponto a, Ponto b, double inicio, double fim) {
        double escala = Math.cos(Math.toRadians(a.latitude())) * METROS_POR_GRAU;
        double x = deltaLongitude(b.longitude() - a.longitude()) * escala;
        double y = (b.latitude() - a.latitude()) * METROS_POR_GRAU;
        double px = deltaLongitude(ponto.longitude() - a.longitude()) * escala;
        double py = (ponto.latitude() - a.latitude()) * METROS_POR_GRAU;
        double comprimentoQuadrado = x * x + y * y;
        double t = comprimentoQuadrado == 0 ? 0 : (px * x + py * y) / comprimentoQuadrado;
        t = Math.max(inicio, Math.min(fim, t));
        return new ProjecaoSegmento(t, Math.hypot(px - t * x, py - t * y));
    }

    private static double deltaLongitude(double graus) { return Math.IEEEremainder(graus, 360); }

    private static double distancia(Ponto a, Ponto b) {
        return METROS_POR_GRAU * Math.hypot(b.latitude() - a.latitude(),
                deltaLongitude(b.longitude() - a.longitude()) * Math.cos(Math.toRadians((a.latitude() + b.latitude()) / 2)));
    }
}
