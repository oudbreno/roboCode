package PrimeiroRoboMata;
import robocode.*;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Modo normal (batalha com vários inimigos) - feito pelo Richard.
 *
 * Anti-gravidade: as paredes e os inimigos "empurram" o robô,
 * então ele sempre vai pro lugar mais vazio da arena.
 * Atira no inimigo mais próximo quando ele está no alcance.
 */
public class ModoNormal {

    private ArrayList<EnemyInfo> enemyList = new ArrayList<EnemyInfo>();

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

    //chamado todo tick quando o robo esta no modo normal
    public void agir(AdvancedRobot robo) {
        AntiGravityMovement(robo);
        CalcFire(robo);
    }

    //movimento do robo
    public void AntiGravityMovement(AdvancedRobot robo) {
        double xForce = 0;
        double yForce = 0;

        double robotX = robo.getX();
        double robotY = robo.getY();
        double maxX = robo.getBattleFieldWidth();
        double maxY = robo.getBattleFieldHeight();

        // distancia paredes
        double wallMargin = 60;
        if (robotX < wallMargin) xForce += 500 / (robotX + 1);
        if (robotX > maxX - wallMargin) xForce -= 500 / (maxX - robotX);
        if (robotY < wallMargin) yForce += 500 / (robotY + 1);
        if (robotY > maxY - wallMargin) yForce -= 500 / (maxY - robotY);

        // distancia inimigos
        for (EnemyInfo enemy : enemyList) {
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
        robo.setAhead(100 * Math.cos(goAngle - robo.getHeadingRadians()));
        robo.setTurnRightRadians(normalRelativeAngle(goAngle - robo.getHeadingRadians()));
    }

    //adciona na lista
    public void AddToList(ScannedRobotEvent e, AdvancedRobot robo) {
        String name = e.getName();
        double angle = Math.toRadians(robo.getHeading() + e.getBearing());
        double x = robo.getX() + e.getDistance() * Math.sin(angle);
        double y = robo.getY() + e.getDistance() * Math.cos(angle);

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

    //tira da lista quando o inimigo morre
    public void removerInimigo(String name) {
        Iterator<EnemyInfo> it = enemyList.iterator();
        while (it.hasNext()) {
            if (it.next().name.equals(name)) {
                it.remove();
                break;
            }
        }
    }

    //inimigo mais perto (ou null se ainda nao viu ninguem)
    public EnemyInfo maisProximo() {
        EnemyInfo target = null;
        for (EnemyInfo e : enemyList) {
            if (target == null || e.distance < target.distance) {
                target = e;
            }
        }
        return target;
    }

    //atira na direcao do inimigo mais proximo dentro do alcance
    public void CalcFire(AdvancedRobot robo) {
        EnemyInfo target = maisProximo();
        if (target == null) return;

        if (target.distance <= 180) {
            double angle = Math.toRadians(robo.getHeading() + target.bearing);
            double gunTurn = normalRelativeAngle(angle - robo.getGunHeadingRadians());
            robo.setTurnGunRightRadians(gunTurn);

            if (Math.abs(robo.getGunTurnRemaining()) < 10) {
                robo.setFire(2.0);
            }
        }
    }

    private double normalRelativeAngle(double angle) {
        return Math.atan2(Math.sin(angle), Math.cos(angle));
    }
}
