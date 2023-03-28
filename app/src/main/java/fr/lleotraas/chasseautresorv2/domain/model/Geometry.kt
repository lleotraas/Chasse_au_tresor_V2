package fr.lleotraas.chasseautresorv2.domain.model

data class Geometry(
    val coordinates: List<List<Double>>,
    val type: String
)