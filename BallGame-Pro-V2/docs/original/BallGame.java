import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class BallGame extends JPanel implements ActionListener {


    static final int LARGEUR = 800;
    static final int HAUTEUR = 600;
    static final int RAYON_BALLE = 10;
    static final double VITESSE_BALLE = 6.0;      // pixels par image
    static final int LARGEUR_RAQUETTE = 110;
    static final int HAUTEUR_RAQUETTE = 14;
    static final int DUREE_PARTIE_SEC = 5 * 60;   // 05:00
    static final int INTERVALLE_NOUVELLE_BALLE_SEC = 10;

    static final Color COULEUR_BALLE = new Color(240, 150, 205);
    static final Color COULEUR_RAQUETTE = new Color(230, 240, 215);

    static class Balle {
        double x, y, vx, vy;
        Balle(double x, double y, double vx, double vy) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        }
    }

    private final List<Balle> balles = new ArrayList<>();
    private final Random alea = new Random();
    private final javax.swing.Timer boucle = new javax.swing.Timer(16, this);

    private double raquetteX = LARGEUR / 2.0;     // centre de la raquette
    private int totalGenere = 0;
    private long debutMs;
    private long dernierAjoutMs;
    private boolean terminee = false;

    public BallGame() {
        setPreferredSize(new Dimension(LARGEUR, HAUTEUR));
        setBackground(Color.BLACK);

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e)   { raquetteX = e.getX(); }
            @Override public void mouseDragged(MouseEvent e) { raquetteX = e.getX(); }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (terminee) nouvellePartie();
            }
        });

        nouvellePartie();
        boucle.start();
    }

    private void nouvellePartie() {
        balles.clear();
        totalGenere = 0;
        terminee = false;
        debutMs = System.currentTimeMillis();
        dernierAjoutMs = debutMs;
        ajouterBalle();
    }

    private void ajouterBalle() {

        double angle = Math.toRadians(-60 + alea.nextDouble() * 120);
        double vx = Math.sin(angle) * VITESSE_BALLE;
        double vy = -Math.cos(angle) * VITESSE_BALLE;
        double x = RAYON_BALLE + alea.nextDouble() * (LARGEUR - 2 * RAYON_BALLE);
        double y = HAUTEUR * 0.45;
        balles.add(new Balle(x, y, vx, vy));
        totalGenere++;
    }

    private int yRaquette() {
        return getHeight() - 60;
    }

    private int secondesRestantes() {
        long ecoule = (System.currentTimeMillis() - debutMs) / 1000;
        return (int) Math.max(0, DUREE_PARTIE_SEC - ecoule);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!terminee) {
            mettreAJour();
        }
        repaint();
    }

    private void mettreAJour() {
        int largeur = getWidth();
        int yRaq = yRaquette();
        double gauche = raquetteX - LARGEUR_RAQUETTE / 2.0;
        double droite = raquetteX + LARGEUR_RAQUETTE / 2.0;

        Iterator<Balle> it = balles.iterator();
        while (it.hasNext()) {
            Balle b = it.next();
            b.x += b.vx;
            b.y += b.vy;

            if (b.x < RAYON_BALLE)            { b.x = RAYON_BALLE;            b.vx = Math.abs(b.vx); }
            if (b.x > largeur - RAYON_BALLE)  { b.x = largeur - RAYON_BALLE;  b.vx = -Math.abs(b.vx); }
            // Plafond
            if (b.y < RAYON_BALLE)            { b.y = RAYON_BALLE;            b.vy = Math.abs(b.vy); }

            if (b.vy > 0
                    && b.y + RAYON_BALLE >= yRaq
                    && b.y + RAYON_BALLE <= yRaq + HAUTEUR_RAQUETTE + b.vy
                    && b.x >= gauche - RAYON_BALLE
                    && b.x <= droite + RAYON_BALLE) {
                // Position d'impact : -1 (bord gauche) à +1 (bord droit)
                double impact = (b.x - raquetteX) / (LARGEUR_RAQUETTE / 2.0);
                impact = Math.max(-1, Math.min(1, impact));
                double angle = impact * Math.toRadians(60);
                b.vx = Math.sin(angle) * VITESSE_BALLE;
                b.vy = -Math.cos(angle) * VITESSE_BALLE;
                b.y = yRaq - RAYON_BALLE;
            }

            if (b.y - RAYON_BALLE > getHeight()) {
                it.remove();
            }
        }

        long maintenant = System.currentTimeMillis();
        if (maintenant - dernierAjoutMs >= INTERVALLE_NOUVELLE_BALLE_SEC * 1000L) {
            ajouterBalle();
            dernierAjoutMs = maintenant;
        }

        if (balles.isEmpty() || secondesRestantes() == 0) {
            terminee = true;
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        g.setColor(new Color(40, 25, 25));
        g.fillRect(0, 0, w, h);
        RadialGradientPaint lueur = new RadialGradientPaint(
                new java.awt.geom.Point2D.Float(w * 0.38f, h * 0.38f), Math.max(w, h) * 0.45f,
                new float[]{0f, 1f},
                new Color[]{new Color(200, 20, 40, 230), new Color(60, 20, 20, 0)});
        g.setPaint(lueur);
        g.fillRect(0, 0, w, h);
        GradientPaint ombre = new GradientPaint(0, h * 0.6f, new Color(0, 0, 0, 0),
                0, h, new Color(10, 10, 30, 230));
        g.setPaint(ombre);
        g.fillRect(0, 0, w, h);

        g.setColor(COULEUR_RAQUETTE);
        g.fillRect((int) (raquetteX - LARGEUR_RAQUETTE / 2.0), yRaquette(),
                LARGEUR_RAQUETTE, HAUTEUR_RAQUETTE);

        g.setColor(COULEUR_BALLE);
        for (Balle b : balles) {
            g.fill(new Ellipse2D.Double(b.x - RAYON_BALLE, b.y - RAYON_BALLE,
                    RAYON_BALLE * 2, RAYON_BALLE * 2));
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        int sec = secondesRestantes();
        g.drawString(String.format("Temps restant : %02d:%02d", sec / 60, sec % 60), 8, 20);
        g.drawString("Balles actives : " + balles.size() + " / Total généré : " + totalGenere, 8, 40);

        if (terminee) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, w, h);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 28));
            String titre = "Partie terminée";
            g.drawString(titre, (w - g.getFontMetrics().stringWidth(titre)) / 2, h / 2 - 10);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            String aide = "Clique pour rejouer";
            g.drawString(aide, (w - g.getFontMetrics().stringWidth(aide)) / 2, h / 2 + 20);
        }
        g.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Jeu de balle");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(new BallGame());
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}