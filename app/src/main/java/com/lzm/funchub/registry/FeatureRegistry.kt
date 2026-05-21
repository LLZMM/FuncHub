package com.lzm.funchub.registry

object FeatureRegistry {
    private val _features = mutableListOf<FeatureEntry>()
    val features: List<FeatureEntry> get() = _features.toList()

    fun register(feature: FeatureEntry) {
        _features.add(feature)
    }
}
