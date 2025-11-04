package com.books_goo_hzz.lib_slot_core

import kotlinx.coroutines.runBlocking


/**
 * 主入口文件
 * 用于执行预生成结果盘的批处理任务
 */
fun main() {
    runBlocking {
        DynamicSlotGenerator.findSlotResultByPayout(2400, 1000)
    }
}