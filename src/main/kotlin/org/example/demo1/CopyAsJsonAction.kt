package org.example.demo1

import com.google.gson.GsonBuilder
import com.intellij.debugger.engine.JavaValue
import com.intellij.debugger.ui.impl.watch.ValueDescriptorImpl
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTree
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl
import com.sun.jdi.*
import java.awt.datatransfer.StringSelection
import java.util.concurrent.atomic.AtomicInteger

class CopyAsJsonAction : AnAction() {
    private val LOG = Logger.getInstance(CopyAsJsonAction::class.java)
    private val MAX_DEPTH = 10 // 防止循环引用导致无限递归
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val debugSession = XDebuggerManager.getInstance(project).currentSession ?: return
        
        val tree = e.getData(XDebuggerTree.XDEBUGGER_TREE_KEY)
        val node = tree?.selectionPath?.lastPathComponent as? XValueNodeImpl ?: return
        
        val xValue = node.valueContainer
        if (xValue !is JavaValue) {
            Messages.showErrorDialog(project, "只支持Java对象", "无法复制为JSON")
            return
        }
        
        try {
            val descriptor = xValue.descriptor as? ValueDescriptorImpl
            if (descriptor == null) {
                Messages.showErrorDialog(project, "无法获取对象描述符", "转换失败")
                return
            }
            
            val jdiValue = descriptor.value
            if (jdiValue == null) {
                CopyPasteManager.getInstance().setContents(StringSelection("null"))
                Messages.showInfoMessage(project, "已复制: null", "成功")
                return
            }
            
            // 处理原始类型和字符串的简单情况
            if (jdiValue is PrimitiveValue || jdiValue is StringReference) {
                val text = when (jdiValue) {
                    is StringReference -> "\"${StringUtil.escapeStringCharacters(jdiValue.value())}\""
                    else -> jdiValue.toString()
                }
                CopyPasteManager.getInstance().setContents(StringSelection(text))
                Messages.showInfoMessage(project, "已复制: $text", "成功")
                return
            }
            
            // 对于复杂对象，使用我们的转换逻辑
            val jsonObject = convertToJsonObject(jdiValue, AtomicInteger(0))
            
            // 使用Gson美化输出
            val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
            val jsonString = gson.toJson(jsonObject)
            
            // 复制到剪贴板
            ApplicationManager.getApplication().invokeLater {
                CopyPasteManager.getInstance().setContents(StringSelection(jsonString))
                Messages.showInfoMessage(project, "已复制对象为JSON", "成功")
            }
        } catch (ex: Exception) {
            LOG.error("转换对象时发生错误", ex)
            Messages.showErrorDialog(project, "转换对象时发生错误: ${ex.message}", "转换失败")
        }
    }
    
    private fun convertToJsonObject(value: Value?, depth: AtomicInteger): Any? {
        if (value == null) return null
        if (depth.incrementAndGet() > MAX_DEPTH) return "[对象嵌套过深]"
        
        try {
            return when (value) {
                is PrimitiveValue -> {
                    when (value) {
                        is BooleanValue -> value.value()
                        is ByteValue -> value.value()
                        is ShortValue -> value.value()
                        is IntegerValue -> value.value()
                        is LongValue -> value.value()
                        is FloatValue -> value.value()
                        is DoubleValue -> value.value()
                        is CharValue -> value.value().toString()
                        else -> value.toString()
                    }
                }
                is StringReference -> value.value()
                is ArrayReference -> {
                    val list = mutableListOf<Any?>()
                    for (i in 0 until value.length()) {
                        list.add(convertToJsonObject(value.getValue(i), AtomicInteger(depth.get())))
                    }
                    list
                }
                is ObjectReference -> {
                    // 对于复杂对象，仅处理字段，不调用方法
                    processObject(value, depth)
                }
                else -> value.toString()
            }
        } finally {
            depth.decrementAndGet()
        }
    }
    
    private fun processObject(value: ObjectReference, depth: AtomicInteger): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        val referenceType = value.referenceType()
        
        // 获取类名，但不添加到结果中
        val className = referenceType.name()
        
        // 特殊处理一些常见类型
        when {
            className.startsWith("java.util.ArrayList") ||
            className.startsWith("java.util.LinkedList") ||
            className.startsWith("java.util.Vector") -> {
                // 尝试直接获取数组的底层存储
                try {
                    val elementDataField = referenceType.fieldByName("elementData")
                    if (elementDataField != null) {
                        val arrayRef = value.getValue(elementDataField) as? ArrayReference
                        if (arrayRef != null) {
                            val sizeField = referenceType.fieldByName("size")
                            val size = (value.getValue(sizeField) as? IntegerValue)?.value() ?: arrayRef.length()
                            
                            val elements = mutableListOf<Any?>()
                            for (i in 0 until size) {
                                if (i < arrayRef.length()) {
                                    elements.add(convertToJsonObject(arrayRef.getValue(i), AtomicInteger(depth.get())))
                                }
                            }
                            result["elements"] = elements
                            return result
                        }
                    }
                } catch (e: Exception) {
                    LOG.warn("处理ArrayList时出错", e)
                }
            }
            className.startsWith("java.util.HashMap") ||
            className.startsWith("java.util.LinkedHashMap") -> {
                // 尝试获取HashMap的table数组和EntrySet
                try {
                    val tableField = referenceType.fieldByName("table")
                    if (tableField != null) {
                        val tableArray = value.getValue(tableField) as? ArrayReference
                        if (tableArray != null) {
                            val entries = mutableMapOf<String, Any?>()
                            
                            // 遍历table数组中的每个节点
                            for (i in 0 until tableArray.length()) {
                                val entryNodeRef = tableArray.getValue(i)
                                if (entryNodeRef !is ObjectReference) continue
                                
                                var currentNode: ObjectReference? = entryNodeRef
                                while (currentNode != null) {
                                    val nodeType = currentNode.referenceType()
                                    val keyField = nodeType.fieldByName("key")
                                    val valueField = nodeType.fieldByName("value")
                                    val nextField = nodeType.fieldByName("next")
                                    
                                    if (keyField != null && valueField != null) {
                                        val keyObj = currentNode.getValue(keyField)
                                        val valueObj = currentNode.getValue(valueField)
                                        
                                        val keyString = when (keyObj) {
                                            is StringReference -> keyObj.value()
                                            is PrimitiveValue -> keyObj.toString()
                                            null -> "null"
                                            else -> keyObj.toString()
                                        }
                                        
                                        entries[keyString] = convertToJsonObject(valueObj, AtomicInteger(depth.get()))
                                    }
                                    
                                    // 移动到下一个节点，仔细处理null值
                                    currentNode = if (nextField != null) {
                                        val nextNode = currentNode.getValue(nextField)
                                        if (nextNode is ObjectReference) nextNode else null
                                    } else {
                                        null
                                    }
                                }
                            }
                            
                            result["entries"] = entries
                            return result
                        }
                    }
                } catch (e: Exception) {
                    LOG.warn("处理HashMap时出错", e)
                }
            }
        }
        
        // 获取所有字段（包括继承的）
        val fields = getAllFields(referenceType)
        
        // 处理所有字段
        for (field in fields) {
            if (field.isStatic()) continue // 跳过静态字段
            
            val fieldName = field.name()
            val fieldValue = value.getValue(field)
            
            // 忽略合成字段和this$0这样的引用外部类的字段
            if (!field.isSynthetic() && !fieldName.startsWith("this$")) {
                result[fieldName] = convertToJsonObject(fieldValue, AtomicInteger(depth.get()))
            }
        }
        
        return result
    }
    
    private fun getAllFields(type: ReferenceType): List<Field> {
        val fields = mutableListOf<Field>()
        
        if (type is ClassType) {
            // 获取当前类型的字段
            fields.addAll(type.fields())
            
            // 递归获取父类的字段
            var superclass = type.superclass()
            while (superclass != null) {
                fields.addAll(superclass.fields())
                superclass = superclass.superclass()
            }
        } else {
            fields.addAll(type.fields())
        }
        
        return fields
    }
    
    override fun update(e: AnActionEvent) {
        val project = e.project
        val debugSession = if (project != null) XDebuggerManager.getInstance(project).currentSession else null
        val tree = e.getData(XDebuggerTree.XDEBUGGER_TREE_KEY)
        val node = tree?.selectionPath?.lastPathComponent as? XValueNodeImpl
        
        // 只在调试会话中，且选中了值节点时启用
        e.presentation.isEnabled = debugSession != null && node != null
    }
} 