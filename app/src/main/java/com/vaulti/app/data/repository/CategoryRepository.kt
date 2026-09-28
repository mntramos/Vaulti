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
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1L
        val existing = categoryDao.findByName(trimmed)
        if (existing != null) return existing.id
        val id = categoryDao.insert(Category(name = trimmed))
        val saved = categoryDao.getById(id)
        if (saved != null) syncManager.pushCategory(saved)
        return id
    }

    // Restores an exported category with its original id (import path).
    // Duplicate names collapse onto the first match instead of creating a
    // second row.
    suspend fun insert(category: Category) {
        val id = categoryDao.insertDeduped(category)
        val saved = categoryDao.getById(id)
        if (saved != null) syncManager.pushCategory(saved)
    }

    suspend fun delete(category: Category) {
        categoryDao.delete(category)
        syncManager.deleteCategory(category.id)
    }
}
