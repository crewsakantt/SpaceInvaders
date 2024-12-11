import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class SpaceInvadersGame extends JPanel implements ActionListener {

    private final int WIDTH = 800;
    private final int HEIGHT = 600;
    private final int PLAYER_WIDTH = 50;
    private final int PLAYER_HEIGHT = 50;
    private final int BULLET_WIDTH = 10;
    private final int BULLET_HEIGHT = 20;
    private final int ENEMY_WIDTH = 50;
    private final int ENEMY_HEIGHT = 50;
    private final int NUM_ENEMIES = 10;
    private final int ENEMY_ROWS = 3;

    private Timer timer;
    private int playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
    private int playerY = HEIGHT - PLAYER_HEIGHT - 30;
    private int playerDX = 0;

    private List<Rectangle> bullets = new ArrayList<>();
    private List<Rectangle> enemies = new ArrayList<>();
    private int score = 0;

    private Image playerImage, bulletImage, enemyImage, backgroundImage;
    private boolean isPaused = false;
    private int highScore = 0;

    public SpaceInvadersGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    playerDX = -5;
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    playerDX = 5;
                } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    shootBullet();
                } else if (e.getKeyCode() == KeyEvent.VK_P) {
                    isPaused = !isPaused;
                    if (isPaused) {
                        timer.stop();
                    } else {
                        timer.start();
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    playerDX = 0;
                }
            }
        });

        initializeEnemies();
        loadAssets();

        timer = new Timer(16, this);  // 60 FPS
        timer.start();
    }

    private void loadAssets() {
        try {
            playerImage = new ImageIcon("assets/player.jpg").getImage();
            bulletImage = new ImageIcon("assets/bullet.jpg").getImage();
            enemyImage = new ImageIcon("assets/enemy.png").getImage();
            backgroundImage = new ImageIcon("assets/background.jpg").getImage();
    
            if (enemyImage == null || enemyImage.getWidth(null) <= 0 || enemyImage.getHeight(null) <= 0) {
                System.out.println("Enemy image not loaded or has invalid dimensions");
            }
        } catch (Exception e) {
            System.err.println("Failed to load images.");
            e.printStackTrace();
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (!isPaused) {
            movePlayer();
            moveBullets();
            moveEnemies();
            checkCollisions();
        }
        repaint();
    }

    private void movePlayer() {
        playerX += playerDX;
        if (playerX < 0) {
            playerX = 0;
        } else if (playerX + PLAYER_WIDTH > WIDTH) {
            playerX = WIDTH - PLAYER_WIDTH;
        }
    }

    private void moveBullets() {
        List<Rectangle> newBullets = new ArrayList<>();
        for (Rectangle bullet : bullets) {
            bullet.y -= 10; // Move bullet up
            if (bullet.y > 0) {
                newBullets.add(bullet);
            }
        }
        bullets = newBullets;
    }

    private void moveEnemies() {
        for (Rectangle enemy : enemies) {
            enemy.y += 1;
            if (Math.random() < 0.01) {
                enemy.x += (Math.random() > 0.5 ? 1 : -1) * 10;
            }
        }

        // Check if any enemies have reached the player's position
        for (Rectangle enemy : enemies) {
            if (enemy.y + ENEMY_HEIGHT >= playerY) {
                gameOver();
                break;
            }
        }
    }

    private void shootBullet() {
        Rectangle bullet = new Rectangle(playerX + PLAYER_WIDTH / 2 - BULLET_WIDTH / 2, playerY - BULLET_HEIGHT, BULLET_WIDTH, BULLET_HEIGHT);
        bullets.add(bullet);
    }

    private void initializeEnemies() {
        enemies.clear();  // Clear any previous entries
        for (int i = 0; i < ENEMY_ROWS; i++) {
            for (int j = 0; j < NUM_ENEMIES; j++) {
                Rectangle enemy = new Rectangle(j * (ENEMY_WIDTH + 10) + 50, i * (ENEMY_HEIGHT + 10) + 50, ENEMY_WIDTH, ENEMY_HEIGHT);
                enemies.add(enemy);
            }
        }
    }
    

    private void checkCollisions() {
        List<Rectangle> newEnemies = new ArrayList<>();
        for (Rectangle enemy : enemies) {
            boolean hit = false;
            for (Rectangle bullet : bullets) {
                if (bullet.intersects(enemy)) {
                    hit = true;
                    score += 10;
                    break;
                }
            }
            if (!hit) {
                newEnemies.add(enemy);
            }
        }
        enemies = newEnemies;

        if (enemies.isEmpty()) {
            gameWon();
        }
    }

    private void gameOver() {
        timer.stop();
        JOptionPane.showMessageDialog(this, "Game Over! Score: " + score);
        if (score > highScore) {
            highScore = score;
            JOptionPane.showMessageDialog(this, "New High Score: " + highScore);
        }
        resetGame();
    }

    private void gameWon() {
        timer.stop();
        JOptionPane.showMessageDialog(this, "You Won! Score: " + score);
        if (score > highScore) {
            highScore = score;
            JOptionPane.showMessageDialog(this, "New High Score: " + highScore);
        }
        resetGame();
    }

    private void resetGame() {
        playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
        playerY = HEIGHT - PLAYER_HEIGHT - 30;
        bullets.clear();
        enemies.clear();
        initializeEnemies();
        score = 0;
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.drawImage(backgroundImage, 0, 0, WIDTH, HEIGHT, null);

        g.drawImage(playerImage, playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT, null);

        for (Rectangle bullet : bullets) {
            g.drawImage(bulletImage, bullet.x, bullet.y, bullet.width, bullet.height, null);
        }

        for (Rectangle enemy : enemies) {
            g.drawImage(enemyImage, enemy.x, enemy.y, enemy.width, enemy.height, null);
        }        

        g.setColor(Color.WHITE);
        g.setFont(new Font("TickerBit", Font.BOLD, 20));
        g.drawString("Score: " + score, 20, 30);
        g.drawString("High Score: " + highScore, 20, 60);

        if (isPaused) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("TickerBit", Font.BOLD, 40));
            g.drawString("PAUSED", WIDTH / 2 - 100, HEIGHT / 2);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("Space Invaders");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                SpaceInvadersGame game = new SpaceInvadersGame();
                frame.getContentPane().add(game);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
