package org.example.demo1.projectmanager.service

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.util.xmlb.XmlSerializerUtil
import org.example.demo1.projectmanager.model.ProjectEntry
import java.io.File

/**
 * 存储项目条目列表的服务类
 */
@State(
    name = "ProjectManagerSettings",
    storages = [Storage("project_manager_settings.xml")]
)
class ProjectManagerService : PersistentStateComponent<ProjectManagerService> {
    private val LOG = Logger.getInstance(ProjectManagerService::class.java)
    
    // 项目列表
    var projects: MutableList<ProjectEntry> = mutableListOf()
    
    // 添加项目
    fun addProject(path: String, alias: String = ""): ProjectEntry? {
        // 检查路径是否有效
        val file = File(path)
        if (!file.exists() || !file.isDirectory) {
            LOG.warn("尝试添加无效的项目路径: $path")
            return null
        }
        
        // 检查是否已存在
        if (projects.any { it.path == path }) {
            LOG.info("尝试添加已存在的项目: $path")
            return projects.find { it.path == path }
        }
        
        // 创建并添加新项目
        val newProject = ProjectEntry(path, alias)
        projects.add(newProject)
        LOG.info("添加新项目: $path, 别名: $alias")
        return newProject
    }
    
    // 删除项目
    fun removeProject(id: String): Boolean {
        val size = projects.size
        projects.removeIf { it.id == id }
        return size != projects.size
    }
    
    // 更新项目
    fun updateProject(id: String, alias: String): Boolean {
        val project = projects.find { it.id == id } ?: return false
        project.alias = alias
        return true
    }
    
    // 获取项目
    fun getProject(id: String): ProjectEntry? {
        return projects.find { it.id == id }
    }
    
    // 获取所有项目
    fun getAllProjects(): List<ProjectEntry> {
        return projects.toList()
    }
    
    // 标记项目已访问
    fun markProjectAccessed(id: String) {
        val project = projects.find { it.id == id } ?: return
        project.lastAccessTime = System.currentTimeMillis()
    }
    
    // 添加当前打开的项目
    fun addCurrentProject(project: Project, alias: String = ""): ProjectEntry? {
        val path = project.basePath ?: return null
        return addProject(path, alias)
    }
    
    // PersistentStateComponent接口实现
    override fun getState(): ProjectManagerService = this
    
    override fun loadState(state: ProjectManagerService) {
        XmlSerializerUtil.copyBean(state, this)
    }
    
    companion object {
        // 获取服务实例
        fun getInstance(): ProjectManagerService {
            return ServiceManager.getService(ProjectManagerService::class.java)
        }
    }
} 