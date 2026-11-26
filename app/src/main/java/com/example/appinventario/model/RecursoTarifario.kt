package com.example.appinventario.model

/** Categorías operativas de recursos utilizados en terreno por Biosur SpA. */
enum class CategoriaRecurso(val etiqueta: String) {
    PERSONAL("Personal"),
    EQUIPO("Equipos"),
    MATERIAL("Materiales")
}

/** Ítem tarifario seleccionable al registrar una mantención o faena sanitaria. */
data class RecursoTarifario(
    val id: String,
    val nombre: String,
    val categoria: CategoriaRecurso,
    val valorUnitarioClp: Double
)

/** Catálogo predefinido de materiales, personal y equipos con tarifa unitaria propia. */
object CatalogoRecursosBiosur {
    val recursos: List<RecursoTarifario> = listOf(
        RecursoTarifario("PER_CUADRILLA", "Cuadrilla técnica (2 operarios)", CategoriaRecurso.PERSONAL, 45000.0),
        RecursoTarifario("EQ_LIMPIAFOSAS", "Camión limpiafosas", CategoriaRecurso.EQUIPO, 120000.0),
        RecursoTarifario("EQ_BOMBA", "Bomba sumergible / hidrojet", CategoriaRecurso.EQUIPO, 55000.0),
        RecursoTarifario("MAT_QUIMICOS", "Insumos químicos biodegradables", CategoriaRecurso.MATERIAL, 35000.0)
    )

    fun calcularTotal(idsSeleccionados: Set<String>): Double =
        recursos.filter { it.id in idsSeleccionados }.sumOf { it.valorUnitarioClp }
}
