package com.DMHelper.basic.menus;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;

public final class Ui_Theme {
    public static final Color APP_BACKGROUND = new Color(241, 234, 218);
    public static final Color PANEL_SURFACE = new Color(250, 246, 237);
    public static final Color PANEL_ELEVATED = new Color(255, 251, 244);
    public static final Color ACCENT_PRIMARY = new Color(52, 83, 66);
    public static final Color ACCENT_SECONDARY = new Color(151, 112, 57);
    public static final Color BORDER_COLOR = new Color(183, 166, 133);
    public static final Color TEXT_PRIMARY = new Color(49, 39, 28);
    public static final Color TEXT_MUTED = new Color(105, 93, 74);
    public static final Color LIST_SELECTION = new Color(218, 228, 207);

    private static final Font UI_FONT_PLAIN = new Font("Microsoft YaHei UI", Font.PLAIN, 13);
    private static final Font UI_FONT_BOLD = new Font("Microsoft YaHei UI", Font.BOLD, 13);
    private static final Font TITLE_FONT = new Font("Microsoft YaHei UI", Font.BOLD, 15);
    private static boolean installed;

    private Ui_Theme() {
    }

    public static void install_global_theme() {
        if (installed) {
            return;
        }
        installed = true;

        UIManager.put("Panel.background", APP_BACKGROUND);
        UIManager.put("Label.foreground", TEXT_PRIMARY);
        UIManager.put("Label.font", UI_FONT_PLAIN);
        UIManager.put("Button.font", UI_FONT_BOLD);
        UIManager.put("Button.background", new Color(230, 221, 204));
        UIManager.put("Button.foreground", TEXT_PRIMARY);
        UIManager.put("Button.disabledText", TEXT_MUTED);
        UIManager.put("Button.focus", APP_BACKGROUND);
        UIManager.put("Button.select", new Color(214, 205, 188));
        UIManager.put("ToggleButton.font", UI_FONT_PLAIN);
        UIManager.put("ComboBox.font", UI_FONT_PLAIN);
        UIManager.put("ComboBox.background", PANEL_ELEVATED);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("TextField.font", UI_FONT_PLAIN);
        UIManager.put("TextField.background", PANEL_ELEVATED);
        UIManager.put("TextField.foreground", TEXT_PRIMARY);
        UIManager.put("FormattedTextField.font", UI_FONT_PLAIN);
        UIManager.put("FormattedTextField.background", PANEL_ELEVATED);
        UIManager.put("Spinner.font", UI_FONT_PLAIN);
        UIManager.put("TextArea.font", UI_FONT_PLAIN);
        UIManager.put("TextArea.background", PANEL_ELEVATED);
        UIManager.put("TextArea.foreground", TEXT_PRIMARY);
        UIManager.put("List.font", UI_FONT_PLAIN);
        UIManager.put("List.background", PANEL_ELEVATED);
        UIManager.put("List.foreground", TEXT_PRIMARY);
        UIManager.put("List.selectionBackground", LIST_SELECTION);
        UIManager.put("List.selectionForeground", TEXT_PRIMARY);
        UIManager.put("TabbedPane.font", UI_FONT_BOLD);
        UIManager.put("TabbedPane.background", PANEL_SURFACE);
        UIManager.put("TabbedPane.foreground", TEXT_PRIMARY);
        UIManager.put("TabbedPane.selected", PANEL_ELEVATED);
        UIManager.put("OptionPane.background", PANEL_SURFACE);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("OptionPane.foreground", TEXT_PRIMARY);
        UIManager.put("OptionPane.buttonAreaBorder", BorderFactory.createEmptyBorder(8, 0, 0, 0));
        UIManager.put("ScrollPane.background", PANEL_ELEVATED);
        UIManager.put("Viewport.background", PANEL_ELEVATED);
    }

    public static void apply_window(Window window) {
        if (window == null) {
            return;
        }
        if (window instanceof JFrame) {
            JFrame frame = (JFrame) window;
            if (frame.getContentPane() != null) {
                frame.getContentPane().setBackground(APP_BACKGROUND);
                style_component_tree(frame.getContentPane());
            }
        } else if (window instanceof JDialog) {
            JDialog dialog = (JDialog) window;
            if (dialog.getContentPane() != null) {
                dialog.getContentPane().setBackground(APP_BACKGROUND);
                style_component_tree(dialog.getContentPane());
            }
        } else {
            style_component_tree(window);
        }
    }

