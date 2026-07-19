package frog.entityScanner.model

data class EntityRelation(
    val source: String,
    val targetQualifiedName: String,
    val targetName: String,
    val fieldName: String,
    val type: RelationType
)
