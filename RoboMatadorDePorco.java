package PrimeiroRoboMata;
import robocode.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.Color;

// API help : https://robocode.sourceforge.io/docs/robocode/robocode/Robot.html

/**
 * RoboMatadorDePorco - robô principal da equipe.
 *
 * Junta os 4 estados. A ordem de prioridade é:
 *   1) HalfLife   - nossa energia em 50 ou menos (Augusto)
 *   2) X1         - só sobrou 1 inimigo (Arthur)
 *   3) CloseRange - inimigo mais próximo a menos de 250px (Bernardo)
 *   4) Normal     - vários inimigos longe (Richard)
 */
public class RoboMatadorDePorco extends AdvancedRobot
{
	private static final int NORMAL = 0;
	private static final int X1 = 1;
	private static final int CLOSE_RANGE = 2;
	private static final int HALF_LIFE = 3;

	private int estado = NORMAL;

	// x1
	private static Inimigo inimigo = new Inimigo();
	private Modulos modulos = new Modulos();
	private List<OndaInimiga> ondas = new ArrayList<OndaInimiga>();
	private List<OndaPropria> ondasPropria = new ArrayList<OndaPropria>();

	// outros estados
	private CloseRange closeRange = new CloseRange();
	private HalfLife halfLife = new HalfLife();
	private ModoNormal modoNormal = new ModoNormal();

	public void run() {
		setColors(Color.red, Color.black, Color.white);

		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);
		setTurnRadarRightRadians(Double.POSITIVE_INFINITY);

		while(true){
			if (getOthers() == 1) {
				// 1x1: radar fica travado no inimigo (se perder, gira de novo)
				Modulos.controlaCamera(inimigo.tempoScan , this);
				modulos.processarOndasPropria(this, inimigo, ondasPropria);
			} else {
				// vários inimigos: radar gira sem parar pra ver todo mundo
				setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
			}

			// x1 e normal rodam todo tick; close-range e half-life rodam no onScannedRobot
			if (estado == X1) {
				modulos.orbitar(this, ondas, inimigo);
				modulos.mirarEAtirar(inimigo, this);
			} else if (estado == NORMAL) {
				modoNormal.agir(this);
			}

			execute();
		}
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		modoNormal.AddToList(e, this);

		if (getOthers() == 1) {
			atualizarX1(e);
		}

		estado = escolherEstado();

		// close-range e half-life só reagem ao inimigo mais perto
		if (!e.getName().equals(modoNormal.maisProximo().name)) return;

		if (estado == HALF_LIFE) {
			halfLife.agir(e, this);
		} else if (estado == CLOSE_RANGE) {
			closeRange.agir(e, this);
		}
	}

	private int escolherEstado() {
		if (HalfLife.ativo(this)) return HALF_LIFE;
		if (getOthers() == 1) return X1;

		ModoNormal.EnemyInfo perto = modoNormal.maisProximo();
		if (perto != null && perto.distance < CloseRange.DISTANCIA_MAXIMA) return CLOSE_RANGE;

		return NORMAL;
	}

	// tudo que o x1 precisa saber a cada scan (do Arthur)
	private void atualizarX1(ScannedRobotEvent e) {
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
		}

		Modulos.moveScanner(e, this);
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
		modoNormal.removerInimigo(e.getName());
	}
}
