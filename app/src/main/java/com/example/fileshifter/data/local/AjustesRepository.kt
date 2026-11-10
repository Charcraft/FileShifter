package com.example.fileshifter.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.fileshifter.data.model.AjustesFlujo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fileshifter_ajustes")

class AjustesRepository(private val context: Context) {
    private object Claves {
        val CORRECTOR = booleanPreferencesKey("sugerir_corrector")
        val ENTREGA = booleanPreferencesKey("sugerir_entrega")
        val VALIDADOR = booleanPreferencesKey("sugerir_validador")
        val NO_PREGUNTAR = booleanPreferencesKey("no_preguntar_mas")
    }

    val ajustes: Flow<AjustesFlujo> = context.dataStore.data.map { prefs ->
        AjustesFlujo(
            sugerirCorrector = prefs[Claves.CORRECTOR] ?: true,
            sugerirEntrega = prefs[Claves.ENTREGA] ?: true,
            sugerirValidador = prefs[Claves.VALIDADOR] ?: true,
            noPreguntarMas = prefs[Claves.NO_PREGUNTAR] ?: false
        )
    }

    suspend fun setSugerirCorrector(valor: Boolean) = guardar(Claves.CORRECTOR, valor)
    suspend fun setSugerirEntrega(valor: Boolean) = guardar(Claves.ENTREGA, valor)
    suspend fun setSugerirValidador(valor: Boolean) = guardar(Claves.VALIDADOR, valor)
    suspend fun setNoPreguntarMas(valor: Boolean) = guardar(Claves.NO_PREGUNTAR, valor)

    private suspend fun guardar(clave: Preferences.Key<Boolean>, valor: Boolean) {
        context.dataStore.edit { prefs -> prefs[clave] = valor }
    }
}
