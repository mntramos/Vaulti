package com.vaulti.app.data.database.dao

import androidx.room.*
import com.vaulti.app.data.database.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE ASC")
    fun getAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): Category?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long

    @Delete
    suspend fun delete(category: Category)

    // Case-insensitive lookup, whitespace-tolerant. TRIM() guards against
    // names that differ only by surrounding spaces.
    @Query("SELECT * FROM categories WHERE TRIM(name) = TRIM(:name) COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): Category?

    // Returns the id of the existing category when the name is already taken,
    // otherwise inserts and returns the new id. Atomic, so concurrent adds of
    // the same name cannot both insert.
    @Transaction
    suspend fun insertDeduped(category: Category): Long {
        val existing = findByName(category.name)
        if (existing != null) return existing.id
        return insert(category)
    }

    @Query("SELECT id FROM categories")
    suspend fun getAllIds(): List<Long>

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)
}
