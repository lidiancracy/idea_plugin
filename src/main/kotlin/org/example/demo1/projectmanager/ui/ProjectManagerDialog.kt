package org.example.demo1.projectmanager.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import org.example.demo1.projectmanager.model.ProjectEntry
import org.example.demo1.projectmanager.service.ProjectManagerService
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.io.File
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent
import javax.swing.table.AbstractTableModel
import javax.swing.JButton

/**
 * 项目管理主对话框
 */
class ProjectManagerDialog(private val project: Project) : DialogWrapper(project) {
    
    private val service = ProjectManagerService.getInstance()
    private val projectTable = JBTable()
    private val tableModel = ProjectTableModel(service.getAllProjects())
    
    // 搜索框
    private val searchField = JBTextField()
    
    // 排序下拉框
    private val sortComboBox = ComboBox<String>()
    private val sortOptions = arrayOf("名称", "最后访问时间")
    
    // 分页控件
    private val prevPageButton = JButton("上一页")
    private val nextPageButton = JButton("下一页")
    private val pageLabel = JBLabel("第1页/共1页")
    private val pageSize = 10 // 每页显示10个项目
    private var currentPage = 1
    private var totalPages = 1
    
    // 当前搜索和排序状态
    private var currentSearchText = ""
    private var currentSortBy = "名称"
    private var isSortAscending = true // 是否升序排列
    
