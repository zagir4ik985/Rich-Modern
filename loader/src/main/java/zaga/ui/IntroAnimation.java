package zaga.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class IntroAnimation extends JPanel implements ActionListener {

    private static final Color BODY = new Color(230, 230, 230);
    private static final Color LEGS = new Color(140, 140, 140);
    private static final Color EYES = Color.BLACK;
    private static final Color CAPE_COLOR = new Color(180, 0, 0);

    private enum State {
        IDLE, APPEAR, LOOK, FLY_TO_1, LASER_1,
        ASH_1, FLY_TO_2, LASER_2, ASH_2, FLY_AWAY, PAUSE
    }

    private State state = State.IDLE;
    private int stateTimer = 0;

    private double runner1X = 80;
    private double runner2X = 350;
    private boolean runner1Right = true;
    private boolean runner2Right = false;

    private double flyerX = 240;
    private double flyerY = -30;
    private double flyerTargetX = 240;
    private double flyerTargetY = 100;
    private boolean flyerFacingRight = true;
    private boolean laserActive = false;
    private double laserTargetX = 0;
    private double laserTargetY = 0;
    private float laserPower = 0f;

    private boolean runner1Gone = false;
    private boolean runner2Gone = false;

    private float ash1Alpha = 1f;
    private float ash1Y = 0;
    private float ash2Alpha = 1f;
    private float ash2Y = 0;
    private final List<AshParticle> ash1Particles = new ArrayList<>();
    private final List<AshParticle> ash2Particles = new ArrayList<>();

    private int groundY;
    private int tick = 0;
    private final Random rand = new Random();
    private final javax.swing.Timer timer;

    private static class AshParticle {
        double x, y, vx, vy;
        float alpha;
        int size;
        AshParticle(double x, double y, Random r) {
            this.x = x;
            this.y = y;
            this.vx = (r.nextDouble() - 0.5) * 3;
            this.vy = -r.nextDouble() * 3 - 1;
            this.alpha = 1f;
            this.size = r.nextInt(3) + 1;
        }
    }

    public IntroAnimation() {
        setOpaque(false);
        setDoubleBuffered(true);
        timer = new javax.swing.Timer(50, this);
    }

    public void startAnimation() {
        state = State.IDLE;
        stateTimer = 0;
        resetAll();
        timer.start();
    }

    private void resetAll() {
        runner1X = 80;
        runner2X = 350;
        runner1Right = true;
        runner2Right = false;
        flyerX = 240;
        flyerY = -30;
        flyerFacingRight = true;
        laserActive = false;
        laserPower = 0f;
        ash1Alpha = 1f;
        ash1Y = 0;
        ash2Alpha = 1f;
        ash2Y = 0;
        runner1Gone = false;
        runner2Gone = false;
        ash1Particles.clear();
        ash2Particles.clear();
        tick = 0;
    }

    @Override
    public boolean contains(int x, int y) {
        return false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (getParent() == null || getParent().getWidth() < 50) return;
        groundY = getParent().getHeight() - 15;
        tick++;

        switch (state) {
            case IDLE -> {
                stateTimer++;
                runner1X += runner1Right ? 1.2 : -1.2;
                if (runner1X < 20 || runner1X > 220) runner1Right = !runner1Right;
                runner2X += runner2Right ? 1.2 : -1.2;
                if (runner2X < 260 || runner2X > 450) runner2Right = !runner2Right;
                if (stateTimer > 60) {
                    state = State.APPEAR;
                    stateTimer = 0;
                    flyerTargetX = 240;
                    flyerTargetY = 80;
                }
            }
            case APPEAR -> {
                stateTimer++;
                flyerY += (flyerTargetY - flyerY) * 0.08;
                flyerX += (flyerTargetX - flyerX) * 0.08;
                runner1X += runner1Right ? 1.0 : -1.0;
                if (runner1X < 20 || runner1X > 220) runner1Right = !runner1Right;
                runner2X += runner2Right ? 1.0 : -1.0;
                if (runner2X < 260 || runner2X > 450) runner2Right = !runner2Right;
                if (stateTimer > 40) {
                    state = State.LOOK;
                    stateTimer = 0;
                }
            }
            case LOOK -> {
                stateTimer++;
                flyerY += Math.sin(stateTimer * 0.15) * 0.8;
                runner1X += runner1Right ? 1.0 : -1.0;
                if (runner1X < 20 || runner1X > 220) runner1Right = !runner1Right;
                runner2X += runner2Right ? 1.0 : -1.0;
                if (runner2X < 260 || runner2X > 450) runner2Right = !runner2Right;
                flyerFacingRight = (runner1X + 5) > flyerX;
                if (stateTimer > 35) {
                    state = State.FLY_TO_1;
                    stateTimer = 0;
                    flyerTargetX = runner1X;
                    flyerTargetY = groundY - 40;
                }
            }
            case FLY_TO_1 -> {
                stateTimer++;
                double dx = flyerTargetX + 10 - flyerX;
                double dy = flyerTargetY - flyerY;
                flyerX += dx * 0.08;
                flyerY += dy * 0.08;
                flyerFacingRight = dx > 0;
                if (stateTimer > 30) {
                    state = State.LASER_1;
                    stateTimer = 0;
                    flyerX = flyerTargetX + 10;
                    flyerY = flyerTargetY;
                    laserActive = true;
                    laserPower = 0f;
                }
            }
            case LASER_1 -> {
                stateTimer++;
                laserActive = true;
                laserTargetX = runner1X + 4;
                laserTargetY = groundY - 9;
                laserPower = Math.min(laserPower + 0.12f, 1f);
                if (stateTimer > 25) {
                    laserActive = false;
                    state = State.ASH_1;
                    stateTimer = 0;
                    runner1Gone = true;
                    ash1Alpha = 1f;
                    ash1Y = 0;
                    ash1Particles.clear();
                    for (int i = 0; i < 30; i++) {
                        ash1Particles.add(new AshParticle(runner1X + 4, groundY - 5, rand));
                    }
                }
            }
            case ASH_1 -> {
                stateTimer++;
                ash1Alpha = Math.max(0, ash1Alpha - 0.025f);
                ash1Y -= 0.5f;
                for (AshParticle p : ash1Particles) {
                    p.x += p.vx;
                    p.y += p.vy;
                    p.vy -= 0.02;
                    p.alpha = Math.max(0, p.alpha - 0.015f);
                }
                if (stateTimer > 50) {
                    state = State.FLY_TO_2;
                    stateTimer = 0;
                    flyerTargetX = runner2X;
                    flyerTargetY = groundY - 40;
                }
            }
            case FLY_TO_2 -> {
                stateTimer++;
                double dx = flyerTargetX + 10 - flyerX;
                double dy = flyerTargetY - flyerY;
                flyerX += dx * 0.08;
                flyerY += dy * 0.08;
                flyerFacingRight = dx > 0;
                if (stateTimer > 30) {
                    state = State.LASER_2;
                    stateTimer = 0;
                    flyerX = flyerTargetX + 10;
                    flyerY = flyerTargetY;
                    laserActive = true;
                    laserPower = 0f;
                }
            }
            case LASER_2 -> {
                stateTimer++;
                laserActive = true;
                laserTargetX = runner2X + 4;
                laserTargetY = groundY - 9;
                laserPower = Math.min(laserPower + 0.12f, 1f);
                if (stateTimer > 25) {
                    laserActive = false;
                    state = State.ASH_2;
                    stateTimer = 0;
                    runner2Gone = true;
                    ash2Alpha = 1f;
                    ash2Y = 0;
                    ash2Particles.clear();
                    for (int i = 0; i < 30; i++) {
                        ash2Particles.add(new AshParticle(runner2X + 4, groundY - 5, rand));
                    }
                }
            }
            case ASH_2 -> {
                stateTimer++;
                ash2Alpha = Math.max(0, ash2Alpha - 0.025f);
                ash2Y -= 0.5f;
                for (AshParticle p : ash2Particles) {
                    p.x += p.vx;
                    p.y += p.vy;
                    p.vy -= 0.02;
                    p.alpha = Math.max(0, p.alpha - 0.015f);
                }
                if (stateTimer > 50) {
                    state = State.FLY_AWAY;
                    stateTimer = 0;
                }
            }
            case FLY_AWAY -> {
                stateTimer++;
                flyerY -= 3;
                flyerX += (rand.nextBoolean() ? 1 : -1) * 0.5;
                if (flyerY < -50) {
                    state = State.PAUSE;
                    stateTimer = 0;
                }
            }
            case PAUSE -> {
                stateTimer++;
                if (stateTimer > 20) {
                    state = State.IDLE;
                    stateTimer = 0;
                    resetAll();
                }
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getParent() == null || getParent().getWidth() < 50) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (!runner1Gone) drawRunner(g2, runner1X, groundY, runner1Right, "Пионер");
        if (!runner2Gone) drawRunner(g2, runner2X, groundY, runner2Right, "Домер");

        drawAshParticles(g2, ash1Particles, ash1Alpha);
        drawAshParticles(g2, ash2Particles, ash2Alpha);

        if (flyerY > -40) {
            drawFlyer(g2, flyerX, flyerY, flyerFacingRight, laserActive, laserTargetX, laserTargetY);
            drawNickname(g2, "Зага", flyerX + 1, flyerY - 18, new Color(255, 60, 60));
        }
    }

    private void drawRunner(Graphics2D g2, double bx, int by, boolean right, String name) {
        double s = 1.0;
        int legFrame = tick % 4;

        g2.setColor(BODY);
        g2.fillRect((int)(bx + 3 * s), (int)(by - 11 * s), (int)(3 * s), (int)(3 * s));
        g2.fillRect((int)(bx + 3 * s), (int)(by - 8 * s), (int)(3 * s), (int)(4 * s));

        int swing = legFrame < 2 ? 0 : 1;
        g2.fillRect((int)(bx + 6 * s), (int)(by + (-8 + swing) * s), (int)s, (int)(3 * s));
        g2.fillRect((int)(bx + 0 * s), (int)(by + (-8 - swing) * s), (int)s, (int)(3 * s));

        g2.setColor(LEGS);
        if (legFrame == 0) {
            g2.fillRect((int)(bx + 3 * s), (int)(by - 4 * s), (int)s, (int)(4 * s));
            g2.fillRect((int)(bx + 5 * s), (int)(by - 4 * s), (int)s, (int)(4 * s));
        } else if (legFrame == 1) {
            g2.fillRect((int)(bx + 2 * s), (int)(by - 4 * s), (int)s, (int)(4 * s));
            g2.fillRect((int)(bx + 6 * s), (int)(by - 5 * s), (int)s, (int)(5 * s));
        } else if (legFrame == 2) {
            g2.fillRect((int)(bx + 3 * s), (int)(by - 4 * s), (int)s, (int)(4 * s));
            g2.fillRect((int)(bx + 5 * s), (int)(by - 4 * s), (int)s, (int)(4 * s));
        } else {
            g2.fillRect((int)(bx + 2 * s), (int)(by - 5 * s), (int)s, (int)(5 * s));
            g2.fillRect((int)(bx + 6 * s), (int)(by - 4 * s), (int)s, (int)(4 * s));
        }

        g2.setColor(EYES);
        int eyeX = right ? (int)(bx + 5 * s) : (int)(bx + 3 * s);
        g2.fillRect(eyeX, (int)(by - 10 * s), (int)s, (int)s);

        drawNickname(g2, name, bx + 4, by - 16, new Color(255, 255, 255));
    }

    private void drawFlyer(Graphics2D g2, double fx, double fy, boolean right, boolean laser, double targetX, double targetY) {
        int bx = (int) fx;
        int by = (int) fy;

        g2.setColor(CAPE_COLOR);
        int capeWing = (int)(Math.sin(tick * 0.3) * 3);
        if (right) {
            int[] cx = {bx - 2, bx - 8 - capeWing, bx - 5 - capeWing, bx - 1};
            int[] cy = {by - 8, by - 2, by - 6, by - 10};
            g2.fillPolygon(cx, cy, 4);
        } else {
            int[] cx = {bx + 4, bx + 10 + capeWing, bx + 7 + capeWing, bx + 3};
            int[] cy = {by - 8, by - 2, by - 6, by - 10};
            g2.fillPolygon(cx, cy, 4);
        }

        g2.setColor(BODY);
        g2.fillRect(bx, by - 11, 3, 3);
        g2.fillRect(bx, by - 8, 3, 4);

        g2.setColor(LEGS);
        int wingOffset = (int)(Math.sin(tick * 0.4) * 2);
        g2.fillRect(bx, by - 4, 1, 4);
        g2.fillRect(bx + 2, by - 4, 1, 4);

        g2.setColor(new Color(255, 80, 80));
        int wingX = right ? bx + 3 : bx - 1;
        int wingY = by - 8 + wingOffset;
        g2.fillRect(wingX, wingY, 4, 1);

        g2.setColor(laser ? new Color(255, 0, 0) : EYES);
        int eyeX = right ? bx + 2 : bx;
        g2.fillRect(eyeX, by - 10, 1, 1);

        if (laser) {
            drawLaser(g2, eyeX, by - 10, (int) targetX, (int) targetY, laserPower);
        }
    }

    private void drawLaser(Graphics2D g2, int x1, int y1, int x2, int y2, float power) {
        if (power <= 0) return;

        float dx = x2 - x1;
        float dy = y2 - y1;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < 1) return;

        float segments = Math.max(10, dist / 3);

        for (int layer = 0; layer < 4; layer++) {
            float width = (4 - layer) * 1.2f * power;
            float alpha = (0.15f + layer * 0.15f) * power;
            Color c;
            if (layer == 0) {
                c = new Color(255, 50, 50, (int)(alpha * 255));
            } else if (layer == 1) {
                c = new Color(255, 100, 30, (int)(alpha * 255));
            } else if (layer == 2) {
                c = new Color(255, 200, 80, (int)(alpha * 255));
            } else {
                c = new Color(255, 255, 200, (int)(alpha * 255));
            }
            g2.setColor(c);
            g2.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            float px = x1;
            float py = y1;
            for (int i = 1; i <= segments; i++) {
                float t = i / segments;
                float nx = x1 + dx * t;
                float ny = y1 + dy * t;
                float wave = (float) Math.sin(tick * 0.5 + t * 6) * (1.5f - layer * 0.3f) * power;
                float perpX = -dy / dist;
                float perpY = dx / dist;
                nx += perpX * wave;
                ny += perpY * wave;
                g2.drawLine((int) px, (int) py, (int) nx, (int) ny);
                px = nx;
                py = ny;
            }
        }

        Ellipse2D.Double impact = new Ellipse2D.Double(x2 - 4 * power, y2 - 4 * power, 8 * power, 8 * power);
        g2.setColor(new Color(255, 200, 100, (int)(180 * power)));
        g2.fill(impact);
        g2.setColor(new Color(255, 255, 200, (int)(120 * power)));
        g2.fill(new Ellipse2D.Double(x2 - 2 * power, y2 - 2 * power, 4 * power, 4 * power));

        g2.setStroke(new BasicStroke(1));
    }

    private void drawAshParticles(Graphics2D g2, List<AshParticle> particles, float alpha) {
        if (alpha <= 0 && particles.isEmpty()) return;
        for (AshParticle p : particles) {
            if (p.alpha <= 0) continue;
            float a = p.alpha * alpha;
            g2.setColor(new Color(80, 80, 80, (int)(a * 255)));
            g2.fillRect((int) p.x, (int) p.y, p.size, p.size);
        }
    }

    private void drawNickname(Graphics2D g2, String name, double x, double y, Color color) {
        Font font = new Font("Consolas", Font.BOLD, 7);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(name);
        int tx = (int) x - textW / 2;

        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRoundRect(tx - 2, (int) y - 1, textW + 4, fm.getHeight() + 1, 4, 4);
        g2.setColor(color);
        g2.drawString(name, tx, (int) y + fm.getAscent());
    }
}
