package com.DMHelper.basic.menus;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.playerclass.Character_Class;
import com.DMHelper.basic.playerclass.Fighter.Fighter_Class;
import com.DMHelper.basic.playerclass.paladin.Paladin_Class;
import com.DMHelper.basic.playerclass.sorcerer.Sorcerer_Class;
import com.DMHelper.basic.playerclass.warlock.Warlock_Class;
import com.DMHelper.basic.playerclass.wizard.Wizard_Class;
import com.DMHelper.basic.Stats;
import com.DMHelper.basic.database.Character_DAO;
import com.DMHelper.basic.database.Global_Data;
import com.DMHelper.basic.race.*;

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
        setSize(560, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel rootPanel = new JPanel(new BorderLayout(12, 12));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel headerPanel = new JPanel(new BorderLayout(0, 6));
        JLabel titleLabel = new JLabel("创建新角色");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 24));
        titleLabel.setForeground(Ui_Theme.ACCENT_PRIMARY);
        JLabel subtitleLabel = new JLabel("填写基础信息与六维属性，然后继续完善背景与性格。");
        subtitleLabel.setForeground(Ui_Theme.TEXT_MUTED);
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        rootPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridLayout(2, 1, 12, 12));
        JPanel baseInfoPanel = new JPanel(new GridLayout(5, 2, 10, 15));
        baseInfoPanel.setBorder(BorderFactory.createCompoundBorder(
                Ui_Theme.create_section_border("基础信息"),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));

        JPanel statPanel = new JPanel(new GridLayout(6, 2, 10, 15));
        statPanel.setBorder(BorderFactory.createCompoundBorder(
                Ui_Theme.create_section_border("属性值"),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));

        baseInfoPanel.add(new JLabel("角色姓名:"));
        name_field = new JTextField();
        baseInfoPanel.add(name_field);

        baseInfoPanel.add(new JLabel("角色年龄:"));
        age_spinner = new JSpinner(new SpinnerNumberModel(20, 1, 1000, 1));
        baseInfoPanel.add(age_spinner);

        baseInfoPanel.add(new JLabel("角色性别:"));
        String[] genders = {"男", "女", "无性别", "其他"};
        gender_box = new JComboBox<>(genders);
        baseInfoPanel.add(gender_box);

        baseInfoPanel.add(new JLabel("选择种族:"));
        // 严格限定为 9 大核心种族
        String[] races = {
                "人类 (Human)", "精灵 (Elf)", "矮人 (Dwarf)", "半身人 (Halfling)",
                "龙裔 (Dragonborn)", "侏儒 (Gnome)", "半精灵 (Half-Elf)",
                "半兽人 (Half-Orc)", "提夫林 (Tiefling)"
        };
        race_box = new JComboBox<>(races);
        baseInfoPanel.add(race_box);

        baseInfoPanel.add(new JLabel("选择职业:"));
        String[] classes = {"战士 (Fighter)", "法师 (Wizard)", "术士 (Sorcerer)", "邪术士 (Warlock)", "圣武士 (Paladin)"};
        class_box = new JComboBox<>(classes);
        baseInfoPanel.add(class_box);

        statPanel.add(new JLabel("力量 (STR):"));
        str_spinner = new JSpinner(new SpinnerNumberModel(10, 1, 20, 1));
        statPanel.add(str_spinner);

        statPanel.add(new JLabel("敏捷 (DEX):"));
        dex_spinner = new JSpinner(new SpinnerNumberModel(10, 1, 20, 1));
        statPanel.add(dex_spinner);

        statPanel.add(new JLabel("体质 (CON):"));
        con_spinner = new JSpinner(new SpinnerNumberModel(10, 1, 20, 1));
        statPanel.add(con_spinner);

        statPanel.add(new JLabel("智力 (INT):"));
        intel_spinner = new JSpinner(new SpinnerNumberModel(10, 1, 20, 1));
        statPanel.add(intel_spinner);

        statPanel.add(new JLabel("感知 (WIS):"));
        wis_spinner = new JSpinner(new SpinnerNumberModel(10, 1, 20, 1));
        statPanel.add(wis_spinner);

        statPanel.add(new JLabel("魅力 (CHA):"));
        cha_spinner = new JSpinner(new SpinnerNumberModel(10, 1, 20, 1));
        statPanel.add(cha_spinner);

        contentPanel.add(baseInfoPanel);
        contentPanel.add(statPanel);

        JPanel btn_panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton submit_btn = new JButton("生成角色面板");
        Ui_Theme.style_primary_button(submit_btn);
        btn_panel.add(submit_btn);

        submit_btn.addActionListener(e -> build_character());

        rootPanel.add(contentPanel, BorderLayout.CENTER);
        rootPanel.add(btn_panel, BorderLayout.SOUTH);
        add(rootPanel);
        Ui_Theme.apply_window(this);
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
        Character_Race race = build_selected_race(selected_race);

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

        Ui_Theme.style_secondary_button(cancelButton);
        Ui_Theme.style_primary_button(confirmButton);
        Ui_Theme.apply_window(dialog);
        dialog.setVisible(true);
        return result.values;
    }

    private Character_Race build_selected_race(String selectedRace) {
        if ("精灵 (Elf)".equals(selectedRace)) {
            String choice = choose_option("选择精灵子种族",
                    "请选择精灵子种族：",
                    new String[]{"高等精灵 (High Elf)", "木精灵 (Wood Elf)", "卓尔精灵 (Drow)"});
            if (choice.contains("木精灵")) return new Elf_Race("WOOD");
            if (choice.contains("卓尔")) return new Elf_Race("DROW");
            return new Elf_Race("HIGH");
        }
        if ("矮人 (Dwarf)".equals(selectedRace)) {
            String choice = choose_option("选择矮人子种族",
                    "请选择矮人子种族：",
                    new String[]{"丘陵矮人 (Hill Dwarf)", "山地矮人 (Mountain Dwarf)"});
            return choice.contains("山地") ? new Dwarf_Race("MOUNTAIN") : new Dwarf_Race("HILL");
        }
        if ("半身人 (Halfling)".equals(selectedRace)) {
            String choice = choose_option("选择半身人子种族",
                    "请选择半身人子种族：",
                    new String[]{"轻足半身人 (Lightfoot Halfling)", "健壮半身人 (Stout Halfling)"});
            return choice.contains("健壮") ? new Halfling_Race("STOUT") : new Halfling_Race("LIGHTFOOT");
        }
        if ("龙裔 (Dragonborn)".equals(selectedRace)) {
            String choice = choose_option("选择龙裔血脉",
                    "请选择龙裔的龙族先祖：",
                    new String[]{
                            "黑龙 (Black) - 强酸", "蓝龙 (Blue) - 闪电", "黄铜龙 (Brass) - 火焰", "青铜龙 (Bronze) - 闪电",
                            "赤铜龙 (Copper) - 强酸", "金龙 (Gold) - 火焰", "绿龙 (Green) - 毒素", "红龙 (Red) - 火焰",
                            "银龙 (Silver) - 寒冷", "白龙 (White) - 寒冷"
                    });
            if (choice.contains("黑龙")) return new Dragonborn_Race("BLACK");
            if (choice.contains("蓝龙")) return new Dragonborn_Race("BLUE");
            if (choice.contains("黄铜")) return new Dragonborn_Race("BRASS");
            if (choice.contains("青铜")) return new Dragonborn_Race("BRONZE");
            if (choice.contains("赤铜")) return new Dragonborn_Race("COPPER");
            if (choice.contains("金龙")) return new Dragonborn_Race("GOLD");
            if (choice.contains("绿龙")) return new Dragonborn_Race("GREEN");
            if (choice.contains("银龙")) return new Dragonborn_Race("SILVER");
            if (choice.contains("白龙")) return new Dragonborn_Race("WHITE");
            return new Dragonborn_Race("RED");
        }
        if ("侏儒 (Gnome)".equals(selectedRace)) {
            String choice = choose_option("选择侏儒子种族",
                    "请选择侏儒子种族：",
                    new String[]{"森林侏儒 (Forest Gnome)", "岩侏儒 (Rock Gnome)"});
            return choice.contains("岩侏儒") ? new Gnome_Race("ROCK") : new Gnome_Race("FOREST");
        }
        if ("半精灵 (Half-Elf)".equals(selectedRace)) {
            String first = choose_option("选择半精灵属性加值",
                    "请选择半精灵额外 +1 的第一项属性：",
                    new String[]{"力量", "敏捷", "体质", "智力", "感知"});
            String second = choose_option("选择半精灵属性加值",
                    "请选择半精灵额外 +1 的第二项属性：",
                    build_remaining_half_elf_options(first));
            return new Half_Elf_Race(to_half_elf_bonus_key(first), to_half_elf_bonus_key(second));
        }
        if ("半兽人 (Half-Orc)".equals(selectedRace)) return new Half_Orc_Race();
        if ("提夫林 (Tiefling)".equals(selectedRace)) return new Tiefling_Race();
        if ("人类 (Human)".equals(selectedRace)) return new Human_Race();
        return new Human_Race();
    }

    private String[] build_remaining_half_elf_options(String first) {
        java.util.List<String> options = new java.util.ArrayList<>();
        for (String option : new String[]{"力量", "敏捷", "体质", "智力", "感知"}) {
            if (!option.equals(first)) {
                options.add(option);
            }
        }
        return options.toArray(new String[0]);
    }

    private String to_half_elf_bonus_key(String label) {
        if ("力量".equals(label)) return "STR";
        if ("敏捷".equals(label)) return "DEX";
        if ("体质".equals(label)) return "CON";
        if ("智力".equals(label)) return "INT";
        if ("感知".equals(label)) return "WIS";
        return "DEX";
    }

    private String choose_option(String title, String prompt, String[] options) {
        String selected = (String) JOptionPane.showInputDialog(
                this,
                prompt,
                title,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options.length > 0 ? options[0] : null
        );
        if (selected == null || selected.trim().isEmpty()) {
            return options.length > 0 ? options[0] : "";
        }
        return selected;
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
        panel.setBorder(BorderFactory.createCompoundBorder(
                Ui_Theme.create_section_border(title),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        panel.add(Ui_Theme.wrap_scroll(area), BorderLayout.CENTER);
        return panel;
    }

    private static class ProfileDialogResult {
        private Map<String, String> values;
    }
}
