package frog.entityScanner.renderer

import frog.entityScanner.model.EntityGraph
import frog.entityScanner.model.RelationType

class MermaidRenderer {

    fun render(graph: EntityGraph): String {
        val sb = StringBuilder()
        sb.appendLine("erDiagram")

        val idByQualifiedName = graph.entities.associate {
            it.qualifiedName to safeId(it.qualifiedName)
        }
        val nameByQualifiedName = graph.entities.associate {
            it.qualifiedName to it.name
        }

        // Оголошуємо сутності (навіть без атрибутів — erDiagram вимагає entity name)
        graph.entities.forEach { entity ->
            val id = idByQualifiedName[entity.qualifiedName] ?: return@forEach
            sb.appendLine("    $id[\"${entity.name}\"]")
        }
        sb.appendLine()

        // Зливаємо дзеркальні зв'язки (Team->TeamPlayer ONE_TO_MANY + TeamPlayer->Team MANY_TO_ONE) в один edge
        val merged = mergeRelations(graph)

        merged.forEach { rel ->
            val sourceId = idByQualifiedName[rel.sourceQualifiedName] ?: safeId(rel.sourceQualifiedName)
            val targetId = idByQualifiedName[rel.targetQualifiedName] ?: safeId(rel.targetQualifiedName)
            val notation = crowsFootNotation(rel.type)
            sb.appendLine("    $sourceId $notation $targetId : \"${rel.label}\"")
        }

        return sb.toString()
    }

    // Внутрішня модель одного вже об'єднаного зв'язку
    private data class MergedRelation(
        val sourceQualifiedName: String,
        val targetQualifiedName: String,
        val type: RelationType,
        val label: String
    )

    private fun mergeRelations(graph: EntityGraph): List<MergedRelation> {
        data class RawRelation(
            val sourceQualifiedName: String,
            val targetQualifiedName: String,
            val fieldName: String,
            val type: RelationType
        )

        val raw = graph.entities.flatMap { entity ->
            entity.relations.map {
                RawRelation(entity.qualifiedName, it.targetQualifiedName, it.fieldName, it.type)
            }
        }

        val used = mutableSetOf<Int>()
        val result = mutableListOf<MergedRelation>()

        for (i in raw.indices) {
            if (i in used) continue
            val r = raw[i]

            val mirrorIndex = raw.indices.firstOrNull { j ->
                j != i && j !in used &&
                        raw[j].sourceQualifiedName == r.targetQualifiedName &&
                        raw[j].targetQualifiedName == r.sourceQualifiedName &&
                        isMirrorType(r.type, raw[j].type)
            }

            if (mirrorIndex != null) {
                val mirror = raw[mirrorIndex]
                used.add(i)
                used.add(mirrorIndex)

                // Той, у кого ONE_TO_MANY (або перший з ONE_TO_ONE/MANY_TO_MANY) стає "джерелом" в erDiagram
                val owner = if (r.type == RelationType.ONE_TO_MANY) r else mirror
                val inverse = if (owner === r) mirror else r

                result.add(
                    MergedRelation(owner.sourceQualifiedName, owner.targetQualifiedName, owner.type,
                        "${owner.fieldName} / ${inverse.fieldName}")
                )
            } else {
                used.add(i)
                result.add(MergedRelation(r.sourceQualifiedName, r.targetQualifiedName, r.type, r.fieldName))
            }
        }

        return result
    }

    private fun isMirrorType(a: RelationType, b: RelationType): Boolean = when (a) {
        RelationType.ONE_TO_MANY -> b == RelationType.MANY_TO_ONE
        RelationType.MANY_TO_ONE -> b == RelationType.ONE_TO_MANY
        RelationType.ONE_TO_ONE -> b == RelationType.ONE_TO_ONE
        RelationType.MANY_TO_MANY -> b == RelationType.MANY_TO_MANY
    }

    // Для пари ONE_TO_MANY/MANY_TO_ONE завжди повертаємо ONE_TO_MANY стороною першою (owner = "1" бік)
    private fun <T> orientOneToMany(r: T, mirror: T): Pair<T, T> where T : Any {
        val type = (r as? Any)?.let {
            it.javaClass.getDeclaredField("type").let { f -> f.isAccessible = true; f.get(it) as RelationType }
        }
        return if (type == RelationType.ONE_TO_MANY) r to mirror else mirror to r
    }

    // Crow's foot нотація mermaid erDiagram:
    // || = точно один, o| = нуль-або-один, }o = нуль-або-багато, }| = один-або-багато
    private fun crowsFootNotation(type: RelationType): String = when (type) {
        RelationType.ONE_TO_ONE   -> "||--||"
        RelationType.ONE_TO_MANY  -> "||--o{"
        RelationType.MANY_TO_ONE  -> "}o--||"
        RelationType.MANY_TO_MANY -> "}o--o{"
    }

    private fun safeId(raw: String): String =
        raw.replace(Regex("[^A-Za-z0-9_]"), "_")
            .let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }
}
