package com.DMDHelper.basic.database;

import com.DMDHelper.basic.Character_Sheet;

import java.util.ArrayList;
import java.util.List;

// 全局数据仓库，用于在不同的窗口之间共享数据
public class Global_Data {
    // 静态的 List 容器，程序运行期间一直存在，存放所有可用的角色面板
    public static List<Character_Sheet> character_pool = new ArrayList<>();
}