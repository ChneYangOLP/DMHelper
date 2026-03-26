package com.DMHelper.basic.menus;

import com.DMHelper.basic.spell.Spell_Definition;
import com.DMHelper.basic.spell.Spell_Library;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Spell_Management_Helper {

    public static List<String> open_selection_dialog(Component parent,
                                                     String title,
                                                     String prompt,
                                                     List<String> availableKeys,
                                                     List<String> selectedKeys,
                                                     int maxSelected) {
        return open_selection_dialog(parent, title, prompt, availableKeys, selectedKeys, 0, maxSelected);
    }

    public static List<String> open_required_selection_dialog(Component parent,
                                                              String title,
                                                              String prompt,
                                                              List<String> availableKeys,
                                                              List<String> selectedKeys,
                                                              int requiredSelected) {
        return open_selection_dialog(parent, title, prompt, availableKeys, selectedKeys, requiredSelected, requiredSelected);
    }

    public static List<String> open_selection_dialog(Component parent,
                                                     String title,
                                                     String prompt,
                                                     List<String> availableKeys,
                                                     List<String> selectedKeys,
                                                     int minSelected,
                                                     int maxSelected) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        SpellSelectionDialog dialog = new SpellSelectionDialog(owner, title, prompt, availableKeys, selectedKeys, minSelected, maxSelected);
        dialog.setVisible(true);
        return dialog.getSelectedKeys();
    }

    private static class SpellSelectionDialog extends JDialog {
        private final List<String> availableKeys;
        private final int minSelected;
        private final int maxSelected;
        private final DefaultListModel<String> availableModel;
        private final DefaultListModel<String> selectedModel;
        private final JList<String> availableList;
        private final JList<String> selectedList;
        private final List<String> originalSelectedKeys;
        private final Set<String> selectedKeySet;
        private final JLabel infoLabel;
        private final JButton confirmButton;
        private boolean confirmed;

        SpellSelectionDialog(Window owner,
                             String title,
                             String prompt,
                             List<String> availableKeys,
                             List<String> selectedKeys,
                             int minSelected,
                             int maxSelected) {
            super(owner, title, ModalityType.APPLICATION_MODAL);
            this.availableKeys = new ArrayList<>(availableKeys);
            this.minSelected = minSelected;
            this.maxSelected = maxSelected;
            this.originalSelectedKeys = new ArrayList<>(selectedKeys);
            this.selectedKeySet = new LinkedHashSet<>(selectedKeys);
            this.availableModel = new DefaultListModel<>();
            this.selectedModel = new DefaultListModel<>();
            this.availableList = new JList<>(this.availableModel);
            this.selectedList = new JList<>(this.selectedModel);
            this.infoLabel = new JLabel();
            this.confirmButton = new JButton("确认");

            setSize(900, 520);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(10, 10));

            JLabel promptLabel = new JLabel("<html>" + prompt.replace("\n", "<br>") + "<br><br>双击左边加入，双击右边移除。</html>");
            promptLabel.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
            add(promptLabel, BorderLayout.NORTH);

            availableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            selectedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            availableList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int index = availableList.locationToIndex(e.getPoint());
                        if (index >= 0) {
                            addSpell(availableKeys.get(index));
                        }
                    }
                }
            });

            selectedList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int index = selectedList.locationToIndex(e.getPoint());
                        if (index >= 0) {
                            removeSpell(new ArrayList<>(selectedKeySet).get(index));
                        }
                    }
                }
            });

            JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 0));
            centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            centerPanel.add(wrapList("可选法术", availableList));
            centerPanel.add(wrapList("已选法术", selectedList));
            add(centerPanel, BorderLayout.CENTER);

            JPanel bottomPanel = new JPanel(new BorderLayout());
            infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));
            bottomPanel.add(infoLabel, BorderLayout.NORTH);

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

            refreshModels();
        }

        private JPanel wrapList(String title, JList<String> list) {
            JPanel panel = new JPanel(new BorderLayout(0, 8));
            panel.add(new JLabel(title), BorderLayout.NORTH);
            panel.add(new JScrollPane(list), BorderLayout.CENTER);
            return panel;
        }

        private void addSpell(String spellKey) {
            if (this.selectedKeySet.contains(spellKey)) {
                return;
            }
            if (this.selectedKeySet.size() >= this.maxSelected) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            this.selectedKeySet.add(spellKey);
            refreshModels();
        }

        private void removeSpell(String spellKey) {
            this.selectedKeySet.remove(spellKey);
            refreshModels();
        }

        private void refreshModels() {
            this.availableModel.clear();
            this.selectedModel.clear();

            for (String spellKey : this.availableKeys) {
                Spell_Definition spell = Spell_Library.get_spell(spellKey);
                if (spell != null) {
                    this.availableModel.addElement(spell.to_detail_line());
                }
            }
            for (String spellKey : this.selectedKeySet) {
                Spell_Definition spell = Spell_Library.get_spell(spellKey);
                if (spell != null) {
                    this.selectedModel.addElement(spell.to_detail_line());
                }
            }

            String rangeText = this.minSelected == this.maxSelected
                    ? this.maxSelected + ""
                    : this.minSelected + "-" + this.maxSelected;
            this.infoLabel.setText("已选择 " + this.selectedKeySet.size() + "，要求 " + rangeText + " 个");
            this.confirmButton.setEnabled(this.selectedKeySet.size() >= this.minSelected
                    && this.selectedKeySet.size() <= this.maxSelected);
        }

        List<String> getSelectedKeys() {
            return this.confirmed ? new ArrayList<>(this.selectedKeySet) : new ArrayList<>(this.originalSelectedKeys);
        }
    }
}
