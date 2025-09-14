import javax.swing.*;

public class Main {
    public static void main(String[] args)throws Exception{
        int boardHeight = 640;
        int boardWidth = 360;

        JFrame frame = new JFrame("Flappy Bird");
        frame.setSize(boardWidth,boardHeight);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        FlappyBird flappybird = new FlappyBird();
        frame.add(flappybird);
        frame.pack();
        flappybird.requestFocus();
        frame.setVisible(true);
    }
}