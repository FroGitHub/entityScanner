package frog.entityScanner.scanner

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiField
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import frog.entityScanner.model.EntityGraph
import frog.entityScanner.model.EntityNode
import frog.entityScanner.model.EntityRelation
import frog.entityScanner.model.RelationType

private val LOG = logger<EntityScanner>()

class EntityScanner(
    private val project: Project
) {

    /** діагностика останнього сканування — щоб порожній результат не був німим */
    var scannedFiles: Int = 0
        private set

    var scannedClasses: Int = 0
        private set

    fun scan(): EntityGraph {

        return EntityGraph(
            entities = scanNodes()
        )
    }

    fun scanNodes(): List<EntityNode> {

        val result = mutableListOf<EntityNode>()

        scannedFiles = 0
        scannedClasses = 0

        val scope = GlobalSearchScope.projectScope(project)

        FileTypeIndex.getFiles(
            JavaFileType.INSTANCE,
            scope
        ).forEach { virtualFile ->

            val psiFile = PsiManager
                .getInstance(project)
                .findFile(virtualFile)

            val javaFile = psiFile as? PsiJavaFile
                ?: return@forEach

            scannedFiles++

            javaFile.classes.forEach { psiClass ->

                scannedClasses++

                val isEntity = psiClass.annotations.any {
                    it.isPersistenceAnnotation("Entity")
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

        LOG.info(
            "Скановано .java файлів: $scannedFiles, класів: $scannedClasses, " +
                "знайдено сутностей: ${result.size}"
        )

        if (result.isEmpty() && scannedClasses > 0) {
            logAnnotationSample(scope)
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

        val annotations = field.annotations

        return when {

            annotations.any { it.isPersistenceAnnotation("ManyToOne") } ->
                RelationType.MANY_TO_ONE

            annotations.any { it.isPersistenceAnnotation("OneToMany") } ->
                RelationType.ONE_TO_MANY

            annotations.any { it.isPersistenceAnnotation("OneToOne") } ->
                RelationType.ONE_TO_ONE

            annotations.any { it.isPersistenceAnnotation("ManyToMany") } ->
                RelationType.MANY_TO_MANY

            else -> null
        }
    }

    /**
     * Приймає і jakarta, і javax, і голе коротке ім'я — останнє трапляється,
     * коли анотація не зарезолвилась (не підтягнута бібліотека / не завершений імпорт Maven).
     */
    private fun PsiAnnotation.isPersistenceAnnotation(simpleName: String): Boolean {

        val qualifiedName = this.qualifiedName ?: return false

        return qualifiedName == "jakarta.persistence.$simpleName" ||
            qualifiedName == "javax.persistence.$simpleName" ||
            qualifiedName == simpleName
    }

    /** у лог — які взагалі анотації класів бачить сканер, коли сутностей 0 */
    private fun logAnnotationSample(scope: GlobalSearchScope) {

        val names = FileTypeIndex.getFiles(JavaFileType.INSTANCE, scope)
            .asSequence()
            .mapNotNull { PsiManager.getInstance(project).findFile(it) as? PsiJavaFile }
            .flatMap { it.classes.asSequence() }
            .flatMap { psiClass: PsiClass -> psiClass.annotations.asSequence() }
            .mapNotNull { it.qualifiedName }
            .distinct()
            .take(30)
            .toList()

        LOG.warn("Сутностей не знайдено. Анотації класів у проєкті: $names")
    }
}
