package com.nexora.app.domain.repository

import com.nexora.app.core.common.Resource
import com.nexora.app.data.remote.dto.*
import kotlinx.coroutines.flow.Flow

interface PoolRepository {
    fun createPool(request: CreatePoolRequest): Flow<Resource<PoolData>>
    fun addPoolItem(poolId: String, request: AddPoolItemRequest): Flow<Resource<PoolItemData>>
    fun fetchNearbyPools(): Flow<Resource<List<PoolData>>>
    fun joinPool(poolId: String): Flow<Resource<PoolData>>
    fun fetchPoolItems(poolId: String): Flow<Resource<List<PoolItemData>>>
}
