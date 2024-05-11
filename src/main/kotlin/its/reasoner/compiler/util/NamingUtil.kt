package its.reasoner.compiler.util

/**
 * Индекс для переменных
 */
private var varIndex: Long = 0L
    get(): Long = ++field

/**
 * Индекс для предикатов
 */
private var predicateIndex: Long = 0L
    get(): Long = ++field

/**
 * Генерирует уникальное имя для переменной, не совпадающее с пользовательскими именами переменных
 */
fun genVariableName(): String = genVariable("var$varIndex$PROTECTIVE_CHARS")

/**
 * Генерирует уникальное имя для предиката, не совпадающее с пользовательскими именами предикатов
 */
fun genPredicateName(): String = genURI(POAS_PREF, "predicate$predicateIndex$PROTECTIVE_CHARS")
