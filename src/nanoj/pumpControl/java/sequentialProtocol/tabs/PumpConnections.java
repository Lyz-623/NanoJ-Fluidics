package nanoj.pumpControl.java.sequentialProtocol.tabs;

import gnu.io.NRSerialPort; //串口通信包
import nanoj.pumpControl.java.pumps.ConnectedSubPump;
import nanoj.pumpControl.java.pumps.Pump;
import nanoj.pumpControl.java.pumps.PumpManager;
import nanoj.pumpControl.java.sequentialProtocol.GUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.prefs.Preferences;

public class PumpConnections extends JPanel {
    //版本号
    JLabel version = new JLabel("version: 2022/10/14");

    private Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
    private PumpManager pumpManager = PumpManager.INSTANCE;
    private GUI gui;
    public String name = "Pump Connections";

    private static final String VIRTUAL_PUMP = "Virtual Pump";
    private static final String VIRTUAL_PORT = "Virtual Port";
    private static final String PORT = "com";
    private static final String PUMP = "pump";

    //pump选择
    JLabel pumpListLabel = new JLabel("Pump type");
    JComboBox availablePumpsList;

    //串口选择
    JLabel connectLabel = new JLabel("Serial port");
    JComboBox portsList;

    //pump连接+断开连接按钮
    JButton connectButton = new JButton("Connect");
    JButton disconnectButton = new JButton("Disconnect");

    //当前连接的pump列表
    JLabel connectedPumpsLabel = new JLabel("List of currently connected pumps");
    private JTable connectedPumpsTable;
    private ConnectionsTable connectedPumpsTableModel;
    JScrollPane connectedPumpsListPane;

    public PumpConnections(GUI gui) {
        super(); //当前父类
        this.gui = gui;

        connectedPumpsTableModel = new ConnectionsTable();
        connectedPumpsTable = new JTable(connectedPumpsTableModel);
        connectedPumpsListPane = new JScrollPane(connectedPumpsTable);

        availablePumpsList = new JComboBox(pumpManager.getAvailablePumpsList()); //获取可连接的pump
        availablePumpsList.setSelectedItem(prefs.get(PUMP, VIRTUAL_PUMP));

        portsList = new JComboBox(new Vector(NRSerialPort.getAvailableSerialPorts()));
        portsList.addItem(VIRTUAL_PORT);
        portsList.setSelectedItem(prefs.get(PORT, VIRTUAL_PORT));

        setLayout( new PumpConnectionsLayout(this) ); //布局

        //点击操作监视器
        connectButton.addActionListener(new PumpConnections.Connect());
        disconnectButton.addActionListener(new PumpConnections.Disconnect());

        //设置字体
        Font newFont = new Font("Time New Roman",Font.PLAIN,12);
        pumpListLabel.setFont(newFont);
        connectLabel.setFont(newFont);
        connectButton.setFont(newFont);
        disconnectButton.setFont(newFont);
        connectedPumpsLabel.setFont(newFont);
        version.setFont(newFont);
        availablePumpsList.setFont(newFont);
        portsList.setFont(newFont);
        connectedPumpsTable.setFont(newFont);
        connectedPumpsListPane.setFont(newFont);
    }

    public void rememberSettings() {
        prefs.put(PORT, (String) portsList.getSelectedItem());
        prefs.put(PUMP, (String) availablePumpsList.getSelectedItem());
    }

    private class Connect implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String port = (String) portsList.getSelectedItem();
            prefs.put(PORT, port);
            try {
                String isItConnected = pumpManager.connect((String) availablePumpsList.getSelectedItem(), port);
                if (isItConnected.equals(PumpManager.PORT_ALREADY_CONNECTED)) {
                    gui.log.message("Tried to connect to " +
                            availablePumpsList.getSelectedItem() + " on port " +
                            port + ", but that port is already in use!");
                    return;
                } else if (isItConnected.equals(Pump.FAILED_TO_CONNECT)) {
                    gui.log.message("Tried to connect to " +
                            availablePumpsList.getSelectedItem() + " on port " +
                            port + ", but there was an error!");
                    return;
                }

                connectedPumpsTableModel.setRowCount(0);

                for (ConnectedSubPump subPump: pumpManager.getConnectedPumpsList())
                    connectedPumpsTableModel.addRow(subPump.asConnectionArray());

                gui.log.message("Connected to " + pumpManager.getAvailablePumpsList()[availablePumpsList.getSelectedIndex()] +
                        " on port " + port);
            } catch (Exception e1) {
                gui.log.message("Error, can not connect, check core log.");
                e1.printStackTrace();
            }
        }
    }

    private class Disconnect implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean success = false;
            String port = "Undefined";
            String name = "Undefined";
            try {
                port = (String) portsList.getSelectedItem();
                name = pumpManager.getPumpNameOnPort(port);
                success = pumpManager.disconnect(port);
            } catch (Exception e1) {
                gui.log.message("Error, failed to disconnect properly.");
                e1.printStackTrace();
            }
            if (success) {
                connectedPumpsTableModel.setRowCount(0);

                for (ConnectedSubPump subPump: pumpManager.getConnectedPumpsList())
                    connectedPumpsTableModel.addRow(subPump.asConnectionArray());

                gui.log.message("Disconnected from " + name + " on port " + port + ".");
            }
        }
    }

    public class ConnectionsTable extends DefaultTableModel {

        protected ConnectionsTable() {
            super(
                    new String[][]{{PumpManager.NO_PUMP_CONNECTED,"",""}},
                    new String[]{"Pump","Sub-Pump","COM port"});
        }

        public boolean isCellEditable(int row, int column){
            return false;
        }

    }
}
