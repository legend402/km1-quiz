package com.km1quiz.app.ui.pixel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * 平滑矢量图标集（Material 图标路径，Apache 2.0）
 * 供非像素皮肤使用：PixelIcon 按字形引用自动查表切换，调用点零改动。
 * 未映射的字形（如像素小车）保持像素渲染。
 */
private fun vector(name: String, path: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        addPath(
            pathData = addPathNodes(path),
            pathFillType = PathFillType.NonZero,
            fill = SolidColor(Color.White),
            fillAlpha = 1f,
            stroke = null
        )
    }.build()

private val vList = vector(
    "List",
    "M3,13h2v-2L3,11v2zM3,17h2v-2L3,15v2zM3,9h2L5,7L3,7v2zM7,13h14v-2L7,11v2zM7,17h14v-2L7,15v2zM7,7v2h14L21,7L7,7z"
)
private val vShuffle = vector(
    "Shuffle",
    "M10.59,9.17L5.41,4 4,5.41l5.17,5.17 1.42,-1.41zM14.5,4l2.04,2.04L4,18.59 5.41,20 17.96,7.46 20,9.5L20,4h-5.5zM14.83,13.41l-1.42,1.42 3.13,3.13L14.5,20L20,20v-5.5l-2.04,2.04 -3.13,-3.13z"
)
private val vAssignmentCheck = vector(
    "AssignmentCheck",
    "M19,3h-4.18C14.4,1.84 13.3,1 12,1c-1.3,0 -2.4,0.84 -2.82,2L5,3c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2L21,5c0,-1.1 -0.9,-2 -2,-2zM12,3c0.55,0 1,0.45 1,1s-0.45,1 -1,1 -1,-0.45 -1,-1 0.45,-1 1,-1zM10,17l-4,-4 1.41,-1.41L10,14.17l6.59,-6.59L18,9l-8,8z"
)
private val vCancel = vector(
    "Cancel",
    "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.41,0 -8,-3.59 -8,-8s3.59,-8 8,-8 8,3.59 8,8 -3.59,8 -8,8zM15.59,7L12,10.59 8.41,7 7,8.41 10.59,12 7,15.59 8.41,17 12,13.41 15.59,17 17,15.59 13.41,12 17,8.41z"
)
private val vStar = vector(
    "Star",
    "M12,17.27L18.18,21l-1.64,-7.03L22,9.24l-7.19,-0.61L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21z"
)
private val vClose = vector(
    "Close",
    "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z"
)
private val vCheck = vector(
    "Check",
    "M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z"
)
private val vVolumeUp = vector(
    "VolumeUp",
    "M3,9v6h4l5,5L12,4L7,9L3,9zM16.5,12c0,-1.77 -1.02,-3.29 -2.5,-4.03v8.05c1.48,-0.73 2.5,-2.25 2.5,-4.02zM14,3.23v2.06c2.89,0.86 5,3.54 5,6.71s-2.11,5.85 -5,6.71v2.06c4.01,-0.91 7,-4.49 7,-8.77s-2.99,-7.86 -7,-8.77z"
)
private val vVolumeOff = vector(
    "VolumeOff",
    "M16.5,12c0,-1.77 -1.02,-3.29 -2.5,-4.03v2.21l2.45,2.45c0.03,-0.2 0.05,-0.41 0.05,-0.63zM19,12c0,0.94 -0.2,1.82 -0.54,2.64l1.51,1.51C20.63,14.91 21,13.5 21,12c0,-4.28 -2.99,-7.86 -7,-8.77v2.06c2.89,0.86 5,3.54 5,6.71zM4.27,3L3,4.27 7.73,9L3,9v6h4l5,5v-6.73l4.25,4.25c-0.67,0.52 -1.42,0.93 -2.25,1.18v2.06c1.38,-0.31 2.63,-0.95 3.69,-1.81L19.73,21 21,19.73l-9,-9L4.27,3zM12,4L9.91,6.09 12,8.18L12,4z"
)
private val vArrowBack = vector(
    "ArrowBack",
    "M20,11L7.83,11l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13L20,13v-2z"
)
private val vArrowForward = vector(
    "ArrowForward",
    "M12,4l-1.41,1.41L16.17,11L4,11v2h12.17l-5.58,5.59L12,20l8,-8z"
)
private val vSchedule = vector(
    "Schedule",
    "M11.99,2C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 11.99,2zM12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8zM12.5,7L11,7v6l5.25,3.15 0.75,-1.23 -4.5,-2.67z"
)

/** 字形引用 → 矢量图标映射；未命中（如像素小车）回落像素渲染 */
val SoftVectors: Map<List<String>, ImageVector> = mapOf(
    Glyphs.list to vList,
    Glyphs.dice to vShuffle,
    Glyphs.paperCheck to vAssignmentCheck,
    Glyphs.xCircle to vCancel,
    Glyphs.star to vStar,
    Glyphs.close to vClose,
    Glyphs.check to vCheck,
    Glyphs.speakerOn to vVolumeUp,
    Glyphs.speakerOff to vVolumeOff,
    Glyphs.arrowLeft to vArrowBack,
    Glyphs.arrowRight to vArrowForward,
    Glyphs.clock to vSchedule
)
