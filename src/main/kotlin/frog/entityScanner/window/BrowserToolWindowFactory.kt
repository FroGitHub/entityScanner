package frog.entityScanner.window

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.SwingUtilities

class BrowserToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val panel = JPanel(BorderLayout())

        // Панель управління (введення лінки та кнопка)
        val topPanel = JPanel(BorderLayout())
        val urlField = JTextField("https://example.com") // Для старту краще використовувати прості сайти без важкого JS
        val goButton = JButton("Go")
        topPanel.add(urlField, BorderLayout.CENTER)
        topPanel.add(goButton, BorderLayout.EAST)
        panel.add(topPanel, BorderLayout.NORTH)

        // Створюємо стандартний вбудований Java HTML-браузер
        val htmlPage = JEditorPane()
        htmlPage.isEditable = false
        htmlPage.contentType = "text/html"

        // Загортаємо в Scroll Pane для прокручування
        val scrollPane = JBScrollPane(htmlPage)
        panel.add(scrollPane, BorderLayout.CENTER)

        try {
            htmlPage.setPage("https://example.com")
        } catch (e: Exception) {
            htmlPage.text = "<html><body><h4>Не вдалося завантажити сторінку: ${e.message}</h4></body></html>"
        }

        // Логіка для кнопки Go
        goButton.addActionListener {
            var targetUrl = urlField.text.trim()
            if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
                targetUrl = "https://$targetUrl"
            }

            // Запускаємо завантаження у фоновому потоці, щоб IDE не зависала
            Thread {
                try {
                    htmlPage.setPage(targetUrl)
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        htmlPage.text = "<html><body><h4>Помилка завантаження $targetUrl: ${e.message}</h4></body></html>"
                    }
                }
            }.start()
        }

        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