    public static void style_component_tree(Component component) {
        if (component == null) {
            return;
        }

        if (component instanceof JPanel) {
            style_panel((JPanel) component);
        } else if (component instanceof AbstractButton) {
            style_button((AbstractButton) component);
        } else if (component instanceof JTextArea) {
            style_text_area((JTextArea) component);
        } else if (component instanceof JTextField) {
            style_text_field((JTextField) component);
        } else if (component instanceof JComboBox) {
            style_combo_box((JComboBox<?>) component);
        } else if (component instanceof JSpinner) {
            style_spinner((JSpinner) component);
        } else if (component instanceof JList) {
            style_list((JList<?>) component);
        } else if (component instanceof JScrollPane) {
            style_scroll_pane((JScrollPane) component);
        } else if (component instanceof JTabbedPane) {
            style_tabbed_pane((JTabbedPane) component);
        } else if (component instanceof JLabel) {
            style_label((JLabel) component);
        } else if (component instanceof JSplitPane) {
            component.setBackground(APP_BACKGROUND);
        } else if (component instanceof JViewport) {
            component.setBackground(PANEL_ELEVATED);
        }

        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                style_component_tree(child);
            }
        }
    }

    public static void style_primary_button(AbstractButton button) {
        if (button == null) {
            return;
        }
        button.setFont(UI_FONT_BOLD);
        button.setBackground(ACCENT_PRIMARY);
        button.setForeground(new Color(252, 247, 238));
        button.getModel().setRollover(false);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_PRIMARY.darker(), 1, true),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
    }

    public static void style_secondary_button(AbstractButton button) {
        style_button(button);
    }

    public static Border create_section_border(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                        BorderFactory.createEmptyBorder(8, 8, 8, 8)
                ),
                title,
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                TITLE_FONT,
                ACCENT_PRIMARY
        );
    }

    public static JScrollPane wrap_scroll(JComponent component) {
        JScrollPane scrollPane = new JScrollPane(component);
        style_scroll_pane(scrollPane);
        return scrollPane;
    }

    private static void style_panel(JPanel panel) {
        panel.setBackground(PANEL_SURFACE);
        Border border = panel.getBorder();
        if (border instanceof TitledBorder) {
            TitledBorder titledBorder = (TitledBorder) border;
            panel.setBorder(create_section_border(titledBorder.getTitle()));
        } else if (border instanceof CompoundBorder) {
            Border outside = ((CompoundBorder) border).getOutsideBorder();
            if (outside instanceof TitledBorder) {
                TitledBorder titledBorder = (TitledBorder) outside;
                panel.setBorder(create_section_border(titledBorder.getTitle()));
            }
        }
    }

    private static void style_button(AbstractButton button) {
        button.setFont(UI_FONT_PLAIN);
        button.setBackground(new Color(230, 221, 204));
        button.setForeground(TEXT_PRIMARY);
        button.getModel().setRollover(false);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
    }

    private static void style_text_area(JTextArea area) {
        area.setFont(UI_FONT_PLAIN);
        area.setForeground(TEXT_PRIMARY);
        area.setCaretColor(ACCENT_PRIMARY);
        area.setBackground(area.isEditable() ? PANEL_ELEVATED : PANEL_SURFACE);
        area.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
    }

    private static void style_text_field(JTextField field) {
        field.setFont(UI_FONT_PLAIN);
        field.setBackground(PANEL_ELEVATED);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(ACCENT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)
        ));
    }

    private static void style_combo_box(JComboBox<?> comboBox) {
        comboBox.setFont(UI_FONT_PLAIN);
        comboBox.setBackground(PANEL_ELEVATED);
        comboBox.setForeground(TEXT_PRIMARY);
        comboBox.setOpaque(true);
        comboBox.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true));
        comboBox.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton button = new JButton("\u2304");
                button.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
                button.setBackground(PANEL_ELEVATED);
                button.setForeground(TEXT_MUTED);
                button.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER_COLOR));
                button.setFocusPainted(false);
                button.setContentAreaFilled(true);
                button.setOpaque(true);
                return button;
            }
        });
        comboBox.setRenderer(new BasicComboBoxRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setFont(UI_FONT_PLAIN);
                setText(value == null ? "" : value.toString());
                setForeground(TEXT_PRIMARY);
                setOpaque(true);
                if (index >= 0) {
                    setBackground(isSelected ? LIST_SELECTION : PANEL_ELEVATED);
                    setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                } else {
                    setBackground(PANEL_ELEVATED);
                    setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                }
                return this;
            }
        });
    }

    private static void style_spinner(JSpinner spinner) {
        spinner.setFont(UI_FONT_PLAIN);
        spinner.setBackground(PANEL_ELEVATED);
        spinner.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true));
        JComponent editor = spinner.getEditor();
        if (editor != null) {
            style_component_tree(editor);
        }
    }

    private static void style_list(JList<?> list) {
        list.setFont(UI_FONT_PLAIN);
        list.setBackground(PANEL_ELEVATED);
        list.setForeground(TEXT_PRIMARY);
        list.setSelectionBackground(LIST_SELECTION);
        list.setSelectionForeground(TEXT_PRIMARY);
        list.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    private static void style_scroll_pane(JScrollPane scrollPane) {
        scrollPane.setBackground(PANEL_ELEVATED);
        scrollPane.getViewport().setBackground(PANEL_ELEVATED);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true));
    }

    private static void style_tabbed_pane(JTabbedPane tabbedPane) {
        tabbedPane.setFont(UI_FONT_BOLD);
        tabbedPane.setBackground(PANEL_SURFACE);
        tabbedPane.setForeground(TEXT_PRIMARY);
    }

    private static void style_label(JLabel label) {
        label.setForeground(TEXT_PRIMARY);
    }
}
