package com.ballgame.pro;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * BallGame Pro V2 - Java 21 / Swing, no external dependencies.
 * Original mechanic retained: moving paddle, wall/paddle collisions, 5-minute
 * arcade timer, and a new ball every 10 seconds in Arcade mode.
 */
public final class BallGame extends JPanel implements ActionListener {
    private static final int W = 1000, H = 700;
    private static final int LEFT = 35, RIGHT = 965, TOP = 148, BOTTOM = 668;
    private static final int PADDLE_Y = 620, PADDLE_HEIGHT = 14, BASE_PADDLE_WIDTH = 110;
    private static final int BALL_RADIUS = 10;
    private static final double BASE_SPEED = 360; // original 6 px / 16ms, approximately
    private static final double ARCADE_SECONDS = 300;
    private static final double NEW_BALL_SECONDS = 10;
    private static final int MAX_BALLS = 10;

    private static final Color BG = new Color(7, 12, 29);
    private static final Color PANEL = new Color(15, 24, 48);
    private static final Color INK = new Color(239, 246, 255);
    private static final Color MUTED = new Color(147, 164, 197);
    private static final Color CYAN = new Color(67, 232, 246);
    private static final Color PURPLE = new Color(168, 113, 255);
    private static final Color PINK = new Color(255, 102, 191);
    private static final Color GOLD = new Color(255, 202, 95);
    private static final Color GREEN = new Color(125, 255, 174);

