package com.DMDHelper.basic.menus;

import com.DMDHelper.basic.Character_Sheet;
import com.DMDHelper.basic.Class.Character_Class;
import com.DMDHelper.basic.Class.Fighter.Fighter_Class;
import com.DMDHelper.basic.Class.Fighter.Fighter_Subclass;
import com.DMDHelper.basic.Class.wizard.Wizard_Class;
import com.DMDHelper.basic.Class.wizard.Wizard_Subclass;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Character_Advancement_Helper {

    private static final Map<String, String> FEAT_DESCRIPTIONS = new LinkedHashMap<>();

    static {
        FEAT_DESCRIPTIONS.put("Alert", "警觉：先攻 +5，且不因未察觉敌人而陷入措手不及。");
        FEAT_DESCRIPTIONS.put("Great Weapon Master", "巨武器大师：重击/击倒后可追加攻击，也可用 -5 命中换 +10 伤害。");
        FEAT_DESCRIPTIONS.put("Lucky", "幸运：每长休获得 3 点幸运点，可重掷 d20。");
        FEAT_DESCRIPTIONS.put("Mage Slayer", "法师杀手：压制你邻近施法者的施法。");
        FEAT_DESCRIPTIONS.put("Sharpshooter", "神射手：远程攻击可无视远距离劣势与部分掩体，也可 -5 命中换 +10 伤害。");
        FEAT_DESCRIPTIONS.put("Tough", "强韧：每级额外获得 2 点最大生命值。");
        FEAT_DESCRIPTIONS.put("War Caster", "战斗施法者：更擅长维持专注并可在借机攻击时施法。");
    }

    public static void configure_new_character(Component parent, Character_Sheet character) {
        choose_starting_skills(parent, character.job);

        if (character.job instanceof Fighter_Class) {
            Fighter_Class fighter = (Fighter_Class) character.job;
            fighter.fighting_style_name = choose_single_option(
                    parent,
                    "选择战斗风格",
                    "请选择战士 1 级战斗风格：",
                    fighter.get_available_fighting_styles(false)
            );
            character.record_advancement("1级选择战斗风格：" + fighter.fighting_style_name);
        }

        character.recalculate_derived_stats();
    }

    public static void process_pending_choices(Component parent, Character_Sheet character) {
        boolean processedChoice;

        do {
            processedChoice = false;

            if (character.job instanceof Fighter_Class) {
                Fighter_Class fighter = (Fighter_Class) character.job;

                if (character.job.current_level >= 3 && fighter.fighter_subclass == Fighter_Subclass.NONE) {
                    String pickedSubclass = choose_single_option(
                            parent,
                            "选择战士子职业",
                            "请选择战士子职业：",
                            Arrays.asList("Champion", "Battle Master")
                    );
                    fighter.fighter_subclass = "Champion".equals(pickedSubclass)
                            ? Fighter_Subclass.CHAMPION
                            : Fighter_Subclass.BATTLE_MASTER;
                    character.record_advancement("选择战士子职业：" + fighter.get_subclass_name());
                    character.recalculate_derived_stats();
                    processedChoice = true;
                }

                int pendingManeuvers = Math.max(0, fighter.get_expected_maneuver_count() - fighter.maneuver_names.size());
                if (fighter.fighter_subclass == Fighter_Subclass.BATTLE_MASTER && pendingManeuvers > 0) {
                    List<String> chosenManeuvers = choose_multi_options(
                            parent,
                            "选择战技",
                            "请选择 " + pendingManeuvers + " 个战技：",
                            fighter.get_available_maneuvers(),
                            pendingManeuvers
                    );
                    fighter.maneuver_names.addAll(chosenManeuvers);
                    character.record_advancement("学习战技：" + String.join("、", chosenManeuvers));
                    character.recalculate_derived_stats();
                    processedChoice = true;
                }

                if (fighter.fighter_subclass == Fighter_Subclass.CHAMPION
                        && character.job.current_level >= 10
                        && (fighter.extra_fighting_style_name == null || fighter.extra_fighting_style_name.trim().isEmpty())) {
                    fighter.extra_fighting_style_name = choose_single_option(
                            parent,
                            "选择额外战斗风格",
                            "请选择冠军勇士额外战斗风格：",
                            fighter.get_available_fighting_styles(true)
                    );
                    character.record_advancement("冠军勇士获得额外战斗风格：" + fighter.extra_fighting_style_name);
                    character.recalculate_derived_stats();
                    processedChoice = true;
                }
            }

            if (character.job instanceof Wizard_Class) {
                Wizard_Class wizard = (Wizard_Class) character.job;
                if (character.job.current_level >= 2 && wizard.wizard_subclass == Wizard_Subclass.NONE) {
                    String pickedSubclass = choose_single_option(
                            parent,
                            "选择奥术传承",
                            "请选择法师奥术传承：",
                            Arrays.asList("Evocation", "Abjuration")
                    );
                    wizard.wizard_subclass = "Abjuration".equals(pickedSubclass)
                            ? Wizard_Subclass.ABJURATION
                            : Wizard_Subclass.EVOCATION;
                    character.record_advancement("选择奥术传承：" + wizard.get_subclass_name());
                    character.recalculate_derived_stats();
                    processedChoice = true;
                }
            }

            while (get_pending_asi_count(character.job) > 0) {
                resolve_asi_or_feat(parent, character);
                character.job.used_asi_choices++;
                character.recalculate_derived_stats();
                processedChoice = true;
            }
        } while (processedChoice);
    }

    private static void choose_starting_skills(Component parent, Character_Class job) {
        if (job.skill_choose_count <= 0) {
            return;
        }
        if (job.skill_proficiencies.size() == job.skill_choose_count) {
            return;
        }

        List<String> chosenSkills = choose_multi_options(
                parent,
                "选择技能熟练",
                "请选择 " + job.skill_choose_count + " 项技能熟练：",
                job.skill_options,
                job.skill_choose_count
        );
        job.select_skills(chosenSkills);
    }

    private static void resolve_asi_or_feat(Component parent, Character_Sheet character) {
        String decision = choose_single_option(
                parent,
                "属性值提升或专长",
                "本次升级请选择属性值提升还是专长：",
                Arrays.asList("属性值提升", "专长")
        );

        if ("专长".equals(decision)) {
            choose_feat(parent, character);
        } else {
            apply_ability_score_improvement(parent, character);
        }
    }

    private static void choose_feat(Component parent, Character_Sheet character) {
        List<String> availableFeats = new ArrayList<>(FEAT_DESCRIPTIONS.keySet());
        availableFeats.removeAll(character.job.feat_names);
        if (availableFeats.isEmpty()) {
            apply_ability_score_improvement(parent, character);
            return;
        }

        String featName = choose_single_option(
                parent,
                "选择专长",
                build_feat_prompt(),
                availableFeats
        );
        character.job.feat_names.add(featName);
        character.record_advancement("获得专长：" + featName + " - " + FEAT_DESCRIPTIONS.get(featName));
    }

    private static String build_feat_prompt() {
        StringBuilder sb = new StringBuilder("请选择专长：\n");
        for (Map.Entry<String, String> entry : FEAT_DESCRIPTIONS.entrySet()) {
            sb.append(entry.getKey()).append(" - ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    private static void apply_ability_score_improvement(Component parent, Character_Sheet character) {
        String mode = choose_single_option(
                parent,
                "分配属性提升",
                "请选择本次属性提升的分配方式：",
                Arrays.asList("+2 到一项属性", "+1 到两项属性")
        );

        List<String> stats = Arrays.asList("力量", "敏捷", "体质", "智力", "感知", "魅力");

        if ("+1 到两项属性".equals(mode)) {
            List<String> chosenStats = choose_multi_options(
                    parent,
                    "选择两项属性",
                    "请选择 2 项不同属性，各提升 +1：",
                    stats,
                    2
            );
            for (String statName : chosenStats) {
                increase_stat(character, statName, 1);
            }
            character.record_advancement("属性值提升："
                    + chosenStats.get(0) + " +1，"
                    + chosenStats.get(1) + " +1");
        } else {
            String chosenStat = choose_single_option(parent, "选择属性", "请选择提升 +2 的属性：", stats);
            increase_stat(character, chosenStat, 2);
            character.record_advancement("属性值提升：" + chosenStat + " +2");
        }
    }

    private static void increase_stat(Character_Sheet character, String stat_name, int amount) {
        if ("力量".equals(stat_name)) {
            character.stats.str = Math.min(20, character.stats.str + amount);
        } else if ("敏捷".equals(stat_name)) {
            character.stats.dex = Math.min(20, character.stats.dex + amount);
        } else if ("体质".equals(stat_name)) {
            character.stats.con = Math.min(20, character.stats.con + amount);
        } else if ("智力".equals(stat_name)) {
            character.stats.intel = Math.min(20, character.stats.intel + amount);
        } else if ("感知".equals(stat_name)) {
            character.stats.wis = Math.min(20, character.stats.wis + amount);
        } else if ("魅力".equals(stat_name)) {
            character.stats.cha = Math.min(20, character.stats.cha + amount);
        }
    }

    private static int get_pending_asi_count(Character_Class job) {
        if (job instanceof Fighter_Class) {
            return ((Fighter_Class) job).pending_asi_count;
        }
        if (job instanceof Wizard_Class) {
            return ((Wizard_Class) job).pending_asi_count;
        }
        return 0;
    }

    private static String choose_single_option(Component parent, String title, String prompt, List<String> options) {
        String[] optionArray = options.toArray(new String[0]);
        String selection = (String) JOptionPane.showInputDialog(
                parent,
                prompt,
                title,
                JOptionPane.PLAIN_MESSAGE,
                null,
                optionArray,
                optionArray.length > 0 ? optionArray[0] : null
        );

        if (selection == null || selection.trim().isEmpty()) {
            return optionArray.length > 0 ? optionArray[0] : "";
        }
        return selection;
    }

    private static List<String> choose_multi_options(Component parent,
                                                     String title,
                                                     String prompt,
                                                     List<String> options,
                                                     int requiredCount) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        MultiSelectDialog dialog = new MultiSelectDialog(owner, title, prompt, options, requiredCount);
        dialog.setVisible(true);

        List<String> selected = dialog.getSelectedValues();
        if (selected.size() != requiredCount) {
            return new ArrayList<>(options.subList(0, Math.min(requiredCount, options.size())));
        }
        return selected;
    }

    private static class MultiSelectDialog extends JDialog {
        private final List<String> allOptions;
        private final Set<String> selectedValues;
        private final int requiredCount;
        private final DefaultListModel<String> listModel;
        private final JList<String> optionList;
        private final JLabel selectedLabel;
        private final JButton confirmButton;

        private boolean confirmed;

        MultiSelectDialog(Window owner, String title, String prompt, List<String> options, int requiredCount) {
            super(owner, title, ModalityType.APPLICATION_MODAL);
            this.allOptions = new ArrayList<>(options);
            this.selectedValues = new LinkedHashSet<>();
            this.requiredCount = requiredCount;
            this.listModel = new DefaultListModel<>();
            this.optionList = new JList<>(this.listModel);
            this.selectedLabel = new JLabel();
            this.confirmButton = new JButton("确认选择");
            this.confirmed = false;

            setSize(460, 420);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(10, 10));

            JLabel promptLabel = new JLabel("<html>" + prompt.replace("\n", "<br>") + "<br><br>双击条目可切换已选状态。</html>");
            promptLabel.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
            add(promptLabel, BorderLayout.NORTH);

            refreshListModel();
            this.optionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            this.optionList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
                JLabel label = new JLabel(value);
                label.setOpaque(true);
                label.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
                if (isSelected) {
                    label.setBackground(new Color(210, 228, 255));
                } else {
                    label.setBackground(Color.WHITE);
                }

                String rawValue = allOptions.get(index);
                if (selectedValues.contains(rawValue)) {
                    label.setForeground(new Color(20, 120, 40));
                    label.setFont(label.getFont().deriveFont(Font.BOLD));
                } else {
                    label.setForeground(Color.BLACK);
                }
                return label;
            });

            this.optionList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int index = optionList.locationToIndex(e.getPoint());
                        if (index >= 0) {
                            toggleSelection(allOptions.get(index));
                        }
                    }
                }
            });

            JScrollPane scrollPane = new JScrollPane(this.optionList);
            scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            add(scrollPane, BorderLayout.CENTER);

            JPanel bottomPanel = new JPanel(new BorderLayout());
            this.selectedLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));
            bottomPanel.add(this.selectedLabel, BorderLayout.NORTH);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton cancelButton = new JButton("取消");
            cancelButton.addActionListener(e -> dispose());
            this.confirmButton.addActionListener(e -> {
                this.confirmed = true;
                dispose();
            });
            buttonPanel.add(cancelButton);
            buttonPanel.add(this.confirmButton);
            bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

            add(bottomPanel, BorderLayout.SOUTH);
            updateFooter();
        }

        private void toggleSelection(String value) {
            if (this.selectedValues.contains(value)) {
                this.selectedValues.remove(value);
            } else {
                if (this.selectedValues.size() >= this.requiredCount) {
                    Toolkit.getDefaultToolkit().beep();
                    return;
                }
                this.selectedValues.add(value);
            }
            refreshListModel();
            updateFooter();
            this.optionList.repaint();
        }

        private void refreshListModel() {
            this.listModel.clear();
            for (String option : this.allOptions) {
                if (this.selectedValues.contains(option)) {
                    this.listModel.addElement("[已选] " + option);
                } else {
                    this.listModel.addElement(option);
                }
            }
        }

        private void updateFooter() {
            List<String> selected = new ArrayList<>(this.selectedValues);
            String text = selected.isEmpty()
                    ? "已选择 0/" + this.requiredCount
                    : "已选择 " + selected.size() + "/" + this.requiredCount + "： " + String.join("、", selected);
            this.selectedLabel.setText(text);
            this.confirmButton.setEnabled(this.selectedValues.size() == this.requiredCount);
        }

        List<String> getSelectedValues() {
            if (!this.confirmed) {
                return new ArrayList<>();
            }
            return new ArrayList<>(this.selectedValues);
        }
    }
}
