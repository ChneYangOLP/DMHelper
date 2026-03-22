package com.DMDHelper.basic.menus;

import com.DMDHelper.basic.Character_Sheet;
import com.DMDHelper.basic.Class.Character_Class;
import com.DMDHelper.basic.Class.Fighter.Fighter_Class;
import com.DMDHelper.basic.Class.wizard.Wizard_Class;
import com.DMDHelper.basic.Stats;
import com.DMDHelper.basic.database.Character_DAO;
import com.DMDHelper.basic.database.Global_Data;
import com.DMDHelper.basic.race.*;

import javax.swing.*;
import java.awt.*;

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
        String[] classes = {"战士 (Fighter)", "法师 (Wizard)"};
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
        else job = new Fighter_Class();

        Character_Sheet new_character = Character_Sheet.create_new_character(name, age, gender, race, job, raw_stats);
        Character_Advancement_Helper.configure_new_character(this, new_character);

        Global_Data.character_pool.add(new_character);
        Character_DAO.save_character(new_character);

        new Character_Summary_UI(new_character).setVisible(true);
        this.dispose();
    }
}
