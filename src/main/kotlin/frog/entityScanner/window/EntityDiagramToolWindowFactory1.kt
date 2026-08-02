package frog.entityScanner.window

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import frog.entityScanner.model.EntityGraph
import frog.entityScanner.renderer.MermaidRenderer
import frog.entityScanner.scanner.EntityScanner
import java.awt.BorderLayout
import java.nio.charset.StandardCharsets
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities

class EntityDiagramToolWindowFactory1 : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        if (!JBCefApp.isSupported()) {
            // fallback якщо JCEF недоступний на цій платформі
            val panel = JPanel(BorderLayout())
            panel.add(JLabel("JCEF не підтримується в цій IDE/ОС"), BorderLayout.CENTER)
            val content = ContentFactory.getInstance().createContent(panel, "", false)
            toolWindow.contentManager.addContent(content)
            return
        }

        val mainPanel = JPanel(BorderLayout())

        val topPanel = JPanel(BorderLayout())
        val refreshButton = JButton("Оновити діаграму")
        topPanel.add(refreshButton, BorderLayout.WEST)
        mainPanel.add(topPanel, BorderLayout.NORTH)

        val browser = JBCefBrowser()
        Disposer.register(toolWindow.disposable, browser)
        mainPanel.add(browser.component, BorderLayout.CENTER)

        fun loadAndRenderDiagram() {
            browser.loadHTML(loadingHtml())

            Thread {
                try {
                    val scanner = EntityScanner(project)

                    // чекаємо на завершення індексації: у dumb mode резолв анотацій
                    // кидає IndexNotReadyException
                    val currentGraph: EntityGraph = DumbService.getInstance(project)
                        .runReadActionInSmartMode<EntityGraph> { scanner.scan() }

                    val html = if (currentGraph.entities.isEmpty()) {
                        emptyHtml(scanner.scannedFiles, scanner.scannedClasses)
                    } else {
                        val mermaidCode = MermaidRenderer().render(currentGraph)
                        diagramHtml(mermaidCode)
                    }

                    SwingUtilities.invokeLater {
                        browser.loadHTML(html)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    SwingUtilities.invokeLater {
                        browser.loadHTML(errorHtml(e.message ?: "unknown error"))
                    }
                }
            }.start()
        }

        refreshButton.addActionListener { loadAndRenderDiagram() }
        loadAndRenderDiagram()

        val content = ContentFactory.getInstance().createContent(mainPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun loadingHtml() =
        darkPage("<h3>Очікування індексації, сканування проєкту та генерація графа...</h3>")

    private fun emptyHtml(files: Int, classes: Int) = darkPage(
        """
        <h3>У проєкті не знайдено жодної сутності.</h3>
        <p>Переглянуто .java файлів: $files, класів: $classes.</p>
        """.trimIndent()
    )
    private fun errorHtml(msg: String) = darkPage("<h4>Помилка під час сканування: ${msg.escapeHtml()}</h4>")

    private fun darkPage(bodyHtml: String) = """
        <html><body style="font-family: sans-serif; padding: 20px; color: #a9b7c6; background-color: #2b2b2b;">
        $bodyHtml
        </body></html>
    """.trimIndent()

    private fun diagramHtml(mermaidCode: String): String {
        val mermaidJs = loadMermaidJsSource()
        val escapedMermaid = mermaidCode
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("$", "\\$")

        return """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="utf-8">
        <style>
            html, body {
                margin: 0; padding: 0; height: 100%;
                background-color: #2b2b2b;
                overflow: hidden;
            }
            #viewport {
                width: 100%; height: 100%;
                overflow: hidden;
                cursor: grab;
            }
            #viewport.dragging { cursor: grabbing; }
            #stage {
                transform-origin: 0 0;
                width: fit-content;
            }
            .toolbar {
                position: fixed; top: 8px; right: 8px; z-index: 10;
                display: flex; gap: 4px;
            }
            .toolbar button {
                background: #3c3f41; color: #a9b7c6; border: 1px solid #555;
                border-radius: 4px; padding: 4px 10px; cursor: pointer;
            }
            .toolbar button:hover { background: #4c5052; }
        </style>
        <script>${mermaidJs}</script>
        </head>
        <body>
            <div class="toolbar">
                <button onclick="zoomBy(1.2)">+</button>
                <button onclick="zoomBy(1/1.2)">-</button>
                <button onclick="resetView()">Reset</button>
            </div>
            <div id="viewport">
                <div id="stage">
                    <pre class="mermaid">
${escapedMermaid}
                    </pre>
                </div>
            </div>

            <script>
                mermaid.initialize({
    startOnLoad: true,
    theme: 'dark',
    flowchart: {
        curve: 'basis',
        nodeSpacing: 40,
        rankSpacing: 90,
        useMaxWidth: false,
        htmlLabels: true
    }
});

let scale = 1, originX = 0, originY = 0;
let isDragging = false, lastX = 0, lastY = 0;
const viewport = document.getElementById('viewport');
const stage = document.getElementById('stage');

function applyTransform() {
    stage.style.transform = `translate(${'$'}{originX}px, ${'$'}{originY}px) scale(${'$'}{scale})`;
}
function zoomBy(factor) {
    scale = Math.min(4, Math.max(0.1, scale * factor));
    applyTransform();
}
function resetView() {
    scale = 1; originX = 0; originY = 0;
    applyTransform();
}
viewport.addEventListener('wheel', (e) => {
    e.preventDefault();
    const factor = e.deltaY < 0 ? 1.1 : 1 / 1.1;
    scale = Math.min(4, Math.max(0.1, scale * factor));
    applyTransform();
}, { passive: false });
viewport.addEventListener('mousedown', (e) => {
    isDragging = true; lastX = e.clientX; lastY = e.clientY;
    viewport.classList.add('dragging');
});
window.addEventListener('mouseup', () => { isDragging = false; viewport.classList.remove('dragging'); });
window.addEventListener('mousemove', (e) => {
    if (!isDragging) return;
    originX += e.clientX - lastX; originY += e.clientY - lastY;
    lastX = e.clientX; lastY = e.clientY;
    applyTransform();
});

// --- Клік-хайлайт: підсвічує тільки зв'язки вибраної сутності ---
let selectedNodeId = null;

function setupHighlighting() {
    const svg = stage.querySelector('svg');
    if (!svg) return;

    const nodes = svg.querySelectorAll('.node');
    const edges = svg.querySelectorAll('.edgePath, .edgeLabel, .edgeLabels > g');

    nodes.forEach(node => {
        node.style.cursor = 'pointer';
        node.addEventListener('click', () => {
            const nodeId = node.id;
            if (selectedNodeId === nodeId) {
                clearHighlight();
                selectedNodeId = null;
                return;
            }
            selectedNodeId = nodeId;
            applyHighlight(nodeId);
        });
    });

    function applyHighlight(nodeId) {
        // id зв'язку в mermaid flowchart виглядає як "L-<sourceId>-<targetId>-<n>"
        // node.id виглядає як "flowchart-<sanitizedId>-<index>" — витягуємо чистий id
        const cleanId = extractCleanId(nodeId);

        nodes.forEach(n => {
            const nClean = extractCleanId(n.id);
            n.style.opacity = '0.25';
        });
        svg.querySelectorAll('.edgePaths > path').forEach(p => p.style.opacity = '0.1');
        svg.querySelectorAll('.edgeLabels .edgeLabel').forEach(l => l.style.opacity = '0.1');

        nodes.forEach(n => {
            const nClean = extractCleanId(n.id);
            const edgeId = svgEdgeIdBetween(nClean, cleanId) || svgEdgeIdBetween(cleanId, nClean);
        });

        svg.querySelectorAll('.edgePaths > path').forEach(p => {
            if (p.id.includes(cleanId)) {
                p.style.opacity = '1';
                const match = p.id.match(/^L_(.+?)_(.+?)_\d+$/) || p.id.match(/^L-(.+?)-(.+?)-\d+$/);
                if (match) {
                    highlightNodeById(match[1]);
                    highlightNodeById(match[2]);
                }
            }
        });

        svg.querySelectorAll('.edgeLabels .edgeLabel').forEach(l => {
            const parentEdge = [...svg.querySelectorAll('.edgePaths > path')]
                .find(p => p.id.includes(cleanId));
            if (parentEdge) l.style.opacity = '1';
        });

        function highlightNodeById(idPart) {
            nodes.forEach(n => {
                if (extractCleanId(n.id) === idPart) n.style.opacity = '1';
            });
        }
    }

    function clearHighlight() {
        nodes.forEach(n => n.style.opacity = '1');
        svg.querySelectorAll('.edgePaths > path').forEach(p => p.style.opacity = '1');
        svg.querySelectorAll('.edgeLabels .edgeLabel').forEach(l => l.style.opacity = '1');
    }

    function extractCleanId(rawId) {
        // "flowchart-Team-12" -> "Team"
        return rawId.replace(/^flowchart-/, '').replace(/-\d+$/, '');
    }

    function svgEdgeIdBetween() { return null; } // резерв, не використовується напряму
}

// mermaid рендерить асинхронно, тому чекаємо DOM
const observer = new MutationObserver(() => {
    if (stage.querySelector('svg')) {
        setupHighlighting();
        observer.disconnect();
    }
});
observer.observe(stage, { childList: true, subtree: true });
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    private fun loadMermaidJsSource(): String {
        val stream = javaClass.classLoader.getResourceAsStream("mermaid/mermaid.min.js")
            ?: error("mermaid.min.js not found in resources — поклади файл у resources/mermaid/")
        return stream.bufferedReader(StandardCharsets.UTF_8).readText()
    }

    private fun String.escapeHtml() = this
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
