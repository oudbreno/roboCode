package PrimeiroRoboMata;

import robocode.util.Utils;

public class OndaInimiga {
    public final Inimigo dono;
    public final double x, y;          
    public final double velocidade;    
    public final double anguloDireto;  
    public final int dirLateral;       
    public final long tempoDisparo;
    public final double maxEscape;

    public OndaInimiga(Inimigo dono, double x, double y, double velocidade, double anguloDireto, int dirLateral, long tempoDisparo) {
        this.dono = dono;
        this.x = x;
        this.y = y;
        this.velocidade = velocidade;
        this.anguloDireto = anguloDireto;
        this.dirLateral = dirLateral;
        this.tempoDisparo = tempoDisparo;
        this.maxEscape = Math.asin(8.0 / velocidade);
    }

    public double distanciaPercorrida(long tempo) {
        return (tempo - tempoDisparo) * velocidade;
    }

    public int divisaoDe(double px, double py) {
        double ang = Math.atan2(px - x, py - y);
        double desvio = Utils.normalRelativeAngle(ang - anguloDireto);
        double gf = Math.max(-1, Math.min(1, desvio / maxEscape * dirLateral));
        return (int) Math.round((gf + 1) / 2 * (Inimigo.DIVISOES - 1));
    }
}