package com.mnsf.resturantandroid.repository

import android.util.Log
import com.mnsf.resturantandroid.data.model.*
import java.io.IOException
import com.mnsf.resturantandroid.network.ApiService

class RestaurantRepository(private val apiService: ApiService) {
    suspend fun getRestaurants(): Result<List<Restaurant>> {
        return try {
            Log.d("RestaurantRepository", "getRestaurants: Starting API call")
            Log.e("LoginFlow", "RestaurantRepository.getRestaurants: Making API call with openNow=null")
            Log.e("LoginFlow", "RestaurantRepository.getRestaurants: About to call apiService.getRestaurants()")
            val response = apiService.getRestaurants(openNow = null) // Changed from true to null to show all restaurants
            Log.e("LoginFlow", "RestaurantRepository.getRestaurants: API call completed!")
            Log.d("RestaurantRepository", "getRestaurants: Response received - isSuccessful=${response.isSuccessful}, code=${response.code()}")
            Log.e("LoginFlow", "RestaurantRepository.getRestaurants: Response - success=${response.isSuccessful}, code=${response.code()}, body=${if (response.body() != null) "has body" else "null"}")
            
            if (response.isSuccessful && response.body() != null) {
                try {
                    val responseBody = response.body()!!
                    Log.d("RestaurantRepository", "getRestaurants: Response body received, websites count=${responseBody.websites.size}")
                    
                    // Filter only published restaurants
                    val restaurants = responseBody.websites.filter { 
                        try {
                            it.is_published
                        } catch (e: Exception) {
                            Log.e("RestaurantRepository", "getRestaurants: Error checking is_published for restaurant ${it.id}", e)
                            false
                        }
                    }
                    Log.d("RestaurantRepository", "getRestaurants: Filtered to ${restaurants.size} published restaurants")
                    Result.success(restaurants)
                } catch (e: Exception) {
                    Log.e("RestaurantRepository", "getRestaurants: Error processing response body", e)
                    e.printStackTrace()
                    Result.failure(e)
                }
            } else {
                val errorMessage = response.message() ?: "Failed to fetch restaurants"
                val errorBody = response.errorBody()?.string()
                Log.e("RestaurantRepository", "getRestaurants: API call failed - code=${response.code()}, message=$errorMessage, body=$errorBody")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("RestaurantRepository", "getRestaurants: Exception caught", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun getRestaurant(id: Int): Result<Restaurant> {
        return try {
            Log.d("RestaurantRepository", "getRestaurant: Starting API call for id=$id")
            val response = apiService.getRestaurant(id)
            Log.d("RestaurantRepository", "getRestaurant: Response received - isSuccessful=${response.isSuccessful}, code=${response.code()}")
            
            if (response.isSuccessful && response.body() != null) {
                try {
                    val websiteResponse = response.body()!!
                    val restaurant = websiteResponse.website
                    Log.d("RestaurantRepository", "getRestaurant: Restaurant received - name=${restaurant.restaurant_name}, payment_methods=${restaurant.payment_methods}")
                    Result.success(restaurant)
                } catch (e: Exception) {
                    Log.e("RestaurantRepository", "getRestaurant: Error processing response body", e)
                    e.printStackTrace()
                    Result.failure(e)
                }
            } else {
                val errorMessage = response.message() ?: "Failed to fetch restaurant"
                val errorBody = response.errorBody()?.string()
                Log.e("RestaurantRepository", "getRestaurant: API call failed - code=${response.code()}, message=$errorMessage, body=$errorBody")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("RestaurantRepository", "getRestaurant: Exception caught", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun getProducts(websiteId: Int): Result<List<Product>> {
        return try {
            val response = apiService.getProducts(websiteId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.products)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch products"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOffersList(): Result<List<Offer>> {
        return try {
            val response = apiService.getOffersList()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.offers)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOffersByWebsiteId(websiteId: Int): Result<List<Offer>> {
        return try {
            Log.d("RestaurantDetails", "getOffersByWebsiteId: requesting website_id=$websiteId")
            val response = apiService.getOffersByWebsiteId(websiteId)
            if (response.isSuccessful && response.body() != null) {
                val offers = response.body()!!.offers
                Log.d("RestaurantDetails", "getOffersByWebsiteId: website_id=$websiteId success, ${offers.size} offer(s)")
                Result.success(offers)
            } else {
                val code = response.code()
                val msg = response.message()
                val body = try { response.errorBody()?.string().orEmpty() } catch (_: IOException) { "" }
                Log.e("RestaurantDetails", "getOffersByWebsiteId: website_id=$websiteId failed code=$code message=$msg body=$body")
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Log.e("RestaurantDetails", "getOffersByWebsiteId: website_id=$websiteId exception", e)
            Result.failure(e)
        }
    }
}

