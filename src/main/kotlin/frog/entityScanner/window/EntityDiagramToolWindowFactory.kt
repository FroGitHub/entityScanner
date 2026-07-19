package frog.entityScanner.window

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefBrowser

import java.awt.BorderLayout
import javax.swing.JPanel

class EntityDiagramToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(
        project: com.intellij.openapi.project.Project,
        toolWindow: ToolWindow
    ) {

        val panel = JPanel(BorderLayout())

        val browser = JBCefBrowser()

        panel.add(browser.component, BorderLayout.CENTER)

        val html = javaClass.classLoader
            .getResource("web/test.html")
            ?.readText()
            ?: "<html><body>NOT FOUND</body></html>"

        browser.loadHTML(html)

        val content = ContentFactory.getInstance()
            .createContent(panel, "", false)

        toolWindow.contentManager.addContent(content)
    }
}
