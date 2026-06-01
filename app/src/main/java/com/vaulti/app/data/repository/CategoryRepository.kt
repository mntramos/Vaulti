package com.vaulti.app.data.repository

import com.vaulti.app.data.database.dao.CategoryDao
import com.vaulti.app.data.database.entity.Category
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {
    fun getAll(): Flow<List<Category>> = categoryDao.getAll()

    suspend fun count(): Int = categoryDao.count()

    suspend fun insert(name: String) {
        categoryDao.insert(Category(name = name))
    }

    suspend fun delete(category: Category) {
        categoryDao.delete(category)
    }
}
