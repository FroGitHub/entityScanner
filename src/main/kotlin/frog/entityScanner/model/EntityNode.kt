package frog.entityScanner.model

data class EntityNode(
    val name: String,
    val qualifiedName: String,
    val relations: MutableList<EntityRelation> = mutableListOf()
)
