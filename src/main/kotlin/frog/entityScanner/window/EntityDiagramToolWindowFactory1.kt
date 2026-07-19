package frog.entityScanner.window

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import frog.entityScanner.model.EntityGraph
import frog.entityScanner.renderer.MermaidRenderer
import frog.entityScanner.scanner.EntityScanner
import java.awt.BorderLayout
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.swing.JButton
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.SwingUtilities

class EntityDiagramToolWindowFactory1 : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val mainPanel = JPanel(BorderLayout())

        val topPanel = JPanel(BorderLayout())
        val refreshButton = JButton("Оновити діаграму")
        topPanel.add(refreshButton, BorderLayout.WEST)
        mainPanel.add(topPanel, BorderLayout.NORTH)

        val htmlPage = JEditorPane()
        htmlPage.isEditable = false
        htmlPage.contentType = "text/html"

        val scrollPane = JBScrollPane(htmlPage)
        mainPanel.add(scrollPane, BorderLayout.CENTER)

        // Функція, яка запускає сканер і малює HTML
        fun loadAndRenderDiagram() {
            htmlPage.text = "<html> <body style='font-family: sans-serif; padding: 20px; color: #a9b7c6; background-color: #2b2b2b;'><h3>Сканування проєкту та генерація графа...</h3></body></html>"

            // Запускаємо у фоновому потоці, щоб інтерфейс IDE не фрізився
            Thread {
                try {
                    // Змінна для збереження результату сканування
                    var graph: EntityGraph? = null

                    // ОЦЕЙ БЛОК ВИПРАВЛЯЄ ПОМИЛКУ THREADING:
                    // Ми явно кажемо IntelliJ, що безпечно читаємо індекс / структуру коду
                    com.intellij.openapi.application.runReadAction {
                        graph = EntityScanner(project).scan()
                    }

                    // Перевіряємо, чи успішно пройшло сканування
                    val currentGraph = graph
                    val htmlContent = if (currentGraph == null || currentGraph.entities.isEmpty()) {
                        "<html><body style='font-family: sans-serif; padding: 20px; color: #a9b7c6; background-color: #2b2b2b;'><h3>У проєкті не знайдено жодної сутності.</h3></body></html>"
                    } else {
                        val mermaidCode = MermaidRenderer().render(currentGraph)
                        val encodedMermaid = Base64.getEncoder().encodeToString(
                            mermaidCode.toByteArray(StandardCharsets.UTF_8)
                        )
                        val imageUrl = "https://mermaid.ink/img/$encodedMermaid"

                        """
                    <html>
                    <body style="font-family: sans-serif; padding: 10px; background-color: #2b2b2b; color: #a9b7c6;">
                        <div style="text-align: center; margin-top: 20px;">
                            <img src="$imageUrl" alt="Loading Diagram..." />
                        </div>
                    </body>
                    </html>
                """.trimIndent()
                    }

                    // Повертаємося в головний графічний потік Swing для оновлення тексту
                    SwingUtilities.invokeLater {
                        htmlPage.text = htmlContent
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    SwingUtilities.invokeLater {
                        htmlPage.text = "<html><body><h4>Помилка під час сканування: ${e.message}</h4></body></html>"
                    }
                }
            }.start()
        }

        refreshButton.addActionListener {
            loadAndRenderDiagram()
        }

        loadAndRenderDiagram()

        val content = contentFactory.createContent(mainPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
