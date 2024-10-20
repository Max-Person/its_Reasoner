package its.reasoner.operators

import its.model.TypedVariable
import its.model.definition.*
import its.model.definition.types.Clazz
import its.model.definition.types.Comparison
import its.model.definition.types.EnumValue
import its.model.definition.types.Obj
import its.model.expressions.Operator
import its.model.expressions.literals.*
import its.model.expressions.operators.*
import its.reasoner.*

/**
 * Ризонер для операторов на основе наивной интерпретации:
 * выполняет действия в операторах "как сказано" на основе информации в предметной области ([DomainModel])
 */
class DomainInterpreterReasoner(
    val situation: LearningSituation,
    val varContext: Map<String, Obj> = mutableMapOf(),
) : OperatorReasoner {

    private val domain
        get() = situation.domainModel

    private fun <T> Operator.evalAs(reasoner: OperatorReasoner = this@DomainInterpreterReasoner): T = use(reasoner) as T

    private fun require(condition: Boolean) {
        if (!condition) throw ReasoningMisuseException()
    }

    //---Присвоения---

    override fun process(op: AssignProperty) {
        val obj = op.objectExpr.evalAs<Obj>().def
        val value = op.valueExpr.evalAs<Any>()

        obj.definedPropertyValues.addOrReplace(PropertyValueStatement(obj, op.propertyName, value))
    }

    override fun process(op: AssignDecisionTreeVar) {
        val value = op.valueExpr.evalAs<Obj>()

        if(!situation.decisionTreeVariables.containsKey(op.variableName))
            throw UnknownVariableException("Trying to assign variable '${op.variableName}' that has not been declared")

        situation.decisionTreeVariables[op.variableName] = value
    }

    override fun process(op: AddRelationshipLink) {
        val subj = op.subjectExpr.evalAs<Obj>().def
        val objectNames = op.objectExprs.map { it.evalAs<Obj>().def.name }

        subj.relationshipLinks.add(RelationshipLinkStatement(subj, op.relationshipName, objectNames))
    }

    //---Управляющие конструкции

    override fun process(op: Block) {
        op.nestedExprs.forEach { it.use(this) }
    }

    override fun process(op: IfThen) {
        val condition = op.conditionExpr.evalAs<Boolean>()
        if (condition)
            op.thenExpr.use(this)
    }

    override fun process(op: With): Any? {
        val obj = op.objExpr.evalAs<Obj>()
        return op.nestedExpr.use(this.copy(varContext = this.varContext.plus(op.varName to obj)))
    }

    //---Сравнения---

    override fun process(op: Compare): EnumValue {
        val valA = op.firstExpr.evalAs<Number>().toDouble()
        val valB = op.secondExpr.evalAs<Number>().toDouble()

        val res = compareValues(valA, valB)
        return when {
            res > 0 -> Comparison.Values.Greater
            res < 0 -> Comparison.Values.Less
            else -> Comparison.Values.Equal
        }
    }

    override fun process(op: CompareWithComparisonOperator): Boolean {
        val valA = op.firstExpr.evalAs<Any>()
        val valB = op.secondExpr.evalAs<Any>()

        return when(op.operator){
            CompareWithComparisonOperator.ComparisonOperator.Equal -> valA == valB
            CompareWithComparisonOperator.ComparisonOperator.NotEqual -> valA != valB
            CompareWithComparisonOperator.ComparisonOperator.Greater -> (valA as Number).toDouble() > (valB as Number).toDouble()
            CompareWithComparisonOperator.ComparisonOperator.GreaterEqual -> (valA as Number).toDouble() >= (valB as Number).toDouble()
            CompareWithComparisonOperator.ComparisonOperator.Less -> (valA as Number).toDouble() < (valB as Number).toDouble()
            CompareWithComparisonOperator.ComparisonOperator.LessEqual -> (valA as Number).toDouble() <= (valB as Number).toDouble()
        }
    }

    //---Поиск---

    override fun process(op: GetByCondition): Obj? {
        val f = getObjectsByCondition(op.conditionExpr, op.variable)

        //if (f.isEmpty())
        //    throw InterpretationException(NoSuchElementException("GetByCondition cannot find any objects that fit the condition"))
        if (f.size > 1)
            throw AmbiguousObjectException("GetByCondition found more than 1 fitting object")

        return f.firstOrNull()
    }

    override fun process(op: GetExtreme): Obj? {
        val filtered = getObjectsByCondition(op.conditionExpr, TypedVariable(op.className, op.varName))

        if (filtered.isEmpty())
            return null
        //throw InterpretationException(NoSuchElementException("GetExtreme cannot find any objects that fit the condition"))

        val extreme = filtered.filter { obj ->
            obj.def.fitsCondition(op.extremeConditionExpr, op.extremeVarName)
        }

        //if (extreme.isEmpty())
        //throw throw InterpretationException(NoSuchElementException("GetExtreme cannot find any objects that fit the extreme condition"))
        if (extreme.size > 1)
            throw AmbiguousObjectException("GetExtreme found more than 1 object fitting the extreme condition")

        return extreme.firstOrNull()
    }

    //---Вычисления---

    override fun process(op: GetClass): Clazz {
        val subj = op.objectExpr.evalAs<Obj>().def

        return subj.clazz.reference
    }

    override fun process(op: GetPropertyValue): Any {
        val subj = op.objectExpr.evalAs<Obj>().def

        return subj.getPropertyValue(op.propertyName)
    }

    override fun process(op: GetByRelationship): Obj {
        val subj = op.subjectExpr.evalAs<Obj>().def
        val relationship = subj.findRelationshipDef(op.relationshipName)!!

        val (base, signature) = relationship.getCanonicalDependencySignature()

        return if (signature.contains(DependantRelationshipKind.Type.OPPOSITE)) {
            domain.objects
                .filter { it.isInstanceOf(base.subjectClass) }
                .first { obj ->
                    obj.relationshipLinks.any { link ->
                        link.relationshipName == base.name && link.objects.first() == subj
                    }
                }
                .reference
        } else {
            subj.getByBaseRelationship(base)!!.reference
        }
    }

    //---Типизация---

    override fun process(op: Cast): Obj {
        val subj = op.objectExpr.evalAs<Obj>().def
        val clazz = op.classExpr.evalAs<Clazz>().def
        if (!subj.isInstanceOf(clazz))
            throw TypingException("Cannot cast $subj to type '${clazz.name}'")
        return subj.reference
    }

    //---Проверки---

    override fun process(op: CheckClass): Boolean {
        val subj = op.objectExpr.evalAs<Obj>().def
        val clazz = op.classExpr.evalAs<Clazz>().def
        return subj.isInstanceOf(clazz)
    }

    override fun process(op: CheckPropertyValue): Boolean {
        val get = GetPropertyValue(op.objectExpr, op.propertyName)

        return this.process(get) == op.valueExpr.evalAs<Obj>()
    }

    override fun process(op: CheckRelationship): Boolean {
        val subj = op.subjectExpr.evalAs<Obj>().def
        val objects = op.objectExprs.map { it.evalAs<Obj>().def }

        val relationship = op.getRelationship(subj.clazz)

        val classList = listOf(relationship.subjectClass).plus(relationship.objectClasses)
        val projList = listOf(subj).plus(objects)
            .mapIndexed { i, obj -> obj.getProjection(classList[i]) }

        var res = true
        forEachCombination(projList, { objComb: List<ObjectDef> ->
            res = res && objComb.first().checkRelationship(relationship, objComb.subList(1, objComb.size))
        })
        return res
    }

    //---Логические операции---

    override fun process(op: ExistenceQuantifier): Boolean {
        val objects = getObjectsByCondition(op.selectorExpr, op.variable)
        return objects.any { it.def.fitsCondition(op.conditionExpr, op.variable.varName) }
    }

    override fun process(op: ForAllQuantifier): Boolean {
        val objects = getObjectsByCondition(op.selectorExpr, op.variable)
        return objects.all { it.def.fitsCondition(op.conditionExpr, op.variable.varName) }
    }

    override fun process(op: LogicalAnd): Boolean {
        return op.firstExpr.evalAs<Boolean>() && op.secondExpr.evalAs<Boolean>()
    }

    override fun process(op: LogicalNot): Boolean {
        return !op.operandExpr.evalAs<Boolean>()
    }

    override fun process(op: LogicalOr): Boolean {
        return op.firstExpr.evalAs<Boolean>() || op.secondExpr.evalAs<Boolean>()
    }

    //---Ссылки---

    override fun process(literal: VariableLiteral): Obj {
        if (!varContext.containsKey(literal.name))
            throw UnknownVariableException("Context variable ${literal.name} not present during evaluation.")
        return varContext[literal.name]!!
    }

    override fun process(literal: DecisionTreeVarLiteral): Obj {
        if (!situation.decisionTreeVariables.containsKey(literal.name))
            throw UnknownVariableException("Decision tree variable ${literal.name} not present during evaluation.")
        return situation.decisionTreeVariables[literal.name]!!
    }

    override fun process(literal: ClassLiteral): Clazz {
        return Clazz(literal.name)
    }

    override fun process(literal: ObjectLiteral): Obj {
        return Obj(literal.name)
    }

    //---Значения---

    override fun process(literal: BooleanLiteral): Boolean {
        return literal.value
    }

    override fun process(literal: IntegerLiteral): Int {
        return literal.value
    }

    override fun process(literal: DoubleLiteral): Double {
        return literal.value
    }

    override fun process(literal: StringLiteral): String {
        return literal.value
    }

    override fun process(literal: EnumLiteral): EnumValue {
        return literal.value
    }

    //------------- Вспомогательные функции ------------

    private fun copy(
        situation: LearningSituation = this.situation,
        varContext: Map<String, Obj> = this.varContext,
    ): DomainInterpreterReasoner {
        return DomainInterpreterReasoner(situation, varContext)
    }

    private fun <T> forEachCombination(lists: List<List<T>>, block : (combination : List<T>) -> Unit,
                                       depth: Int = 0, currentComb: List<T> = listOf() ){

        if (depth == lists.size) {
            block(currentComb)
            return
        }

        for (el in lists[depth]) {
            forEachCombination(lists, block, depth + 1, currentComb.plus(el))
        }
    }

    private fun ObjectDef.getProjection(targetClass: ClassDef): List<ObjectDef> {
        if (this.isInstanceOf(targetClass)) return listOf(this)

        val projectionRelationship = this.clazz.getProjectionRelationship(targetClass)

        return this.relationshipLinks
            .filter { it.relationshipName == projectionRelationship.name }
            .map { Obj(it.objectNames.first()).def }
    }

    private fun ObjectDef.checkRelationship(relationship: RelationshipDef, objects: List<ObjectDef>): Boolean {
        return if (relationship.isBinary) {
            this.checkBinaryRelationship(relationship, objects)
        } else if (relationship.kind is BaseRelationshipKind) {
            this.checkBaseNaryRelationship(relationship, objects)
        } else {
            this.checkDependantNaryRelationship(relationship, objects)
        }
    }

    private fun ObjectDef.checkBaseNaryRelationship(relationship: RelationshipDef, objects: List<ObjectDef>): Boolean {
        return this.relationshipLinks.any { link ->
            link.relationshipName == relationship.name
                    && if (relationship.isUnordered)
                link.objects.sortedBy { it.name } == objects.sortedBy { it.name }
            else link.objects == objects
        }
    }

    private fun ObjectDef.checkDependantNaryRelationship(
        relationship: RelationshipDef,
        objects: List<ObjectDef>
    ): Boolean {
        require(
            relationship.kind is DependantRelationshipKind
                    && setOf(
                DependantRelationshipKind.Type.BETWEEN,
                DependantRelationshipKind.Type.CLOSER,
                DependantRelationshipKind.Type.FURTHER
            ).contains((relationship.kind as DependantRelationshipKind).type)
        ) //Проверка на дурака

        val (base, signature) = relationship.getCanonicalDependencySignature()
        val isTransitive = signature.contains(DependantRelationshipKind.Type.TRANSITIVE)

        val (boundaryA, inner, boundaryB) =
            if ((relationship.kind as DependantRelationshipKind).type == DependantRelationshipKind.Type.FURTHER)
                listOf(objects[0], objects[1], this)
            else listOf(objects[0], this, objects[1])

        return boundaryA.canReach(inner, base, isTransitive) && inner.canReach(boundaryB, base, isTransitive)
                || boundaryB.canReach(inner, base, isTransitive) && inner.canReach(boundaryA, base, isTransitive)
    }

    private fun ObjectDef.checkBinaryRelationship(relationship: RelationshipDef, objects: List<ObjectDef>): Boolean {
        val (base, signature) = relationship.getCanonicalDependencySignature()
        val isTransitive = signature.contains(DependantRelationshipKind.Type.TRANSITIVE)

        val (actSubject, actObject) =
            if (signature.contains(DependantRelationshipKind.Type.OPPOSITE))
                (objects.first() to this)
            else
                (this to objects.first())

        return actSubject.canReach(actObject, base, isTransitive)
    }

    private fun ObjectDef.listByBaseRelationship(relationship: RelationshipDef): List<ObjectDef> {
        require(relationship.isBinary)
        return this.relationshipLinks.filter { it.relationshipName == relationship.name }.map { it.objects.first() }
    }

    private fun ObjectDef.getByBaseRelationship(relationship: RelationshipDef): ObjectDef? {
        require(relationship.isBinary && relationship.effectiveQuantifier.objCount == 1)
        return this.listByBaseRelationship(relationship).firstOrNull()
    }

    private fun ObjectDef.canReach(other: ObjectDef, relationship: RelationshipDef, isTransitive: Boolean): Boolean {
        require(
            relationship.kind is BaseRelationshipKind && relationship.isBinary
                    && (!isTransitive || relationship.effectiveQuantifier.objCount == 1)
        )

        if (isTransitive) {
            var next = getByBaseRelationship(relationship)
            while (next != null) {
                if (next == other) return true
                next = next.getByBaseRelationship(relationship)
            }
            return false
        } else {
            return listByBaseRelationship(relationship).contains(other)
        }
    }

    private fun RelationshipDef.getCanonicalDependencySignature(): Pair<RelationshipDef, Set<DependantRelationshipKind.Type>> {
        val signature = mutableSetOf<DependantRelationshipKind.Type>()
        var base = this
        while (base.kind is DependantRelationshipKind) {
            val type = (base.kind as DependantRelationshipKind).type
            if (type == DependantRelationshipKind.Type.OPPOSITE
                && signature.contains(DependantRelationshipKind.Type.OPPOSITE)
            ) {
                signature.remove(DependantRelationshipKind.Type.OPPOSITE) //Два противоположных уничтожаются
            } else {
                signature.add(type)
            }
            base = base.baseRelationship!!
        }
        return base to signature
    }

    private fun ObjectDef.fitsCondition(condition: Operator, asVar: String): Boolean {
        return condition.evalAs<Boolean>(
            this@DomainInterpreterReasoner.copy(varContext = varContext.plus(asVar to this.reference))
        )
    }

    override fun getObjectsByCondition(condition: Operator, asVar: TypedVariable): List<Obj> {
        val objects = domain.objects.filter { it.isInstanceOf(asVar.className) }
        return objects.filter { it.fitsCondition(condition, asVar.varName) }.map { it.reference }
    }

    private val <Def : DomainDef<Def>> DomainRef<Def>.def: Def
        get() {
            try {
                return this.findInOrUnkown(domain)
            } catch (e: UnknownDomainDefinitionException) {
                throw ReasoningException(e)
            }
        }
}