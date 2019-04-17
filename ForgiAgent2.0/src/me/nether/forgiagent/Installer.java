package me.nether.forgiagent;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.tools.attach.VirtualMachine;
import me.nether.forgiagent.utils.FileUtils;
import me.nether.forgiagent.utils.ImagePanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Installer {

    /**
     * Installer instance since this is a singleton
     */
    public static Installer instance;

    /**
     * Used to be sure every component is loaded correctly within different threads
     */
    private boolean finishedLoading;

    /**
     * Maps the MC profiles as name - content
     */
    public Map<String, String> profiles;

    /**
     * MC installation directory
     */
    private File installDirectory;

    private boolean showInstaller;

    /**
     * JSwing components
     */

    /*____________________________________________________*/

    public JFrame frame;
    public ImagePanel logo, logo2;

    private JTextField mc_directory;
    public JTextField dedicatedRam, modpack_directory;

    public JTextPane console;

    private JButton buttonInstall;

    public JProgressBar progressBar;

    public JLabel downloadStatus;

    public Checkbox autoHide;
    public JCheckBox errors_type;

    /*____________________________________________________*/


    /**
     * Launches the application.
     */
    public void loadWindow() {
        EventQueue.invokeLater(() -> {
            Installer.instance.frame.setVisible(true);

            new Thread(() -> {
                while (!Installer.instance.finishedLoading) ;

                while (true) {
                    Installer.instance.logo.update(Installer.instance.frame.getGraphics());
                    Installer.instance.logo2.update(Installer.instance.frame.getGraphics());

                    Installer.instance.buttonInstall.setEnabled(Installer.instance.modpack_directory.getText().trim().length() > 0);

                    if (!Installer.instance.showInstaller) {
                        Installer.instance.progressBar.setVisible(true);
                        Installer.instance.downloadStatus.setVisible(true);
                    }

                    Installer.instance.progressBar.setForeground(ImagePanel.makeColorGradient(
                            System.currentTimeMillis(),
                            0.012,
                            0, 2, 4,
                            125,
                            100,
                            0));

                    Installer.instance.progressBar.setBorder(new LineBorder(new Color(66, 66, 66)));
                }
            }).start();

            ForgiaAgent.log("Application loaded correctly!");
        });
    }

    /**
     * Creates the application.
     **/
    public Installer(boolean showInstaller) {
        this.showInstaller = showInstaller;
        initialize();

        finishedLoading = true;
    }


    /**
     * Initializes the contents of the frame.
     **/
    private void initialize() {
        installDirectory = FileUtils.getWorkingDirectory();

        JComponent array[] = this.setupFrame();

        JPanel mainPanel = (JPanel) array[0];
        JTabbedPane tabbedPane = (JTabbedPane) array[1];

        this.setupLogo(mainPanel);

        this.setupCredits(mainPanel);

        this.setupInstallButton(mainPanel);

        this.setupConsole(tabbedPane, mainPanel);

        this.setupDirectoryChoosers(mainPanel);

        this.setupRam(mainPanel);
    }

    /**
     * Used to get a button that spawns a fileChooser
     **/
    private JButton getFileChooserButton(JTextField tf, Function<File, ?> f) {
        JButton fileChooser = new JButton("...");
        fileChooser.addActionListener((e) -> {
            JFileChooser chooser = new JFileChooser(new File(tf.getText()));
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnValue = chooser.showOpenDialog(null);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                f.apply(chooser.getSelectedFile());
            }
        });
        return fileChooser;
    }

    private JButton getInstallationDirectoryButton() {
        return getFileChooserButton(mc_directory, (file) -> {
            mc_directory.setText(file.getAbsolutePath());
            Map<String, String> profiles = getProfiles(file);

            ForgiaAgent.log("Logging profiles...");
            profiles.forEach((s, s2) -> System.out.println(s + ": " + s2));

            if (profiles.size() == 0) {
                try {
                    FileUtils.readFileLines(new File(file, "launcher_profiles.json")).forEach(s -> System.out.println(s));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                JOptionPane.showMessageDialog(frame, Installer.instance.errors_type.isSelected() ?
                        "No profiles were found in this folder. Are you sure it is the Minecraft installation folder?" :
                        "Figa ma vedi di selezionare la cartella di Minecraft corretta che non ho trovato proprio un cazzo di profilo");
            }
            installDirectory = file;
            return null;
        });
    }

    private JButton getModPackFolderButton() {
        return getFileChooserButton(modpack_directory.getText().trim().length() != 0 ?
                        modpack_directory :
                        new JTextField(System.getProperty("user.home") + "/Desktop"),

                (file) -> {
                    modpack_directory.setText(file.getAbsolutePath());
                    JOptionPane.showMessageDialog(frame, Installer.instance.errors_type.isSelected() ?
                            String.format("The mod pack will be installed in: '%s'", file.getAbsolutePath()) :
                            String.format("Ti installo tutte le schifezze moddatose qua dentro: '%s'", file.getAbsolutePath()));
                    return null;
                });
    }

    private JComponent[] setupFrame() {
        // enable anti-aliasing
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        frame = new JFrame("Forgia Installer");

        try {
            frame.setIconImage(ImageIO.read(this.getClass().getResource("assets/logo2.png")));
        } catch (Exception ex) {
            ForgiaAgent.log("Could not find logo image");
        }

        frame.setBounds((Toolkit.getDefaultToolkit().getScreenSize().width - 438) / 2, (Toolkit.getDefaultToolkit().getScreenSize().height - 323) / 2, 438, showInstaller ? 340 : 390);

        if (showInstaller)
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        else
            frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        frame.getContentPane().setLayout(null);
        frame.setResizable(false);

        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setBounds(0, 0, 432, 400);
        frame.getContentPane().add(tabbedPane);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(null);

        if (showInstaller)
            tabbedPane.addTab("Installer", null, mainPanel, null);

        errors_type = new JCheckBox("Show serious error messages");
        errors_type.setBounds(95, 61, 303, 25);
        mainPanel.add(errors_type);

        return new JComponent[]{mainPanel, tabbedPane};
    }

    private void setupLogo(JPanel mainPanel) {
        JTextPane logoText = new JTextPane();
        logoText.setBounds(99, 0, 316, 40);
        mainPanel.add(logoText);
        logoText.setBackground(SystemColor.control);
        logoText.setEditable(false);
        logoText.setFocusable(false);
        logoText.setFont(new Font("Roboto", Font.BOLD | Font.ITALIC, 30));
        logoText.setText(" FORGIA MOD PACK\r\n");

        logo = new ImagePanel();
        try {
            logo.setBackground(ImageIO.read(this.getClass().getResource("assets/logo.png")));
        } catch (IOException e1) {
        }
        logo.setBounds(12, 11, 75, 75);
        mainPanel.add(logo);
    }

    private void setupCredits(JPanel mainPanel) {
        JTextField credits = new JTextField();
        credits.setFont(new Font("Roboto", Font.PLAIN, 11));
        credits.setEditable(false);
        credits.setBackground(SystemColor.control);
        credits.setForeground(SystemColor.desktop);
        credits.setBorder(null);
        credits.setHorizontalAlignment(SwingConstants.CENTER);
        credits.setText("made by NetherSoul ");
        credits.setBounds(276, 37, 139, 24);
        credits.setColumns(10);

        mainPanel.add(credits);
    }

    private void setupInstallButton(JPanel mainPanel) {
        buttonInstall = new JButton("Install");
        buttonInstall.setBounds(11, 155, 204, 49);
        buttonInstall.setFont(new Font("Roboto", Font.PLAIN, 16));

        buttonInstall.addActionListener((e) -> {
            ForgiaAgent.log("Trying to install the agent...");
            try {
                boolean success = ForgiaAgent.install(Installer.instance.installDirectory);

                if (success) {
                    JOptionPane.showMessageDialog(frame, Installer.instance.errors_type.isSelected() ?
                            "The selected profile has now been configured to keep the mods up to date." :
                            "Ti piace farti installare la roba automaticamente? E' andato tutto per il verso giusto: qua il link di paypal per ringraziarmi");
                    ForgiaAgent.log("Finished setup.");
                }
            } catch (Exception ex) {
                ex.printStackTrace();

                JOptionPane.showMessageDialog(frame, Installer.instance.errors_type.isSelected() ?
                        "Couldn't setup the profile correctly." :
                        "Qualcosa e' andato storto, chiedi a quel coglione del developer");
            }
        });

        mainPanel.add(buttonInstall);
    }

    private void setupConsole(JTabbedPane tabbedPane, JPanel mainPanel) {
        JPanel console_panel = new JPanel();
        tabbedPane.addTab("Console", null, console_panel, null);
        console_panel.setLayout(null);

        logo2 = new ImagePanel();
        try {
            logo2.setBackground(ImageIO.read(this.getClass().getResource("assets/logo.png")));
        } catch (IOException e1) {
        }
        logo2.setBounds(10, 5, 49, 49);
        console_panel.add(logo2);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(10, 64, 407, 191);
        console_panel.add(scrollPane);

        console = new JTextPane();
        scrollPane.setViewportView(console);
        console.setEditable(false);

        JTextPane title = new JTextPane();
        title.setFont(new Font("Roboto", Font.BOLD | Font.ITALIC, 22));
        title.setBackground(SystemColor.control);
        title.setForeground(new Color(0, 0, 0));
        title.setText("FORGIA MOD PACK");
        title.setEditable(false);
        title.setFocusable(false);
        title.setBounds(69, 11, 202, 32);
        console_panel.add(title);

        autoHide = new Checkbox("Automatically hide");
        autoHide.setState(true);
        autoHide.setBounds(277, 32, 140, 22);
        console_panel.add(autoHide);

        if (!this.showInstaller) {
            JButton hideConsole = new JButton("Hide console");
            hideConsole.addActionListener((e) -> frame.setVisible(false));
            hideConsole.setBounds(281, 5, 136, 23);

            //TODO: Fix force close not working
            JButton forceClose = new JButton("Force Close");
            forceClose.addActionListener(e -> {

                VirtualMachine.list().stream().filter(vm -> vm.displayName().contains("minecraft")).forEach(vm -> {
                    System.out.println(vm.displayName());
                });
            });

            forceClose.setVisible(true);
            forceClose.setBounds(281, 31, 136, 23);
            console_panel.add(forceClose);
            console_panel.add(hideConsole);
        }

        //progress bar
        progressBar = new JProgressBar();
        progressBar.setBounds(10, 295, 412, 24);
        progressBar.setVisible(true);
        if (this.showInstaller) mainPanel.add(progressBar);
        else console_panel.add(progressBar);

        //download informations
        downloadStatus = new JLabel("");
        downloadStatus.setFont(new Font("Roboto", Font.PLAIN, 13));
        downloadStatus.setBackground(SystemColor.scrollbar);
        downloadStatus.setBounds(10, 275, 412, 14);
        downloadStatus.setVisible(true);
        if (this.showInstaller) mainPanel.add(downloadStatus);
        else console_panel.add(downloadStatus);
    }

    private void setupRam(JPanel mainPanel) {
        Label labelRam = new Label("Select RAM (4GB recomm.)");
        labelRam.setFont(new Font("Roboto", Font.BOLD, 11));
        labelRam.setBackground(SystemColor.scrollbar);
        labelRam.setAlignment(Label.CENTER);
        labelRam.setBounds(221, 160, 194, 24);
        mainPanel.add(labelRam);

        JLabel lblMb = new JLabel("MB");
        lblMb.setHorizontalAlignment(SwingConstants.LEFT);
        lblMb.setBounds(391, 188, 23, 20);
        mainPanel.add(lblMb);

        dedicatedRam = new JTextField();
        dedicatedRam.setToolTipText("Amount of dedicated RAM");
        dedicatedRam.setHorizontalAlignment(SwingConstants.CENTER);
        dedicatedRam.setFont(new Font("Roboto", Font.BOLD, 13));
        dedicatedRam.setText("4096");
        dedicatedRam.setBounds(225, 188, 150, 20);
        mainPanel.add(dedicatedRam);
        dedicatedRam.setColumns(10);
    }

    private void setupDirectoryChoosers(JPanel mainPanel) {
        //FILE CHOOSER 1
        mc_directory = new JTextField(FileUtils.getWorkingDirectory().getAbsolutePath());
        mc_directory.setFont(new Font("Roboto", Font.BOLD, 13));
        mc_directory.setHorizontalAlignment(SwingConstants.CENTER);
        mc_directory.setBounds(10, 122, 365, 25);
        mc_directory.setColumns(10);
        mainPanel.add(mc_directory);

        Label label_2 = new Label("Select Minecraft installation folder");
        label_2.setFont(new Font("Roboto", Font.BOLD, 13));
        label_2.setBackground(SystemColor.scrollbar);
        label_2.setAlignment(Label.CENTER);
        label_2.setBounds(10, 92, 403, 24);
        mainPanel.add(label_2);

        JButton fileChooser;
        mainPanel.add(fileChooser = getInstallationDirectoryButton());
        fileChooser.setBounds(382, 122, 33, 25);

        //FILECHOOSER 2
        modpack_directory = new JTextField();
        modpack_directory.setHorizontalAlignment(SwingConstants.CENTER);
        modpack_directory.setFont(new Font("Roboto", Font.BOLD, 13));
        modpack_directory.setColumns(10);
        modpack_directory.setBounds(11, 244, 365, 25);
        mainPanel.add(modpack_directory);

        JButton modpackDirectoryChooser;
        mainPanel.add(modpackDirectoryChooser = getModPackFolderButton());
        modpackDirectoryChooser.setBounds(383, 244, 33, 25);

        Label label = new Label("Select ModPack installation folder");
        label.setFont(new Font("Roboto", Font.BOLD, 13));
        label.setBackground(Color.RED);
        label.setAlignment(Label.CENTER);
        label.setBounds(11, 214, 403, 24);
        mainPanel.add(label);
    }

    public Map<String, String> getProfiles(File currentDir) {
        Map<String, String> profileList = new LinkedHashMap<>();
        File profiles_json = new File(currentDir, "launcher_profiles.json");

        try {
            JsonParser p = new JsonParser();
            JsonObject main = p.parse(new FileReader(profiles_json)).getAsJsonObject();
            JsonObject profiles = main.get("profiles").getAsJsonObject();
            profiles.entrySet().iterator().forEachRemaining(s -> {
                try {
                    profileList.put(s.getValue().getAsJsonObject().get("name").getAsString(), s.getKey());
                } catch (Exception ex) {
                    profileList.put(s.getKey(), s.getKey());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (profileList.size() == 0) {
            ForgiaAgent.log("No profiles found in this folder...");
        } else {
            Map<String, String> orderedList = new LinkedHashMap<>();

            profileList.entrySet().stream()
                    .sorted((s1, s2) -> s1.getKey().contains("forge") ? s2.getKey().contains("forge") ? 0 : -1 : 1)
                    .forEach(e -> orderedList.put(e.getKey(), e.getValue()));

            return orderedList;
        }

        return profileList;
    }
}