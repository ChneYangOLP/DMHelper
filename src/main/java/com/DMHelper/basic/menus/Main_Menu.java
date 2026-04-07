package com.DMHelper.basic.menus;

import com.DMHelper.basic.database.Character_DAO;
import com.DMHelper.basic.database.Custom_Equipment_DAO;
import com.DMHelper.basic.database.Global_Data;
import com.DMHelper.basic.database.Init_DB;

import javax.swing.*;
import java.awt.*;

public class Main_Menu extends JFrame {

    public Main_Menu() {
        setTitle("DND 辅助工具 - DM控制台");
        setSize(520, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel main_panel = new JPanel();
        main_panel.setLayout(new BoxLayout(main_panel, BoxLayout.Y_AXIS));
        main_panel.setBorder(BorderFactory.createEmptyBorder(36, 42, 36, 42));
        main_panel.setBackground(Ui_Theme.APP_BACKGROUND);

        JLabel title_label = new JLabel("DND 辅助系统");
        title_label.setFont(new Font("微软雅黑", Font.BOLD, 32));
        title_label.setForeground(Ui_Theme.ACCENT_PRIMARY);
        title_label.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitleLabel = new JLabel("DM 控制台 / 角色、战斗与成长管理");
        subtitleLabel.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        subtitleLabel.setForeground(Ui_Theme.TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel heroPanel = new JPanel();
        heroPanel.setLayout(new BoxLayout(heroPanel, BoxLayout.Y_AXIS));
        heroPanel.setBackground(Ui_Theme.PANEL_SURFACE);
        heroPanel.setBorder(BorderFactory.createCompoundBorder(
                Ui_Theme.create_section_border("主控面板"),
                BorderFactory.createEmptyBorder(16, 18, 18, 18)
        ));
        heroPanel.add(title_label);
        heroPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        heroPanel.add(subtitleLabel);
        heroPanel.add(Box.createRigidArea(new Dimension(0, 28)));

        JPanel buttonColumn = new JPanel();
        buttonColumn.setLayout(new BoxLayout(buttonColumn, BoxLayout.Y_AXIS));
        buttonColumn.setOpaque(false);

        JButton create_char_btn = new JButton("创建角色");
        JButton view_char_btn = new JButton("角色一览 (只读)");
        JButton manage_char_btn = new JButton("角色管理 (装备与升级)");
        JButton combat_sys_btn = new JButton("战斗系统");

        Dimension btn_size = new Dimension(320, 52);
        Font btn_font = new Font("微软雅黑", Font.PLAIN, 17);

        JButton[] buttons = {create_char_btn, view_char_btn, manage_char_btn, combat_sys_btn};
        for (int i = 0; i < buttons.length; i++) {
            JButton btn = buttons[i];
            btn.setMaximumSize(btn_size);
            btn.setFont(btn_font);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            if (i == 0 || i == buttons.length - 1) {
                Ui_Theme.style_primary_button(btn);
            } else {
                Ui_Theme.style_secondary_button(btn);
            }
            buttonColumn.add(btn);
            if (i < buttons.length - 1) {
                buttonColumn.add(Box.createRigidArea(new Dimension(0, 16)));
            }
        }
        heroPanel.add(buttonColumn);
        heroPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        JLabel footerLabel = new JLabel("从这里进入所有核心工作流。");
        footerLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        footerLabel.setForeground(Ui_Theme.TEXT_MUTED);
        footerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        heroPanel.add(footerLabel);
        main_panel.add(heroPanel);

        create_char_btn.addActionListener(e -> new Create_Character_UI().setVisible(true));
        view_char_btn.addActionListener(e -> new View_Characters_UI().setVisible(true));

        manage_char_btn.addActionListener(e -> {
            if (Global_Data.character_pool.isEmpty()) {
                JOptionPane.showMessageDialog(this, "当前没有任何角色存档，请先创建角色！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            new Character_Manager_UI().setVisible(true);
        });

        combat_sys_btn.addActionListener(e -> {
            if (Global_Data.character_pool.isEmpty()) {
                JOptionPane.showMessageDialog(this, "当前没有任何角色存档，请先创建角色！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            new Combat_System_UI().setVisible(true);
        });

        add(main_panel);
        Ui_Theme.apply_window(this);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Ui_Theme.install_global_theme();
            Init_DB.setup_database();
            Custom_Equipment_DAO.load_all_custom_items();
            Character_DAO.load_all_characters();
            Main_Menu menu = new Main_Menu();
            menu.setVisible(true);
        });
    }
}
