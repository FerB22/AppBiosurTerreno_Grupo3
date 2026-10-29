package com.example.appinventario.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.appinventario.repository.ServicioRepository
import com.example.appinventario.util.UserPreferencesManager

class AppViewModelFactory(
    private val servicioRepository: ServicioRepository,
    private val preferencesManager: UserPreferencesManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ServicioViewModel::class.java) -> {
                ServicioViewModel(servicioRepository) as T
            }
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(preferencesManager) as T
            }
            else -> throw IllegalArgumentException("ViewModel clase desconocida: ${modelClass.name}")
        }
    }
}