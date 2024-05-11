package its.reasoner.compiler.util

import its.model.expressions.HasNegativeForm
import its.model.expressions.Operator
import its.model.expressions.literals.BooleanLiteral
import its.model.expressions.operators.LogicalAnd
import its.model.expressions.operators.LogicalNot
import its.model.expressions.operators.LogicalOr

/**
 * Генерирует вспомогательные правила на основе информации из словарей
 */
fun generateAuxiliaryRules(): String {
    var classNumerationRules = PartialScalePatterns.NUMERATION_RULES_PATTERN
    classNumerationRules = classNumerationRules.replace(
        "<partialPredicate>",
        SUBCLASS_PREDICATE
    )
    classNumerationRules = classNumerationRules.replace(
        "<numberPredicate>",
        SUBCLASS_NUMERATION_PREDICATE
    )

    return classNumerationRules
}

/**
 * Семантический анализ дерева
 */
fun Operator.semantic(): Operator = simplify()

/**
 * Упрощает выражение, удаляя из него отрицания
 */
private fun Operator.simplify(isNegative: Boolean = false): Operator {
    if (isNegative) {
        return when (this) {
            is BooleanLiteral -> {
                BooleanLiteral(!value)
            }

            is LogicalNot -> {
                operandExpr.simplify(false)
            }

            is LogicalOr -> {
                LogicalAnd(firstExpr.simplify(true), secondExpr.simplify(true))
            }

            is LogicalAnd -> {
                LogicalOr(firstExpr.simplify(true), secondExpr.simplify(true))
            }

            is HasNegativeForm -> {
                val newArgs = children.map { arg -> arg.simplify() }

                val res = clone(newArgs) as HasNegativeForm
                res.setIsNegative(!res.isNegative())

                res as Operator
            }

            else -> {
                throw IllegalStateException("Отрицание типа asd невозможно.")
            }
        }
    } else {
        return when (this) {
            is LogicalNot -> {
                operandExpr.simplify(true)
            }

            else -> {
                val newArgs = children.map { arg -> arg.simplify() }
                clone(newArgs)
            }
        }
    }
}

