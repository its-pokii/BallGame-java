import com.ballgame.pro.BallGame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;

public class PhysicsTest {
    static Field fld(Class<?> type,String name) throws Exception {
        Field f=type.getDeclaredField(name);f.setAccessible(true);return f;
    }
    static Object get(Object o,String name) throws Exception {return fld(o.getClass(),name).get(o);}
    static void set(Object o,String name,Object val) throws Exception {fld(o.getClass(),name).set(o,val);}
    static void invoke(Object o,String name,Class<?>[] args,Object... val) throws Exception {
        Method m=o.getClass().getDeclaredMethod(name,args);m.setAccessible(true);m.invoke(o,val);
    }
    static void check(boolean c,String msg) {if (!c) throw new AssertionError(msg);}
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(()->{
            try {
                BallGame game=new BallGame();
                ((Timer)get(game,"timer")).stop();
                invoke(game,"startGame",new Class[]{});
                check((int)get(game,"lives")==3,"3 lives");
                check(((List<?>)get(game,"balls")).size()==1,"initial ball");
                Object ball=((List<?>)get(game,"balls")).get(0);
                set(ball,"x",500.0);set(ball,"y",609.0);
                set(ball,"vx",0.0);set(ball,"vy",360.0);
                set(game,"paddleX",500.0);
                invoke(game,"updateGame",new Class[]{double.class},0.03);
                check((int)get(game,"score")==10,"First paddle rebound scores +10");
                check((int)get(game,"combo")==1,"First rebound combo x1");
                check((double)get(ball,"vy")<0,"paddle must bounce upward");

                set(game,"powerCounter",6);
                set(ball,"y",609.0);set(ball,"x",500.0);
                set(ball,"vx",0.0);set(ball,"vy",360.0);
                invoke(game,"updateGame",new Class[]{double.class},0.03);
                check(((List<?>)get(game,"drops")).size()==1,"one power drop after 7 bounces interval");

                Class<?> powerKind=Class.forName("com.ballgame.pro.BallGame$PowerKind");
                Object multi=Enum.valueOf((Class)powerKind,"MULTI");
                invoke(game,"collect",new Class[]{powerKind,double.class,double.class},multi,500.0,400.0);
                check(((List<?>)get(game,"balls")).size()==3,"Multiball duplicates first ball");
                Object wide=Enum.valueOf((Class)powerKind,"WIDE");
                invoke(game,"collect",new Class[]{powerKind,double.class,double.class},wide,500.0,400.0);
                invoke(game,"updateGame",new Class[]{double.class},0.02);
                check((double)get(game,"wideUntil")>0,"Wide mode active");

                set(game,"elapsed",70.0);
                invoke(game,"updateGame",new Class[]{double.class},0.02);
                check((int)get(game,"level")>=3,"Levels progress with time");
                check(((List<?>)get(game,"obstacles")).size()>0,"Obstacles appear from level 2");

                for (int i=0;i<3;i++) {
                    ((List<?>)get(game,"balls")).clear();
                    invoke(game,"updateGame",new Class[]{double.class},0.02);
                }
                check((int)get(game,"lives")==0,"Three missed volleys consume 3 lives");
                check(get(game,"screen").toString().equals("OVER"),"3 lives trigger game over");
                check(Files.exists(Path.of(System.getProperty("user.home"),".ballgame-pro-v2","records.properties")),"Records persisted");
                System.out.println("PHYSICS TEST PASS: scoring, bounce, combo, power spawn, multiball, wide, levels, obstacles, 3 lives, saved highscores");
            } catch (Exception ex) {throw new RuntimeException(ex);}
        });
        System.exit(0);
    }
}
