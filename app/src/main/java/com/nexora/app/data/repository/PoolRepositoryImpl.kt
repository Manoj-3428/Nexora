package com.nexora.app.data.repository

import com.nexora.app.core.common.BaseRepository
import com.nexora.app.core.common.Resource
import com.nexora.app.data.remote.PoolApi
import com.nexora.app.data.remote.dto.*
import com.nexora.app.domain.repository.PoolRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class PoolRepositoryImpl @Inject constructor(
    private val api: PoolApi
) : BaseRepository(), PoolRepository {

    override fun createPool(request: CreatePoolRequest): Flow<Resource<PoolData>> = flow {
        emit(Resource.Loading)
        val result = safeCall { 
            val response = api.createPool(request)
            response.pool ?: throw Exception("Pool data missing from response: ${response.message}")
        }
        emit(result)
    }

    override fun addPoolItem(poolId: String, request: AddPoolItemRequest): Flow<Resource<PoolItemData>> = flow {
        emit(Resource.Loading)
        val result = safeCall { 
            val response = api.addPoolItem(poolId, request)
            response.item ?: throw Exception("Item data missing from response: ${response.message}")
        }
        emit(result)
    }

    override fun fetchNearbyPools(): Flow<Resource<List<PoolData>>> = flow {
        emit(Resource.Loading)
        val result = safeCall { api.getNearbyPools().pools }
        emit(result)
    }

    override fun joinPool(poolId: String): Flow<Resource<PoolData>> = flow {
        emit(Resource.Loading)
        val result = safeCall { 
            val response = api.joinPool(poolId)
            response.pool ?: throw Exception("Join failed: ${response.message}")
        }
        emit(result)
    }

    override fun fetchPoolItems(poolId: String): Flow<Resource<List<PoolItemData>>> = flow {
        emit(Resource.Loading)
        val result = safeCall { api.getPoolItems(poolId).items }
        emit(result)
    }
}
