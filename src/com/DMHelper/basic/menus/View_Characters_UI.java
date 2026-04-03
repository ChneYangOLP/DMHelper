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
        setSize(520, 620);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel top_panel = new JPanel(new BorderLayout());
        top_panel.setBorder(BorderFactory.createCompoundBorder(
                Ui_Theme.create_section_border("档案索引"),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        JLabel title_label = new JLabel("已创建的角色列表");
        title_label.setFont(new Font("微软雅黑", Font.BOLD, 20));
        title_label.setForeground(Ui_Theme.ACCENT_PRIMARY);
        JLabel hintLabel = new JLabel("双击列表项可直接打开角色详情。");
        hintLabel.setForeground(Ui_Theme.TEXT_MUTED);
        top_panel.add(title_label, BorderLayout.NORTH);
        top_panel.add(hintLabel, BorderLayout.SOUTH);

        list_model = new DefaultListModel<>();
        refresh_list();

        character_list_view = new JList<>(list_model);
        character_list_view.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        character_list_view.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Ui_Theme.style_component_tree(character_list_view);

        character_list_view.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int selected_index = character_list_view.locationToIndex(evt.getPoint());
                    if (selected_index != -1) {
                        Character_Sheet selected_char = Global_Data.character_pool.get(selected_index);
                        new Character_Summary_UI(selected_char).setVisible(true);
                    }
                }
            }
        });

        JScrollPane scroll_pane = Ui_Theme.wrap_scroll(character_list_view);
        scroll_pane.setBorder(BorderFactory.createCompoundBorder(
                Ui_Theme.create_section_border("角色清单"),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JPanel btn_panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton view_btn = new JButton("查看详情");
        JButton delete_btn = new JButton("删除角色");
        Ui_Theme.style_secondary_button(view_btn);
        Ui_Theme.style_primary_button(delete_btn);

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
        Ui_Theme.apply_window(this);
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
