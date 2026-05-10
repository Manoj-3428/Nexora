package com.nexora.app.data.remote

import com.nexora.app.data.remote.dto.*
import retrofit2.http.*

interface PoolApi {
    @POST("pools")
    suspend fun createPool(@Body request: CreatePoolRequest): PoolResponse

    @POST("pools/{poolId}/items")
    suspend fun addPoolItem(
        @Path("poolId") poolId: String,
        @Body request: AddPoolItemRequest
    ): PoolItemResponse

    @GET("pools/{poolId}")
    suspend fun getPoolDetails(@Path("poolId") poolId: String): PoolResponse

    @GET("pools/nearby")
    suspend fun getNearbyPools(): PoolListResponse

    @POST("pools/{poolId}/join")
    suspend fun joinPool(@Path("poolId") poolId: String): PoolResponse

    @GET("pools/{poolId}/items")
    suspend fun getPoolItems(@Path("poolId") poolId: String): PoolItemListResponse
}
