package com.example.appinventario.repository

import com.example.appinventario.model.Servicio
import kotlinx.coroutines.flow.Flow

class ServicioRepository(private val servicioDao: ServicioDao) {

    val todosLosServicios: Flow<List<Servicio>> = servicioDao.obtenerTodosLosServicios()

    suspend fun obtenerPorId(id: Long): Servicio? {
        return servicioDao.obtenerServicioPorId(id)
    }

    suspend fun guardarServicio(servicio: Servicio) {
        servicioDao.insertarServicio(servicio)
    }

    suspend fun actualizarServicio(servicio: Servicio) {
        servicioDao.actualizarServicio(servicio)
    }

    suspend fun eliminarServicio(servicio: Servicio) {
        servicioDao.eliminarServicio(servicio)
    }
}