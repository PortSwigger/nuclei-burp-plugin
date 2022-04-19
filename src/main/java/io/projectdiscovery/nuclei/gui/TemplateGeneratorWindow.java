package io.projectdiscovery.nuclei.gui;

import io.projectdiscovery.nuclei.util.SchemaUtils;
import io.projectdiscovery.utils.gui.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class TemplateGeneratorWindow extends JFrame implements TemplateGeneratorTabContainer {

    private static TemplateGeneratorWindow instance;

    private final TemplateGeneratorTabbedPane tabbedPane;

    private TemplateGeneratorWindow(GeneralSettings generalSettings) {
        super("Nuclei Template Generator");
        this.setLayout(new BorderLayout());

        this.tabbedPane = new TemplateGeneratorTabbedPane();
        this.tabbedPane.addChangeListener(e -> {
            if (((JTabbedPane) e.getSource()).getTabCount() == 0) {
                new CloseAction(TemplateGeneratorWindow.this).actionPerformed(null);
            }
        });
        this.add(this.tabbedPane);

        setKeyboardShortcuts(this.tabbedPane, this.rootPane, generalSettings::logError);
        cleanupOnClose();

        this.setJMenuBar(new MenuHelper(this.generalSettings::logError).createMenuBar());
        this.setPreferredSize(new Dimension(800, 600));
        this.setMinimumSize(this.getSize()); // TODO this is platform dependent, custom logic is needed to enforce it
        this.pack();
        this.setLocationRelativeTo(null); // center of the screen
        this.setVisible(true);
    }

    public static TemplateGeneratorWindow getInstance(GeneralSettings generalSettings) {
        if (instance == null) {
            instance = new TemplateGeneratorWindow(generalSettings);
        }

        return instance;
    }

    @Override
    public void addTab(TemplateGeneratorTab templateGeneratorTab) {
        this.tabbedPane.addTab(templateGeneratorTab);
    }

    @Override
    public JComponent getContainer() {
        return this.rootPane;
    }

    @Override
    public List<TemplateGeneratorTab> getTabs() {
        return this.tabbedPane.getTabs();
    }

    @Override
    public Optional<TemplateGeneratorTab> getTab(String tabName) {
        return this.tabbedPane.getTab(tabName);
    }

    private void cleanupOnClose() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                getTabs().forEach(TemplateGeneratorTab::cleanup);
                TemplateGeneratorWindow.this.tabbedPane.removeAll();
                instance = null;
                super.windowClosing(e);
            }
        });
    }

    public void setKeyboardShortcuts(TemplateGeneratorTabbedPane tabbedPane, JComponent parentComponent, Consumer<String> errorMessageConsumer) {
        SwingUtils.setKeyboardShortcut(parentComponent, KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK), new CloseAction(this));

        SwingUtils.setTabSupportKeyboardShortcuts(tabbedPane, parentComponent);

        SwingUtils.setKeyboardShortcut(parentComponent, KeyEvent.VK_F1, () -> MenuHelper.openDocumentationLink(errorMessageConsumer));
    }

    private static class CloseAction extends AbstractAction {
        private final JFrame frame;

        public CloseAction(JFrame frame) {
            this.frame = frame;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            this.frame.dispatchEvent(new WindowEvent(this.frame, WindowEvent.WINDOW_CLOSING));
        }
    }

    public static void main(String[] args) throws Exception {
        final URL url = new URL("http://localhost:8081");

        final String template = "id: template-id\n" +
                                "info:\n" +
                                "  author: forgedhallpass\n" +
                                "  name: Template Name\n" +
                                "  severity: info\n" +
                                "requests:\n" +
                                "  - raw:\n" +
                                "    - |\n" +
                                "      GET / HTTP/1.1\n" +
                                "      Host: {{Hostname}}\n" +
                                "      Accept: */*\n" +
                                "    matchers:\n" +
                                "    - type: status\n" +
                                "      status:\n" +
                                "      - 200\n";

        final GeneralSettings generalSettings = new GeneralSettings.Builder().build();
        final NucleiGeneratorSettings nucleiGeneratorSettings = new NucleiGeneratorSettings.Builder(generalSettings, url, template)
                .withYamlFieldDescriptionMap(SchemaUtils.retrieveYamlFieldWithDescriptions())
                .build();

        final TemplateGeneratorWindow templateGeneratorWindow = new TemplateGeneratorWindow(generalSettings);
        templateGeneratorWindow.addTab(new TemplateGeneratorTab(nucleiGeneratorSettings));
        templateGeneratorWindow.addTab(new TemplateGeneratorTab("Custom name", nucleiGeneratorSettings));
        templateGeneratorWindow.addTab(new TemplateGeneratorTab(nucleiGeneratorSettings));
        templateGeneratorWindow.addTab(new TemplateGeneratorTab("Another custom name", nucleiGeneratorSettings));
        templateGeneratorWindow.addTab(new TemplateGeneratorTab(nucleiGeneratorSettings));

        templateGeneratorWindow.setDefaultCloseOperation(EXIT_ON_CLOSE);
    }
}
