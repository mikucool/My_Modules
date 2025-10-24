package com.books_goo_hzz.lib_slot_core

import java.awt.Point

/**
 * 定义了所有20条连线的静态数据。
 * 每一条连线都是一个由5个Point组成的列表，Point的x代表列(0-4)，y代表行(0-2)。
 * 数据来源于设计文档。
 */
object Payline {
    val allPaylines: List<List<Point>> = listOf(
        // Line 1: Top row
        listOf(Point(0, 0), Point(1, 0), Point(2, 0), Point(3, 0), Point(4, 0)),
        // Line 2: Middle row
        listOf(Point(0, 1), Point(1, 1), Point(2, 1), Point(3, 1), Point(4, 1)),
        // Line 3: Bottom row
        listOf(Point(0, 2), Point(1, 2), Point(2, 2), Point(3, 2), Point(4, 2)),
        // Line 4: V-shape
        listOf(Point(0, 0), Point(1, 1), Point(2, 2), Point(3, 1), Point(4, 0)),
        // Line 5: Inverted V-shape
        listOf(Point(0, 2), Point(1, 1), Point(2, 0), Point(3, 1), Point(4, 2)),
        // Line 6
        listOf(Point(0, 0), Point(1, 0), Point(2, 1), Point(3, 2), Point(4, 2)),
        // Line 7
        listOf(Point(0, 2), Point(1, 2), Point(2, 1), Point(3, 0), Point(4, 0)),
        // Line 8
        listOf(Point(0, 1), Point(1, 2), Point(2, 1), Point(3, 0), Point(4, 1)),
        // Line 9
        listOf(Point(0, 1), Point(1, 0), Point(2, 1), Point(3, 2), Point(4, 1)),
        // Line 10
        listOf(Point(0, 0), Point(1, 1), Point(2, 0), Point(3, 1), Point(4, 0)),
        // Line 11
        listOf(Point(0, 2), Point(1, 1), Point(2, 2), Point(3, 1), Point(4, 2)),
        // Line 12
        listOf(Point(0, 1), Point(1, 0), Point(2, 0), Point(3, 0), Point(4, 1)),
        // Line 13
        listOf(Point(0, 1), Point(1, 2), Point(2, 2), Point(3, 2), Point(4, 1)),
        // Line 14
        listOf(Point(0, 0), Point(1, 1), Point(2, 1), Point(3, 1), Point(4, 0)),
        // Line 15
        listOf(Point(0, 2), Point(1, 1), Point(2, 1), Point(3, 1), Point(4, 2)),
        // Line 16
        listOf(Point(0, 1), Point(1, 1), Point(2, 0), Point(3, 1), Point(4, 1)),
        // Line 17
        listOf(Point(0, 1), Point(1, 1), Point(2, 2), Point(3, 1), Point(4, 1)),
        // Line 18
        listOf(Point(0, 0), Point(1, 0), Point(2, 2), Point(3, 0), Point(4, 0)),
        // Line 19
        listOf(Point(0, 2), Point(1, 2), Point(2, 0), Point(3, 2), Point(4, 2)),
        // Line 20
        listOf(Point(0, 0), Point(1, 2), Point(2, 2), Point(3, 2), Point(4, 0)),
    )
}
