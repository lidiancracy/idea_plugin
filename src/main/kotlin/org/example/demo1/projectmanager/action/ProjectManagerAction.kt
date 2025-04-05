package org.example.demo1.projectmanager.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import org.example.demo1.projectmanager.ui.ProjectManagerDialog

/**
 * 打开项目管理器的Action
 */
class ProjectManagerAction : AnAction(), DumbAware {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val dialog = ProjectManagerDialog(project)
        dialog.show()
    }
    
    override fun update(e: AnActionEvent) {
        // 只在有项目打开时启用
        e.presentation.isEnabled = e.project != null
    }
} 