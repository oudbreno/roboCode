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

    private static final int MIRA_DIRETA = 0;
    private static final int MIRA_LINEAR = 1;
    private static final int MIRA_CIRCULAR = 2;
    private static final int MIRA_GUESSFACTOR = 3;
    private static final int TICKS_PARADO_DIRETA = 2;

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

    private int escolherArma(Inimigo inimigo) {
        boolean parado = inimigo.ticksParado >= TICKS_PARADO_DIRETA;
        boolean desabilitado = inimigo.energia <= 0;
        if (parado || desabilitado) return MIRA_DIRETA;

        return inimigo.melhorArma(MIRA_CIRCULAR);
    }

    public void mirarEAtirar(Inimigo inimigo, AdvancedRobot robo) {
        if (inimigo.nome == null || robo.getTime() - inimigo.tempoScan > 1) return;

        double potencia = escolherPotencia(robo, inimigo);
        if (potencia < 0.1) return;
        double velBala = Rules.getBulletSpeed(potencia);

        double angulo = anguloDaArma(escolherArma(inimigo), velBala, robo, inimigo);
        robo.setTurnGunRightRadians(Utils.normalRelativeAngle(angulo - robo.getGunHeadingRadians()));

        double tolerancia = Math.atan(18 / inimigo.distancia);
        if (robo.getGunHeat() == 0 && Math.abs(robo.getGunTurnRemainingRadians()) < tolerancia) {
            robo.setFire(potencia);
        }
    }

    private double escolherPotencia(AdvancedRobot robo, Inimigo inimigo) {
        double p = inimigo.distancia < 150 ? 3 : (inimigo.distancia < 400 ? 2 : 1);
        return Math.min(p, robo.getEnergy() - 0.1);  
    }

    private double anguloDaArma(int arma, double velBala, AdvancedRobot robo, Inimigo inimigo) {
        double anguloDireto = Math.atan2(inimigo.x - robo.getX(), inimigo.y - robo.getY());

        switch (arma) {
            case MIRA_LINEAR:
            case MIRA_CIRCULAR: {
                Point2D.Double p = preverAlvo(arma == MIRA_CIRCULAR, velBala, robo, inimigo);
                return Math.atan2(p.x - robo.getX(), p.y - robo.getY());
            }
            case MIRA_GUESSFACTOR: {
                int divisao = inimigo.melhorDivisao(Inimigo.faixaDistancia(inimigo.distancia),
                                                    Inimigo.faixaLateral(inimigo.velocidadeLateral));
                double gf = divisao * 2.0 / (Inimigo.DIVISOES - 1) - 1;
                int dir = inimigo.velocidadeLateral >= 0 ? 1 : -1;
                return anguloDireto + gf * Math.asin(8.0 / velBala) * dir;
            }
            default:
                return anguloDireto;
        }
    }

    private Point2D.Double preverAlvo(boolean circular, double velBala, AdvancedRobot robo, Inimigo inimigo) {
        double ex = inimigo.x, ey = inimigo.y;
        double rumo = inimigo.direcao;

        double minX = MARGEM_PAREDE, maxX = robo.getBattleFieldWidth() - MARGEM_PAREDE;
        double minY = MARGEM_PAREDE, maxY = robo.getBattleFieldHeight() - MARGEM_PAREDE;

        for (int t = 1; t <= 1000; t++) {
            if (circular) rumo += inimigo.deltaDirecao;
            ex += Math.sin(rumo) * inimigo.velocidade;
            ey += Math.cos(rumo) * inimigo.velocidade;

            ex = Math.max(minX, Math.min(maxX, ex));
            ey = Math.max(minY, Math.min(maxY, ey));

            if (Point2D.distance(robo.getX(), robo.getY(), ex, ey) <= t * velBala) break;
        }
        return new Point2D.Double(ex, ey);
    }

    public void lancarOndaPropria(AdvancedRobot robo, Inimigo inimigo, List<OndaPropria> ondasPropria) {
        double velBala = Rules.getBulletSpeed(Math.max(0.1, escolherPotencia(robo, inimigo)));
        double anguloDireto = Math.atan2(inimigo.x - robo.getX(), inimigo.y - robo.getY());
        int dir = inimigo.velocidadeLateral >= 0 ? 1 : -1;

        double[] angulos = new double[Inimigo.ARMAS];
        for (int i = 0; i < Inimigo.ARMAS; i++) {
            angulos[i] = anguloDaArma(i, velBala, robo, inimigo);
        }

        ondasPropria.add(new OndaPropria(robo.getX(), robo.getY(), velBala, anguloDireto, dir, robo.getTime(),
                Inimigo.faixaDistancia(inimigo.distancia),
                Inimigo.faixaLateral(inimigo.velocidadeLateral), angulos));
    }

    public void processarOndasPropria(AdvancedRobot robo, Inimigo inimigo, List<OndaPropria> ondasPropria) {
        if (inimigo.nome == null || robo.getTime() - inimigo.tempoScan > 1) return;

        Iterator<OndaPropria> it = ondasPropria.iterator();
        while (it.hasNext()) {
            OndaPropria o = it.next();
            double dist = Point2D.distance(o.x, o.y, inimigo.x, inimigo.y);

            if (o.distanciaPercorrida(robo.getTime()) > dist - 18) {
                inimigo.registrarFator(o.segDistancia, o.segLateral, o.divisaoDe(inimigo.x, inimigo.y));

                double anguloReal = Math.atan2(inimigo.x - o.x, inimigo.y - o.y);
                double tolerancia = Math.atan(18 / dist);
                for (int i = 0; i < Inimigo.ARMAS; i++) {
                    double erro = Math.abs(Utils.normalRelativeAngle(anguloReal - o.angulos[i]));
                    inimigo.atualizarNota(i, erro < tolerancia);
                }
                it.remove();
            }
        }
    }
}