package PrimeiroRoboMata;

import robocode.*;
import robocode.util.Utils;
import java.util.Random;

public class CloseRange {

    public static final double DISTANCIA_MAXIMA = 250;

    public static final double DISTANCIA_IDEAL = 130;
    private static final double MARGEM_PAREDE = 60;
    private static final double ENERGIA_PRA_ATROPELAR = 20;

    private int sentido = 1;
    private long proximaTroca = 0;
    private Random aleatorio = new Random();

    public void agir(ScannedRobotEvent e, AdvancedRobot robo) {
        double anguloInimigo = robo.getHeadingRadians() + e.getBearingRadians();

        if (deveAtropelar(e, robo)) {
            atropelar(e, robo);
        } else {
            andarEmVolta(e, anguloInimigo, robo);
        }

        mirarEAtirar(e, anguloInimigo, robo);
    }

    public void inverterSentido() {
        sentido = -sentido;
    }

    private void andarEmVolta(ScannedRobotEvent e, double anguloInimigo, AdvancedRobot robo) {
        if (robo.getTime() >= proximaTroca) {
            inverterSentido();
            proximaTroca = robo.getTime() + 15 + aleatorio.nextInt(30);
        }

        double correcao = (e.getDistance() - DISTANCIA_IDEAL) / DISTANCIA_IDEAL * 0.6;
        correcao = Math.max(-0.6, Math.min(0.6, correcao));
        double direcao = anguloInimigo + sentido * (Math.PI / 2 - correcao);

        if (vaiBaterNaParede(direcao, robo)) {
            inverterSentido();
            direcao = anguloInimigo + sentido * (Math.PI / 2 - correcao);
        }

        irPara(direcao, 120, robo);
    }

    private boolean deveAtropelar(ScannedRobotEvent e, AdvancedRobot robo) {
        return e.getEnergy() < ENERGIA_PRA_ATROPELAR && robo.getEnergy() > e.getEnergy() * 2;
    }

    private void atropelar(ScannedRobotEvent e, AdvancedRobot robo) {
        double anguloInimigo = robo.getHeadingRadians() + e.getBearingRadians();
        irPara(anguloInimigo, e.getDistance() + 40, robo);
    }

    private boolean vaiBaterNaParede(double direcao, AdvancedRobot robo) {
        double x = robo.getX() + Math.sin(direcao) * 100;
        double y = robo.getY() + Math.cos(direcao) * 100;
        return x < MARGEM_PAREDE || y < MARGEM_PAREDE
            || x > robo.getBattleFieldWidth() - MARGEM_PAREDE
            || y > robo.getBattleFieldHeight() - MARGEM_PAREDE;
    }

    private void irPara(double direcao, double distancia, AdvancedRobot robo) {
        double giro = Utils.normalRelativeAngle(direcao - robo.getHeadingRadians());
        if (Math.abs(giro) > Math.PI / 2) {
            giro = Utils.normalRelativeAngle(giro + Math.PI);
            distancia = -distancia;
        }
        robo.setTurnRightRadians(giro);
        robo.setAhead(distancia);
    }

    private void mirarEAtirar(ScannedRobotEvent e, double anguloInimigo, AdvancedRobot robo) {
        double potencia = escolherPotencia(e, robo);
        double velocidadeBala = Rules.getBulletSpeed(potencia);

        double velocidadeLateral = e.getVelocity() * Math.sin(e.getHeadingRadians() - anguloInimigo);
        double adiantar = Math.asin(velocidadeLateral / velocidadeBala);

        double giroArma = Utils.normalRelativeAngle(anguloInimigo + adiantar - robo.getGunHeadingRadians());
        robo.setTurnGunRightRadians(giroArma);

        if (robo.getGunHeat() == 0 && Math.abs(robo.getGunTurnRemaining()) < 10) {
            robo.setFire(potencia);
        }
    }

    private double escolherPotencia(ScannedRobotEvent e, AdvancedRobot robo) {
        double potencia;
        if (e.getDistance() < 120) {
            potencia = 3;
        } else {
            potencia = 2.4;
        }

        if (robo.getEnergy() < 15) {
            potencia = 1.2;
        }

        if (e.getEnergy() < 4) {
            potencia = Math.max(0.1, e.getEnergy() / 4);
        }

        return potencia;
    }
}
