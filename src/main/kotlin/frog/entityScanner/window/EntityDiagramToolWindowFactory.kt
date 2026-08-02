package frog.entityScanner.window

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser

import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea

private val LOG = logger<EntityDiagramToolWindowFactory>()

class EntityDiagramToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow
    ) {

        val panel = JPanel(BorderLayout())

        // контент додаємо одразу: якщо нижче щось впаде, тулвіндов покаже причину,
        // а не порожнє "Nothing to show"
        val content = ContentFactory.getInstance()
            .createContent(panel, "", false)

        toolWindow.contentManager.addContent(content)

        try {
            check(JBCefApp.isSupported()) {
                "JCEF не підтримується поточним рантаймом (потрібен JBR з JCEF)"
            }

            val browser = JBCefBrowser()
            Disposer.register(toolWindow.disposable, browser)

            panel.add(browser.component, BorderLayout.CENTER)

            val html = javaClass.classLoader
                .getResource("web/test.html")
                ?.readText()
                ?: error("web/test.html не знайдено в ресурсах плагіна")

            browser.loadHTML(html)
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (t: Throwable) {
            LOG.error("Не вдалося створити контент тулвіндова Entity Diagram", t)
            panel.add(JScrollPane(JTextArea(t.stackTraceToString())), BorderLayout.CENTER)
            panel.revalidate()
        }
    }
}
