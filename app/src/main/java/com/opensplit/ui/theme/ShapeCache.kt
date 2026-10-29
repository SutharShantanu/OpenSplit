package com.opensplit.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

/**
 * Cached instances of frequently-used shapes to eliminate repeated allocations and recomposition jank.
 * Includes M3 Expressive shapes (Cookie, Clover, SoftBurst) and rounded surface shapes.
 */
object ShapeCache {
    val smooth8 = RoundedCornerShape(8.dp)
    val smooth12 = RoundedCornerShape(12.dp)
    val smooth16 = RoundedCornerShape(16.dp)
    val smooth20 = RoundedCornerShape(20.dp)
    val smooth24 = RoundedCornerShape(24.dp)
    val smooth28 = RoundedCornerShape(28.dp)
    val smooth32 = RoundedCornerShape(32.dp)
    val smoothPill = RoundedCornerShape(50.dp)

    /** M3 Expressive cookie — avatars and profile placeholders */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    val expressiveAvatar: Shape = RoundedPolygonShape(MaterialShapes.Cookie9Sided)

    /** M3 Expressive clover — group and collection icon containers */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    val expressiveClover: Shape = RoundedPolygonShape(MaterialShapes.Clover8Leaf)

    /** M3 Expressive soft burst — empty-state hero icon containers */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    val expressiveHero: Shape = RoundedPolygonShape(MaterialShapes.SoftBurst)

    /** M3 Expressive sunny — insights & balance summaries */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    val expressiveSunny: Shape = RoundedPolygonShape(MaterialShapes.Sunny)
}

private class RoundedPolygonShape(polygon: RoundedPolygon) : Shape {
    private val basePath = polygon.toPath().asComposePath()

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val matrix = Matrix()
        matrix.scale(size.width, size.height)
        val path = Path()
        path.addPath(basePath)
        path.transform(matrix)
        return Outline.Generic(path)
    }
}
