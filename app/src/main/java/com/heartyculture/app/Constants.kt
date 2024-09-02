package com.heartyculture.app

object Constants {
    const val MODEL_PATH = "plant_detection.tflite"
    val LABELS_PATH: String? = "labels_plant.txt"

    // enable this to get smooth edges but result in more post process time
    const val SMOOTH_EDGES = false
}