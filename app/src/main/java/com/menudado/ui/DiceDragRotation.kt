package com.menudado.ui

private const val DICE_DRAG_ROTATION_SENSITIVITY = 0.25f

data class DiceDragRotation(
    val x: Float = 0f,
    val y: Float = 0f
) {
    fun afterDrag(dragX: Float, dragY: Float): DiceDragRotation {
        return DiceDragRotation(
            x = normalizeAngle(x - dragY * DICE_DRAG_ROTATION_SENSITIVITY),
            y = normalizeAngle(y + dragX * DICE_DRAG_ROTATION_SENSITIVITY)
        )
    }
}

private fun normalizeAngle(angle: Float): Float {
    var normalized = angle % 360f
    if (normalized > 180f) {
        normalized -= 360f
    } else if (normalized < -180f) {
        normalized += 360f
    }
    return normalized
}
