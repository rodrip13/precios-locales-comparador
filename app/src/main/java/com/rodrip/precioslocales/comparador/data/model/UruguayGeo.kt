package com.rodrip.precioslocales.comparador.data.model

/**
 * Catálogo estático de departamentos y localidades de Uruguay con ≥2.000 habitantes (fuente INE).
 * Cada lista de localidades termina con la opción especial OTRA para texto libre.
 */
object UruguayGeo {

    const val OTRA = "Otra…"

    data class Departamento(val nombre: String, val localidades: List<String>)

    val departamentos: List<Departamento> = listOf(
        Departamento(
            "Artigas",
            listOf("Artigas", "Bella Unión", "Baltasar Brum", "Tomás Gomensoro", OTRA)
        ),
        Departamento(
            "Canelones",
            listOf(
                "Barros Blancos", "Canelones", "Ciudad de la Costa", "Empalme Olmos",
                "La Floresta", "La Paz", "Las Piedras", "Migues", "Pando", "Paso Carrasco",
                "Progreso", "San Bautista", "San Jacinto", "San Ramón", "Santa Lucía",
                "Santa Rosa", "Sauce", "Solymar", OTRA
            )
        ),
        Departamento(
            "Cerro Largo",
            listOf("Fraile Muerto", "Melo", "Río Branco", OTRA)
        ),
        Departamento(
            "Colonia",
            listOf(
                "Carmelo", "Colonia del Sacramento", "Juan Lacaze", "Nueva Palmira",
                "Ombúes de Lavalle", "Rosario", "Tarariras", OTRA
            )
        ),
        Departamento(
            "Durazno",
            listOf("Durazno", "Carlos Reyles", "Sarandí del Yí", "Villa del Carmen", OTRA)
        ),
        Departamento(
            "Flores",
            listOf("Trinidad", OTRA)
        ),
        Departamento(
            "Florida",
            listOf("25 de Agosto", "Florida", "Fray Marcos", "Sarandí Grande", OTRA)
        ),
        Departamento(
            "Lavalleja",
            listOf("José Batlle y Ordóñez", "Mariscala", "Minas", "Solís de Mataojo", OTRA)
        ),
        Departamento(
            "Maldonado",
            listOf("Aiguá", "Maldonado", "Pan de Azúcar", "Piriápolis", "Punta del Este", "San Carlos", OTRA)
        ),
        Departamento(
            "Montevideo",
            listOf("Montevideo", OTRA)
        ),
        Departamento(
            "Paysandú",
            listOf("Guichón", "Paysandú", "Quebracho", OTRA)
        ),
        Departamento(
            "Río Negro",
            listOf("Fray Bentos", "San Javier", "Young", OTRA)
        ),
        Departamento(
            "Rivera",
            listOf("Minas de Corrales", "Rivera", "Tranqueras", "Vichadero", OTRA)
        ),
        Departamento(
            "Rocha",
            listOf("Castillos", "Chuy", "La Paloma", "Lascano", "Rocha", "Velázquez", OTRA)
        ),
        Departamento(
            "Salto",
            listOf("Belén", "Constitución", "Salto", OTRA)
        ),
        Departamento(
            "San José",
            listOf("Ecilda Paullier", "Libertad", "Rodríguez", "San José de Mayo", OTRA)
        ),
        Departamento(
            "Soriano",
            listOf("Cardona", "Dolores", "Mercedes", "Palmitas", OTRA)
        ),
        Departamento(
            "Tacuarembó",
            listOf("Paso de los Toros", "San Gregorio de Polanco", "Tacuarembó", OTRA)
        ),
        Departamento(
            "Treinta y Tres",
            listOf("Santa Clara de Olimar", "Treinta y Tres", "Vergara", OTRA)
        )
    )

    /** Nombres de los 19 departamentos. */
    fun getDepartmentNames(): List<String> = departamentos.map { it.nombre }

    /**
     * Localidades predefinidas del departamento dado, incluyendo OTRA al final.
     * Devuelve [OTRA] si el departamento no existe.
     */
    fun getLocalidades(department: String): List<String> =
        departamentos.find { it.nombre == department }?.localidades ?: listOf(OTRA)

    /**
     * Calcula distancia en metros entre dos coordenadas usando la fórmula de Haversine.
     */
    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }

    /** Formatea metros a texto legible (ej. "250 m" o "1.3 km"). */
    fun formatDistance(meters: Double): String =
        if (meters < 1000) "${meters.toInt()} m" else "${"%.1f".format(meters / 1000)} km"
}

