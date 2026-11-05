package PrimeiroRoboMata;

import robocode.*;
import robocode.util.Utils;
import java.util.Random;

/**
 * Estado "50% da vida" (half-life).
 *
 * Quando a nossa energia cai para 50 ou menos, o robô fica mais cuidadoso:
 *  - mantém uma distância MAIOR do inimigo (fica rodando em volta dele, mas de longe),
 *    porque de longe é mais difícil ele nos acertar;
 *  - atira com potência MENOR, porque cada tiro gasta energia nossa
 *    (tiro de potência 3 gasta 3 de energia; tiro de potência 1 gasta só 1);
 *  - nunca gasta mais energia do que precisa pra matar o inimigo;
 *  - nunca atira se o tiro puder nos deixar sem energia (robô sem energia fica desabilitado).
 *
 * Como usar no robô principal (veja o exemplo no final deste arquivo):
 *   if (HalfLife.ativo(this)) halfLife.agir(e, this);
 */
public class HalfLife {

    // a partir dessa energia (ou menos) o robô entra no modo half-life
    public static final double ENERGIA_LIMITE = 50;

    public static final double DISTANCIA_IDEAL = 300;   // distância que a gente tenta manter
    private static final double MARGEM_PAREDE = 60;     // não deixa chegar mais perto que isso da parede
    private static final double POTENCIA_MAXIMA = 2;    // nunca atira mais forte que isso neste modo

    private int sentido = 1;         // 1 = horário, -1 = anti-horário
    private long proximaTroca = 0;   // quando trocar de sentido de novo
    private Random aleatorio = new Random();

    /** Diz se o robô está com 50% da vida (ou menos) e portanto deve usar este modo. */
    public static boolean ativo(AdvancedRobot robo) {
        return robo.getEnergy() <= ENERGIA_LIMITE;
    }

    /**
     * Chamado no onScannedRobot quando o modo half-life está ativo.
     * Faz duas coisas: anda e atira.
     */
    public void agir(ScannedRobotEvent e, AdvancedRobot robo) {
        double anguloInimigo = robo.getHeadingRadians() + e.getBearingRadians();

        andarEmVolta(e, anguloInimigo, robo);
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
            proximaTroca = robo.getTime() + 20 + aleatorio.nextInt(30);
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

        // se a potência for 0, não vale a pena atirar agora (só mira)
        double velocidadeBala = Rules.getBulletSpeed(Math.max(potencia, Rules.MIN_BULLET_POWER));

        // mira um pouco à frente, pro lado que o inimigo está andando
        double velocidadeLateral = e.getVelocity() * Math.sin(e.getHeadingRadians() - anguloInimigo);
        double adiantar = Math.asin(velocidadeLateral / velocidadeBala);

        double giroArma = Utils.normalRelativeAngle(anguloInimigo + adiantar - robo.getGunHeadingRadians());
        robo.setTurnGunRightRadians(giroArma);

        // só atira se tiver potência, a arma estiver fria e mais ou menos apontada
        // (tolerância = metade da largura do robô inimigo vista da nossa distância)
        double tolerancia = Math.atan(18 / e.getDistance());
        if (potencia > 0 && robo.getGunHeat() == 0
                && Math.abs(robo.getGunTurnRemainingRadians()) < tolerancia) {
            robo.setFire(potencia);
        }
    }

    /**
     * Escolhe quanta energia gastar no tiro. Devolve 0 se for melhor não atirar.
     */
    private double escolherPotencia(ScannedRobotEvent e, AdvancedRobot robo) {
        // 1) pela distância: de perto acerta mais fácil, então vale gastar um pouco mais
        double potencia;
        if (e.getDistance() < 150) {
            potencia = POTENCIA_MAXIMA;   // 2
        } else if (e.getDistance() < 300) {
            potencia = 1.5;
        } else {
            potencia = 1;
        }

        // 2) energia baixa (menos de 20): economiza ainda mais
        if (robo.getEnergy() < 20) {
            potencia = Math.min(potencia, Math.max(Rules.MIN_BULLET_POWER, robo.getEnergy() / 10));
        }

        // 3) não gasta mais do que precisa pra matar.
        // dano do tiro = 4 * p   (se p <= 1)
        //              = 4 * p + 2 * (p - 1)   (se p > 1)
        // então a potência que mata é p = energia/4 (p <= 1) ou p = (energia + 2)/6 (p > 1)
        double energiaInimigo = e.getEnergy();
        double potenciaQueMata;
        if (energiaInimigo <= 4) {
            potenciaQueMata = energiaInimigo / 4;
        } else {
            potenciaQueMata = (energiaInimigo + 2) / 6;
        }
        potencia = Math.min(potencia, Math.max(Rules.MIN_BULLET_POWER, potenciaQueMata));

        // 4) nunca atira se o tiro nos deixar sem energia (ficaríamos desabilitados)
        if (robo.getEnergy() - potencia <= 0) {
            return 0;
        }

        return potencia;
    }
}

/* ======================================================================
 * COMO ENCAIXAR NO ROBÔ PRINCIPAL (RoboMatadorDePorco.java)
 *
 * 1) Junto com os outros atributos da classe:
 *        private HalfLife halfLife = new HalfLife();
 *
 * 2) No onScannedRobot, escolhendo o estado:
 *        if (HalfLife.ativo(this)) {
 *            halfLife.agir(e, this);
 *        } else if (e.getDistance() < CloseRange.DISTANCIA_MAXIMA) {
 *            closeRange.agir(e, this);
 *        } else {
 *            ... (estado normal / 1x1 dos outros)
 *        }
 *
 * 3) No onHitByBullet e no onHitWall, junto com o que já existe:
 *        halfLife.inverterSentido();
 *
 * A ORDEM dos "if" (qual estado ganha quando dois valem ao mesmo tempo)
 * é uma decisão que o grupo precisa tomar junto.
 * ====================================================================== */
