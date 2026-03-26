package com.DMHelper.basic.menus;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.database.Character_DAO;
import com.DMHelper.basic.database.Global_Data;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class View_Characters_UI extends JFrame {

    private JList<String> character_list_view;
    private DefaultListModel<String> list_model;

    public View_Characters_UI() {
        setTitle("角色一览");
        setSize(400, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel top_panel = new JPanel();
        JLabel title_label = new JLabel("已创建的角色列表");
        title_label.setFont(new Font("微软雅黑", Font.BOLD, 20));
        top_panel.add(title_label);

        list_model = new DefaultListModel<>();
        refresh_list();

        character_list_view = new JList<>(list_model);
        character_list_view.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        character_list_view.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // --- 核心改进：添加鼠标双击监听器 ---
        character_list_view.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                // 判断是否是双击 (ClickCount == 2)
                if (evt.getClickCount() == 2) {
                    // 获取鼠标双击位置对应的列表索引
                    int selected_index = character_list_view.locationToIndex(evt.getPoint());
                    if (selected_index != -1) {
                        Character_Sheet selected_char = Global_Data.character_pool.get(selected_index);
                        new Character_Summary_UI(selected_char).setVisible(true);
                    }
                }
            }
        });

        JScrollPane scroll_pane = new JScrollPane(character_list_view);
        scroll_pane.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JPanel btn_panel = new JPanel();
        JButton view_btn = new JButton("查看详情");
        JButton delete_btn = new JButton("删除角色");

        // 保留原有的按钮点击逻辑，照顾不同使用习惯的玩家
        view_btn.addActionListener(e -> {
            int selected_index = character_list_view.getSelectedIndex();
            if (selected_index != -1) {
                Character_Sheet selected_char = Global_Data.character_pool.get(selected_index);
                new Character_Summary_UI(selected_char).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "请先在列表中选中一个角色！", "提示", JOptionPane.WARNING_MESSAGE);
            }
        });

        delete_btn.addActionListener(e -> {
            int selected_index = character_list_view.getSelectedIndex();
            if (selected_index != -1) {
                int confirm = JOptionPane.showConfirmDialog(this, "确定要删除该角色吗？", "删除确认", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    Character_Sheet selected_char = Global_Data.character_pool.get(selected_index);
                    Character_DAO.delete_character(selected_char);
                    refresh_list();
                }
            }
        });

        btn_panel.add(view_btn);
        btn_panel.add(delete_btn);

        add(top_panel, BorderLayout.NORTH);
        add(scroll_pane, BorderLayout.CENTER);
        add(btn_panel, BorderLayout.SOUTH);
    }

    private void refresh_list() {
        list_model.clear();
        for (Character_Sheet c : Global_Data.character_pool) {
            String display_text = String.format("%s - %s %s (LV.%d)",
                    c.name, c.race.race_name, c.job.class_name, c.job.current_level);
            list_model.addElement(display_text);
        }
    }
}
