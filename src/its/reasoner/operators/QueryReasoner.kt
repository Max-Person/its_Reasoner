package its.reasoner.operators

import its.model.DomainModel
import its.model.expressions.Operator
import its.model.expressions.literals.*
import its.model.expressions.operators.*
import its.model.expressions.types.Clazz
import its.model.expressions.types.ComparisonResult
import its.model.expressions.types.EnumValue
import its.model.models.RelationshipModel
import its.reasoner.LearningSituation
import its.reasoner.util.JenaUtil
import its.reasoner.util.RDFObj
import its.reasoner.util.RDFUtil.asObj
import its.reasoner.util.RDFUtil.asResource
import its.reasoner.util.RDFUtil.getLineage
import its.reasoner.util.RDFUtil.getLineageExclusive
import its.reasoner.util.RDFUtil.getObjects
import its.reasoner.util.RDFUtil.resource
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.RDFNode

class QueryReasoner(val situation: LearningSituation,
                    val varContext : Map<String, RDFObj> = mutableMapOf(), val existingFilters : MutableMap<String, List<RDFObj>> = mutableMapOf()) :
    OperatorReasoner {

    private val model = situation.model

    //---Присвоения---

    override fun process(op: AssignProperty) {
        val obj = op.objectExpr.use(this) as RDFObj
        val value = op.valueExpr.use(this)
        val rdfProperty = model.getProperty(JenaUtil.genLink(JenaUtil.POAS_PREF, op.propertyName))
        if(value !is EnumValue){
            obj.resource.addLiteral(rdfProperty, value)
        }
        else{
            val enumResource = model.resource(value.value)
            obj.resource.addProperty(rdfProperty, enumResource)
        }
    }

    override fun process(op: AssignVariable) {
        val value = op.valueExpr.use(this) as RDFObj
        if(!situation.decisionTreeVariables.containsKey(op.variableName))
            throw IllegalStateException("Trying to assign variable '${op.variableName}' that has not been declared")
        situation.decisionTreeVariables[op.variableName] = value.name
    }

    //---Сравнения---

    override fun process(op: Compare): ComparisonResult {
        val valA = (op.firstExpr.use(this) as Number).toDouble()
        val valB = (op.secondExpr.use(this) as Number).toDouble()

        //TODO поддержка упорядоченных енамов

        val res = compareValues(valA, valB)
        return when(res){
            1 -> ComparisonResult.Greater
            0 -> ComparisonResult.Equal
            -1 -> ComparisonResult.Less
            else -> ComparisonResult.Undetermined
        }
    }

    override fun process(op: CompareWithComparisonOperator): Boolean {
        val valA = op.firstExpr.use(this)
        val valB = op.secondExpr.use(this)

        //TODO поддержка упорядоченных енамов

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

    override fun process(op: GetByCondition): RDFObj {
        return model.getObjectsByCondition(op.conditionExpr, op.varName).single()
    }

    override fun process(op: GetExtreme): RDFObj {
        val filtered = model.getObjectsByCondition(op.selectorExpr, op.varName)
        existingFilters[op.varName] = filtered
        val extreme = filtered.single { obj ->
            obj.fitsCondition(op.extremeConditionExpr, op.extremeVarName) }
        existingFilters.remove(op.varName)
        return extreme
    }

    //---Вычисления---

    override fun process(op: GetClass): Clazz {
        val subj = op.objectExpr.use(this) as RDFObj

        return subj.classInheritance.first()
    }

    override fun process(op: GetPropertyValue): Any {
        val subj = op.objectExpr.use(this) as RDFObj
        val rdfProperty = model.getProperty(JenaUtil.genLink(JenaUtil.POAS_PREF, op.propertyName))
        val property = DomainModel.propertiesDictionary.get(op.propertyName)!!

        val enumCorrected = { value: RDFNode? ->
            if(property.enumName != null)
                EnumValue(property.enumName!!, value!!.asResource().localName)
            else
                value!!.asLiteral().value
        }

        if(property.isStatic){
            val classes = subj.classInheritance
            for(c in classes){
                val value = c.asResource(model).getProperty(rdfProperty)?.`object`
                if(value != null)
                    return enumCorrected(value)
            }
            throw IllegalArgumentException("Could not find property value ${op.propertyName}")
        }
        else
            return enumCorrected(subj.resource.getProperty(rdfProperty)?.`object`)
    }

    override fun process(op: GetByRelationship): RDFObj {
        val subj = op.subjectExpr.use(this) as RDFObj

        val relationshipName = op.relationshipName
        val relationship = DomainModel.relationshipsDictionary.get(relationshipName)!!
        require(relationship.argsClasses.size == 2) { "Отношение $relationshipName не является бинарным" }

        return subj.getByRelationship(relationship)
    }

    //---Проверки---

    override fun process(op: CheckClass): Boolean {
        val subj = op.objectExpr.use(this) as RDFObj
        val clazz = op.classExpr.use(this) as Clazz
        return subj.allClasses().contains(clazz)
    }

    override fun process(op: CheckPropertyValue): Boolean {
        val get = GetPropertyValue(listOf(op.objectExpr, op.arg(1)))//FIXME op.arg(1)

        return this.process(get) == op.valueExpr.use(this)
    }

    override fun process(op: CheckRelationship): Boolean {
        val relationship = DomainModel.relationshipsDictionary.get(op.relationshipName)!!
        val subj = op.subjectExpr.use(this) as RDFObj
        val obj = op.objectExprs.map{it.use(this) as RDFObj}

        if(relationship.scaleType != null){
            val scaleClass = DomainModel.classesDictionary.get(relationship.argsClasses.first())!!

            //TODO Убедиться что процесс проекции такой, каким и должен быть
            val subjProj = subj.getProjection(scaleClass)
            val objProj = obj.map{it.getProjection(scaleClass)}

            val projList = listOf(subjProj).plus(objProj)

            var res = true
            forEachCombination(projList,  {objComb: List<RDFObj> ->
                res = res && objComb.first().checkRelationship(relationship, objComb.subList(1, objComb.size))
            })
            return res

        }
        else{
            return subj.checkRelationship(relationship, obj)
        }
    }

    //---Логические операции---

    override fun process(op: ExistenceQuantifier): Boolean {
        val objects = model.getObjectsByCondition(op.selectorExpr, op.varName)
        return objects.any { it.fitsCondition(op.conditionExpr, op.varName) }
    }

    override fun process(op: ForAllQuantifier): Boolean {
        val objects = model.getObjectsByCondition(op.selectorExpr, op.varName)
        return objects.all { it.fitsCondition(op.conditionExpr, op.varName) }
    }

    override fun process(op: LogicalAnd): Boolean {
        return op.firstExpr.use(this) as Boolean && op.secondExpr.use(this) as Boolean
    }

    override fun process(op: LogicalNot): Boolean {
        return !(op.operandExpr.use(this) as Boolean)
    }

    override fun process(op: LogicalOr): Boolean {
        return op.firstExpr.use(this) as Boolean || op.secondExpr.use(this) as Boolean
    }

    //---Ссылки---

    override fun process(literal: Variable): RDFObj {
        return varContext[literal.name]!!
    }

    override fun process(literal: DecisionTreeVar): RDFObj {
        return model.resource(situation.decisionTreeVariables[literal.name]!!).asObj()
        //FIXME?
//        val p = model.getProperty(JenaUtil.genLink(JenaUtil.POAS_PREF, JenaUtil.DECISION_TREE_VAR_PREDICATE))
//        return model.listSubjectsWithProperty(p, model.createLiteral(literal.name)).toList().single().asObj()
    }

    override fun process(literal: ClassRef): Clazz {
        return DomainModel.classesDictionary.get(literal.name)!!
    }

    override fun process(literal: ObjectRef): RDFObj {
        return model.resource(literal.name).asObj()
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

    override fun process(literal: ComparisonResultLiteral): ComparisonResult {
        return literal.value
    }

    override fun process(literal: EnumLiteral): EnumValue {
        return literal.value
    }


    override fun getObjectsByCondition(condition: Operator, asVar: String): List<RDFObj> {
        return model.getObjectsByCondition(condition, asVar)
    }


    //------------- Вспомогательные функции ------------

    private fun copy(situation: LearningSituation = this.situation, varContext : Map<String, RDFObj> = this.varContext, existingFilters : MutableMap<String, List<RDFObj>> = this.existingFilters) : QueryReasoner{
        return QueryReasoner(situation, varContext, existingFilters)
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

    private fun RDFObj.getProjection(targetClass: Clazz) : List<RDFObj>{
        val currentClasses = this.classInheritance.map{it.name}
        if(currentClasses.contains(targetClass.name)) return listOf(this)

        val projectionRelationship = DomainModel.relationshipsDictionary.singleOrNull {
            currentClasses.contains(it.argsClasses.first())
                    && it.relationType != null
                    && it.argsClasses[1] == targetClass.name  } //Подумать, что будет, если у целевого класса (например токенов) будет своя иерархия
        require(projectionRelationship != null){"Невозможно проецировать класс ${currentClasses.first()} на класс ${targetClass.name}"}

        val property = model.getProperty(JenaUtil.genLink(JenaUtil.POAS_PREF, projectionRelationship.name))
        val projected = this.resource.listProperties(property).toList().map{it.`object`.asResource().asObj()}

        return projected
    }

    private fun RDFObj.checkRelationship(relationship: RelationshipModel, obj : List<RDFObj>) : Boolean{
        require(relationship.scaleType == null || relationship.scaleType == RelationshipModel.ScaleType.Linear) //TODO

        val base = if(relationship.scaleType == null || relationship.scaleRole == RelationshipModel.ScaleRole.Base) relationship else relationship.scaleBase!!
        val property = model.getProperty(JenaUtil.genLink(JenaUtil.POAS_PREF, base.name))

        when(relationship.scaleRole){
            null, RelationshipModel.ScaleRole.Base-> {
                if(!this.resource.hasProperty(property)) return false
                if(base.argsClasses.size == 2){
                    return this.resource.listProperties(property).toList().map{it.`object`.asResource()}.contains(obj.first().resource)
                }
                else{
                    val links = this.resource.listProperties(property).toList().map{it.`object`.asResource()}
                    return links.any{link -> link.listProperties(property).toList().map{it.`object`.asResource()}.toSet() == obj.map{it.resource}.toSet()}
                }
            }
            RelationshipModel.ScaleRole.Reverse -> {
                if(!obj.first().resource.hasProperty(property)) return false
                return obj.first().resource.listProperties(property).toList().map{it.`object`.asResource()}.contains(this.resource)
            }

            RelationshipModel.ScaleRole.BaseTransitive -> {
                if(!this.resource.hasProperty(property)) return false
                return this.resource.getLineageExclusive(property, true).contains(obj.first().resource)
            }
            RelationshipModel.ScaleRole.ReverseTransitive -> {
                if(!obj.first().resource.hasProperty(property)) return false
                return this.resource.getLineageExclusive(property, false).contains(obj.first().resource)
            }
            RelationshipModel.ScaleRole.Between -> {
                if(this == obj[0] || this == obj[1] || obj[0] == obj[1])
                    return false

                val lin = obj.first().resource.getLineageExclusive(property, false).reversed() + obj.first().resource.getLineage(property, true)
                val thisPos = lin.indexOf(this.resource)
                val firstPos = lin.indexOf(obj[0].resource)
                val secondPos = lin.indexOf(obj[1].resource)

                if(thisPos == -1 || firstPos == -1 || secondPos == -1)
                    throw IllegalArgumentException()

                return ((firstPos < thisPos && thisPos < secondPos) ||
                        (secondPos < thisPos && thisPos < firstPos))

            }
            RelationshipModel.ScaleRole.Closer -> TODO()
            RelationshipModel.ScaleRole.Further -> TODO()
            else -> throw IllegalArgumentException()
        }
    }

    private fun RDFObj.getByRelationship(relationship: RelationshipModel) : RDFObj{
        val a = when(relationship.scaleRole){
            null, RelationshipModel.ScaleRole.Base-> {
                val property = model.getProperty(JenaUtil.genLink(JenaUtil.POAS_PREF, relationship.name))
                this.resource.listProperties(property).toList().map{it.`object`.asResource()}
            }
            RelationshipModel.ScaleRole.Reverse -> {
                val property = model.getProperty(JenaUtil.genLink(JenaUtil.POAS_PREF, relationship.name))
                this.resource.listProperties(property).toList().map{it.`object`.asResource()}
            }
            else -> throw IllegalArgumentException("Невозможно получить объект по отношению ${relationship.name}") //FIXME?
        }.map{it.asObj()}.single()
        return a
    }

    private fun RDFObj.allClasses() : Set<Clazz> {
        val set = this.classInheritance.toMutableSet()
        set.addAll(DomainModel.classesDictionary.filter {
            it.isCalculated
                    && (it.parent == null || set.contains(DomainModel.classesDictionary.get(it.parent!!)))
                    && this.fitsCondition(it.calcExpr!!, "obj")
        })

        return set
    }

    private fun RDFObj.fitsCondition(condition: Operator, asVar: String ) : Boolean {
        return condition.use(this@QueryReasoner.copy(varContext = varContext.plus(asVar to this))) as Boolean
    }

    private fun Model.getObjectsByCondition(condition: Operator, asVar: String ) : List<RDFObj>{
        val objects = this.getObjects()
        return objects.filter {it.fitsCondition(condition, asVar)}
    }
}