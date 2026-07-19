package frog.entityScanner

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import frog.entityScanner.renderer.MermaidRenderer
import frog.entityScanner.scanner.EntityScanner

class ShowEntityDiagramAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {

        val project = e.project ?: return

        val graph = EntityScanner(project).scan()

        println(
            MermaidRenderer().render(graph)
        )
        println("hello")
    }
}
