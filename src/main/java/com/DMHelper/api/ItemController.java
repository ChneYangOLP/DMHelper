package com.DMHelper.api;

import com.DMHelper.basic.equipment.Equipment_Library;
import com.DMHelper.basic.equipment.Equipment_Item;
import io.javalin.http.Context;
import java.util.List;

public class ItemController {

    public static void getAllItems(Context ctx) {
        String search = ctx.queryParam("search");
        List<Equipment_Item> items = Equipment_Library.search_items(search == null ? "" : search, null, true);
        ctx.result(ApiServer.GSON.toJson(items)).contentType("application/json");
    }
}
