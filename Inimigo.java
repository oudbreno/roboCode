package PrimeiroRoboMata;

import robocode.*;
import robocode.util.Utils;

public class Inimigo {
    public static final int DIVISOES = 47;

    public static String nome;
    public double distancia;
	public double ultimoScan;
	public static double x, y;
	public double energia;
	public double direcao;
	public double velocidade;
	public double deltaDirecao;
	public double velocidadeLateral;
	public int ticksParado;
	public long tempoScan;

    public double anguloParaNos, anguloParaNos1, anguloParaNos2;
    public int dirLateral = 1, dirLateral1 = 1, dirLateral2 = 1;

    public double[] perigo = new double[DIVISOES];

    private double ajusteEnergia;

    public double energiaAnterior;
    public double direcaoAnterior;
    public double velocidadeAnterior;
    public double xAnterior, yAnterior;
    public long tempoScanAnterior;

    public  boolean temHistorico = false;

    public void atualizar(ScannedRobotEvent e, AdvancedRobot meuRobo){
        energiaAnterior = energia;
        direcaoAnterior = direcao;
        velocidadeAnterior = velocidade;
        xAnterior = x;
        yAnterior = y;
        tempoScanAnterior = tempoScan;

        anguloParaNos2 = anguloParaNos1;
        anguloParaNos1 = anguloParaNos;
        dirLateral2 = dirLateral1;
        dirLateral1 = dirLateral;

        nome = e.getName();
        distancia = e.getDistance();
        energia = e.getEnergy();
        direcao = e.getHeadingRadians();
        velocidade = e.getVelocity();
        tempoScan = meuRobo.getTime();

        double meuX = meuRobo.getX();
        double meuY = meuRobo.getY();
        double angulo = meuRobo.getHeadingRadians() + e.getBearingRadians();

        x = meuX + Math.sin(angulo) * distancia;
        y = meuY + Math.cos(angulo) * distancia;

        anguloParaNos = Utils.normalAbsoluteAngle(angulo + Math.PI); 

        double latNossa = meuRobo.getVelocity() * Math.sin(meuRobo.getHeadingRadians() - anguloParaNos);
        if (Math.abs(latNossa) > 0.01) dirLateral = latNossa > 0 ? 1 : -1;

        velocidadeLateral = velocidade * Math.sin(direcao - angulo);

        if (temHistorico) {
            long dt = tempoScan - tempoScanAnterior;
            if (dt <= 0) dt = 1;
            deltaDirecao = Utils.normalRelativeAngle(direcao - direcaoAnterior) / dt;

            double w = meuRobo.getBattleFieldWidth(), h = meuRobo.getBattleFieldHeight();
            boolean naParede = x < 19 || y < 19 || x > w - 19 || y > h - 19;
            if (naParede && Math.abs(velocidade) < 0.01 && Math.abs(velocidadeAnterior) > 2) {
                energiaAnterior -= Math.max(Math.abs(velocidadeAnterior) / 2 - 1, 0);
            }
        } else {
            deltaDirecao = 0;
            energiaAnterior = energia;   
            direcaoAnterior = direcao;
            velocidadeAnterior = velocidade;
            xAnterior = x;
            yAnterior = y;
            anguloParaNos1 = anguloParaNos2 = anguloParaNos;
            dirLateral1 = dirLateral2 = dirLateral;
        }
        if (Math.abs(velocidade) < 0.01) {
            ticksParado++;
        } else {
            ticksParado = 0;
        }

        temHistorico = true;
    }

    public void ajustarEnergia(double delta) {
        ajusteEnergia += delta;
    }

    public double quedaEnergia() {
        return energiaAnterior - energia;
    }

    public boolean provavelDisparo() {
        double queda = quedaEnergia();
        return queda >= 0.1 && queda <= 3.0;
    }

    public void registrarPerigo(int divisao) {
        for (int i = 0; i < DIVISOES; i++) {
            double d = (i - divisao) / 2.0;
            perigo[i] += Math.exp(-0.5 * d * d);
        }
    }

    public void imprimir() {
    System.out.println("=== " + nome + " (tick " + tempoScan + ") ===");
    System.out.printf("Posição: x=%.1f y=%.1f%n", x, y);
    System.out.printf("Distância: %.1f%n", distancia);
    System.out.printf("Energia: %.1f (anterior: %.1f)%n", energia, energiaAnterior);
    System.out.printf("Direção: %.3f rad (anterior: %.3f)%n", direcao, direcaoAnterior);
    System.out.printf("Velocidade: %.1f (anterior: %.1f)%n", velocidade, velocidadeAnterior);
    System.out.printf("Delta direção: %.4f rad/tick%n", deltaDirecao);
    System.out.printf("Velocidade lateral: %.2f%n", velocidadeLateral);
    System.out.println("Ticks parado: " + ticksParado);
    System.out.println("Provável disparo: " + provavelDisparo());
    }
}