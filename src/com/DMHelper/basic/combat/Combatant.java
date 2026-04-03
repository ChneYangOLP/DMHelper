package com.DMHelper.basic.combat;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.playerclass.Fighter.Fighter_Class;
import com.DMHelper.basic.playerclass.bard.Bard_Class;
import com.DMHelper.basic.playerclass.paladin.Paladin_Class;
import com.DMHelper.basic.playerclass.sorcerer.Sorcerer_Class;
import com.DMHelper.basic.playerclass.warlock.Warlock_Class;
import com.DMHelper.basic.playerclass.wizard.Wizard_Class;

import java.util.ArrayList;
import java.util.List;

public class Combatant {
    public enum Side {
        PLAYER,
        ENEMY
    }

    public final String display_name;
    public final Side side;
    public final int armor_class;
    public final int max_hp;
    public int current_hp;
    public final int initiative_modifier;
    public int initiative_roll;
    public int initiative_total;
    public final int strength;
    public final int dexterity;
    public final int constitution;
    public final int intelligence;
    public final int wisdom;
    public final int charisma;
    public final int proficiency_bonus;
    public final List<Attack_Option> attack_options;
    public final Character_Sheet linked_character;
    public final Monster_Definition linked_monster;
    public final int xp_reward;
    public int[] spell_slots_remaining;
    public int pact_slots_remaining;
    public int pact_slot_level;
    public int sorcery_points_remaining;
    public int bardic_inspiration_remaining;
    public int superiority_dice_remaining;
    public int superiority_dice_size;
    public int lay_on_hands_remaining;
    public final List<Combat_Status_Effect> status_effects;

    public Combatant(String display_name,
                     Side side,
                     int armor_class,
                     int max_hp,
                     int initiative_modifier,
                     int strength,
                     int dexterity,
                     int constitution,
                     int intelligence,
                     int wisdom,
                     int charisma,
                     int proficiency_bonus,
                     List<Attack_Option> attack_options,
                     Character_Sheet linked_character,
                     Monster_Definition linked_monster,
                     int xp_reward) {
        this.display_name = display_name;
        this.side = side;
        this.armor_class = armor_class;
        this.max_hp = max_hp;
        this.current_hp = max_hp;
        this.initiative_modifier = initiative_modifier;
        this.strength = strength;
        this.dexterity = dexterity;
        this.constitution = constitution;
        this.intelligence = intelligence;
        this.wisdom = wisdom;
        this.charisma = charisma;
        this.proficiency_bonus = proficiency_bonus;
        this.attack_options = new ArrayList<>(attack_options);
        this.linked_character = linked_character;
        this.linked_monster = linked_monster;
        this.xp_reward = xp_reward;
        this.spell_slots_remaining = new int[10];
        this.status_effects = new ArrayList<>();
        if (linked_character != null) {
            this.current_hp = linked_character.current_hp;
        }
        initialize_resources();
    }

    public boolean is_alive() {
        return this.current_hp > 0;
    }

    public int get_saving_throw_bonus(String ability) {
        int statusBonus = 0;
        if ("Dexterity".equals(ability)) {
            for (Combat_Status_Effect effect : this.status_effects) {
                statusBonus += effect.type.dex_save_modifier;
            }
        }
        if (this.linked_character != null) {
            return this.linked_character.get_saving_throw_bonus(ability) + statusBonus;
        }
        if ("Strength".equals(ability)) return get_modifier(this.strength);
        if ("Dexterity".equals(ability)) return get_modifier(this.dexterity) + statusBonus;
        if ("Constitution".equals(ability)) return get_modifier(this.constitution);
        if ("Intelligence".equals(ability)) return get_modifier(this.intelligence);
        if ("Wisdom".equals(ability)) return get_modifier(this.wisdom);
        if ("Charisma".equals(ability)) return get_modifier(this.charisma);
        return 0;
    }

    public int get_effective_attack_modifier() {
        int modifier = 0;
        for (Combat_Status_Effect effect : this.status_effects) {
            modifier += effect.type.attack_modifier;
        }
        return modifier;
    }

    public int get_effective_armor_class() {
        int modifier = 0;
        for (Combat_Status_Effect effect : this.status_effects) {
            modifier += effect.type.armor_class_modifier;
        }
        return Math.max(5, this.armor_class + modifier);
    }

    public boolean is_turn_blocked() {
        for (Combat_Status_Effect effect : this.status_effects) {
            if (effect.type.turn_blocked) return true;
        }
        return false;
    }

    public void apply_status(Combat_Status_Type statusType, int durationRounds) {
        for (Combat_Status_Effect effect : this.status_effects) {
            if (effect.type == statusType) {
                effect.rounds_remaining = Math.max(effect.rounds_remaining, durationRounds);
                return;
            }
        }
        this.status_effects.add(new Combat_Status_Effect(statusType, durationRounds));
    }

    public String get_status_summary() {
        if (this.status_effects.isEmpty()) return "无";
        List<String> labels = new ArrayList<>();
        for (Combat_Status_Effect effect : this.status_effects) {
            labels.add(effect.get_label());
        }
        return String.join("、", labels);
    }

    private void initialize_resources() {
        if (this.linked_character == null) {
            return;
        }
        if (this.linked_character.job instanceof Wizard_Class) {
            this.spell_slots_remaining = ((Wizard_Class) this.linked_character.job).current_spell_slots.clone();
        } else if (this.linked_character.job instanceof Sorcerer_Class) {
            Sorcerer_Class sorcerer = (Sorcerer_Class) this.linked_character.job;
            this.spell_slots_remaining = sorcerer.current_spell_slots.clone();
            this.sorcery_points_remaining = sorcerer.current_sorcery_points;
        } else if (this.linked_character.job instanceof Paladin_Class) {
            Paladin_Class paladin = (Paladin_Class) this.linked_character.job;
            this.spell_slots_remaining = paladin.current_spell_slots.clone();
            this.lay_on_hands_remaining = paladin.current_lay_on_hands_pool;
        } else if (this.linked_character.job instanceof Fighter_Class) {
            Fighter_Class fighter = (Fighter_Class) this.linked_character.job;
            this.superiority_dice_remaining = fighter.current_superiority_dice;
            this.superiority_dice_size = fighter.superiority_dice_type;
        } else if (this.linked_character.job instanceof Warlock_Class) {
            Warlock_Class warlock = (Warlock_Class) this.linked_character.job;
            this.pact_slots_remaining = warlock.current_pact_slot_count;
            this.pact_slot_level = warlock.pact_slot_level;
        } else if (this.linked_character.job instanceof Bard_Class) {
            Bard_Class bard = (Bard_Class) this.linked_character.job;
            this.spell_slots_remaining = bard.current_spell_slots.clone();
            this.bardic_inspiration_remaining = bard.current_bardic_inspiration_uses;
        }
    }

    public static int get_modifier(int score) {
        return Math.floorDiv(score - 10, 2);
    }
}
