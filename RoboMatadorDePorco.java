package PrimeiroRoboMata;
import robocode.*;
import robocode.util.Utils;
import java.awt.Color;

// API help : https://robocode.sourceforge.io/docs/robocode/robocode/Robot.html

/**
 * RoboMatadorDePorco
 *
 * Nesta branch (closeRange-state) só existe o estado de curta distância.
 * Quando o inimigo está longe o robô apenas se aproxima, pra dar pra testar o close-range.
 * Na hora de juntar com os outros estados, é só chamar closeRange.agir(...) quando
 * a distância for menor que CloseRange.DISTANCIA_MAXIMA.
 */
public class RoboMatadorDePorco extends AdvancedRobot
{
	private CloseRange closeRange = new CloseRange();
	private long ultimoScan = 0;
	private double inimigoX = -1, inimigoY = -1;

	public void run() {
		setColors(Color.red, Color.black, Color.white); // corpo, arma, radar

		// arma e radar giram independentes do corpo
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);

		setTurnRadarRightRadians(Double.POSITIVE_INFINITY);

		while(true) {
			// perdeu o inimigo de vista? volta a girar o radar
			if (getTime() - ultimoScan > 1) {
				setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
			}
			execute();
		}
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		ultimoScan = getTime();

		// guarda onde o inimigo está, só pro desenho de debug
		double angulo = getHeadingRadians() + e.getBearingRadians();
		inimigoX = getX() + Math.sin(angulo) * e.getDistance();
		inimigoY = getY() + Math.cos(angulo) * e.getDistance();
		travarRadar(e);

		if (e.getDistance() < CloseRange.DISTANCIA_MAXIMA) {
			closeRange.agir(e, this);
		} else {
			// provisório: só chega mais perto (aqui entram os outros estados)
			setTurnRight(e.getBearing());
			setAhead(e.getDistance() - 150);
		}
	}

	public void onHitByBullet(HitByBulletEvent e) {
		closeRange.inverterSentido();
	}

	public void onHitWall(HitWallEvent e) {
		closeRange.inverterSentido();
	}

	// desenho de debug: aparece ao clicar no robô e ligar "Paint" na janela da batalha
	public void onPaint(java.awt.Graphics2D g) {
		if (inimigoX < 0) return;

		// amarelo: até onde o close-range funciona
		int max = (int) CloseRange.DISTANCIA_MAXIMA;
		g.setColor(Color.yellow);
		g.drawOval((int) inimigoX - max, (int) inimigoY - max, max * 2, max * 2);

		// verde: distância que o robô tenta manter
		int ideal = (int) CloseRange.DISTANCIA_IDEAL;
		g.setColor(Color.green);
		g.drawOval((int) inimigoX - ideal, (int) inimigoY - ideal, ideal * 2, ideal * 2);

		// linha até o inimigo
		g.setColor(Color.red);
		g.drawLine((int) getX(), (int) getY(), (int) inimigoX, (int) inimigoY);
	}

	// mantém o radar sempre em cima do inimigo
	private void travarRadar(ScannedRobotEvent e) {
		double anguloInimigo = getHeadingRadians() + e.getBearingRadians();
		double diferenca = Utils.normalRelativeAngle(anguloInimigo - getRadarHeadingRadians());
		setTurnRadarRightRadians(2 * diferenca);
	}
}
