package com.DMDHelper.basic.menus;

import com.DMDHelper.basic.database.Character_DAO;
import com.DMDHelper.basic.database.Global_Data;
import com.DMDHelper.basic.database.Init_DB;

import javax.swing.*;
import java.awt.*;

public class Main_Menu extends JFrame {

    public Main_Menu() {
        setTitle("DND 辅助工具 - DM控制台");
        setSize(400, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel main_panel = new JPanel();
        main_panel.setLayout(new BoxLayout(main_panel, BoxLayout.Y_AXIS));
        main_panel.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));

        JLabel title_label = new JLabel("DND 辅助系统");
        title_label.setFont(new Font("微软雅黑", Font.BOLD, 30));
        title_label.setAlignmentX(Component.CENTER_ALIGNMENT);
        main_panel.add(title_label);
        main_panel.add(Box.createRigidArea(new Dimension(0, 40)));

        JButton create_char_btn = new JButton("创建角色");
        JButton view_char_btn = new JButton("角色一览 (只读)");
        JButton manage_char_btn = new JButton("角色管理 (装备与升级)");
        JButton combat_sys_btn = new JButton("战斗系统");

        Dimension btn_size = new Dimension(250, 50);
        Font btn_font = new Font("微软雅黑", Font.PLAIN, 18);

        JButton[] buttons = {create_char_btn, view_char_btn, manage_char_btn, combat_sys_btn};
        for (JButton btn : buttons) {
            btn.setMaximumSize(btn_size);
            btn.setFont(btn_font);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setFocusPainted(false);
            main_panel.add(btn);
            main_panel.add(Box.createRigidArea(new Dimension(0, 20)));
        }

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
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Init_DB.setup_database();
            Character_DAO.load_all_characters();
            Main_Menu menu = new Main_Menu();
            menu.setVisible(true);
        });
    }
}
