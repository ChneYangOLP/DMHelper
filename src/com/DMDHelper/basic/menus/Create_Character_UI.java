package com.DMDHelper.basic.menus;

import com.DMDHelper.basic.Character_Sheet;
import com.DMDHelper.basic.playerclass.Character_Class;
import com.DMDHelper.basic.playerclass.Fighter.Fighter_Class;
import com.DMDHelper.basic.playerclass.paladin.Paladin_Class;
import com.DMDHelper.basic.playerclass.sorcerer.Sorcerer_Class;
import com.DMDHelper.basic.playerclass.warlock.Warlock_Class;
import com.DMDHelper.basic.playerclass.wizard.Wizard_Class;
import com.DMDHelper.basic.Stats;
import com.DMDHelper.basic.database.Character_DAO;
import com.DMDHelper.basic.database.Global_Data;
import com.DMDHelper.basic.race.*;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class Create_Character_UI extends JFrame {

    private JTextField name_field;
    private JSpinner age_spinner;
    private JComboBox<String> gender_box;
    private JComboBox<String> race_box;
    private JComboBox<String> class_box;

    private JSpinner str_spinner;
    private JSpinner dex_spinner;
    private JSpinner con_spinner;
    private JSpinner intel_spinner;
    private JSpinner wis_spinner;
    private JSpinner cha_spinner;

    public Create_Character_UI() {
        setTitle("创建新角色");
        setSize(400, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel form_panel = new JPanel(new GridLayout(12, 2, 10, 15));
        form_panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        form_panel.add(new JLabel("角色姓名:"));
        name_field = new JTextField();
        form_panel.add(name_field);

        form_panel.add(new JLabel("角色年龄:"));
        age_spinner = new JSpinner(new SpinnerNumberModel(20, 1, 1000, 1));
        form_panel.add(age_spinner);

        form_panel.add(new JLabel("角色性别:"));
        String[] genders = {"男", "女", "无性别", "其他"};
        gender_box = new JComboBox<>(genders);
        form_panel.add(gender_box);

        form_panel.add(new JLabel("选择种族:"));
        // 严格限定为 9 大核心种族
        String[] races = {
                "人类 (Human)", "精灵 (Elf)", "矮人 (Dwarf)", "半身人 (Halfling)",
                "龙裔 (Dragonborn)", "侏儒 (Gnome)", "半精灵 (Half-Elf)",
                "半兽人 (Half-Orc)", "提夫林 (Tiefling)"
        };
        race_box = new JComboBox<>(races);
        form_panel.add(race_box);

        form_panel.add(new JLabel("选择职业:"));
        String[] classes = {"战士 (Fighter)", "法师 (Wizard)", "术士 (Sorcerer)", "邪术士 (Warlock)", "圣武士 (Paladin)"};
        class_box = new JComboBox<>(classes);
        form_panel.add(class_box);

        form_panel.add(new JLabel("力量 (STR):"));
        str_spinner = new JSpinner(new SpinnerNumberModel(10, 1, 20, 1));
        form_panel.add(str_spinner);

        form_panel.add(new JLabel("敏捷 (DEX):"));
        dex_spinner = new JSpinner(new SpinnerNumberModel(10, 1, 20, 1));
        form_panel.add(dex_spinner);

        form_panel.add(new JLabel("体质 (CON):"));
        con_spinner = new JSpinner(new SpinnerNumberModel(10, 1, 20, 1));
        form_panel.add(con_spinner);

        form_panel.add(new JLabel("智力 (INT):"));
        intel_spinner = new JSpinner(new SpinnerNumberModel(10, 1, 20, 1));
        form_panel.add(intel_spinner);

        form_panel.add(new JLabel("感知 (WIS):"));
        wis_spinner = new JSpinner(new SpinnerNumberModel(10, 1, 20, 1));
        form_panel.add(wis_spinner);

        form_panel.add(new JLabel("魅力 (CHA):"));
        cha_spinner = new JSpinner(new SpinnerNumberModel(10, 1, 20, 1));
        form_panel.add(cha_spinner);

        JPanel btn_panel = new JPanel();
        JButton submit_btn = new JButton("生成角色面板");
        btn_panel.add(submit_btn);

        submit_btn.addActionListener(e -> build_character());

        add(form_panel, BorderLayout.CENTER);
        add(btn_panel, BorderLayout.SOUTH);
    }

    private void build_character() {
        String name = name_field.getText();
        if (name.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "角色姓名不能为空！", "警告", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int age = (Integer) age_spinner.getValue();
        String gender = (String) gender_box.getSelectedItem();

        Stats raw_stats = new Stats(
                (Integer) str_spinner.getValue(), (Integer) dex_spinner.getValue(),
                (Integer) con_spinner.getValue(), (Integer) intel_spinner.getValue(),
                (Integer) wis_spinner.getValue(), (Integer) cha_spinner.getValue()
        );

        String selected_race = (String) race_box.getSelectedItem();
        Character_Race race = null;

        // 9 大核心种族的实例化路由
        if (selected_race.equals("人类 (Human)")) race = new Human_Race();
        else if (selected_race.equals("精灵 (Elf)")) race = new Elf_Race();
        else if (selected_race.equals("矮人 (Dwarf)")) race = new Dwarf_Race();
        else if (selected_race.equals("半身人 (Halfling)")) race = new Halfling_Race();
        else if (selected_race.equals("龙裔 (Dragonborn)")) race = new Dragonborn_Race();
        else if (selected_race.equals("侏儒 (Gnome)")) race = new Gnome_Race();
        else if (selected_race.equals("半精灵 (Half-Elf)")) race = new Half_Elf_Race();
        else if (selected_race.equals("半兽人 (Half-Orc)")) race = new Half_Orc_Race();
        else if (selected_race.equals("提夫林 (Tiefling)")) race = new Tiefling_Race();
        else race = new Human_Race();

        String selected_class = (String) class_box.getSelectedItem();
        Character_Class job = null;
        if (selected_class.equals("战士 (Fighter)")) job = new Fighter_Class();
        else if (selected_class.equals("法师 (Wizard)")) job = new Wizard_Class();
        else if (selected_class.equals("术士 (Sorcerer)")) job = new Sorcerer_Class();
        else if (selected_class.equals("邪术士 (Warlock)")) job = new Warlock_Class();
        else if (selected_class.equals("圣武士 (Paladin)")) job = new Paladin_Class();
        else job = new Fighter_Class();

        Character_Sheet new_character = Character_Sheet.create_new_character(name, age, gender, race, job, raw_stats);
        Map<String, String> profileInputs = open_profile_dialog();
        if (profileInputs == null) {
            return;
        }
        new_character.background_story = profileInputs.get("background_story");
        new_character.personality_traits = profileInputs.get("personality_traits");
        new_character.ideals = profileInputs.get("ideals");
        new_character.bonds = profileInputs.get("bonds");
        new_character.flaws = profileInputs.get("flaws");
        Character_Advancement_Helper.configure_new_character(this, new_character);

        Global_Data.character_pool.add(new_character);
        Character_DAO.save_character(new_character);

        new Character_Summary_UI(new_character).setVisible(true);
        this.dispose();
    }

    private Map<String, String> open_profile_dialog() {
        JDialog dialog = new JDialog(this, "最后一步：角色背景与性格", true);
        dialog.setSize(620, 620);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel contentPanel = new JPanel(new GridLayout(5, 1, 10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTextArea backgroundArea = build_profile_area("背景故事：可以写出身、经历、职业道路。");
        JTextArea personalityArea = build_profile_area("性格特点：比如冷静、暴躁、乐观、谨慎。");
        JTextArea idealsArea = build_profile_area("理想信念：角色最在意的原则、追求与目标。");
        JTextArea bondsArea = build_profile_area("羁绊关系：重要的人、组织、承诺或牵挂。");
        JTextArea flawsArea = build_profile_area("缺陷弱点：恐惧、偏执、贪念、固执等等。");

        contentPanel.add(wrap_profile_field("背景故事", backgroundArea));
        contentPanel.add(wrap_profile_field("性格特点", personalityArea));
        contentPanel.add(wrap_profile_field("理想信念", idealsArea));
        contentPanel.add(wrap_profile_field("羁绊关系", bondsArea));
        contentPanel.add(wrap_profile_field("缺陷弱点", flawsArea));

        dialog.add(new JScrollPane(contentPanel), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("取消");
        JButton confirmButton = new JButton("完成创建");
        final ProfileDialogResult result = new ProfileDialogResult();

        cancelButton.addActionListener(e -> dialog.dispose());
        confirmButton.addActionListener(e -> {
            Map<String, String> values = new LinkedHashMap<>();
            values.put("background_story", read_profile_value(backgroundArea));
            values.put("personality_traits", read_profile_value(personalityArea));
            values.put("ideals", read_profile_value(idealsArea));
            values.put("bonds", read_profile_value(bondsArea));
            values.put("flaws", read_profile_value(flawsArea));
            result.values = values;
            dialog.dispose();
        });
        buttonPanel.add(cancelButton);
        buttonPanel.add(confirmButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
        return result.values;
    }

    private JTextArea build_profile_area(String hint) {
        JTextArea area = new JTextArea(hint);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        area.setMargin(new Insets(6, 6, 6, 6));
        area.setRows(3);
        area.setForeground(new Color(90, 90, 90));
        area.putClientProperty("hint_text", hint);
        area.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (area.getText().equals(hint)) {
                    area.setText("");
                    area.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (area.getText().trim().isEmpty()) {
                    area.setText(hint);
                    area.setForeground(new Color(90, 90, 90));
                }
            }
        });
        return area;
    }

    private String read_profile_value(JTextArea area) {
        Object hint = area.getClientProperty("hint_text");
        String text = area.getText().trim();
        if (hint instanceof String && text.equals(hint)) {
            return "";
        }
        return text;
    }

    private JPanel wrap_profile_field(String title, JTextArea area) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.add(new JLabel(title), BorderLayout.NORTH);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    private static class ProfileDialogResult {
        private Map<String, String> values;
    }
}
