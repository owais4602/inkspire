package dev.stupifranc.inkspire.core

data class ItemBox(val key: String, val left: Float, val top: Float, val width: Float, val height: Float)

fun reorderTarget(dragX: Float, dragY: Float, boxes: List<ItemBox>, draggedKey: String): String? {
    val box = boxes.find {
        dragX >= it.left && dragX < it.left + it.width &&
        dragY >= it.top && dragY < it.top + it.height
    }
    if (box == null || box.key == draggedKey) {
        return null
    }
    return box.key
}
