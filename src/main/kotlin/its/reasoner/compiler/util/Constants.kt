package its.reasoner.compiler.util

// +++++++++++++++++++++++++++++++++ Префиксы ++++++++++++++++++++++++++++++++++
// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

/**
 * POAS префикс
 */
const val POAS_PREF: String = "http://www.vstu.ru/poas/code#"

/**
 * XSD префикс
 */
const val XSD_PREF: String = "http://www.w3.org/2001/XMLSchema#"

/**
 * RDF префикс
 */
const val RDF_PREF: String = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"

/**
 * RDFS префикс
 */
const val RDFS_PREF: String = "http://www.w3.org/2000/01/rdf-schema#"

// +++++++++++++++++++++++++++++++++ Константы +++++++++++++++++++++++++++++++++
// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

/**
 * Маркировка паузы
 */
const val PAUSE_MARK: String = "\n<pause>\n\n"

/**
 * Разделитель для уровней нумерации объектов порядковой шкалы
 */
const val PARTIAL_SCALE_SEPARATOR: String = "."

/**
 * Символы, которые добавляются в генерируемые имена для защиты от совпадений с пользовательскими
 */
const val PROTECTIVE_CHARS: String = "..."

/**
 * Предикат переменной из модели
 */
const val VAR_PREDICATE: String = "${POAS_PREF}var$PROTECTIVE_CHARS"

/**
 * Предикат класса
 */
const val CLASS_PREDICATE: String = "${RDF_PREF}type"

/**
 * Предикат подкласса
 */
const val SUBCLASS_PREDICATE: String = "${RDFS_PREF}subClassOf"

/**
 * Предикат, задающий нумерацию для порядковой шкалы классов
 */
const val SUBCLASS_NUMERATION_PREDICATE: String = "${POAS_PREF}subClassOf_numeration$PROTECTIVE_CHARS"

/**
 * Разделитель столбцов в CSV файле словаря
 */
const val COLUMNS_SEPARATOR: Char = '|'

/**
 * Разделитель элементов списка в ячейке CSV файла словаря
 */
const val LIST_ITEMS_SEPARATOR: Char = ';'

/**
 * Разделитель возможных значений у свойств
 */
const val RANGE_SEPARATOR = '-'

/**
 * Имя перечисления, описывающего результат сравнения
 */
const val COMPARISON_RESULT_ENUM_NAME = "comparisonResult"

/**
 * Плейсхолдер, на место которого нужно поставить область определения для переменных
 */
const val DEFINITION_AREA_PLACEHOLDER = "<definitionArea>"
