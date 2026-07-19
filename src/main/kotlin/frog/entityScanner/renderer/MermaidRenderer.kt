package frog.entityScanner.renderer

import frog.entityScanner.model.EntityGraph

class MermaidRenderer {

    fun render(graph: EntityGraph): String {

        val sb = StringBuilder()

        sb.appendLine("classDiagram")

        graph.entities.forEach { entity ->
            sb.appendLine("class ${entity.name}")
        }

        graph.entities.forEach { entity ->

            entity.relations.forEach { relation ->

                sb.appendLine(
                    "${entity.name} --> ${relation.targetName} : ${relation.fieldName}"
                )
            }
        }

        return sb.toString()
    }
}
