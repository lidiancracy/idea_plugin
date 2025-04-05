package org.example.demo1.projectmanager.ui

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import org.example.demo1.projectmanager.model.ProjectEntry
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * 项目编辑对话框
 */
class ProjectEditDialog(
    private val project: Project,
    private val projectEntry: ProjectEntry? = null
) : DialogWrapper(project) {
    
    private val pathField = TextFieldWithBrowseButton()
    private val aliasField = JBTextField()
    
    init {
        title = if (projectEntry == null) "添加项目" else "编辑项目"
        
        // 设置文件选择器
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        descriptor.title = "选择项目文件夹"
        pathField.addBrowseFolderListener("选择项目文件夹", "请选择一个项目文件夹", project, descriptor)
        
        // 如果是编辑模式，填充现有数据
        projectEntry?.let {
            pathField.text = it.path
            aliasField.text = it.alias
            pathField.isEnabled = false // 编辑模式下路径不可修改
        }
        
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        
        val formBuilder = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("项目路径:"), pathField, 1, false)
            .addLabeledComponent(JBLabel("项目别名:"), aliasField, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            
        panel.add(formBuilder.panel, BorderLayout.CENTER)
        panel.preferredSize = Dimension(450, 120)
        
        return panel
    }
    
    // 获取路径
    fun getPath(): String = pathField.text
    
    // 获取别名
    fun getAlias(): String = aliasField.text
} 