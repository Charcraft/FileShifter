package com.example.fileshifter.data.source

import com.example.fileshifter.data.model.Referencia
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Cliente del catalogo abierto Crossref (https://api.crossref.org).
 * Es el servicio que usa el buscador de bibliografia del Modo Entrega.
 */

/** Cuerpo de la respuesta de /works. */
data class CrossrefRespuesta(
    @SerializedName("message") val mensaje: CrossrefMensaje? = null
)

data class CrossrefMensaje(
    @SerializedName("items") val elementos: List<CrossrefItem>? = null
)

data class CrossrefItem(
    @SerializedName("DOI") val doi: String? = null,
    @SerializedName("title") val titulos: List<String>? = null,
    @SerializedName("author") val autores: List<CrossrefAutor>? = null,
    @SerializedName("issued") val publicado: CrossrefFecha? = null,
    @SerializedName("container-title") val contenedor: List<String>? = null,
    @SerializedName("publisher") val editorial: String? = null,
    @SerializedName("type") val tipo: String? = null
)

data class CrossrefAutor(
    @SerializedName("given") val nombre: String? = null,
    @SerializedName("family") val apellido: String? = null
)

data class CrossrefFecha(
    @SerializedName("date-parts") val partes: List<List<Int>>? = null
)

interface CrossrefApi {

    @GET("works")
    suspend fun buscar(
        @Query("query.bibliographic") consulta: String,
        @Query("rows") filas: Int,
        @Query("select") campos: String
    ): CrossrefRespuesta
}

/** Instancia unica del cliente, con tiempos de espera cortos para movil. */
object ClienteReferencias {

    const val CAMPOS = "DOI,title,author,issued,container-title,publisher,type"

    private const val BASE = "https://api.crossref.org/"
    private const val CORREO = "charcraftycarlos@gmail.com"

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .addInterceptor { cadena ->
                cadena.proceed(
                    cadena.request().newBuilder()
                        .header("User-Agent", "FileShifter/1.0 (mailto:$CORREO)")
                        .build()
                )
            }
            .build()
    }

    val api: CrossrefApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CrossrefApi::class.java)
    }

    /** Convierte el JSON de Crossref al modelo que usa la app. */
    fun aReferencia(item: CrossrefItem): Referencia? {
        val titulo = item.titulos?.firstOrNull()?.trim().orEmpty()
        if (titulo.isEmpty()) return null

        val anio = item.publicado?.partes
            ?.firstOrNull()
            ?.firstOrNull()
            ?.toString()
            .orEmpty()

        val autores = item.autores.orEmpty()
            .mapNotNull { autor ->
                val apellido = autor.apellido?.trim().orEmpty()
                val nombre = autor.nombre?.trim().orEmpty()
                when {
                    apellido.isEmpty() && nombre.isEmpty() -> null
                    apellido.isEmpty() -> nombre
                    nombre.isEmpty() -> apellido
                    else -> "$apellido, ${nombre.first()}."
                }
            }
            .let { lista ->
                when {
                    lista.isEmpty() -> ""
                    lista.size <= 4 -> lista.joinToString(", ")
                    else -> lista.take(4).joinToString(", ") + " et al."
                }
            }

        val fuente = item.contenedor?.firstOrNull()?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: item.editorial?.trim().orEmpty()

        return Referencia(
            doi = item.doi.orEmpty(),
            titulo = titulo,
            autores = autores,
            anio = anio,
            fuente = fuente,
            tipo = item.tipo.orEmpty()
        )
    }
}
