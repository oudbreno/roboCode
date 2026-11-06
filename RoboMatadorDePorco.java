package PrimeiroRoboMata;
import robocode.*;
import robocode.util.Utils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.awt.Color;
import java.awt.Graphics2D;
import PrimeiroRoboMata.NormalMode.AntiGravityBot;
//import java.awt.Color;

// API help : https://robocode.sourceforge.io/docs/robocode/robocode/Robot.html

/**
 * RoboMatadorDePorco - robo principal da equipe
 *
 * Cada estado continua no arquivo de quem fez:
 *   HalfLife.java              - Augusto  (nossa energia em 50 ou menos)
 *   Modulos/Inimigo/Ondas      - Arthur   (so sobrou 1 inimigo)
 *   CloseRange.java            - Bernardo (inimigo a menos de 250px)
 *   NormalMode/AntiGravityBot  - Richard  (varios inimigos)
 *
 * Ordem: HalfLife > X1 > CloseRange > Normal
 */
public class RoboMatadorDePorco extends AntiGravityBot
{
	private static final int NORMAL = 0;
	private static final int X1 = 1;
	private static final int CLOSE_RANGE = 2;
	private static final int HALF_LIFE = 3;
	private int estado = NORMAL;

	private static Inimigo inimigo = new Inimigo();
	private Modulos modulos = new Modulos();
	private List<OndaInimiga> ondas = new ArrayList<OndaInimiga>();
	private int ondasCriadas = 0;

	private List<OndaPropria> ondasPropria = new ArrayList<OndaPropria>();

	private CloseRange closeRange = new CloseRange();
	private HalfLife halfLife = new HalfLife();

	// inimigo mais perto (usado pra saber quando entra o close-range)
	private String maisPerto = null;
	private double distanciaMaisPerto = Double.MAX_VALUE;

	public void run() {
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);
		setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
		while(true){
			if (estado == NORMAL) {
				// o modo do Richard fica rodando ate o onScannedRobot chamar stopNormalState()
				NormalStateMode();
			}

			if (getOthers() == 1) {
				Modulos.controlaCamera(inimigo.tempoScan , this);
				modulos.processarOndasPropria(this, inimigo, ondasPropria);
			} else {
				setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
			}

			if (estado == X1) {
				modulos.orbitar(this, ondas, inimigo);
				modulos.mirarEAtirar(inimigo, this);
			}
			execute();
		}
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		super.onScannedRobot(e); // lista de inimigos do modo normal

		if (e.getName().equals(maisPerto) || e.getDistance() < distanciaMaisPerto) {
			maisPerto = e.getName();
			distanciaMaisPerto = e.getDistance();
		}

		if (getOthers() == 1) {
			inimigo.atualizar(e, this);

			modulos.lancarOndaPropria(this, inimigo, ondasPropria);

			if (inimigo.provavelDisparo()) {
	            double potencia = inimigo.quedaEnergia();
	            ondas.add(new OndaInimiga(
	                    inimigo,
	                    inimigo.xAnterior, inimigo.yAnterior,
	                    Rules.getBulletSpeed(potencia),
	                    inimigo.anguloParaNos2,
	                    inimigo.dirLateral2,
	                    getTime() - 1));
				ondasCriadas++;
	        }

			Modulos.moveScanner(e, this);
		}

		estado = escolherEstado();
		if (estado != NORMAL) {
			stopNormalState();
		}

		// half-life e close-range so reagem ao inimigo mais perto
		if (!e.getName().equals(maisPerto)) return;

		if (estado == HALF_LIFE) {
			halfLife.agir(e, this);
		} else if (estado == CLOSE_RANGE) {
			closeRange.agir(e, this);
		}
	}

	private int escolherEstado() {
		if (HalfLife.ativo(this)) return HALF_LIFE;
		if (getOthers() == 1) return X1;
		if (distanciaMaisPerto < CloseRange.DISTANCIA_MAXIMA) return CLOSE_RANGE;
		return NORMAL;
	}

	public void onBulletHit(BulletHitEvent e) {
        inimigo.ajustarEnergia(-Rules.getBulletDamage(e.getBullet().getPower()));
    }

	public void onHitByBullet(HitByBulletEvent e) {
		inimigo.ajustarEnergia(Rules.getBulletHitBonus(e.getPower()));
        modulos.registrarTiro(e.getBullet(), ondas, this);

		if (estado == CLOSE_RANGE) closeRange.inverterSentido();
		if (estado == HALF_LIFE) halfLife.inverterSentido();
	}

	public void onHitRobot(HitRobotEvent e) {
        inimigo.ajustarEnergia(-0.6);
    }

    public void onBulletHitBullet(BulletHitBulletEvent e) {
        modulos.registrarTiro(e.getHitBullet(), ondas, this);
    }

	public void onHitWall(HitWallEvent e) {
		modulos.sentido = -modulos.sentido;
		closeRange.inverterSentido();
		halfLife.inverterSentido();
	}

	public void onRobotDeath(RobotDeathEvent e) {
		super.onRobotDeath(e); // tira da lista do modo normal

		if (e.getName().equals(maisPerto)) {
			maisPerto = null;
			distanciaMaisPerto = Double.MAX_VALUE;
		}
	}
}
