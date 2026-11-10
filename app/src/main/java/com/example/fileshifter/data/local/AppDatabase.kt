package com.example.fileshifter.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

/**
 * Referencias bibliograficas guardadas por el alumno.
 * Son el respaldo local del buscador: se leen sin conexion.
 */
@Entity(tableName = "referencia")
data class ReferenciaEntity(
    @PrimaryKey val doi: String,
    val titulo: String,
    val autores: String,
    val anio: String,
    val fuente: String,
    val tipo: String,
    val guardadoEn: Long
)

@Dao
interface ReferenciaDao {

    @Query("SELECT * FROM referencia ORDER BY guardadoEn DESC")
    fun observar(): Flow<List<ReferenciaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(item: ReferenciaEntity)

    @Query(
        "SELECT * FROM referencia " +
            "WHERE titulo LIKE '%' || :consulta || '%' " +
            "OR autores LIKE '%' || :consulta || '%' " +
            "OR doi LIKE '%' || :consulta || '%' " +
            "ORDER BY guardadoEn DESC"
    )
    suspend fun buscarLocal(consulta: String): List<ReferenciaEntity>

    @Query("DELETE FROM referencia WHERE doi = :doi")
    suspend fun eliminar(doi: String)

    @Query("DELETE FROM referencia")
    suspend fun limpiar()
}

@Database(entities = [HistorialEntity::class, ReferenciaEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun historialDao(): HistorialDao

    abstract fun referenciaDao(): ReferenciaDao

    companion object {

        /** La version 2 agrega la tabla de referencias bibliograficas. */
        private val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `referencia` (" +
                        "`doi` TEXT NOT NULL, " +
                        "`titulo` TEXT NOT NULL, " +
                        "`autores` TEXT NOT NULL, " +
                        "`anio` TEXT NOT NULL, " +
                        "`fuente` TEXT NOT NULL, " +
                        "`tipo` TEXT NOT NULL, " +
                        "`guardadoEn` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`doi`))"
                )
            }
        }

        @Volatile
        private var instancia: AppDatabase? = null

        fun obtener(context: Context): AppDatabase = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "fileshifter.db"
            )
                .addMigrations(MIGRACION_1_2)
                .build()
                .also { instancia = it }
        }
    }
}
