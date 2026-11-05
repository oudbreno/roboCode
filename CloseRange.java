package PrimeiroRoboMata;

import robocode.*;
import robocode.util.Utils;
import java.util.Random;

/**
 * Estado de curta distância (close-range).
 *
 * A ideia é simples:
 *  - quando o inimigo está perto, a gente fica rodando em volta dele, sem parar,
 *    tentando manter uma distância boa (nem colado, nem longe);
 *  - de perto é difícil errar, então atira forte;
 *  - se ele estiver quase sem energia e a gente estiver melhor, vai pra cima e bate nele.
 */
public class CloseRange {

    // abaixo dessa distância o robô entra no modo close-range
    public static final double DISTANCIA_MAXIMA = 250;

    public static final double DISTANCIA_IDEAL = 130;    // distância que a gente tenta manter
    private static final double MARGEM_PAREDE = 60;       // não deixa chegar mais perto que isso da parede
    private static final double ENERGIA_PRA_ATROPELAR = 20;

    private int sentido = 1;         // 1 = horário, -1 = anti-horário
    private long proximaTroca = 0;   // quando trocar de sentido de novo
    private Random aleatorio = new Random();

    /**
     * Chamado no onScannedRobot quando o inimigo está perto.
     */
    public void agir(ScannedRobotEvent e, AdvancedRobot robo) {
        double anguloInimigo = robo.getHeadingRadians() + e.getBearingRadians();

        if (deveAtropelar(e, robo)) {
            atropelar(e, robo);
        } else {
            andarEmVolta(e, anguloInimigo, robo);
        }

        mirarEAtirar(e, anguloInimigo, robo);
    }

    /** Usado quando bate na parede ou toma tiro: muda o sentido da volta. */
    public void inverterSentido() {
        sentido = -sentido;
    }

    // ---------------- movimento ----------------

    private void andarEmVolta(ScannedRobotEvent e, double anguloInimigo, AdvancedRobot robo) {
        // troca de sentido de vez em quando pra não ficar previsível
        if (robo.getTime() >= proximaTroca) {
            inverterSentido();
            proximaTroca = robo.getTime() + 15 + aleatorio.nextInt(30);
        }

        // andar de lado (90 graus) em relação ao inimigo.
        // se tá longe demais, puxa um pouco pra dentro; se tá perto demais, abre um pouco.
        double correcao = (e.getDistance() - DISTANCIA_IDEAL) / DISTANCIA_IDEAL * 0.6;
        correcao = Math.max(-0.6, Math.min(0.6, correcao));
        double direcao = anguloInimigo + sentido * (Math.PI / 2 - correcao);

        // se esse caminho leva pra parede, vai pro outro lado
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
        // vai reto em cima dele (passa um pouco do ponto pra garantir a batida)
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

    /** Vira pra direção e anda. Se for mais rápido ir de ré, vai de ré. */
    private void irPara(double direcao, double distancia, AdvancedRobot robo) {
        double giro = Utils.normalRelativeAngle(direcao - robo.getHeadingRadians());
        if (Math.abs(giro) > Math.PI / 2) {
            giro = Utils.normalRelativeAngle(giro + Math.PI);
            distancia = -distancia;
        }
        robo.setTurnRightRadians(giro);
        robo.setAhead(distancia);
    }

    // ---------------- tiro ----------------

    private void mirarEAtirar(ScannedRobotEvent e, double anguloInimigo, AdvancedRobot robo) {
        double potencia = escolherPotencia(e, robo);
        double velocidadeBala = Rules.getBulletSpeed(potencia);

        // mira um pouco à frente, pro lado que o inimigo está andando
        double velocidadeLateral = e.getVelocity() * Math.sin(e.getHeadingRadians() - anguloInimigo);
        double adiantar = Math.asin(velocidadeLateral / velocidadeBala);

        double giroArma = Utils.normalRelativeAngle(anguloInimigo + adiantar - robo.getGunHeadingRadians());
        robo.setTurnGunRightRadians(giroArma);

        // só atira se a arma estiver fria e mais ou menos apontada
        if (robo.getGunHeat() == 0 && Math.abs(robo.getGunTurnRemaining()) < 10) {
            robo.setFire(potencia);
        }
    }

    private double escolherPotencia(ScannedRobotEvent e, AdvancedRobot robo) {
        double potencia;
        if (e.getDistance() < 120) {
            potencia = 3;      // colado: tiro máximo
        } else {
            potencia = 2.4;
        }

        // pouca energia: economiza pra não morrer atirando
        if (robo.getEnergy() < 15) {
            potencia = 1.2;
        }

        // não gasta mais do que precisa pra matar
        if (e.getEnergy() < 4) {
            potencia = Math.max(0.1, e.getEnergy() / 4);
        }

        return potencia;
    }
}
