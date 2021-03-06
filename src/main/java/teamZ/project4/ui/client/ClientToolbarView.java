package teamZ.project4.ui.client;

import teamZ.project4.constants.ColorConstants;
import teamZ.project4.constants.TextConstants;
import teamZ.project4.controllers.client.ClientToolbarController;
import teamZ.project4.listeners.ClientListener;
import teamZ.project4.model.client.ClientModel;
import teamZ.project4.model.server.ServerModel;
import teamZ.project4.ui.server.ServerView;
import teamZ.project4.util.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * UI toolbar for the ClientView
 * @author  David Henderson (dchende2@asu.edu)
 */
public class ClientToolbarView extends JMenuBar {
    private JMenu menu;
    private JMenuItem menuItemStateChange;
    private JButton buttonStatus;
    private JLabel textTime;
    private ClientToolbarController controller;

    /**
     * Constructor for ClientToolbarView, the toolbar at the top of the ClientView
     */
    public ClientToolbarView() {

        controller = new ClientToolbarController(this);

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            // If CONTROL + SHIFT + Q, open server
            if (e.isShiftDown() && e.getKeyCode() == 81 && e.getID() == KeyEvent.KEY_RELEASED) {
                openServerPanel();
                return true;
            }
            return false;
        });

        ClientModel.get().addListener(new ClientListener() {
            @Override
            public void valuesChanged() {
                textTime.setText(" " + (ClientModel.get().getNewestPacket() == null ? "0.0" : Float.toString(ClientModel.get().getNewestPacket().getTick())));
            }

            @Override
            public void valuesReset() { }

            @Override
            public void valuesAdded() { }

            @Override
            public void started() {
                menuItemStateChange.setText("Disconnect from server");
            }

            @Override
            public void shutdown() {
                menuItemStateChange.setText("Connect to server");
            }
        });

        JMenuItem menuItem;

        this.add(Box.createHorizontalStrut(8));

        menu = new JMenu(Character.toString((char) 0x2630));
        menu.setFont(TextConstants.LARGE_FONT);
        this.add(menu);

        menuItemStateChange = new JMenuItem(ClientModel.get().isConnected() ? "Disconnect from server" : "Connect to server");
        menuItemStateChange.addActionListener(e -> controller.handleConnectToServer());
        menu.add(menuItemStateChange);

        menuItem = new JMenuItem("Change server host/port");
        menuItem.addActionListener(e -> controller.displayChangeHostDialog());
        menu.add(menuItem);

        menu.addSeparator();

        menuItem = new JMenuItem("Open composer (server)");
        menuItem.addActionListener(e -> {
            openServerPanel();
        });
        menu.add(menuItem);

        this.add(Box.createHorizontalStrut(32));

        JLabel promptStatus = new JLabel("Status:");
        promptStatus.setFont(TextConstants.DEFAULT_FONT);
        this.add(promptStatus);

        buttonStatus = new JButton(Character.toString((char) 0x2022));
        buttonStatus.setFont(new Font("Monospaced", Font.PLAIN, TextConstants.DEFAULT_FONT.getSize() * 2));
        buttonStatus.setOpaque(false);
        buttonStatus.setContentAreaFilled(false);
        buttonStatus.setBorderPainted(false);
        buttonStatus.setFocusPainted(false);
        buttonStatus.setForeground(ColorConstants.INDICATOR_OFF);
        buttonStatus.addActionListener(e -> controller.handleConnectToServer());
        this.add(buttonStatus);

        // Create a timer to blink if on or off
        Timer timer = new Timer(1000, e2 -> {
            if(ClientModel.get().isConnected()) {
                if(buttonStatus.getForeground() == ColorConstants.INDICATOR_ON_DIM) {
                    buttonStatus.setForeground(ColorConstants.INDICATOR_ON_BRIGHT);
                } else {
                    buttonStatus.setForeground(ColorConstants.INDICATOR_ON_DIM);
                }
            } else {
                buttonStatus.setForeground(ColorConstants.INDICATOR_OFF);
            }
        });
        timer.start();

        this.add(Box.createHorizontalStrut(32));

        textTime = new JLabel(" " + (ClientModel.get().getNewestPacket() == null ? "0.0" : Float.toString(ClientModel.get().getNewestPacket().getTick())));
        textTime.setIcon(new ImageIcon("src/main/resources/team04/project3/images/clock.jpeg"));
        textTime.setForeground(Color.BLACK);
        textTime.setFont(TextConstants.LARGE_FONT);
        this.add(textTime);
    }

    /**
     * Opens the server composer UI, if not open, otherwise refocuses it. Attempts connection if starting up server.
     */
    public void openServerPanel() {
        if(ServerView.getInstance().isDisplayable()) {
            ServerView.getInstance().toFront();
            ServerView.getInstance().repaint();
        } else {
            ServerView.getInstance().init();
            int x = ClientView.getInstance().getX() - (ClientView.getInstance().getWidth() / 2) - (ServerView.getInstance().getWidth() / 2);
            if (x < ServerView.getInstance().getWidth() / 2) {
                x = ClientView.getInstance().getX() + (ClientView.getInstance().getWidth() / 2) + (ServerView.getInstance().getWidth() / 2);
            }
            if(x > Toolkit.getDefaultToolkit().getScreenSize().width - ServerView.getInstance().getWidth() / 2) {
                x = ClientView.getInstance().getX();
            }

            ServerView.getInstance().setLocation(
                    x, ClientView.getInstance().getLocation().getLocation().y
            );
        }

        // Try to connect the client in a moment
        new Thread(() -> {
            try {
                long timeout = System.currentTimeMillis() + 2000L;
                while(System.currentTimeMillis() < timeout && !ServerModel.get().isRunning()) {
                    Thread.sleep(100L);
                }
            } catch(InterruptedException e) {
                Log.w("Failed to wait while server starts up (" + e.getMessage() + ")", ClientToolbarView.class);
            }

            if(!ClientModel.get().isRunning())
                ClientModel.get().start();
        }).start();
    }
}
