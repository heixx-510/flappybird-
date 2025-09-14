import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

public class FlappyBird extends JPanel implements ActionListener, KeyListener {
    int boardWidth = 360;
    int boardHeight = 640;

    Image backgroundImg;
    Image birdImg;
    Image topPipeImg;
    Image bottomPipeImg;

    // Bird properties
    int birdX = boardWidth / 8;
    int birdY = boardHeight / 2;
    int birdWidth = 34;
    int birdHeight = 24;

    class Bird {
        int x = birdX;
        int y = birdY;
        int width = birdWidth;
        int height = birdHeight;
        Image img;

        Bird(Image img) {
            this.img = img;
        }
    }

    int pipeX = boardWidth;
    int pipeY = 0;
    int pipeWidth = 64;
    int pipeHeight = 512;

    class Pipe {
        int x = pipeX;
        int y = pipeY;
        int width = pipeWidth;
        int height = pipeHeight;
        Image img;
        boolean passed = false;

        Pipe(Image img) {
            this.img = img;
        }
    }

    Bird bird;
    int velocityX = -4;
    int velocityY = 0;
    int gravity = 1;

    ArrayList<Pipe> pipes;
    Random random = new Random();

    Timer gameLoop;
    Timer placePipesTimer;

    private String currentUsername = "Anonymous";
    private String currentPassword = "";

    boolean gameover = false;
    private int score = 0;
    private int highscore = 0;

    // Login components
    private JFrame loginFrame;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel messageLabel;

    private boolean loggedIn = false;
    private boolean showLeaderboard = false;
    private java.util.List<String[]> leaderboardData;

    public FlappyBird() {
        // Show login screen first
        showLoginScreen();
    }

    private void showLoginScreen() {
        loginFrame = new JFrame("Flappy Bird Login");
        loginFrame.setSize(300, 200);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setLayout(new GridLayout(4, 1));
        loginFrame.setLocationRelativeTo(null);

        JPanel usernamePanel = new JPanel(new FlowLayout());
        JLabel usernameLabel = new JLabel("Username:");
        usernameField = new JTextField(15);
        usernamePanel.add(usernameLabel);
        usernamePanel.add(usernameField);

        JPanel passwordPanel = new JPanel(new FlowLayout());
        JLabel passwordLabel = new JLabel("Password:");
        passwordField = new JPasswordField(15);
        passwordPanel.add(passwordLabel);
        passwordPanel.add(passwordField);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        loginButton = new JButton("Login");
        loginButton.addActionListener(e -> attemptLogin());
        buttonPanel.add(loginButton);

        messageLabel = new JLabel(" ", JLabel.CENTER);

        loginFrame.add(usernamePanel);
        loginFrame.add(passwordPanel);
        loginFrame.add(buttonPanel);
        loginFrame.add(messageLabel);

        loginFrame.setVisible(true);
    }

