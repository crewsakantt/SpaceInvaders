import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

class Enemy {
    Rectangle bounds;
    int health;
    boolean canShoot;
    int type; // 0 = regular, 1 = shooter, 2 = strong non-shooter

    public Enemy(int x, int y, int health, boolean canShoot, int type) {
        this.bounds = new Rectangle(x, y, 50, 50);
        this.health = health;
        this.canShoot = canShoot;
        this.type = type; // Assigning the enemy type
    }

    public boolean isAlive() {
        return health > 0;
    }

    public void hit() {
        health--;
    }
}

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
    private int playerLives = 3;
    private boolean enemiesAllDestroyed = false; // Track if all enemies are destroyed
    private Timer respawnTimer; // Timer for respawning enemies


    private List<Rectangle> bullets = new ArrayList<>();
    private List<Enemy> enemies = new ArrayList<>();
    private List<Rectangle> enemyBullets = new ArrayList<>(); // For shooting enemies
    private int score = 0;

    private Image playerImage, bulletImage, enemyImage, backgroundImage;
    private boolean isPaused = false;
    private int highScore = 0;
    private Image enemyImageStrong;
    private Image enemyImageShooter;

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
        timer = new Timer(16, this); // 60 FPS
        timer.start();
    }
    private void loadAssets() {
        try {
            playerImage = new ImageIcon("assets/player.png").getImage();
            bulletImage = new ImageIcon("assets/bul.png").getImage();
            enemyImage = new ImageIcon("assets/green.png").getImage(); // Regular enemy
            enemyImageShooter = new ImageIcon("assets/red.png").getImage(); // Shooter enemy
            enemyImageStrong = new ImageIcon("assets/yellow.png").getImage(); // Strong non-shooter enemy
            backgroundImage = new ImageIcon("assets/bg.png").getImage();
        } catch (Exception e) {
            System.err.println("Failed to load images.");
            e.printStackTrace();
        }
    }

    private void initializeEnemies() {
        enemies.clear(); // Clear any previous enemies
        for (int i = 0; i < ENEMY_ROWS; i++) {
            for (int j = 0; j < NUM_ENEMIES; j++) {
                int type = (int)(Math.random() * 3); // Randomly assign a type (0, 1, or 2)
                int health = 1; // Default health for regular
                boolean canShoot = false; // Default no shooting
    
                if (type == 1) { // Shooter
                    health = 2; // 2 health for shooter
                    canShoot = true;
                } else if (type == 2) { // Strong non-shooter
                    health = 3; // 3 health for strong non-shooter
                    canShoot = false;
                }
    
                Enemy enemy = new Enemy(j * (ENEMY_WIDTH + 10) + 50, i * (ENEMY_HEIGHT + 10) + 50, health, canShoot, type);
                enemies.add(enemy);
            }
        }
    }       

    private void shootBullet() {
        Rectangle bullet = new Rectangle(playerX + PLAYER_WIDTH / 2 - BULLET_WIDTH / 2, playerY - BULLET_HEIGHT, BULLET_WIDTH, BULLET_HEIGHT);
        bullets.add(bullet);
    }    

    public void actionPerformed(ActionEvent e) {
        if (!isPaused) {
            movePlayer();
            moveBullets();
            moveEnemies();
            checkCollisions();
            moveEnemyBullets();
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
        List<Enemy> toRespawn = new ArrayList<>();
        
        for (Enemy enemy : enemies) {
            enemy.bounds.y += 1; // Move down
            
            if (enemy.isAlive() && Math.random() < 0.01 && enemy.canShoot) {
                shootEnemyBullet(enemy); // Call to shoot if enemy can shoot
            }
            
            // Check if enemy has moved past the player's position
            if (enemy.bounds.y + ENEMY_HEIGHT >= playerY) {
                playerLives--; // Decrease lives
                System.out.println("Player hit! Lives left: " + playerLives);
                if (playerLives <= 0) {
                    gameOver();
                    return; // Stop further processing if the game is over
                } else {
                    resetPlayerPosition(); // Clear the player’s position if hit
                }
    
                enemy.health = 0; // Mark enemy as destroyed by setting health to 0
                toRespawn.add(enemy); // Add to respawn list
            }
        }
    
        // Remove enemies that need to respawn
        enemies.removeAll(toRespawn);
        System.out.println("Enemies removed for respawn: " + toRespawn.size());
    
        // Respawn logic
        for (Enemy enemy : toRespawn) {
            // Create multiple new enemies for each one being removed
            for (int i = 0; i < 2; // You can adjust this value to increase respawning quantity
                 i++) {
                // Randomly assign a type (0, 1, or 2)
                int newType = (int)(Math.random() * 3);
                int health = (newType == 1) ? 2 : (newType == 2) ? 3 : 1; // Set health based on type
                boolean canShoot = newType == 1; // Only shoot if type is 1
                
                // Spawn enemies slightly above the screen
                Enemy newEnemy = new Enemy(enemy.bounds.x + (i * (ENEMY_WIDTH + 10)), 
                                            0, health, canShoot, newType);
                enemies.add(newEnemy); // Add the respawned enemy
                System.out.println("New enemy added at: " + newEnemy.bounds.x + ", " + newEnemy.bounds.y);
            }
        }
    }    
    
    private void shootEnemyBullet(Enemy enemy) {
        Rectangle bullet = new Rectangle(enemy.bounds.x + ENEMY_WIDTH / 2 - BULLET_WIDTH / 2, enemy.bounds.y + ENEMY_HEIGHT, BULLET_WIDTH, BULLET_HEIGHT);
        enemyBullets.add(bullet);
    }

    private void moveEnemyBullets() {
        List<Rectangle> newEnemyBullets = new ArrayList<>();
        for (Rectangle enemyBullet : enemyBullets) {
            enemyBullet.y += 5; // Move enemy bullet down
            if (enemyBullet.y < HEIGHT) {
                newEnemyBullets.add(enemyBullet);
            }
        }
        enemyBullets = newEnemyBullets;
    }

    private void checkCollisions() {
        List<Enemy> newEnemies = new ArrayList<>();
        List<Rectangle> newBullets = new ArrayList<>(); 
    
        for (Enemy enemy : enemies) {
            boolean hit = false;
            for (Rectangle bullet : bullets) {
                if (bullet.intersects(enemy.bounds)) {
                    enemy.hit(); // Decrease health
                    newBullets.add(bullet); // Mark the bullet for removal
                    
                    // Only add points if the enemy was shot down
                    if (!enemy.isAlive()) {
                        score += 10; // Only score if the enemy is destroyed
                    }
                    hit = true;
                    break; // Exit the loop after hitting an enemy
                }
            }
            if (!hit && enemy.isAlive()) {
                newEnemies.add(enemy);
            }
        }
        bullets.removeAll(newBullets); // Remove the bullets that hit enemies
        enemies = newEnemies;
    
        // Check if no enemies are left, and set the flag
        if (enemies.isEmpty() && !enemiesAllDestroyed) {
            enemiesAllDestroyed = true;
            startRespawnTimer(); // Start the respawn timer
            System.out.println("All enemies destroyed! Starting respawn countdown.");
        }
    
        // Check for collisions between enemy bullets and the player
        for (Rectangle enemyBullet : enemyBullets) {
            if (enemyBullet.intersects(new Rectangle(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT))) {
                playerLives--; // Decrease lives
                enemyBullets.remove(enemyBullet); // Remove bullet after hit
                if (playerLives <= 0) {
                    gameOver();
                }
                break; // Exit check loop
            }
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
    
    private void resetPlayerPosition() {
        playerX = WIDTH / 2 - PLAYER_WIDTH / 2; // Reset player's X position to center
        playerY = HEIGHT - PLAYER_HEIGHT - 30; // Reset player's Y position to just above the bottom
    }    

    private void resetGame() {
        playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
        playerY = HEIGHT - PLAYER_HEIGHT - 30;
        playerDX = 0;
        playerLives = 3; // Reset player lives
        bullets.clear();
        enemyBullets.clear(); // Clear enemy bullets
        enemies.clear();
        initializeEnemies();
        score = 0;
        timer.start();
    }

    private void startRespawnTimer() {
        respawnTimer = new Timer(1000, new ActionListener() { // 3000 milliseconds = 3 seconds
            @Override
            public void actionPerformed(ActionEvent e) {
                respawnEnemies();
                respawnTimer.stop();
                enemiesAllDestroyed = false; // Reset the flag
            }
        });
        respawnTimer.setRepeats(false); // Only run once
        respawnTimer.start();
    }

    private void respawnEnemies() {
        initializeEnemies(); // Re-initialize all enemies
        System.out.println("New set of enemies respawned!");
    }

    private void paintEnemies(Graphics g) {
        for (Enemy enemy : enemies) {
            if (enemy.type == 0) {
                g.drawImage(enemyImage, enemy.bounds.x, enemy.bounds.y, ENEMY_WIDTH, ENEMY_HEIGHT, null); // Regular
            } else if (enemy.type == 1) {
                // Use shooter enemy image
                g.drawImage(enemyImageShooter, enemy.bounds.x, enemy.bounds.y, ENEMY_WIDTH, ENEMY_HEIGHT, null);
            } else if (enemy.type == 2) {
                // Use strong non-shooter enemy image
                g.drawImage(enemyImageStrong, enemy.bounds.x, enemy.bounds.y, ENEMY_WIDTH, ENEMY_HEIGHT, null);
            }
        }
    }
     

    private void paintBullets(Graphics g) {
        for (Rectangle bullet : bullets) {
            g.drawImage(bulletImage, bullet.x, bullet.y, BULLET_WIDTH, BULLET_HEIGHT, null);
        }

        // Draw enemy bullets
        g.setColor(Color.RED);
        for (Rectangle enemyBullet : enemyBullets) {
            g.fillRect(enemyBullet.x, enemyBullet.y, BULLET_WIDTH, BULLET_HEIGHT);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(backgroundImage, 0, 0, WIDTH, HEIGHT, null);
        g.drawImage(playerImage, playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT, null);

        paintBullets(g);
        paintEnemies(g);

        g.setColor(Color.WHITE);
        g.setFont(new Font("TickerBit", Font.BOLD, 20));
        g.drawString("Score: " + score, 20, 30);
        g.drawString("High Score: " + highScore, 20, 60);
        g.drawString("Lives: " + playerLives, 20, 90); // Display player lives

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
