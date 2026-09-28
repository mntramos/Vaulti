package com.vaulti.app.data.repository

import com.vaulti.app.data.database.dao.CategoryDao
import com.vaulti.app.data.database.entity.Category
import com.vaulti.app.data.sync.SyncManager
import kotlinx.coroutines.flow.Flow

class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val syncManager: SyncManager
) {
    fun getAll(): Flow<List<Category>> = categoryDao.getAll()

    suspend fun count(): Int = categoryDao.count()

    suspend fun insert(name: String): Long {
        val id = categoryDao.insert(Category(name = name))
        val saved = categoryDao.getById(id)
        if (saved != null) syncManager.pushCategory(saved)
        return id
    }

    // Restores an exported category with its original id (import path).
    suspend fun insert(category: Category) {
        categoryDao.insert(category)
        syncManager.pushCategory(category)
    }

    suspend fun delete(category: Category) {
        categoryDao.delete(category)
        syncManager.deleteCategory(category.id)
    }
}
