package com.example.fileshifter.data.repository

import com.example.fileshifter.data.local.ReferenciaDao
import com.example.fileshifter.data.local.ReferenciaEntity
import com.example.fileshifter.data.model.Referencia
import com.example.fileshifter.data.source.ClienteReferencias
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface ReferenciasRepository {
    val guardadas: Flow<List<ReferenciaEntity>>

    suspend fun buscar(consulta: String): Result<List<Referencia>>

    suspend fun buscarGuardadas(consulta: String): List<Referencia>

    suspend fun guardar(referencia: Referencia)

    suspend fun eliminar(doi: String)

    suspend fun limpiar()
}

class ReferenciasRepositoryImpl(
    private val dao: ReferenciaDao
) : ReferenciasRepository {
    override val guardadas: Flow<List<ReferenciaEntity>> = dao.observar()

    override suspend fun buscar(consulta: String): Result<List<Referencia>> =
        withContext(Dispatchers.IO) {
            var ultimoError: Throwable? = null

            repeat(2) { intento ->
                try {
                    val respuesta = ClienteReferencias.api.buscar(
                        consulta = consulta.trim(),
                        filas = 12,
                        campos = ClienteReferencias.CAMPOS
                    )
                    val elementos = respuesta.mensaje?.elementos.orEmpty()
                    val referencias = elementos.mapNotNull { ClienteReferencias.aReferencia(it) }
                    return@withContext Result.success(referencias)
                } catch (e: Exception) {
                    ultimoError = e
                    if (intento == 0) delay(600)
                }
            }

            Result.failure(ultimoError ?: IllegalStateException("Sin respuesta del catálogo"))
        }

    override suspend fun buscarGuardadas(consulta: String): List<Referencia> =
        withContext(Dispatchers.IO) {
            dao.buscarLocal(consulta.trim()).map { it.aModelo() }
        }

    override suspend fun guardar(referencia: Referencia) = withContext(Dispatchers.IO) {
        dao.guardar(
            ReferenciaEntity(
                doi = referencia.doi.ifBlank { referencia.titulo.hashCode().toString() },
                titulo = referencia.titulo,
                autores = referencia.autores,
                anio = referencia.anio,
                fuente = referencia.fuente,
                tipo = referencia.tipo,
                guardadoEn = System.currentTimeMillis()
            )
        )
    }

    override suspend fun eliminar(doi: String) = withContext(Dispatchers.IO) {
        dao.eliminar(doi)
    }

    override suspend fun limpiar() = withContext(Dispatchers.IO) {
        dao.limpiar()
    }
}

fun ReferenciaEntity.aModelo(): Referencia = Referencia(
    doi = doi,
    titulo = titulo,
    autores = autores,
    anio = anio,
    fuente = fuente,
    tipo = tipo
)