    enum Screen { MENU, PLAY, PAUSE, OVER, SCORES, HELP }
    enum Mode { ARCADE, SURVIVAL }
    enum Difficulty {
        EASY("FACILE", 0.82), NORMAL("NORMAL", 1.00), HARD("DIFFICILE", 1.18), EXTREME("EXTRÊME", 1.36);
        final String label;
        final double multiplier;
        Difficulty(String label, double multiplier) { this.label = label; this.multiplier = multiplier; }
    }
    enum PowerKind {
        WIDE("LARGE", new Color(71, 240, 232)),
        SLOW("SLOW", new Color(153, 159, 255)),
        MULTI("MULTI", new Color(252, 122, 205)),
        DOUBLE("X2", new Color(255, 203, 101)),
        HEAL("+1", new Color(120, 252, 152));
        final String label;
        final Color color;
        PowerKind(String label, Color color) { this.label = label; this.color = color; }
    }
    static final class Ball {
        double x, y, vx, vy;
        final ArrayDeque<Point2D.Double> trail = new ArrayDeque<>();
        Ball(double x, double y, double vx, double vy) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        }
    }
    static final class Particle {
        double x, y, vx, vy, age, lifetime, radius;
        Color color;
        Particle(double x, double y, double vx, double vy, double life, double size, Color c) {
            this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.lifetime=life;this.radius=size;this.color=c;
        }
    }
    static final class Drop {
        double x, y, speed;
        PowerKind kind;
        Drop(double x, double y, PowerKind kind) { this.x=x;this.y=y;this.kind=kind;this.speed=116; }
    }
    static final class Obstacle {
        double x, y, width, phase;
        Obstacle(double y, double width, double phase) { this.y=y;this.width=width;this.phase=phase; }
        double height() { return 14; }
    }
    static final class FloatingText {
        final String text;
        final Color color;
        double x, y, age;
        FloatingText(String text, double x, double y, Color color) {
            this.text=text;this.x=x;this.y=y;this.color=color;
        }
    }
    record Button(int x, int y, int w, int h, Runnable action) {
        boolean contains(int mx, int my) { return mx>=x && mx<=x+w && my>=y && my<=y+h; }
    }

    private final List<Ball> balls = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<Drop> drops = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private final List<Button> buttons = new ArrayList<>();
    private final List<Point> stars = new ArrayList<>();
    private final Random random = new Random();
    private final ScoreStore scores = new ScoreStore();
    private final AudioEngine audio = new AudioEngine();
    private final Timer timer = new Timer(16, this);

    private Screen screen = Screen.MENU;
    private Mode mode = Mode.ARCADE;
    private Difficulty difficulty = Difficulty.NORMAL;
    private boolean leftPressed, rightPressed;
    private boolean newRecord;
    private String gameOverReason = "";
    private int pointerX = -10, pointerY = -10;
    private double paddleX = W / 2.0;
    private double elapsed, spawningClock, globalAnimation;
    private double wideUntil, slowUntil, doubleUntil;
    private long lastNano;
    private int lives, score, combo, bestCombo, rebounds, missed, totalGenerated, level;
    private int collected, powerCounter;
    private boolean fullScreen;
    private JFrame frame;

    public BallGame() {
        setPreferredSize(new Dimension(W, H));
        setBackground(BG);
        setFocusable(true);
        for (int i = 0; i < 125; i++) stars.add(new Point(random.nextInt(W), random.nextInt(H)));
        installInput();
        lastNano = System.nanoTime();
        timer.start();
        audio.startMusic();
    }

    private void installInput() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) { handleMove(e); }
            @Override public void mouseDragged(MouseEvent e) { handleMove(e); }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                requestFocusInWindow();
                Point p = logicalPoint(e.getPoint());
                for (int i = buttons.size() - 1; i >= 0; i--) {
                    Button b = buttons.get(i);
                    if (b.contains(p.x, p.y)) {
                        audio.play("ui");
                        b.action().run();
                        repaint();
                        return;
                    }
                }
                if (screen == Screen.OVER && p.y > 130 && p.y < 668) startGame();
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT, KeyEvent.VK_A -> leftPressed = true;
                    case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> rightPressed = true;
                    case KeyEvent.VK_P, KeyEvent.VK_SPACE, KeyEvent.VK_ESCAPE -> togglePause();
                    case KeyEvent.VK_ENTER -> {
                        if (screen == Screen.MENU || screen == Screen.OVER) startGame();
                        else if (screen == Screen.PAUSE) resume();
                    }
                    case KeyEvent.VK_R -> { if (screen == Screen.OVER) startGame(); }
                    case KeyEvent.VK_M -> audio.setMusicEnabled(!audio.isMusicEnabled());
                    case KeyEvent.VK_S -> audio.setEffectsEnabled(!audio.isEffectsEnabled());
                    case KeyEvent.VK_F11 -> toggleFullscreen();
                    case KeyEvent.VK_1 -> { if (screen == Screen.MENU) mode = Mode.ARCADE; }
                    case KeyEvent.VK_2 -> { if (screen == Screen.MENU) mode = Mode.SURVIVAL; }
                }
                repaint();
            }
            @Override public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A) leftPressed=false;
                if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D) rightPressed=false;
            }
        });
    }

    private void handleMove(MouseEvent e) {
        Point p = logicalPoint(e.getPoint());
        pointerX = p.x; pointerY = p.y;
        if (screen == Screen.PLAY && p.y >= TOP && p.y <= BOTTOM + 20)
            paddleX = clamp(p.x, LEFT + paddleWidth()/2.0, RIGHT - paddleWidth()/2.0);
        boolean hovering = buttons.stream().anyMatch(b -> b.contains(pointerX, pointerY));
        setCursor(Cursor.getPredefinedCursor(hovering ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }
    private Point logicalPoint(Point p) {
        return new Point((int) (p.x * W / Math.max(1.0, getWidth())),
                (int) (p.y * H / Math.max(1.0, getHeight())));
    }

    void setFrame(JFrame frame) { this.frame = frame; }

    private void toggleFullscreen() {
        if (frame == null) return;
        fullScreen = !fullScreen;
        frame.dispose();
        frame.setUndecorated(fullScreen);
        frame.setExtendedState(fullScreen ? JFrame.MAXIMIZED_BOTH : JFrame.NORMAL);
        if (!fullScreen) frame.setSize(W + 16, H + 39);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        requestFocusInWindow();
    }

    private void startGame() {
        balls.clear(); particles.clear(); drops.clear(); obstacles.clear(); floatingTexts.clear();
        elapsed=0;spawningClock=0;wideUntil=0;slowUntil=0;doubleUntil=0;
        lives=3;score=0;combo=0;bestCombo=0;rebounds=0;missed=0;totalGenerated=0;
        collected=0;powerCounter=0;level=1;
        newRecord=false;
        paddleX=W/2.0;
        leftPressed=rightPressed=false;
        screen=Screen.PLAY;
        spawnBall();
        lastNano=System.nanoTime();
        audio.play("start");
        requestFocusInWindow();
    }

    private void togglePause() {
        if (screen == Screen.PLAY) {
            screen = Screen.PAUSE;
            leftPressed=rightPressed=false;
        } else if (screen == Screen.PAUSE) resume();
    }
    private void resume() { screen=Screen.PLAY;lastNano=System.nanoTime();requestFocusInWindow(); }
    private void returnMenu() { screen=Screen.MENU;leftPressed=rightPressed=false;requestFocusInWindow(); }
    private void finish(String reason) {
        if (screen != Screen.PLAY) return;
        screen=Screen.OVER;
        floatingTexts.clear();
        particles.clear();
        gameOverReason=reason;
        newRecord=scores.submit(mode.name(), difficulty.name(), score, (int)elapsed, bestCombo, level);
        audio.play("gameover");
    }

    private double paddleWidth() { return BASE_PADDLE_WIDTH * (elapsed < wideUntil ? 1.65 : 1.0); }
    private double speed() {
        return BASE_SPEED * difficulty.multiplier * (1 + (level-1)*0.065)
                * (elapsed < slowUntil ? 0.68 : 1.0);
    }
    private double spawnInterval() {
        return mode == Mode.ARCADE ? NEW_BALL_SECONDS : Math.max(3.1, 10.0 - level*.62);
    }
    private void spawnBall() {
        if (balls.size() >= MAX_BALLS) return;
        double a=Math.toRadians(-60+random.nextDouble()*120);
        double x=LEFT+BALL_RADIUS+random.nextDouble()*(RIGHT-LEFT-2*BALL_RADIUS);
        double y=TOP+205;
        double v=speed();
        balls.add(new Ball(x,y,Math.sin(a)*v,-Math.cos(a)*v));
        totalGenerated++;
        emit(x,y,PINK,12);
    }

    private void spawnMultiball() {
        if (balls.isEmpty()) { spawnBall(); return; }
        Ball source=balls.get(0);
        double baseAngle=Math.atan2(source.vy,source.vx);
        for (int direction : new int[]{-1,1}) {
            if (balls.size()>=MAX_BALLS) break;
            double a=baseAngle+direction*.44;
            balls.add(new Ball(source.x+direction*13,source.y,Math.cos(a)*speed(),Math.sin(a)*speed()));
            totalGenerated++;
        }
        emit(source.x,source.y,PINK,30);
    }

    @Override public void actionPerformed(ActionEvent event) {
        long nano=System.nanoTime();
        double dt=Math.min(0.035, Math.max(0, (nano-lastNano)/1_000_000_000.0));
        lastNano=nano;
        globalAnimation+=dt;
        if (screen==Screen.PLAY) updateGame(dt);
        repaint();
    }

    private void updateGame(double dt) {
        elapsed+=dt;
        int targetLevel=1+(int)(elapsed/(mode==Mode.ARCADE ? 35.0 : 28.0));
        if (targetLevel > level) {
            level=targetLevel;
            floatingTexts.add(new FloatingText("NIVEAU " + level, 500, 235, GOLD));
            emit(500,265,GOLD,35);
            audio.play("power");
        }
        if (mode == Mode.ARCADE && elapsed >= ARCADE_SECONDS) { finish("TEMPS ÉCOULÉ — DÉFI TERMINÉ");return; }

        double paddleMove=660*dt;
        if (leftPressed) paddleX-=paddleMove;
        if (rightPressed) paddleX+=paddleMove;
        paddleX=clamp(paddleX,LEFT+paddleWidth()/2,RIGHT-paddleWidth()/2);

        spawningClock+=dt;
        if (spawningClock>=spawnInterval()) {
            spawningClock-=spawnInterval();
            spawnBall();
        }
        updateObstacles();
        double targetSpeed=speed();
        for (Iterator<Ball> it=balls.iterator();it.hasNext();) {
            Ball ball=it.next();
            ball.trail.addLast(new Point2D.Double(ball.x,ball.y));
            if (ball.trail.size()>8) ball.trail.removeFirst();
            double oldX=ball.x, oldY=ball.y;
            double oldSpeed=Math.hypot(ball.vx,ball.vy);
            if (oldSpeed>0.01) { ball.vx*=targetSpeed/oldSpeed;ball.vy*=targetSpeed/oldSpeed; }
            ball.x+=ball.vx*dt;
            ball.y+=ball.vy*dt;
            if (ball.x < LEFT+BALL_RADIUS) { ball.x=LEFT+BALL_RADIUS;ball.vx=Math.abs(ball.vx);emit(ball.x,ball.y,CYAN,5);audio.play("bounce"); }
            if (ball.x > RIGHT-BALL_RADIUS) { ball.x=RIGHT-BALL_RADIUS;ball.vx=-Math.abs(ball.vx);emit(ball.x,ball.y,CYAN,5);audio.play("bounce"); }
            if (ball.y < TOP+BALL_RADIUS) { ball.y=TOP+BALL_RADIUS;ball.vy=Math.abs(ball.vy);emit(ball.x,ball.y,CYAN,5);audio.play("bounce"); }

            for (Obstacle obstacle:obstacles) {
                if (ball.x+BALL_RADIUS<obstacle.x || ball.x-BALL_RADIUS>obstacle.x+obstacle.width ||
                        ball.y+BALL_RADIUS<obstacle.y || ball.y-BALL_RADIUS>obstacle.y+obstacle.height()) continue;
                boolean fromAbove = oldY + BALL_RADIUS <= obstacle.y;
                boolean fromBelow = oldY - BALL_RADIUS >= obstacle.y+obstacle.height();
                if (fromAbove) { ball.y=obstacle.y-BALL_RADIUS;ball.vy=-Math.abs(ball.vy); }
                else if (fromBelow) { ball.y=obstacle.y+obstacle.height()+BALL_RADIUS;ball.vy=Math.abs(ball.vy); }
                else if (oldX<obstacle.x) {ball.x=obstacle.x-BALL_RADIUS;ball.vx=-Math.abs(ball.vx);}
                else {ball.x=obstacle.x+obstacle.width+BALL_RADIUS;ball.vx=Math.abs(ball.vx);}
                emit(ball.x,ball.y,PURPLE,8);
                audio.play("bounce");
            }

            double pw=paddleWidth();
            if (ball.vy>0 && oldY+BALL_RADIUS<=PADDLE_Y+BALL_RADIUS &&
                    ball.y+BALL_RADIUS>=PADDLE_Y && ball.y-BALL_RADIUS<=PADDLE_Y+PADDLE_HEIGHT &&
                    ball.x>=paddleX-pw/2-BALL_RADIUS && ball.x<=paddleX+pw/2+BALL_RADIUS) {
                double impact=clamp((ball.x-paddleX)/(pw/2),-1,1);
                double angle=impact*Math.toRadians(63);
                ball.vx=Math.sin(angle)*targetSpeed;
                ball.vy=-Math.cos(angle)*targetSpeed;
                ball.y=PADDLE_Y-BALL_RADIUS;
                rebounds++;combo++;bestCombo=Math.max(bestCombo,combo);
                int points=10*(1+Math.min(4,combo/5))*(elapsed<doubleUntil?2:1);
                score+=points;
                emit(ball.x,PADDLE_Y,CYAN,12);
                floatingTexts.add(new FloatingText("+"+points,ball.x,PADDLE_Y-20,GREEN));
                audio.play("bounce");
                if (++powerCounter>=7) {
                    powerCounter=0;
                    PowerKind kind=PowerKind.values()[random.nextInt(PowerKind.values().length)];
                    drops.add(new Drop(clamp(ball.x+random.nextInt(151)-75,LEFT+30,RIGHT-30),PADDLE_Y-150,kind));
                }
            }
            if (ball.y-BALL_RADIUS>BOTTOM) {
                it.remove();missed++;emit(ball.x,BOTTOM,PINK,14);
            }
        }
        if (balls.isEmpty()) {
            lives--;
            combo=0;
            audio.play("hurt");
            floatingTexts.add(new FloatingText("-1 VIE",500,370,PINK));
            if (lives<=0) { finish("PLUS DE VIES");return; }
            spawnBall();
        }
        for (Iterator<Drop> it=drops.iterator();it.hasNext();) {
            Drop d=it.next();
            d.y+=d.speed*dt;
            if (d.y+14>=PADDLE_Y && d.y-12<=PADDLE_Y+PADDLE_HEIGHT &&
                    Math.abs(d.x-paddleX)<=paddleWidth()/2+30) {
                collect(d.kind,d.x,d.y);
                it.remove();
            } else if (d.y>BOTTOM+15) it.remove();
        }
        updateParticles(dt);
        floatingTexts.removeIf(f -> (f.age+=dt)>1.25);
        for (FloatingText f:floatingTexts) f.y-=38*dt;
    }

    private void updateObstacles() {
        int count=level>=7?3:level>=4?2:level>=2?1:0;
        while (obstacles.size()<count) obstacles.add(new Obstacle(
                275+obstacles.size()*116, 115+obstacles.size()*15, 0.8+obstacles.size()*2.4));
        for (Obstacle o:obstacles) o.x=LEFT+72 + (RIGHT-LEFT-o.width-144) *
                (0.5+0.5*Math.sin(elapsed*(.8+level*.035)+o.phase));
    }

    private void collect(PowerKind kind,double x,double y) {
        collected++;
        switch (kind) {
            case WIDE -> wideUntil=Math.max(elapsed,wideUntil)+12;
            case SLOW -> slowUntil=Math.max(elapsed,slowUntil)+9;
            case MULTI -> spawnMultiball();
            case DOUBLE -> doubleUntil=Math.max(elapsed,doubleUntil)+12;
            case HEAL -> lives=Math.min(3,lives+1);
        }
        floatingTexts.add(new FloatingText(kind.label+" ACTIVÉ !",x,y-30,kind.color));
        emit(x,y,kind.color,30);
        audio.play("power");
    }

    private void updateParticles(double dt) {
        for (Iterator<Particle> it=particles.iterator();it.hasNext();) {
            Particle p=it.next();
            p.age+=dt;
            p.x+=p.vx*dt;p.y+=p.vy*dt;p.vy+=35*dt;
            if (p.age>=p.lifetime) it.remove();
        }
    }
    private void emit(double x,double y,Color color,int count) {
        while (particles.size()>550) particles.remove(0);
        for (int i=0;i<count;i++) {
            double a=random.nextDouble()*Math.PI*2,v=35+random.nextDouble()*155;
            particles.add(new Particle(x,y,Math.cos(a)*v,Math.sin(a)*v,
                    .25+random.nextDouble()*.6,1.5+random.nextDouble()*3.6,color));
        }
    }
    private static double clamp(double v,double lo,double hi) { return Math.max(lo,Math.min(hi,v)); }
    private int remaining() { return Math.max(0, (int)Math.ceil(ARCADE_SECONDS-elapsed)); }
    private static String clock(int sec) { return String.format("%02d:%02d",sec/60,sec%60); }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g=(Graphics2D)graphics.create();
        g.scale(getWidth()/(double)W,getHeight()/(double)H);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
        buttons.clear();
        drawBackground(g);
        if (screen==Screen.MENU) paintMenu(g);
        else if (screen==Screen.SCORES) paintScores(g);
        else if (screen==Screen.HELP) paintHelp(g);
        else {
            paintGame(g);
            if (screen==Screen.PAUSE) paintPause(g);
            else if (screen==Screen.OVER) paintGameOver(g);
        }
        g.dispose();
    }

    private void drawBackground(Graphics2D g) {
        g.setPaint(new GradientPaint(0,0,new Color(9,15,37),W,H,new Color(6,8,22)));
        g.fillRect(0,0,W,H);
        softGlow(g,140,200,360,new Color(41,58,157,65));
        softGlow(g,875,520,380,new Color(102,30,154,65));
        softGlow(g,635,-55,220,new Color(18,120,153,35));
        g.setColor(new Color(70,103,159,17));
        for (int x=0;x<W;x+=40) g.drawLine(x,0,x,H);
        for (int y=0;y<H;y+=40) g.drawLine(0,y,W,y);
        for (int i=0;i<stars.size();i++) {
            Point s=stars.get(i);
            int r=i%9==0?2:1;
            int alpha=50+(int)(35*(1+Math.sin(globalAnimation*.9+i)));
            g.setColor(new Color(170,218,255,Math.min(170,alpha)));
            g.fillOval(s.x,s.y,r,r);
        }
    }
    private static void softGlow(Graphics2D g,float x,float y,float r,Color core) {
        g.setPaint(new RadialGradientPaint(new Point2D.Float(x,y),r,
                new float[]{0f,.35f,1f},new Color[]{core,
                    new Color(core.getRed(),core.getGreen(),core.getBlue(),core.getAlpha()/3),
                    new Color(core.getRed(),core.getGreen(),core.getBlue(),0)}));
        g.fill(new Ellipse2D.Double(x-r,y-r,r*2,r*2));
    }
    private static Font font(int size,boolean bold) { return new Font("SansSerif",bold?Font.BOLD:Font.PLAIN,size); }
    private static Font mono(int size,boolean bold) { return new Font("Monospaced",bold?Font.BOLD:Font.PLAIN,size); }
    private static void text(Graphics2D g,String value,int x,int y,int size,Color color,boolean bold) {
        g.setFont(font(size,bold));g.setColor(color);g.drawString(value,x,y);
    }
    private static void centered(Graphics2D g,String value,int centerX,int y,int size,Color color,boolean bold) {
        g.setFont(font(size,bold));FontMetrics fm=g.getFontMetrics();g.setColor(color);
        g.drawString(value,centerX-fm.stringWidth(value)/2,y);
    }
    private static void panel(Graphics2D g,int x,int y,int w,int h,int arc) {
        g.setColor(new Color(13,24,49,222));
        g.fill(new RoundRectangle2D.Double(x,y,w,h,arc,arc));
        g.setStroke(new BasicStroke(1.1f));g.setColor(new Color(78,109,160,73));
        g.draw(new RoundRectangle2D.Double(x+.5,y+.5,w-1,h-1,arc,arc));
    }
    private static void smallLabel(Graphics2D g,String value,int x,int y) {
        g.setFont(mono(12,true));g.setColor(MUTED);g.drawString(value,x,y);
    }
    private void header(Graphics2D g) {
        g.setColor(new Color(10,18,40,217));g.fillRect(0,0,W,77);
        g.setColor(new Color(67,232,246,62));g.drawLine(0,76,W,76);
        softGlow(g,44,35,55,new Color(65,227,249,85));
        g.setColor(CYAN);g.setStroke(new BasicStroke(2.3f));
        g.drawRoundRect(37,25,27,27,8,8);
        g.fillOval(43,31,7,7);g.fillOval(53,39,5,5);
        text(g,"BALLGAME",77,44,22,INK,true);
        text(g,"/ PRO",208,43,15,CYAN,true);
        pill(g,282,26,46,24,"V2",PURPLE);
        drawButton(g,739,23,98,32,audio.isMusicEnabled()?"♪  ON":"♪  OFF",false,PURPLE,
                ()->audio.setMusicEnabled(!audio.isMusicEnabled()));
        drawButton(g,845,23,101,32,audio.isEffectsEnabled()?"SFX ON":"SFX OFF",false,CYAN,
                ()->audio.setEffectsEnabled(!audio.isEffectsEnabled()));
    }
    private static void pill(Graphics2D g,int x,int y,int w,int h,String s,Color c) {
        g.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),33));g.fillRoundRect(x,y,w,h,12,12);
        g.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),105));g.drawRoundRect(x,y,w,h,12,12);
        centered(g,s,x+w/2,y+h/2+5,12,c,true);
    }
    private void drawButton(Graphics2D g,int x,int y,int w,int h,String label,
                            boolean primary,Color accent,Runnable action) {
        boolean hover=pointerX>=x&&pointerX<=x+w&&pointerY>=y&&pointerY<=y+h;
        if (primary) {
            g.setPaint(new GradientPaint(x,y,hover?accent.brighter():accent,x+w,y+h,
                    hover?new Color(138,133,254):new Color(91,112,238)));
            g.fillRoundRect(x,y,w,h,17,17);
            if (hover) {g.setColor(new Color(255,255,255,100));g.drawRoundRect(x+1,y+1,w-2,h-2,17,17);}
        } else {
            g.setColor(hover?new Color(35,52,88):new Color(22,35,63));g.fillRoundRect(x,y,w,h,15,15);
            g.setColor(hover?accent:new Color(86,109,157,122));g.drawRoundRect(x,y,w,h,15,15);
        }
        centered(g,label,x+w/2,y+h/2+(h>=50?7:5),h>=50?16:12,primary?new Color(6,16,35):INK,true);
        buttons.add(new Button(x,y,w,h,action));
    }

    private void paintMenu(Graphics2D g) {
        header(g);
        pill(g,65,116,189,27,"THE ARCADE EXPERIENCE",CYAN);
        text(g,"DEFEND",61,197,58,INK,true);
        text(g,"THE LIGHT.",61,261,58,CYAN,true);
        text(g,"Garde les balles en jeu. Déclenche les combos.",65,296,15,MUTED,false);
        text(g,"Mode de jeu",65,344,16,INK,true);

        modeCard(g,65,360,283,90,Mode.ARCADE,"01  ARCADE","05:00  •  +1 balle / 10 s",CYAN);
        modeCard(g,360,360,283,90,Mode.SURVIVAL,"02  SURVIVAL","Infini  •  Vitesse croissante",PURPLE);
        smallLabel(g,"DIFFICULTÉ",65,484);
        int xx=65;
        for (Difficulty d:Difficulty.values()) {
            final Difficulty choice=d;
            boolean selected=difficulty==d;
            drawButton(g,xx,501,137,38,d.label,selected,selected?CYAN:PURPLE,()->difficulty=choice);
            xx+=146;
        }
        drawButton(g,65,572,367,58,"▶   JOUER MAINTENANT",true,CYAN,this::startGame);
        drawButton(g,443,572,201,58,"RECORDS",false,PURPLE,()->screen=Screen.SCORES);
        drawButton(g,65,642,579,35,"?  COMMENT JOUER     •     F11 PLEIN ÉCRAN",false,CYAN,
                ()->screen=Screen.HELP);
        preview(g);
        centered(g,"JAVA 21   •   SWING   •   100% OFFLINE",500,691,11,new Color(125,148,194),false);
    }
    private void modeCard(Graphics2D g,int x,int y,int w,int h,Mode target,
                          String name,String description,Color accent) {
        boolean active=mode==target;
        g.setColor(active?new Color(26,63,78):new Color(16,26,54));g.fillRoundRect(x,y,w,h,20,20);
        g.setStroke(new BasicStroke(active?2f:1f));
        g.setColor(active?accent:new Color(81,109,161));g.drawRoundRect(x,y,w,h,20,20);
        text(g,name,x+19,y+34,17,active?accent:INK,true);
        text(g,description,x+19,y+64,12,MUTED,false);
        buttons.add(new Button(x,y,w,h,()->mode=target));
    }
    private void preview(Graphics2D g) {
        panel(g,664,114,285,552,29);
        smallLabel(g,"LIVE  //  PREVIEW",687,143);
        pill(g,854,127,69,23,"READY",GREEN);
        double phase=globalAnimation;
        softGlow(g,810,338,155,new Color(61,116,243,83));
        g.setColor(new Color(73,105,163,65));g.drawOval(712,244,187,187);
        g.drawOval(736,266,143,143);
        double bx=802+60*Math.sin(phase*1.3), by=323+55*Math.cos(phase*1.8);
        for (int i=6;i>=1;i--) {
            double px=bx-i*7*Math.sin(phase*1.3+1),py=by-i*7*Math.cos(phase*1.8+1);
            g.setColor(new Color(67,232,246,Math.max(13,90-i*12)));
            g.fillOval((int)px-8,(int)py-8,16,16);
        }
        softGlow(g,(float)bx,(float)by,45,new Color(67,232,246,105));
        g.setColor(CYAN);g.fillOval((int)bx-11,(int)by-11,22,22);
        g.setColor(new Color(234,255,255));g.fillOval((int)bx-4,(int)by-4,7,7);
        g.setPaint(new GradientPaint(735,493,CYAN,867,505,PURPLE));
        g.fillRoundRect(740+(int)(42*Math.sin(phase*.7)),502,124,13,8,8);
        g.setColor(new Color(97,126,167,100));g.drawLine(700,534,914,534);
        smallLabel(g,"PROGRESSION",690,572);
        g.setColor(new Color(43,58,90));g.fillRoundRect(690,587,234,7,7,7);
        g.setPaint(new GradientPaint(690,587,CYAN,840,594,PURPLE));g.fillRoundRect(690,587,150,7,7,7);
        text(g,"3 VIES   /   COMBOS   /   BONUS",690,630,12,MUTED,true);
    }

    private void paintGame(Graphics2D g) {
        header(g);
        g.setColor(new Color(9,21,47,208));g.fillRoundRect(35,90,930,47,16,16);
        hud(g,54,"SCORE",String.format("%,d",score).replace(',',' '),CYAN);
        hud(g,235,"COMBO","x"+combo,GOLD);
        hud(g,370,"NIVEAU",String.valueOf(level),PURPLE);
        hud(g,508,"VIES", "♥ ".repeat(Math.max(0,lives)),PINK);
        hud(g,640,"BALLES",balls.size()+" / "+totalGenerated,INK);
        hud(g,807,mode==Mode.ARCADE?"TEMPS":"SURVIE",
                clock(mode==Mode.ARCADE?remaining():(int)elapsed),GREEN);

        g.setColor(new Color(6,13,35,237));g.fillRoundRect(LEFT,TOP,RIGHT-LEFT,BOTTOM-TOP,20,20);
        g.setColor(new Color(74,110,164,100));g.setStroke(new BasicStroke(1.2f));
        g.drawRoundRect(LEFT,TOP,RIGHT-LEFT,BOTTOM-TOP,20,20);
        g.setColor(new Color(57,87,143,19));
        for (int x=LEFT+25;x<RIGHT;x+=35) g.drawLine(x,TOP+8,x,BOTTOM-8);
        for (int y=TOP+25;y<BOTTOM;y+=35) g.drawLine(LEFT+8,y,RIGHT-8,y);
        softGlow(g,500,TOP+15,390,new Color(55,90,210,32));
        g.setColor(new Color(80,132,245,24));g.fillRect(LEFT+2,BOTTOM-40,RIGHT-LEFT-4,37);

        for (Obstacle o:obstacles) {
            softGlow(g,(float)(o.x+o.width/2),(float)o.y,60,new Color(168,113,255,70));
            g.setPaint(new GradientPaint((int)o.x,(int)o.y,PURPLE,(int)(o.x+o.width),(int)o.y,new Color(74,131,236)));
            g.fillRoundRect((int)o.x,(int)o.y,(int)o.width,(int)o.height(),10,10);
            g.setColor(new Color(255,255,255,100));g.drawLine((int)o.x+12,(int)o.y+3,(int)(o.x+o.width-12),(int)o.y+3);
        }
        for (Particle p:particles) {
            float a=(float)Math.max(0,1-p.age/p.lifetime);
            g.setColor(new Color(p.color.getRed(),p.color.getGreen(),p.color.getBlue(),(int)(a*180)));
            g.fill(new Ellipse2D.Double(p.x-p.radius/2,p.y-p.radius/2,p.radius,p.radius));
        }
        for (Ball b:balls) {
            int i=0;
            for (Point2D.Double t:b.trail) {
                float k=(i+1f)/(b.trail.size()+1f);
                g.setColor(new Color(67,232,246,(int)(k*110)));
                double r=BALL_RADIUS*k*.95;
                g.fill(new Ellipse2D.Double(t.x-r,t.y-r,r*2,r*2));
                i++;
            }
            softGlow(g,(float)b.x,(float)b.y,35,new Color(67,232,246,85));
            g.setColor(CYAN);g.fill(new Ellipse2D.Double(b.x-BALL_RADIUS,b.y-BALL_RADIUS,20,20));
            g.setColor(Color.WHITE);g.fill(new Ellipse2D.Double(b.x-3.7,b.y-4.0,7.4,7.4));
        }
        for (Drop d:drops) {
            int pulse=(int)(2*Math.sin(globalAnimation*5));
            softGlow(g,(float)d.x,(float)d.y,44,new Color(d.kind.color.getRed(),d.kind.color.getGreen(),d.kind.color.getBlue(),87));
            g.setColor(new Color(d.kind.color.getRed(),d.kind.color.getGreen(),d.kind.color.getBlue(),65));
            g.fillRoundRect((int)d.x-33,(int)d.y-14-pulse,66,28,15,15);
            g.setColor(d.kind.color);
            g.drawRoundRect((int)d.x-33,(int)d.y-14-pulse,66,28,15,15);
            centered(g,d.kind.label,(int)d.x,(int)d.y+4-pulse,12,d.kind.color,true);
        }
        float pw=(float)paddleWidth();
        softGlow(g,(float)paddleX,PADDLE_Y,90,new Color(67,232,246,98));
        g.setColor(new Color(67,232,246,44));g.fillRoundRect((int)(paddleX-pw/2-8),PADDLE_Y-7,(int)pw+16,28,17,17);
        g.setPaint(new GradientPaint((float)(paddleX-pw/2),PADDLE_Y,CYAN,(float)(paddleX+pw/2),PADDLE_Y,PURPLE));
        g.fillRoundRect((int)(paddleX-pw/2),PADDLE_Y,(int)pw,PADDLE_HEIGHT,11,11);
        g.setColor(new Color(250,250,255,120));g.drawLine((int)(paddleX-pw/2+11),PADDLE_Y+2,(int)(paddleX+pw/2-11),PADDLE_Y+2);

        for (FloatingText f:floatingTexts) {
            int a=(int)(255*Math.max(0,1-f.age/1.25));
            centered(g,f.text,(int)f.x,(int)f.y,16,
                    new Color(f.color.getRed(),f.color.getGreen(),f.color.getBlue(),a),true);
        }
        if (elapsed<wideUntil) statusPill(g,53,160,"LARGE "+(int)Math.ceil(wideUntil-elapsed)+"s",CYAN);
        if (elapsed<slowUntil) statusPill(g,185,160,"SLOW "+(int)Math.ceil(slowUntil-elapsed)+"s",PURPLE);
        if (elapsed<doubleUntil) statusPill(g,315,160,"SCORE X2 "+(int)Math.ceil(doubleUntil-elapsed)+"s",GOLD);
        if (screen==Screen.PLAY) drawButton(g,855,159,91,29,"Ⅱ  PAUSE",false,CYAN,this::togglePause);
        text(g,"P/ESPACE  PAUSE     ← → / A D  DÉPLACER     SOURIS  DÉPLACER     M MUSIQUE   S SFX",49,689,11,MUTED,false);
    }
    private static void hud(Graphics2D g,int x,String label,String value,Color color) {
        smallLabel(g,label,x,106);
        text(g,value,x,127,17,color,true);
    }
    private static void statusPill(Graphics2D g,int x,int y,String label,Color color) {
        int w=label.length()*8+20;
        pill(g,x,y,w,24,label,color);
    }

    private void backdrop(Graphics2D g) {
        g.setColor(new Color(2,6,19,192));g.fillRect(0,77,W,H-77);
    }
    private void paintPause(Graphics2D g) {
        backdrop(g);panel(g,316,204,368,291,27);
        centered(g,"Ⅱ",500,285,49,CYAN,true);
        centered(g,"PAUSE",500,337,35,INK,true);
        centered(g,"La partie attend ton retour.",500,368,14,MUTED,false);
        drawButton(g,345,390,310,49,"REPRENDRE  (P / ENTRÉE)",true,CYAN,this::resume);
        drawButton(g,345,450,148,33,"RECOMMENCER",false,PURPLE,this::startGame);
        drawButton(g,504,450,151,33,"MENU",false,PINK,this::returnMenu);
    }
    private void paintGameOver(Graphics2D g) {
        backdrop(g);panel(g,278,143,444,447,30);
        pill(g,420,168,160,28,newRecord?"NOUVEAU RECORD":"FIN DE PARTIE",newRecord?GOLD:PURPLE);
        centered(g,mode==Mode.ARCADE&&lives>0?"DÉFI TERMINÉ":"GAME OVER",500,243,38,INK,true);
        centered(g,gameOverReason,500,273,12,MUTED,true);
        centered(g,String.format("%,d",score).replace(',',' '),500,337,52,CYAN,true);
        centered(g,"POINTS",500,361,12,MUTED,true);
        g.setColor(new Color(84,113,160,82));g.drawLine(331,381,669,381);
        centered(g,"NIVEAU " + level,363,408,13,PURPLE,true);
        centered(g,"COMBO x" + bestCombo,500,408,13,GOLD,true);
        centered(g,"REBONDS " + rebounds,637,408,13,GREEN,true);
        centered(g,"TEMPS " + clock((int)elapsed),363,439,12,MUTED,true);
        centered(g,"BONUS " + collected,500,439,12,MUTED,true);
        centered(g,"BALLES " + totalGenerated,637,439,12,MUTED,true);
        drawButton(g,304,462,392,48,"↻  REJOUER",true,CYAN,this::startGame);
        drawButton(g,304,523,187,35,"MENU PRINCIPAL",false,PURPLE,this::returnMenu);
        drawButton(g,509,523,187,35,"RECORDS",false,PINK,()->screen=Screen.SCORES);
    }

    private void paintScores(Graphics2D g) {
        header(g);pill(g,55,106,121,26,"HALL OF FAME",GOLD);
        text(g,"MEILLEURS SCORES",55,191,44,INK,true);
        text(g,"Tes performances sont enregistrées localement et automatiquement.",56,222,15,MUTED,false);
        drawButton(g,827,121,120,39,"←  RETOUR",false,CYAN,this::returnMenu);
        panel(g,55,256,890,392,24);
        text(g,"MODE : "+mode.name()+"    /    "+difficulty.label,79,298,19,CYAN,true);
        drawButton(g,640,271,122,36,"MODE",false,PURPLE,
                ()->mode=mode==Mode.ARCADE?Mode.SURVIVAL:Mode.ARCADE);
        drawButton(g,772,271,144,36,"DIFFICULTÉ",false,CYAN,
                ()->difficulty=Difficulty.values()[(difficulty.ordinal()+1)%Difficulty.values().length]);
        smallLabel(g,"RANG",89,347);smallLabel(g,"SCORE",235,347);
        smallLabel(g,"DURÉE",425,347);smallLabel(g,"COMBO",581,347);smallLabel(g,"DATE",736,347);
        List<ScoreStore.Entry> top=scores.top(mode.name(),difficulty.name());
        if (top.isEmpty()) centered(g,"Aucun score encore. Lance une partie !",500,464,17,MUTED,false);
        for (int i=0;i<top.size();i++) {
            ScoreStore.Entry entry=top.get(i);
            int y=379+i*49;
            if (i%2==0) {g.setColor(new Color(57,78,129,36));g.fillRoundRect(79,y-23,834,38,9,9);}
            text(g,"#"+(i+1),89,y,17,i==0?GOLD:INK,true);
            text(g,String.format("%,d",entry.score()).replace(',',' '),235,y,18,i==0?CYAN:INK,true);
            text(g,clock(entry.seconds()),425,y,14,MUTED,false);
            text(g,"x"+entry.maxCombo(),581,y,14,PURPLE,true);
            text(g,entry.date(),736,y,14,MUTED,false);
        }
        text(g,"PARTIES JOUÉES : "+scores.gamesPlayed(),60,679,12,MUTED,true);
        text(g,"RECORD GLOBAL : "+scores.allTimeBest(),735,679,12,GOLD,true);
    }

    private void paintHelp(Graphics2D g) {
        header(g);pill(g,55,106,120,26,"GUIDE DU JEU",CYAN);
        text(g,"COMMENT JOUER",55,189,45,INK,true);
        drawButton(g,819,120,127,42,"←  RETOUR",false,CYAN,this::returnMenu);
        panel(g,55,218,890,423,28);
        helpLine(g,84,273,"DÉPLACEMENT","Souris, flèches ← → ou touches A / D",CYAN);
        helpLine(g,84,328,"PAUSE","P, ESPACE ou ÉCHAP (reprendre avec P)",PURPLE);
        helpLine(g,84,383,"ARCADE","5 minutes • nouvelle balle toutes les 10 secondes",GREEN);
        helpLine(g,84,438,"SURVIVAL","Tiens le plus longtemps possible, difficulté croissante",GOLD);
        helpLine(g,84,493,"COMBOS","Chaque rebond donne des points, combos = multiplicateurs",PINK);
        helpLine(g,84,548,"BONUS","LARGE, SLOW, MULTI, X2 et +1 vie (max 3)",CYAN);
        helpLine(g,84,603,"OPTIONS","M = musique • S = effets • F11 = plein écran",PURPLE);
        centered(g,"Une vie est perdue uniquement quand toutes les balles quittent le terrain.",500,674,13,MUTED,false);
    }
    private static void helpLine(Graphics2D g,int x,int y,String name,String detail,Color color) {
        g.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),90));g.fillRoundRect(x,y-20,4,28,4,4);
        text(g,name,x+18,y,15,color,true);
        text(g,detail,x+216,y,15,INK,false);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(()->{
            try {javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());}
            catch (Exception ignored) { }
            JFrame window=new JFrame("BallGame Pro V2 — Neon Arcade");
            BallGame game=new BallGame();
            game.setFrame(window);
            try {
                var resource=BallGame.class.getResource("/icon.png");
                if (resource != null) window.setIconImage(javax.imageio.ImageIO.read(resource));
            } catch (java.io.IOException ignored) { }
            window.setContentPane(game);
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.pack();
            window.setMinimumSize(new Dimension(800,595));
            window.setLocationRelativeTo(null);
            window.setVisible(true);
            game.requestFocusInWindow();
            window.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosing(java.awt.event.WindowEvent e) {game.audio.close();}
            });
        });
    }
}
