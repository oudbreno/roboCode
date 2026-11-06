package PrimeiroRoboMata;

import robocode.util.Utils;

public class OndaPropria {
    public final double x, y;            
    public final double velocidade;
    public final double anguloDireto;   
    public final double maxEscape;      
    public final int dirLateral;
    public final long tempoCriacao;
    public final int segDistancia, segLateral;
    public final double[] angulos;

    public OndaPropria(double x, double y, double velocidade, double anguloDireto, int dirLateral, long tempoCriacao, int segDistancia, int segLateral, double[] angulos ) {
        this.x = x;
        this.y = y;
        this.velocidade = velocidade;
        this.anguloDireto = anguloDireto;
        this.dirLateral = dirLateral;
        this.tempoCriacao = tempoCriacao;
        this.segDistancia = segDistancia;
        this.segLateral = segLateral;
        this.maxEscape = Math.asin(8.0 / velocidade);
        this.angulos = angulos;
    }

    public double distanciaPercorrida(long tempo) {
        return (tempo - tempoCriacao) * velocidade;
    }

    public int divisaoDe(double px, double py) {
        double desvio = Utils.normalRelativeAngle(Math.atan2(px - x, py - y) - anguloDireto);
        double gf = Math.max(-1, Math.min(1, desvio / maxEscape * dirLateral));
        return (int) Math.round((gf + 1) / 2 * (Inimigo.DIVISOES - 1));
    }
}
