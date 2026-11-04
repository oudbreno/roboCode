package PrimeiroRoboMata;
import robocode.*;
import robocode.util.Utils;
import java.awt.geom.Rectangle2D;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.awt.geom.Point2D;
import java.util.Iterator;

public class Modulos{
    private static final double TAMANHO_BASTAO = 160;
    private static final double MARGEM_PAREDE = 18;  
    private static final double PASSO_GIRO = 0.05;    
    public int sentido = 1;                        
    private long proximaTroca = 0;
    private Random aleatorio = new Random();

    public static void controlaCamera(long tempoScan, AdvancedRobot robo){
		if (robo.getTime() - tempoScan > 1) {
        robo.setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
        }
    }

    public static void moveScanner(ScannedRobotEvent e, AdvancedRobot robo) {
    	double anguloInimigo = robo.getHeadingRadians() + e.getBearingRadians();
		double diferenca = Utils.normalRelativeAngle(anguloInimigo - robo.getRadarHeadingRadians());
        robo.setTurnRadarRightRadians(2 * diferenca);
    }
    
    public void orbitar(AdvancedRobot robo, List<OndaInimiga> ondas, Inimigo inimigo) {
        if (inimigo.nome == null) return; 

        OndaInimiga onda = ondaMaisProxima(ondas, robo);
        double ox, oy;

        if (onda != null) {
            sentido = preverPerigo(onda, -1, robo) < preverPerigo(onda, 1, robo) ? -1 : 1;
            ox = onda.x;
            oy = onda.y;
        } else {
            if (robo.getTime() >= proximaTroca) {
                sentido = -sentido;
                proximaTroca = robo.getTime() + 20 + aleatorio.nextInt(41);
            }
            ox = inimigo.x;
            oy = inimigo.y;
        }

        double anguloDaOrigem = Math.atan2(robo.getX() - ox, robo.getY() - oy);
        double desejado = suavizarParede(robo.getX(), robo.getY(), anguloDaOrigem + sentido * Math.PI / 2, sentido, robo);
        irNaDirecao(desejado, robo);
    }

    private double preverPerigo(OndaInimiga onda, int dir, AdvancedRobot robo) {
        double px = robo.getX(), py = robo.getY();
        double rumo = robo.getHeadingRadians();
        double vel = robo.getVelocity();
        long t = robo.getTime();

        for (int i = 0; i < 500; i++) {
            double aOrigem = Math.atan2(px - onda.x, py - onda.y);
            double desejado = suavizarParede(px, py, aOrigem + dir * Math.PI / 2, dir, robo);

            double giro = Utils.normalRelativeAngle(desejado - rumo);
            int marcha = 1;
            if (Math.abs(giro) > Math.PI / 2) {
                giro = Utils.normalRelativeAngle(giro + Math.PI);
                marcha = -1;
            }

            double maxGiro = Rules.getTurnRateRadians(Math.abs(vel));
            rumo += Math.max(-maxGiro, Math.min(maxGiro, giro));

            if (vel * marcha < 0) vel += marcha * Rules.DECELERATION;
            else vel += marcha * Rules.ACCELERATION;
            vel = Math.max(-Rules.MAX_VELOCITY, Math.min(Rules.MAX_VELOCITY, vel));

            px += Math.sin(rumo) * vel;
            py += Math.cos(rumo) * vel;
            t++;

            if (onda.distanciaPercorrida(t) >= Point2D.distance(onda.x, onda.y, px, py) - 18) {
                return onda.dono.perigo[onda.divisaoDe(px, py)];
            }
        }
        return onda.dono.perigo[onda.divisaoDe(px, py)];
    }

    private double suavizarParede(double px, double py, double angulo, int orientacao, AdvancedRobot robo) {
        Rectangle2D.Double campo = new Rectangle2D.Double(MARGEM_PAREDE, MARGEM_PAREDE, robo.getBattleFieldWidth() - 2 * MARGEM_PAREDE, robo.getBattleFieldHeight() - 2 * MARGEM_PAREDE);

        int tentativas = 0;
        while (!campo.contains(px + Math.sin(angulo) * TAMANHO_BASTAO, py + Math.cos(angulo) * TAMANHO_BASTAO) && tentativas < 130){
            angulo += orientacao * PASSO_GIRO;
            tentativas++;
        }
        return angulo;
    }

    private void irNaDirecao(double angulo, AdvancedRobot robo) {
        double giro = Utils.normalRelativeAngle(angulo - robo.getHeadingRadians());
        double distancia = 100;

        if (Math.abs(giro) > Math.PI / 2) {
            giro = Utils.normalRelativeAngle(giro + Math.PI);
            distancia = -distancia;
        }

        robo.setTurnRightRadians(giro);
        robo.setAhead(distancia);
    }

    public void registrarTiro(Bullet b, List<OndaInimiga> ondas, AdvancedRobot robo) {
        OndaInimiga onda = acharOnda(b, ondas, robo);
        if (onda != null) {
            onda.dono.registrarPerigo(onda.divisaoDe(b.getX(), b.getY()));
            ondas.remove(onda);
        }
    }

    private OndaInimiga acharOnda(Bullet b, List<OndaInimiga> ondas, AdvancedRobot robo) {
        OndaInimiga melhor = null;
        double menorErro = 50;
        for (OndaInimiga o : ondas) {
            if (Math.abs(o.velocidade - b.getVelocity()) > 0.001) continue;
            double erro = Math.abs(o.distanciaPercorrida(robo.getTime()) - Point2D.distance(o.x, o.y, b.getX(), b.getY()));
            if (erro < menorErro) {
                menorErro = erro;
                melhor = o;
            }
        }
        return melhor;
    }

    private void removerOndasPassadas(List<OndaInimiga> ondas, AdvancedRobot robo) {
        Iterator<OndaInimiga> it = ondas.iterator();
        while (it.hasNext()) {
            OndaInimiga o = it.next();
            if (o.distanciaPercorrida(robo.getTime()) > Point2D.distance(o.x, o.y, robo.getX(), robo.getY()) + 50) {
                it.remove();
            }
        }
    }

    private OndaInimiga ondaMaisProxima(List<OndaInimiga> ondas, AdvancedRobot robo) {
        OndaInimiga melhor = null;
        double menor = Double.MAX_VALUE;
        for (OndaInimiga o : ondas) {
            double falta = Point2D.distance(o.x, o.y, robo.getX(), robo.getY()) - o.distanciaPercorrida(robo.getTime());
            if (falta > 0 && falta < menor) {
                menor = falta;
                melhor = o;
            }
        }
        return melhor;
    }
}