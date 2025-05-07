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
import its.model.expressions.utils.ParamsValuesExprList
import its.reasoner.*

/**
 * Ризонер для операторов на основе наивной интерпретации:
 * выполняет действия в операторах "как сказано" на основе информации в предметной области ([DomainModel])
 */
class DomainInterpreterReasoner(
    val situation: LearningSituation,
    val varContext: Map<String, Any> = mutableMapOf(),
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

        val propertyParams = obj.findPropertyDef(op.propertyName)!!.paramsDecl
        val paramsValues = NamedParamsValues(evalParamsToMap(op.paramsValues, propertyParams))

        obj.definedPropertyValues.addOrReplace(PropertyValueStatement(obj, op.propertyName, paramsValues, value))
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

        val relationshipParams = subj.findRelationshipDef(op.relationshipName)!!.effectiveParams
        val paramsValues = NamedParamsValues(evalParamsToMap(op.paramsValues, relationshipParams))

        subj.relationshipLinks.add(RelationshipLinkStatement(subj, op.relationshipName, objectNames, paramsValues))
    }

    //---Управляющие конструкции

    override fun process(op: Block): Any? {
        return op.nestedExprs.map { it.use(this) }.last()
    }

    override fun process(op: IfThen): Any? {
        val isConditionSatisfied = op.conditionExpr.evalAs<Boolean>()
        if (isConditionSatisfied) {
            val thenVal = op.thenExpr.use(this)
            if (op.elseExpr != null)
                return thenVal
            return null
        }
        return op.elseExpr?.use(this)
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
            //Проверяем, что текущий объект obj "экстремальней" всех остальных объектов other
            filtered.filter { it != obj }.all { other ->
                op.extremeConditionExpr.evalAs<Boolean>(
                    this.copy(
                        varContext = varContext
                            .plus(op.varName to other)
                            .plus(op.extremeVarName to obj)
                    )
                )
            }
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
        val obj = op.objectExpr.evalAs<Obj>().def

        val propertyParams = obj.findPropertyDef(op.propertyName)!!.paramsDecl
        val paramsValuesMap = evalParamsToMap(op.paramsValues, propertyParams)

        return obj.getPropertyValue(op.propertyName, paramsValuesMap)
    }

    override fun process(op: GetByRelationship): Obj {
        val subj = op.subjectExpr.evalAs<Obj>().def
        val relationship = subj.findRelationshipDef(op.relationshipName)!!

        val relationshipParams = subj.findRelationshipDef(op.relationshipName)!!.effectiveParams
        val paramsValues = evalParamsToMap(op.paramsValues, relationshipParams)

        return RelationshipUtils.findSingleRelationshipLinkOrThrow(
            subj,
            relationship,
            objects = null,
            paramsValues = paramsValues
        ).objects[0].reference
    }

    override fun process(op: GetRelationshipParamValue): Any {
        val subj = op.subjectExpr.evalAs<Obj>().def
        val relationship = subj.findRelationshipDef(op.relationshipName)!!
        val objects = op.objectExprs.map { it.evalAs<Obj>().def }

        val relationshipParams = subj.findRelationshipDef(op.relationshipName)!!.effectiveParams
        val paramsValues = evalParamsToMap(op.paramsValues, relationshipParams)

        return RelationshipUtils.findSingleRelationshipLinkOrThrow(
            subj,
            relationship,
            objects,
            paramsValues
        ).paramsValues.asMap(relationshipParams)[op.paramName]!!
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

    override fun process(op: CheckRelationship): Boolean {
        val subj = op.subjectExpr.evalAs<Obj>().def
        val objects = op.objectExprs.map { it.evalAs<Obj>().def }

        val relationship = op.getRelationship(subj.clazz)

        val classList = listOf(relationship.subjectClass).plus(relationship.objectClasses)
        val projList = listOf(subj).plus(objects)
            .mapIndexed { i, obj -> obj.getProjection(classList[i]) }

        val paramsValues = evalParamsToMap(op.paramsValues, relationship.effectiveParams)

        var res = true
        forEachCombination(projList, { objComb: List<ObjectDef> ->
            res = res && RelationshipUtils.findRelationshipLinks(
                objComb.first(),
                relationship,
                objComb.subList(1, objComb.size),
                paramsValues
            ).isNotEmpty()
        })
        return res
    }

    //---Логические операции---

    override fun process(op: ExistenceQuantifier): Boolean? {
        val objects = getObjectsByCondition(op.selectorExpr, op.variable)
        for (obj in objects) {
            val value = op.conditionExpr.use(this.copy(varContext = varContext.plus(op.variable.varName to obj)))
            //Продолжаем цикл только если встретили false - т.е. это булевский режим, и данный объект не подходит под условие
            if (value != false) {
                //Во всех остальных случаях возвращаемся
                return if (value == true)
                    true
                else
                    null
            }
        }
        //Если прошлись по всем объектам (или объектов и не
        return false
    }

    override fun process(op: ForAllQuantifier): Boolean? {
        val objects = getObjectsByCondition(op.selectorExpr, op.variable)
        val values = mutableListOf<Any?>()
        for (obj in objects) {
            val value = op.conditionExpr.use(this.copy(varContext = varContext.plus(op.variable.varName to obj)))
            //Если в булевском режиме и встречаем false, то останавливаем сразу
            if (value == false) {
                return false
            }
            values.add(value)
        }
        //Возвращаем true, если все значения булевские true
        return if (values.all { it == true })
            true
        else
            null //в режиме цикла возвращаем null
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

    override fun process(literal: VariableLiteral): Any {
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
        varContext: Map<String, Any> = this.varContext,
    ): DomainInterpreterReasoner {
        return DomainInterpreterReasoner(situation, varContext)
    }

    private fun evalParamsToMap(paramsValuesExprList: ParamsValuesExprList, paramsDecl: ParamsDecl): Map<String, Any> {
        return paramsValuesExprList.asMap(paramsDecl).mapValues { it.value.evalAs<Any>() }
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

    private fun ObjectDef.fitsCondition(condition: Operator, asVar: String): Boolean {
        return condition.evalAs<Boolean>(
            this@DomainInterpreterReasoner.copy(varContext = varContext.plus(asVar to this.reference))
        )
    }

    override fun getObjectsByCondition(condition: Operator?, asVar: TypedVariable): List<Obj> {
        val objects = domain.objects.filter { it.isInstanceOf(asVar.className) }
        if (condition == null) return objects.map { it.reference }
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