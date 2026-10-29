package com.example.appinventario.model

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromTipoServicio(value: TipoServicio): String = value.name

    @TypeConverter
    fun toTipoServicio(value: String): TipoServicio = TipoServicio.valueOf(value)

    @TypeConverter
    fun fromEstadoServicio(value: EstadoServicio): String = value.name

    @TypeConverter
    fun toEstadoServicio(value: String): EstadoServicio = EstadoServicio.valueOf(value)

    @TypeConverter
    fun fromRolUsuario(value: RolUsuario): String = value.name

    @TypeConverter
    fun toRolUsuario(value: String): RolUsuario = RolUsuario.valueOf(value)
}