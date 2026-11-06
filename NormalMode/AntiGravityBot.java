package PrimeiroRoboMata.NormalMode;
import robocode.*;
import java.util.ArrayList;
import java.util.Iterator;

public class AntiGravityBot extends AdvancedRobot {

	private boolean keepRunning = true;
	
    private ArrayList<EnemyInfo> enemyList = new ArrayList<EnemyInfo>();

    //metodo do robo.
    public void NormalStateMode() {
        	keepRunning = true;

			while (keepRunning) {
            
            setTurnRadarRight(360);

            AntiGravityMovement(enemyList);

            CalcFire(enemyList);			 
            
            execute();
            }
    }

    //para o metodo do robo
    public void stopNormalState() {
    keepRunning = false;
	}

    //classe dos inimigos
    public class EnemyInfo {
        public String name;
        public double x, y, energy, distance, bearing;

        public EnemyInfo(String name, double x, double y, double energy, double distance, double bearing) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.energy = energy;
            this.distance = distance;
            this.bearing = bearing;
        }
    }

    //movimento do robo
    public void AntiGravityMovement(ArrayList<EnemyInfo> enemies) {
        double xForce = 0;
        double yForce = 0;

        double robotX = getX();
        double robotY = getY();
        double maxX = getBattleFieldWidth();
        double maxY = getBattleFieldHeight();

        // distancia paredes
        double wallMargin = 60;
        if (robotX < wallMargin) xForce += 500 / (robotX + 1);
        if (robotX > maxX - wallMargin) xForce -= 500 / (maxX - robotX);
        if (robotY < wallMargin) yForce += 500 / (robotY + 1);
        if (robotY > maxY - wallMargin) yForce -= 500 / (maxY - robotY);

        // distancia inimigos
        for (EnemyInfo enemy : enemies) {
            double dx = robotX - enemy.x;
            double dy = robotY - enemy.y;
            double distance = Math.hypot(dx, dy);

            if (distance > 0 && distance < 400) { 
                double force = 40000 / (distance * distance);
                xForce += force * (dx / distance);
                yForce += force * (dy / distance);
            }
        }

        // direcao robo
        double goAngle = Math.atan2(xForce, yForce);

        // Move o robo na direcao 
        setAhead(100 * Math.cos(goAngle - getHeadingRadians()));
        setTurnRightRadians(normalRelativeAngle(goAngle - getHeadingRadians()));
    }

    //adciona na lista
    public void AddToList(ScannedRobotEvent e) {
        String name = e.getName();
        double angle = Math.toRadians(getHeading() + e.getBearing());
        double x = getX() + e.getDistance() * Math.sin(angle);
        double y = getY() + e.getDistance() * Math.cos(angle);

        boolean found = false;
        for (EnemyInfo enemy : enemyList) {
            if (enemy.name.equals(name)) {
                enemy.x = x;
                enemy.y = y;
                enemy.energy = e.getEnergy();
                enemy.distance = e.getDistance();
                enemy.bearing = e.getBearing();
                found = true;
                break;
            }
        }

        if (!found) {
            enemyList.add(new EnemyInfo(name, x, y, e.getEnergy(), e.getDistance(), e.getBearing()));
        }
    }

    //atira na direcao do inimigo mais proximo dentro do alcance
    public void CalcFire(ArrayList<EnemyInfo> enemies) {
        if (enemies.isEmpty()) return;

        EnemyInfo target = enemies.get(0);
        for (EnemyInfo e : enemies) {
            if (e.distance < target.distance) {
                target = e;
            }
        }

        if (target.distance <= 180) {
            double angle = Math.toRadians(getHeading() + target.bearing);
            double gunTurn = normalRelativeAngle(angle - getGunHeadingRadians());
            setTurnGunRightRadians(gunTurn);

            if (Math.abs(getGunTurnRemaining()) < 10) {
                setFire(2.0); 
            }
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        AddToList(e);
    }


    @Override
    public void onRobotDeath(RobotDeathEvent e) {
        Iterator<EnemyInfo> it = enemyList.iterator();
        while (it.hasNext()) {
            EnemyInfo enemy = it.next();
            if (enemy.name.equals(e.getName())) {
                it.remove();
                break;
            }
        }
    }

    private double normalRelativeAngle(double angle) {
        return Math.atan2(Math.sin(angle), Math.cos(angle));
    }

    
}