    private void attemptLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter both username and password");
            return;
        }

        try {
            String response = sendLoginRequest(username, password);

            if (response.contains("successful") || response.contains("created")) {
                loggedIn = true;
                currentUsername = username;
                currentPassword = password;
                messageLabel.setText("Login successful! Starting game...");

                // Initialize game after a short delay
                Timer timer = new Timer(1000, e -> {
                    loginFrame.dispose();
                    initializeGame();
                    createAndShowGUI();
                });
                timer.setRepeats(false);
                timer.start();
            } else if (response.contains("Wrong password")) {
                messageLabel.setText("Wrong password. Please try again.");
            } else {
                messageLabel.setText("Login failed. Please try again.");
            }
        } catch (IOException ex) {
            messageLabel.setText("Connection error. Please try again.");
            if (ex.getMessage().contains("401")) {
                messageLabel.setText("Error:Username taken & wrong password");
            }
            ex.printStackTrace();
        }
    }

    private String sendLoginRequest(String username, String password) throws IOException {
        URL url = new URL("http://localhost:8080/api/login");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        String jsonInputString = "{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}";

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Check response code first
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Server returned HTTP response code: " + responseCode + " for URL: " + url);
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
    }

    private void initializeGame() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setFocusable(true);
        addKeyListener(this);

        // Load images or create placeholders
        try {
            backgroundImg = new ImageIcon(getClass().getResource("/bg-4.jpg")).getImage();
            birdImg = new ImageIcon(getClass().getResource("/bird-2.png")).getImage();
            topPipeImg = new ImageIcon(getClass().getResource("/pipe-up.png")).getImage();
            bottomPipeImg = new ImageIcon(getClass().getResource("/pipe-down.png")).getImage();
        } catch (Exception e) {
            System.out.println("Error loading images: " + e.getMessage());
            // Create placeholder images if files not found
            backgroundImg = createColorImage(boardWidth, boardHeight, Color.CYAN);
            birdImg = createColorImage(birdWidth, birdHeight, Color.RED);
            topPipeImg = createColorImage(pipeWidth, pipeHeight, Color.GREEN);
            bottomPipeImg = createColorImage(pipeWidth, pipeHeight, Color.GREEN);
        }

        bird = new Bird(birdImg);
        pipes = new ArrayList<>();

        placePipesTimer = new Timer(1500, e -> placePipes());
        placePipesTimer.start();

        gameLoop = new Timer(1000 / 60, this);
        gameLoop.start();
    }

    private Image createColorImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(color);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();
        return image;
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Flappy Bird");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(this);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        requestFocus();
    }

    private void sendScoreUpdate(String username, int score) {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/api/updateScore");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                String jsonInputString = "{\"username\": \"" + username + "\", \"score\": " + score + "}";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        System.out.println("Score update response: " + response.toString());
                    }
                } else {
                    System.out.println("Score update failed with response code: " + responseCode);
                }
            } catch (IOException ex) {
                System.out.println("Error updating score: " + ex.getMessage());
            }
        }).start();
    }

    private void fetchLeaderboard() {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/api/leaderboard");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine);
                        }

                        // Simple JSON parsing without external libraries
                        leaderboardData = parseJsonResponse(response.toString());

                        showLeaderboard = true;
                        repaint();

                        // Hide leaderboard after 5 seconds
                        Timer leaderboardTimer = new Timer(5000, e -> {
                            showLeaderboard = false;
                            repaint();
                        });
                        leaderboardTimer.setRepeats(false);
                        leaderboardTimer.start();
                    }
                } else {
                    System.out.println("Leaderboard fetch failed with response code: " + responseCode);
                    showErrorLeaderboard();
                }
            } catch (Exception ex) {
                System.out.println("Error fetching leaderboard: " + ex.getMessage());
                showErrorLeaderboard();
            }
        }).start();
    }

    private void showErrorLeaderboard() {
        showLeaderboard = true;
        leaderboardData = null;
        repaint();

        Timer leaderboardTimer = new Timer(5000, e -> {
            showLeaderboard = false;
            repaint();
        });
        leaderboardTimer.setRepeats(false);
        leaderboardTimer.start();
    }

    private java.util.List<String[]> parseJsonResponse(String json) {
        java.util.List<String[]> result = new ArrayList<>();

        // Remove brackets and split into objects
        String cleanJson = json.replaceAll("[\\[\\]]", "");
        if (cleanJson.isEmpty()) {
            return result;
        }

        String[] objects = cleanJson.split("\\},\\s*\\{");
        for (String obj : objects) {
            obj = obj.replaceAll("\\{", "").replaceAll("\\}", "");
            String[] pairs = obj.split(",");
            String username = null;
            String highscore = null;

            for (String pair : pairs) {
                String[] keyValue = pair.split(":");
                if (keyValue.length == 2) {
                    String key = keyValue[0].replaceAll("\"", "").trim();
                    String value = keyValue[1].replaceAll("\"", "").trim();

                    if ("username".equals(key)) {
                        username = value;
                    } else if ("highscore".equals(key)) {
                        highscore = value;
                    }
                }
            }

            if (username != null && highscore != null) {
                result.add(new String[]{username, highscore});
            }
        }

        return result;
    }

    public void placePipes() {
        int randomPipeY = (int) (pipeY - pipeHeight / 4 - Math.random() * (pipeHeight / 2));
        int openingSpace = boardHeight / 4;

        Pipe topPipe = new Pipe(topPipeImg);
        topPipe.y = randomPipeY;
        pipes.add(topPipe);

        Pipe bottomPipe = new Pipe(bottomPipeImg);
        bottomPipe.y = topPipe.y + pipeHeight + openingSpace;
        pipes.add(bottomPipe);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        if (showLeaderboard) {
            drawLeaderboard(g);
            return;
        }

        g.drawImage(backgroundImg, 0, 0, boardWidth, boardHeight, null);
        g.drawImage(bird.img, bird.x, bird.y, bird.width, bird.height, null);

        for (Pipe pipe : pipes) {
            g.drawImage(pipe.img, pipe.x, pipe.y, pipe.width, pipe.height, null);
        }

        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.PLAIN, 32));
        if (gameover) {
            g.drawString("Game Over: " + score, 10, 35);
            g.drawString("Highscore: " + highscore, 10, 70);
            g.drawString("Press SPACE to restart", 10, 105);
        } else {
            g.drawString(String.valueOf(score), 10, 35);
        }

        // Show username
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString("User: " + currentUsername, 10, boardHeight - 20);
        g.drawString("Press L for Leaderboard", 10, boardHeight - 40);
    }

    private void drawLeaderboard(Graphics g) {
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, boardWidth, boardHeight);

        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("LEADERBOARD - TOP 10", 50, 40);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 18));

        if (leaderboardData != null && !leaderboardData.isEmpty()) {
            int y = 80;
            for (int i = 0; i < Math.min(leaderboardData.size(), 10); i++) {
                String[] entry = leaderboardData.get(i);
                g.drawString((i+1) + ". " + entry[0] + " - " + entry[1], 50, y);
                y += 30;
            }
        } else {
            g.drawString("No data available", 50, 80);
            g.drawString("or connection error", 50, 110);
        }

        g.setColor(Color.CYAN);
        g.setFont(new Font("Arial", Font.ITALIC, 14));
        g.drawString("Press any key to continue", 50, boardHeight - 30);
    }

    public void move() {
        velocityY += gravity;
        bird.y += velocityY;
        bird.y = Math.max(bird.y, 0);

        for (Pipe pipe : pipes) {
            pipe.x += velocityX;

            if (!pipe.passed && bird.x > pipe.x + pipe.width) {
                pipe.passed = true;
                score++;
            }

            if (collision(bird, pipe)) {
                gameover = true;
            }
        }

        if (bird.y > boardHeight) {
            gameover = true;
        }
    }

    public boolean collision(Bird a, Pipe b) {
        return a.x < b.x + b.width &&
                a.x + a.width > b.x &&
                a.y < b.y + b.height &&
                a.y + a.height > b.y;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameover) {
            move();
        } else {
            placePipesTimer.stop();
            gameLoop.stop();
            gameOver();
        }
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (showLeaderboard) {
            showLeaderboard = false;
            repaint();
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            velocityY = -9;
            if (gameover) {
                resetGame();
            }
        } else if (e.getKeyCode() == KeyEvent.VK_L) {
            // Show leaderboard when L key is pressed
            fetchLeaderboard();
        }
    }

    private void resetGame() {
        bird.y = birdY;
        velocityY = 0;
        pipes.clear();
        score = 0;
        gameover = false;
        placePipesTimer.start();
        gameLoop.start();
    }

    @Override
    public void keyTyped(KeyEvent e) { }

    @Override
    public void keyReleased(KeyEvent e) { }

    public void gameOver() {
        if (score > highscore) {
            highscore = score;
            // Send score update to server
            sendScoreUpdate(currentUsername, highscore);
        }
        score = 0; // reset for new game
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new FlappyBird();
        });
    }
}

