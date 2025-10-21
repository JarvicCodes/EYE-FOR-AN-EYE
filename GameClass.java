import javax.swing.*;
import java.awt.*;

public class GameClass extends JPanel {
    public GameClass() {
        setPreferredSize(new Dimension(960, 540));
        setLayout(new BorderLayout());

        // Create a background label
        JLabel background = new JLabel();
        background.setIcon(new ImageIcon("casinobackground.png"));
        Dimension size = background.getPreferredSize(); //Gets the size of the image
        background.setBounds(50, 30, size.width, size.height); //Sets the location of the image
        background.setLayout(new BorderLayout()); // allow adding children

        // Add your content *to the background label*
        JLabel label = new JLabel("Game Started!", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 32));
        label.setForeground(Color.BLACK);
        background.add(label, BorderLayout.CENTER);

        add(background);
    }
}
