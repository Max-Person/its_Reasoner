package its.reasoner.compiler.util

// +++++++++++++++++++++++++++++++++ Шаблоны +++++++++++++++++++++++++++++++++++
// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

/**
 * Шаблон триплета в правиле
 */
private const val TRIPLE_PATTERN: String = "(<subj> <predicate> <obj>) <comment>\n"

// FIXME: Убрал (?a... ?b... ?c...), т.к. сильно замедляет работу, надо найти другой способ

/**
 * Основной шаблон правила с возвращаемым значением
 */
private val MAIN_RULE_PATTERN: String = """
    
    [
    <ruleBody>makeSkolem(<skolemName>)
    ->
    (<skolemName> <resPredicateName> <resVarName>)
    ]
    
""".trimIndent()

/**
 * Шаблон для boolean правила
 */
private val BOOLEAN_RULE_PATTERN: String = """
    
    [
    <ruleBody>makeSkolem(<skolemName>)
    ->
    (<skolemName> <resPredicateName> "true"^^${XSD_PREF}boolean)
    ]
    
""".trimIndent()

// ++++++++++++++++++++++++++++ Методы для генерации +++++++++++++++++++++++++++
// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

/**
 * Сгенерировать комментарий в правиле
 */
fun genComment(comment: String?): String = comment?.let { "" }.orEmpty()

/**
 * Сгенерировать строковую константу
 */
fun genValue(value: String): String = "\"$value\"^^${XSD_PREF}string"

/**
 * Сгенерировать булеву константу
 */
fun genValue(value: Boolean): String = "\"$value\"^^${XSD_PREF}boolean"

/**
 * Сгенерировать целочисленную константу
 */
fun genValue(value: Int): String = "\"$value\"^^${XSD_PREF}integer"

/**
 * Сгенерировать дробную константу
 */
fun genValue(value: Double): String = "\"$value\"^^${XSD_PREF}double"

/**
 * Сгенерировать URI
 */
fun genURI(pref: String, name: String): String = "$pref$name"

/**
 * Сгенерировать переменную
 */
fun genVariable(name: String): String = "?$name"

/**
 * Сгенерировать примитив, проверяющий эквивалентность
 */
fun genEqualPrim(first: String, second: String, comment: String? = null): String =
    "equal($first,$second) ${genComment(comment)}\n"

/**
 * Сгенерировать примитив, проверяющий неэквивалентность
 */
fun genNotEqualPrim(first: String, second: String, comment: String? = null): String =
    "notEqual($first,$second) ${genComment(comment)}\n"

/**
 * Сгенерировать примитив, проверяющий, что первый операнд меньше второго
 */
fun genLessThanPrim(first: String, second: String, comment: String? = null): String =
    "lessThan($first,$second) ${genComment(comment)}\n"

/**
 * Сгенерировать примитив, проверяющий, что первый операнд больше второго
 */
fun genGreaterThanPrim(first: String, second: String, comment: String? = null): String =
    "greaterThan($first,$second) ${genComment(comment)}\n"

/**
 * Сгенерировать примитив, проверяющий, что первый операнд меньше второго или равен ему
 */
fun genLessEqualPrim(first: String, second: String, comment: String? = null): String =
    "le($first,$second) ${genComment(comment)}\n"

/**
 * Сгенерировать примитив, проверяющий, что первый операнд больше второго или равен ему
 */
fun genGreaterEqualPrim(first: String, second: String, comment: String? = null): String =
    "ge($first,$second) ${genComment(comment)}\n"

/**
 * Сгенерировать примитив, проверяющий отсутствие у объекта указанного предиката
 */
fun genNoValuePrim(subj: String, predicate: String, comment: String? = null): String =
    "noValue($subj,$predicate) ${genComment(comment)}\n"

/**
 * Сгенерировать примитив, проверяющий отсутствие у объекта указанного предиката с указанным значением
 */
fun genNoValuePrim(subj: String, predicate: String, obj: String, comment: String? = null): String =
    "noValue($subj,$predicate,$obj) ${genComment(comment)}\n"

/**
 * Сгенерировать примитив, создающий сколем с указанным именем
 */
fun genMakeSkolemPrim(skolemName: String, comment: String? = null): String =
    "makeSkolem($skolemName) ${genComment(comment)}\n"

/**
 * Сгенерировать примитив, записывающий значение одной переменной в другую
 */
fun genBindPrim(from: String, to: String, comment: String? = null): String = "bind($from,$to) ${genComment(comment)}\n"

/**
 * Сгенерировать примитив, подсчитывающий количество связанных объектов
 */
fun genCountValuesPrim(obj: String, predicate: String, res: String, comment: String? = null): String =
    "countValues($obj,$predicate,$res) ${genComment(comment)}\n"

/**
 * Сгенерировать примитив, проверяющий, что все объекты первого предиката являются также объектами второго
 */
fun genForAllPrim(first: String, second: String, comment: String? = null): String =
    "forAll($first,$second) ${genComment(comment)}\n"

/**
 * Сгенерировать примитив, проверяющий, что конечный объект достижим/не достижим из начального по указанному предикату
 */
fun genIsReachablePrim(
    start: String,
    predicate: String,
    end: String,
    expected: Boolean,
    comment: String? = null
): String =
    "isReachable($start,$predicate,$end,\"$expected\"^^${XSD_PREF}boolean) ${genComment(comment)}\n"

/**
 * Сгенерировать триплет в правиле
 */
fun genTriple(subj: String, predicate: String, obj: String, comment: String? = null): String {
    var res = TRIPLE_PATTERN
    res = res.replace("<subj>", subj)
    res = res.replace("<predicate>", predicate)
    res = res.replace("<obj>", obj)
    res = res.replace("<comment>", genComment(comment))
    return res
}

/**
 * Сгенерировать правило с возвращаемым значением
 */
fun genRule(ruleBody: String, skolemName: String, resPredicateName: String, resVarName: String): String {
    var rule = MAIN_RULE_PATTERN
    rule = rule.replace("<ruleBody>", ruleBody)
    rule = rule.replace("<skolemName>", skolemName)
    rule = rule.replace("<resPredicateName>", resPredicateName)
    rule = rule.replace("<resVarName>", resVarName)
    return rule
}

/**
 * Сгенерировать булево правило
 */
fun genBooleanRule(ruleBody: String, skolemName: String, resPredicateName: String): String {
    var rule = BOOLEAN_RULE_PATTERN
    rule = rule.replace("<ruleBody>", ruleBody)
    rule = rule.replace("<skolemName>", skolemName)
    rule = rule.replace("<resPredicateName>", resPredicateName)
    return rule
}

private val BOOLEAN_RULE_PATTERN2: String = """
    
    [
    <ruleBody>makeSkolem(<skolemName>)
    combine(<resVarName>, <vars>)
    ->
    (<skolemName> <resPredicateName> <resVarName>)
    ]
    
""".trimIndent()

fun genBooleanRule(
    ruleBody: String,
    skolemName: String,
    resPredicateName: String,
    resVarName: String,
    vars: List<String>
): String {
    var rule = BOOLEAN_RULE_PATTERN2
    rule = rule.replace("<ruleBody>", ruleBody)
    rule = rule.replace("<skolemName>", skolemName)
    rule = rule.replace("<resPredicateName>", resPredicateName)
    rule = rule.replace("<resVarName>", resVarName)

    var tmp = ""
    vars.forEach {
        tmp += it + ", "
    }
    tmp = tmp.dropLast(2)

    rule = rule.replace("<vars>", tmp)
    return rule
}
