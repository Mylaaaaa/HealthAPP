package com.zihanwang.myhealth.data.api

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class FoodItem(
    val description: String?,
    val foodNutrients: List<Nutrient>?
)

data class Nutrient(
    val nutrientName: String?,
    val value: Double?,
    val unitName: String?
)

data class FoodSearchResponse(
    val foods: List<FoodItem>?
)

interface NutritionApiService {
    @GET("foods/search")
    suspend fun searchFood(
        @Query("query") query: String,
        @Query("api_key") apiKey: String
    ): FoodSearchResponse

    companion object {
        private const val BASE_URL = "https://api.nal.usda.gov/fdc/v1/"

        fun create(): NutritionApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(NutritionApiService::class.java)
        }
    }
}
