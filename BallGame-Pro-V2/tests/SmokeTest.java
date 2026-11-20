import com.ballgame.pro.BallGame;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;

public class SmokeTest {
    private static Object get(Object o,String name) throws Exception {
        Field field=BallGame.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(o);
    }
    private static void set(Object o,String name,Object value) throws Exception {
        Field field=BallGame.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(o,value);
    }
    private static void require(boolean condition,String msg) { if (!condition) throw new AssertionError(msg); }
    private static void click(Robot r,int x,int y) {
        r.mouseMove(x,y);
        r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        r.delay(80);
        r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        r.delay(160);
    }
    private static void key(Robot r,int k) {r.keyPress(k);r.delay(70);r.keyRelease(k);r.delay(150);}
    private static void shot(Robot r,String name) throws Exception {
        ImageIO.write(r.createScreenCapture(new Rectangle(0,0,1000,700)),"png",
                Path.of("/mnt/data/BallGame-Pro-V2/docs",name).toFile());
    }
    public static void main(String[] args) throws Exception {
        JFrame[] frame=new JFrame[1];BallGame[] app=new BallGame[1];
        SwingUtilities.invokeAndWait(()->{
            frame[0]=new JFrame("BallGame smoke test");
            frame[0].setUndecorated(true);
            app[0]=new BallGame();
            frame[0].setContentPane(app[0]);
            frame[0].setSize(1000,700);
            frame[0].setLocation(0,0);
            frame[0].setVisible(true);
            app[0].requestFocusInWindow();
        });
        Robot r=new Robot();r.setAutoDelay(35);r.delay(850);
        shot(r,"preview-menu.png");
        click(r,240,598);r.delay(350);
        require(get(app[0],"screen").toString().equals("PLAY"),"Click Play should start a game");
        require((Integer)get(app[0],"lives")==3,"3 lives at start");
        require(((List<?>)get(app[0],"balls")).size()==1,"One ball at start");
        shot(r,"preview-game.png");
        key(r,KeyEvent.VK_P);
        require(get(app[0],"screen").toString().equals("PAUSE"),"P pauses");
        double before=(Double)get(app[0],"elapsed");r.delay(200);
        double after=(Double)get(app[0],"elapsed");
        require(Math.abs(before-after)<.00001,"Timer freezes on pause");
        shot(r,"preview-pause.png");
        key(r,KeyEvent.VK_P);
        require(get(app[0],"screen").toString().equals("PLAY"),"P resumes");
        SwingUtilities.invokeAndWait(()->{
            try {set(app[0],"elapsed",76.0);} catch(Exception ex){throw new RuntimeException(ex);}
        });
        r.delay(90);
        shot(r,"preview-levels.png");
        require((Integer)get(app[0],"level")>=3,"Level progression after time");
        require(((List<?>)get(app[0],"obstacles")).size()>0,"Obstacles activated");
        SwingUtilities.invokeAndWait(()->{
            try {set(app[0],"elapsed",299.99);} catch(Exception ex){throw new RuntimeException(ex);}
        });
        r.delay(200);
        require(get(app[0],"screen").toString().equals("OVER"),"Arcade ends after 5 min");
        shot(r,"preview-gameover.png");
        click(r,394,537);r.delay(150);
        require(get(app[0],"screen").toString().equals("MENU"),"Return to menu");
        click(r,489,402); // Select Survival card
        click(r,236,596); // Start Survival
        require(get(app[0],"mode").toString().equals("SURVIVAL"),"Survival mode selected");
        require(get(app[0],"screen").toString().equals("PLAY"),"Survival starts");
        System.out.println("SMOKE TEST PASS: main menu, Arcade, timer, lives, level, obstacles, pause, records, Survival");
        SwingUtilities.invokeAndWait(()->frame[0].dispose());
        System.exit(0);
    }
}
