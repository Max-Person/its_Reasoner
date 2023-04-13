package its.reasoner.operators

import its.model.expressions.literals.*
import its.model.expressions.operators.*
import its.model.expressions.types.Clazz
import its.model.expressions.types.ComparisonResult
import its.model.expressions.types.EnumValue
import its.model.expressions.types.Obj
import its.model.expressions.visitors.OperatorBehaviour

interface OperatorReasoner : OperatorBehaviour<Any> {
    override fun process(op: AssignProperty)
    override fun process(op: AssignVariable)

    override fun process(op: Compare): ComparisonResult
    override fun process(op: CompareWithComparisonOperator): Boolean

    override fun process(op: GetByCondition): Obj
    override fun process(op: GetExtreme): Obj

    override fun process(op: GetClass): Clazz
    override fun process(op: GetPropertyValue): Any
    override fun process(op: GetByRelationship): Obj

    override fun process(op: CheckClass): Boolean
    override fun process(op: CheckPropertyValue): Boolean
    override fun process(op: CheckRelationship): Boolean

    override fun process(op: ExistenceQuantifier): Boolean
    override fun process(op: ForAllQuantifier): Boolean
    override fun process(op: LogicalAnd): Boolean
    override fun process(op: LogicalNot): Boolean
    override fun process(op: LogicalOr): Boolean

    override fun process(literal: Variable): Obj
    override fun process(literal: DecisionTreeVar): Obj
    override fun process(literal: ClassRef): Clazz
    override fun process(literal: ObjectRef): Obj
    override fun process(literal: PropertyRef) = throw IllegalStateException("Нельзя компилировать PropertyRef")
    override fun process(literal: RelationshipRef) = throw IllegalStateException("Нельзя компилировать RelationshipRef")

    override fun process(literal: BooleanLiteral): Boolean
    override fun process(literal: IntegerLiteral): Int
    override fun process(literal: DoubleLiteral): Double
    override fun process(literal: StringLiteral): String
    override fun process(literal: ComparisonResultLiteral): ComparisonResult
    override fun process(literal: EnumLiteral): EnumValue
}