package PrimeiroRoboMata;
import robocode.*;
import robocode.util.Utils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.awt.Color;
import java.awt.Graphics2D;
//import java.awt.Color;

// API help : https://robocode.sourceforge.io/docs/robocode/robocode/Robot.html

/**
 * RoboMatadorDePorco - a robot by (your name here)
 */
public class RoboMatadorDePorco extends AdvancedRobot
{
	private static Inimigo inimigo = new Inimigo();
	private Modulos modulos = new Modulos();
	private List<OndaInimiga> ondas = new ArrayList<OndaInimiga>();
	private int ondasCriadas = 0;

	private List<OndaPropria> ondasPropria = new ArrayList<OndaPropria>();

	public void run() {
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);
		setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
		while(true){
			Modulos.controlaCamera(inimigo.tempoScan , this);
			modulos.orbitar(this, ondas, inimigo);
			modulos.processarOndasPropria(this, inimigo, ondasPropria);
			modulos.mirarEAtirar(inimigo, this);
			execute();
		}
	}

	public void onScannedRobot(ScannedRobotEvent e) {
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

	public void onBulletHit(BulletHitEvent e) {         
        inimigo.ajustarEnergia(-Rules.getBulletDamage(e.getBullet().getPower()));
    }

	public void onHitByBullet(HitByBulletEvent e) {
		inimigo.ajustarEnergia(Rules.getBulletHitBonus(e.getPower()));
        modulos.registrarTiro(e.getBullet(), ondas, this);
	}

	public void onHitRobot(HitRobotEvent e) {            
        inimigo.ajustarEnergia(-0.6);
    }

    public void onBulletHitBullet(BulletHitBulletEvent e) {   
        modulos.registrarTiro(e.getHitBullet(), ondas, this);                     
    }
	
	public void onHitWall(HitWallEvent e) {
		modulos.sentido = -modulos.sentido;
	}	
}


