package its.reasoner.compiler.util

/**
 * Описывает промежуточный или конечный результат компиляции выражения на Jena
 * @param value Указатель на результат.
 *                 Это может быть предикат, указывающий на элемент графа или имя переменной в правиле
 * @param bodies Части правил Jena, которые инициализируют переменную, указанную в value.
 *                 Используется только в процессе компиляции для передачи частей правил.
 *                 В результате компиляции выражения эта переменная содержит массив с пустой строкой
 * @param rules Полностью скомпилированные правила Jena
 */
data class CompilationResult(
    val value: String = "",
    val bodies: List<String> = listOf(""),
    val rules: String = ""
)