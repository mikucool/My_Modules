package com.books_goo_hzz.lib_slot_core

/**
 * A simple, platform-independent data class to represent a 2D coordinate.
 * This replaces the dependency on `java.awt.Point` to ensure compatibility with Android.
 */
data class Point(val x: Int, val y: Int)
