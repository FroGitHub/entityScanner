package frog.entityScanner.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import frog.entityScanner.model.EntityGraph
import frog.entityScanner.scanner.EntityScanner

@Service(Service.Level.PROJECT)
class EntityGraphService(private val project: Project) {
    
    // Зберігаємо поточний граф. Спочатку він порожній.
    var currentGraph: EntityGraph = EntityGraph(entities = emptyList())

    // Метод для запуску сканування та оновлення стану
    fun updateGraph() {
        currentGraph = EntityScanner(project).scan()
    }
}
