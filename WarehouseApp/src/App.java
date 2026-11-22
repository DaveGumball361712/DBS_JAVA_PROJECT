import java.awt.EventQueue;

import view.LoginFrame;

public class App {
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}