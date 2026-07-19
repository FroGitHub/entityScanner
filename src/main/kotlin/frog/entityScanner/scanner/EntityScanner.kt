package frog.entityScanner.scanner

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiField
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import frog.entityScanner.model.EntityGraph
import frog.entityScanner.model.EntityNode
import frog.entityScanner.model.EntityRelation
import frog.entityScanner.model.RelationType

class EntityScanner(
    private val project: Project
) {

    fun scan(): EntityGraph {

        return EntityGraph(
            entities = scanNodes()
        )
    }

    fun scanNodes(): List<EntityNode> {

        val result = mutableListOf<EntityNode>()

        val scope = GlobalSearchScope.projectScope(project)

        FilenameIndex.getAllFilesByExt(
            project,
            "java",
            scope
        ).forEach { virtualFile ->

            val psiFile = PsiManager
                .getInstance(project)
                .findFile(virtualFile)

            val javaFile = psiFile as? PsiJavaFile
                ?: return@forEach


            javaFile.classes.forEach { psiClass ->

                val isEntity = psiClass.annotations.any {
                    it.qualifiedName == "jakarta.persistence.Entity"
                }

                if (isEntity) {

                    val node = EntityNode(
                        name = psiClass.name ?: "",
                        qualifiedName = psiClass.qualifiedName ?: ""
                    )

                    psiClass.fields.forEach { field ->

                        getRelationType(field)?.let { type ->

                            val target = resolveTarget(field) ?: return@let

                            node.relations.add(
                                EntityRelation(
                                    source = node.qualifiedName,
                                    targetQualifiedName = target.qualifiedName,
                                    targetName = target.name,
                                    fieldName = field.name,
                                    type = type
                                )
                            )
                        }
                    }

                    result.add(node)
                }
            }
        }

        return result
    }

    private fun resolveTarget(field: PsiField): ResolvedTarget? {

        val classType = field.type as? PsiClassType
            ?: return null

        val targetClass = if (classType.parameters.isNotEmpty()) {
            (classType.parameters[0] as? PsiClassType)?.resolve()
        } else {
            classType.resolve()
        }

        targetClass ?: return null

        return ResolvedTarget(
            name = targetClass.name ?: return null,
            qualifiedName = targetClass.qualifiedName ?: return null
        )
    }

    private fun getRelationType(field: PsiField): RelationType? {

        return when {

            field.hasAnnotation("jakarta.persistence.ManyToOne") ->
                RelationType.MANY_TO_ONE

            field.hasAnnotation("jakarta.persistence.OneToMany") ->
                RelationType.ONE_TO_MANY

            field.hasAnnotation("jakarta.persistence.OneToOne") ->
                RelationType.ONE_TO_ONE

            field.hasAnnotation("jakarta.persistence.ManyToMany") ->
                RelationType.MANY_TO_MANY

            else -> null
        }
    }
}
