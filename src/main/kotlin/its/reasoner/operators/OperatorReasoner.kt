package its.reasoner.operators

import its.model.TypedVariable
import its.model.definition.types.Clazz
import its.model.definition.types.EnumValue
import its.model.definition.types.Obj
import its.model.expressions.Operator
import its.model.expressions.literals.*
import its.model.expressions.operators.*
import its.model.expressions.visitors.OperatorBehaviour
import its.reasoner.LearningSituation

/**
 * Ризонер для операторов: поведение, принимающее оператор и возвращающее результат его вычисления
 */
interface OperatorReasoner : OperatorBehaviour<Any?> {
    override fun process(op: AssignProperty)
    override fun process(op: AssignDecisionTreeVar)
    override fun process(op: AddRelationshipLink)

    override fun process(op: Block)
    override fun process(op: IfThen)
    override fun process(op: With): Any?

    override fun process(op: Compare): EnumValue
    override fun process(op: CompareWithComparisonOperator): Boolean

    override fun process(op: GetByCondition): Obj?
    override fun process(op: GetExtreme): Obj?

    override fun process(op: GetClass): Clazz
    override fun process(op: GetPropertyValue): Any
    override fun process(op: GetByRelationship): Obj

    override fun process(op: Cast): Obj

    override fun process(op: CheckClass): Boolean
    override fun process(op: CheckPropertyValue): Boolean
    override fun process(op: CheckRelationship): Boolean

    override fun process(op: ExistenceQuantifier): Boolean
    override fun process(op: ForAllQuantifier): Boolean
    override fun process(op: LogicalAnd): Boolean
    override fun process(op: LogicalNot): Boolean
    override fun process(op: LogicalOr): Boolean

    override fun process(literal: VariableLiteral): Obj
    override fun process(literal: DecisionTreeVarLiteral): Obj
    override fun process(literal: ClassLiteral): Clazz
    override fun process(literal: ObjectLiteral): Obj

    override fun process(literal: BooleanLiteral): Boolean
    override fun process(literal: IntegerLiteral): Int
    override fun process(literal: DoubleLiteral): Double
    override fun process(literal: StringLiteral): String
    override fun process(literal: EnumLiteral): EnumValue

    fun getObjectsByCondition(condition: Operator, asVar: TypedVariable): List<Obj>

    companion object {
        @JvmStatic
        fun <T> Operator.evalAs(reasoner: OperatorReasoner): T = use(reasoner) as T
        @JvmStatic
        fun defaultReasoner(situation: LearningSituation) = DomainInterpreterReasoner(situation)
    }
}