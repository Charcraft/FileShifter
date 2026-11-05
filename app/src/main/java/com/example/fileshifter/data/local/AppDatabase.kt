package com.example.fileshifter.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Historial de archivos generados.
 * Room guarda solo los metadatos; el archivo vive en la carpeta FileShifter/.
 */
@Entity(tableName = "historial")
data class HistorialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val ruta: String,
    val operacion: String,
    val tamanoBytes: Long,
    val tipoMime: String,
    val creadoEn: Long
)

@Dao
interface HistorialDao {

    @Query("SELECT * FROM historial ORDER BY creadoEn DESC")
    fun observarTodo(): Flow<List<HistorialEntity>>

    @Insert
    suspend fun insertar(item: HistorialEntity): Long

    @Query("DELETE FROM historial WHERE id = :id")
    suspend fun eliminar(id: Long)

    @Query("DELETE FROM historial")
    suspend fun limpiar()
}

@Database(entities = [HistorialEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun historialDao(): HistorialDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun obtener(context: Context): AppDatabase = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "fileshifter.db"
            ).build().also { instancia = it }
        }
    }
}
