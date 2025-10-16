package com.example.myhealth.data.nutrition.db

// Initial food list seeded into the DB on first creation.
object Prepopulate {
    fun foods(): List<FoodEntity> = listOf(
        FoodEntity("OAT", "Oatmeal (plain)", kcal=68,  carb=12f, protein=2.4f, fat=1.4f, sodium=2,   sugar=0.5f, satFat=0.2f),
        FoodEntity("EGG", "Egg (boiled)",    kcal=155, carb=1.1f, protein=13f,  fat=11f,  sodium=124, sugar=1.1f, satFat=3.3f),
        FoodEntity("CHK", "Chicken Breast (grilled)", kcal=165, carb=0f, protein=31f, fat=3.6f, sodium=74, sugar=0f,  satFat=1f),
        FoodEntity("RCE", "Rice (cooked)",   kcal=130, carb=28f, protein=2.7f, fat=0.3f, sodium=1,   sugar=0.1f, satFat=0.1f),
        FoodEntity("APL", "Apple",           kcal=52,  carb=14f, protein=0.3f, fat=0.2f, sodium=1,   sugar=10f,  satFat=0f),
        FoodEntity("MIL", "Milk (low-fat)",  kcal=42,  carb=5f,  protein=3.4f, fat=1f,   sodium=44,  sugar=5.2f, satFat=0.6f)
    )
}
