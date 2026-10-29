package com.example.appinventario.repository

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.appinventario.model.Servicio
import kotlinx.coroutines.flow.Flow

@Dao
interface ServicioDao {

    @Query("SELECT * FROM servicios ORDER BY id DESC")
    fun obtenerTodosLosServicios(): Flow<List<Servicio>>

    @Query("SELECT * FROM servicios WHERE id = :id LIMIT 1")
    suspend fun obtenerServicioPorId(id: Long): Servicio?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarServicio(servicio: Servicio)

    @Update
    suspend fun actualizarServicio(servicio: Servicio)

    @Delete
    suspend fun eliminarServicio(servicio: Servicio)
}