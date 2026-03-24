import com.DMDHelper.basic.playerclass.Fighter.Fighter_Class;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Fighter_Class fighter = new Fighter_Class();

        System.out.println("--- 测试 1: 故意选择错误数量的技能 ---");
        List<String> bad_count_skills = new ArrayList<>();
        bad_count_skills.add("Athletics (运动)");
        fighter.select_skills(bad_count_skills); // 应该失败并提示需要2项

        System.out.println("\n--- 测试 2: 故意选择不在战士列表里的技能 ---");
        List<String> bad_name_skills = new ArrayList<>();
        bad_name_skills.add("Athletics (运动)");
        bad_name_skills.add("Arcana (奥秘)"); // 法系技能，战士不能选
        fighter.select_skills(bad_name_skills); // 应该失败并提示非法

        System.out.println("\n--- 测试 3: 正确的技能选择 ---");
        List<String> good_skills = new ArrayList<>();
        good_skills.add("Athletics (运动)");
        good_skills.add("Perception (察觉)");
        fighter.select_skills(good_skills); // 应该成功

        // 验证结果
        System.out.println("\n当前战士已熟练的技能: " + fighter.skill_proficiencies);
    }
}