    init {
        title = "项目管理器"
        setOKButtonText("关闭")
        setCancelButtonText("取消")
        
        // 初始化排序下拉框
        sortComboBox.model = DefaultComboBoxModel(sortOptions)
        sortComboBox.addActionListener {
            currentSortBy = sortComboBox.selectedItem as String
            refreshTable()
        }
        
        // 初始化表格
        projectTable.model = tableModel
        projectTable.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        projectTable.rowHeight = 24
        projectTable.emptyText.text = "没有项目，请点击添加按钮添加项目"
        
        // 初始化搜索框
        searchField.emptyText.text = "输入项目名称搜索..."
        searchField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                currentSearchText = searchField.text.trim()
                currentPage = 1 // 搜索时重置到第一页
                refreshTable()
            }
        })
        
        // 初始化分页按钮
        prevPageButton.addActionListener {
            if (currentPage > 1) {
                currentPage--
                refreshTable()
            }
        }
        
        nextPageButton.addActionListener {
            if (currentPage < totalPages) {
                currentPage++
                refreshTable()
            }
        }
        
        // 初次加载数据
        refreshTable()
        
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        
        // 创建工具栏
        val toolbarGroup = DefaultActionGroup()
        toolbarGroup.add(AddAction())
        toolbarGroup.add(AddCurrentAction())
        toolbarGroup.add(EditAction())
        toolbarGroup.add(RemoveAction())
        toolbarGroup.add(OpenAction())
        toolbarGroup.addSeparator()
        toolbarGroup.add(ToggleSortOrderAction())
        
        val toolbar = ActionManager.getInstance().createActionToolbar("ProjectManagerToolbar", toolbarGroup, true)
        toolbar.setTargetComponent(panel)
        
        // 创建搜索和排序面板
        val topPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        
        // 添加搜索框
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.WEST
        topPanel.add(searchField, gbc)
        
        // 添加排序标签
        gbc.gridx = 1
        gbc.gridy = 0
        gbc.weightx = 0.0
        gbc.fill = GridBagConstraints.NONE
        gbc.anchor = GridBagConstraints.EAST
        topPanel.add(JBLabel("排序方式: "), gbc)
        
        // 添加排序下拉框
        gbc.gridx = 2
        gbc.gridy = 0
        gbc.weightx = 0.0
        gbc.fill = GridBagConstraints.NONE
        gbc.anchor = GridBagConstraints.EAST
        topPanel.add(sortComboBox, gbc)
        
        // 创建表格的滚动面板
        val scrollPane = ScrollPaneFactory.createScrollPane(projectTable)
        
        // 创建分页面板
        val paginationPanel = JPanel(FlowLayout(FlowLayout.CENTER))
        paginationPanel.add(prevPageButton)
        paginationPanel.add(pageLabel)
        paginationPanel.add(nextPageButton)
        
        // 组装面板
        panel.add(toolbar.component, BorderLayout.NORTH)
        panel.add(topPanel, BorderLayout.SOUTH)
        panel.add(scrollPane, BorderLayout.CENTER)
        panel.add(paginationPanel, BorderLayout.SOUTH)
        panel.preferredSize = Dimension(700, 450)
        
        return panel
    }
    
    override fun doOKAction() {
        close(OK_EXIT_CODE)
    }
    
    // 刷新表格数据
    private fun refreshTable() {
        // 获取原始数据
        var filteredProjects = service.getAllProjects()
        
        // 应用搜索过滤
        if (currentSearchText.isNotEmpty()) {
            filteredProjects = filteredProjects.filter { 
                it.getDisplayName().contains(currentSearchText, ignoreCase = true) || 
                it.path.contains(currentSearchText, ignoreCase = true)
            }
        }
        
        // 应用排序
        filteredProjects = when (currentSortBy) {
            "名称" -> {
                if (isSortAscending)
                    filteredProjects.sortedBy { it.getDisplayName().lowercase() }
                else
                    filteredProjects.sortedByDescending { it.getDisplayName().lowercase() }
            }
            "最后访问时间" -> {
                if (isSortAscending)
                    filteredProjects.sortedBy { it.lastAccessTime }
                else
                    filteredProjects.sortedByDescending { it.lastAccessTime }
            }
            else -> filteredProjects
        }
        
        // 计算总页数
        totalPages = (filteredProjects.size + pageSize - 1) / pageSize
        totalPages = if (totalPages < 1) 1 else totalPages
        
        // 确保当前页在有效范围内
        if (currentPage > totalPages) currentPage = totalPages
        if (currentPage < 1) currentPage = 1
        
        // 应用分页
        val startIndex = (currentPage - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, filteredProjects.size)
        val pagedProjects = if (filteredProjects.isEmpty()) {
            emptyList()
        } else {
            filteredProjects.subList(startIndex, endIndex)
        }
        
        // 更新页码标签
        pageLabel.text = "第${currentPage}页/共${totalPages}页"
        
        // 更新分页按钮状态
        prevPageButton.isEnabled = currentPage > 1
        nextPageButton.isEnabled = currentPage < totalPages
        
        // 更新表格数据
        tableModel.projects = pagedProjects
        tableModel.fireTableDataChanged()
    }
    
    // 获取当前选中的项目
    private fun getSelectedProject(): ProjectEntry? {
        val row = projectTable.selectedRow
        if (row < 0 || row >= tableModel.projects.size) return null
        return tableModel.projects[row]
    }
    
    // 添加项目动作
    private inner class AddAction : AnAction("添加项目", "添加一个新项目", AllIcons.General.Add) {
        override fun actionPerformed(e: AnActionEvent) {
            val dialog = ProjectEditDialog(project)
            if (dialog.showAndGet()) {
                val path = dialog.getPath()
                val alias = dialog.getAlias()
                
                if (path.isBlank()) {
                    Messages.showErrorDialog(project, "项目路径不能为空", "错误")
                    return
                }
                
                val newProject = service.addProject(path, alias)
                if (newProject == null) {
                    Messages.showErrorDialog(project, "无效的项目路径：$path", "错误")
                    return
                }
                
                refreshTable()
            }
        }
    }
    
    // 添加当前项目动作 - 更换了图标
    private inner class AddCurrentAction : AnAction("添加当前项目", "添加当前打开的项目", AllIcons.Actions.Annotate) {
        override fun actionPerformed(e: AnActionEvent) {
            val dialog = Messages.showInputDialog(
                project,
                "请为当前项目输入一个别名（可选）：",
                "添加当前项目",
                Messages.getQuestionIcon()
            )
            
            if (dialog != null) { // 可以为null，表示用户取消
                val newProject = service.addCurrentProject(project, dialog)
                if (newProject == null) {
                    Messages.showErrorDialog(project, "无法添加当前项目", "错误")
                    return
                }
                
                refreshTable()
            }
        }
    }
    
    // 切换排序顺序动作
    private inner class ToggleSortOrderAction : AnAction("切换排序顺序", "在升序和降序之间切换", AllIcons.General.ArrowUp) {
        override fun actionPerformed(e: AnActionEvent) {
            isSortAscending = !isSortAscending
            e.presentation.icon = if (isSortAscending) AllIcons.General.ArrowUp else AllIcons.General.ArrowDown
            refreshTable()
        }
        
        override fun update(e: AnActionEvent) {
            e.presentation.icon = if (isSortAscending) AllIcons.General.ArrowUp else AllIcons.General.ArrowDown
        }
    }
    
    // 编辑项目动作
    private inner class EditAction : AnAction("编辑项目", "编辑选中的项目", AllIcons.Actions.Edit) {
        override fun actionPerformed(e: AnActionEvent) {
            val selectedProject = getSelectedProject()
            if (selectedProject == null) {
                Messages.showInfoMessage(project, "请先选择一个项目", "提示")
                return
            }
            
            val dialog = ProjectEditDialog(project, selectedProject)
            if (dialog.showAndGet()) {
                val alias = dialog.getAlias()
                service.updateProject(selectedProject.id, alias)
                refreshTable()
            }
        }
        
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = getSelectedProject() != null
        }
    }
    
    // 删除项目动作
    private inner class RemoveAction : AnAction("删除项目", "删除选中的项目", AllIcons.General.Remove) {
        override fun actionPerformed(e: AnActionEvent) {
            val selectedProject = getSelectedProject()
            if (selectedProject == null) {
                Messages.showInfoMessage(project, "请先选择一个项目", "提示")
                return
            }
            
            val result = Messages.showYesNoDialog(
                project,
                "确定要删除项目 '${selectedProject.getDisplayName()}' 吗？",
                "确认删除",
                AllIcons.General.QuestionDialog
            )
            
            if (result == Messages.YES) {
                service.removeProject(selectedProject.id)
                refreshTable()
            }
        }
        
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = getSelectedProject() != null
        }
    }
    
    // 打开项目动作
    private inner class OpenAction : AnAction("打开项目", "打开选中的项目", AllIcons.Actions.Execute) {
        override fun actionPerformed(e: AnActionEvent) {
            val selectedProject = getSelectedProject()
            if (selectedProject == null) {
                Messages.showInfoMessage(project, "请先选择一个项目", "提示")
                return
            }
            
            if (!selectedProject.exists()) {
                Messages.showErrorDialog(
                    project,
                    "项目路径不存在：${selectedProject.path}",
                    "无法打开项目"
                )
                return
            }
            
            // 通知服务更新访问时间
            service.markProjectAccessed(selectedProject.id)
            
            // 打开项目
            val projectToOpen = File(selectedProject.path)
            val projectManager = ProjectManager.getInstance()
            
            ApplicationManager.getApplication().invokeLater {
                try {
                    projectManager.loadAndOpenProject(projectToOpen.path)
                    refreshTable()
                } catch (ex: Exception) {
                    Messages.showErrorDialog(
                        project,
                        "打开项目失败：${ex.message}",
                        "错误"
                    )
                }
            }
        }
        
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = getSelectedProject() != null
        }
    }
}

/**
 * 项目表格模型
 */
class ProjectTableModel(var projects: List<ProjectEntry>) : AbstractTableModel() {
    
    private val columns = arrayOf("项目名称", "项目路径", "最后访问时间")
    
    override fun getRowCount(): Int = projects.size
    
    override fun getColumnCount(): Int = columns.size
    
    override fun getColumnName(column: Int): String = columns[column]
    
    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val project = projects[rowIndex]
        
        return when (columnIndex) {
            0 -> project.getDisplayName()
            1 -> project.path
            2 -> formatLastAccessTime(project.lastAccessTime)
            else -> ""
        }
    }
    
    // 格式化最后访问时间
    private fun formatLastAccessTime(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return format.format(date)
    }
} 