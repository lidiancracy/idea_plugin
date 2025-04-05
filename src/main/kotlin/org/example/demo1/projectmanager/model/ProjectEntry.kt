package org.example.demo1.projectmanager.model

import java.io.File
import java.util.UUID

/**
 * 表示一个项目条目
 */
data class ProjectEntry(
    // 项目路径
    val path: String,
    // 项目别名
    var alias: String,
    // 最后访问时间
    var lastAccessTime: Long = System.currentTimeMillis(),
    // 项目唯一ID
    val id: String = UUID.randomUUID().toString()
) {
    // 获取项目名称（如果别名为空则返回目录名）
    fun getDisplayName(): String {
        if (alias.isNotBlank()) return alias
        return File(path).name
    }
    
    // 检查项目是否存在
    fun exists(): Boolean {
        val file = File(path)
        return file.exists() && file.isDirectory
    }
} 