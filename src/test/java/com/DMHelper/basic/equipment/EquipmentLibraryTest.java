package com.DMHelper.basic.equipment;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquipmentLibraryTest {
    private static final List<String> STANDARD_ARMOR_KEYS = List.of(
            "padded_armor", "leather_armor", "studded_leather",
            "hide_armor", "chain_shirt", "scale_mail", "breastplate", "half_plate",
            "ring_mail", "chain_mail", "splint_armor", "plate_armor"
    );

    @Test
    void containsCompleteStandardArmorTable() {
        for (String key : STANDARD_ARMOR_KEYS) {
            Equipment_Item armor = Equipment_Library.get_item(key);
            assertNotNull(armor, "缺少标准护甲：" + key);
            assertEquals(Equipment_Slot.ARMOR, armor.slot, key + " 应属于护甲槽位");
            assertTrue(armor.base_ac >= 11 && armor.base_ac <= 18, key + " 的基础 AC 无效");
            assertTrue(armor.value_in_cp > 0, key + " 应配置商店价格");
        }
    }

    @Test
    void armorTypesAndPricesUseRuleData() {
        Equipment_Item studdedLeather = Equipment_Library.get_item("studded_leather");
        assertEquals("Light", studdedLeather.armor_type);
        assertEquals(12, studdedLeather.base_ac);
        assertEquals(4500, studdedLeather.value_in_cp);

        Equipment_Item breastplate = Equipment_Library.get_item("breastplate");
        assertEquals("Medium", breastplate.armor_type);
        assertEquals(14, breastplate.base_ac);
        assertEquals(40000, breastplate.value_in_cp);

        Equipment_Item plate = Equipment_Library.get_item("plate_armor");
        assertEquals("Heavy", plate.armor_type);
        assertEquals(18, plate.base_ac);
        assertEquals(150000, plate.value_in_cp);
    }
}
