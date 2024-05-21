package its.reasoner.operators

import its.model.TypedVariable
import its.model.definition.ObjectRef
import its.model.definition.VariableDef
import its.model.definition.rdf.DomainRDFWriter
import its.model.definition.types.Clazz
import its.model.definition.types.EnumValue
import its.model.definition.types.Obj
import its.model.expressions.Operator
import its.model.expressions.literals.*
import its.model.expressions.operators.*
import its.reasoner.LearningSituation
import its.reasoner.compiler.OperatorJenaCompiler
import its.reasoner.compiler.util.PAUSE_MARK
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner
import org.apache.jena.reasoner.rulesys.Rule
import org.apache.jena.vocabulary.AS.to

var counter: Int = 0
    get() = ++field

val rules = mapOf<Int, String>(
    1 to """

[
(?var384... http://www.vstu.ru/poas/code#var... "stage"^^http://www.w3.org/2001/XMLSchema#string)
(?var384... http://www.vstu.ru/poas/code#current_stage ?var383...)
makeSkolem(?var382...)
->
(?var382... http://www.vstu.ru/poas/code#predicate26... ?var383...)
]

    """.trimIndent(),
    2 to """

[
(?var386... http://www.vstu.ru/poas/code#var... "currentItem"^^http://www.w3.org/2001/XMLSchema#string)
(?var387... http://www.vstu.ru/poas/code#var... "targetItem"^^http://www.w3.org/2001/XMLSchema#string)
equal(?var386...,?var387...)
makeSkolem(?var385...)
->
(?var385... http://www.vstu.ru/poas/code#predicate27... "true"^^http://www.w3.org/2001/XMLSchema#boolean)
]

    """.trimIndent(),
    3 to """

[
(?var391... http://www.vstu.ru/poas/code#var... "symbol"^^http://www.w3.org/2001/XMLSchema#string)
(?var391... http://www.vstu.ru/poas/code#operator ??var390...)
(??var390... http://www.vstu.ru/poas/code#operator_type ??var389...)
(?var395... http://www.vstu.ru/poas/code#var... "currentItem"^^http://www.w3.org/2001/XMLSchema#string)
(?var395... http://www.vstu.ru/poas/code#item_type ??var394...)
(??var389... http://www.vstu.ru/poas/code#can_be_applied_to ??var394...)
makeSkolem(?var388...)
->
(?var388... http://www.vstu.ru/poas/code#predicate28... "true"^^http://www.w3.org/2001/XMLSchema#boolean)
]

    """.trimIndent(),
    4 to """

[
(?var401... http://www.vstu.ru/poas/code#var... "symbol"^^http://www.w3.org/2001/XMLSchema#string)
(?var401... http://www.vstu.ru/poas/code#operand ??var400...)
(??var400... http://www.vstu.ru/poas/code#operand_type ??var399...)
(?var401... http://www.vstu.ru/poas/code#operator ??var405...)
(??var405... http://www.vstu.ru/poas/code#operator_type ??var404...)
(?var410... http://www.vstu.ru/poas/code#var... "currentItem"^^http://www.w3.org/2001/XMLSchema#string)
(?var410... http://www.vstu.ru/poas/code#item_type ??var409...)
(??var399... http://www.vstu.ru/poas/code#compatible_subj ?var413...)
(?var413... http://www.vstu.ru/poas/code#compatible_obj_0 ??var404...)
(?var413... http://www.vstu.ru/poas/code#compatible_obj_1 ??var409...)
makeSkolem(?var398...)
->
(?var398... http://www.vstu.ru/poas/code#predicate29... "true"^^http://www.w3.org/2001/XMLSchema#boolean)
]

    """.trimIndent(),
    5 to """

[
(?item http://www.w3.org/1999/02/22-rdf-syntax-ns#type http://www.vstu.ru/poas/code#Item)
(?connection http://www.w3.org/1999/02/22-rdf-syntax-ns#type http://www.vstu.ru/poas/code#Connection)
(?item http://www.vstu.ru/poas/code#is_a ?connection)
(?var418... http://www.vstu.ru/poas/code#var... "currentItem"^^http://www.w3.org/2001/XMLSchema#string)
(?var418... http://www.vstu.ru/poas/code#has ?connection)
(?var422... http://www.vstu.ru/poas/code#var... "symbol"^^http://www.w3.org/2001/XMLSchema#string)
(?var422... http://www.vstu.ru/poas/code#operator ??var421...)
(??var421... http://www.vstu.ru/poas/code#operator_type ??var420...)
(?connection http://www.vstu.ru/poas/code#connection_type ??var425...)
(??var420... http://www.vstu.ru/poas/code#associated_with ??var425...)
(?var428... http://www.vstu.ru/poas/code#var... "targetItem"^^http://www.w3.org/2001/XMLSchema#string)
(?var428... http://www.vstu.ru/poas/code#reachable_from ?item)
makeSkolem(?var414...)
->
(?var414... http://www.vstu.ru/poas/code#predicate30... "true"^^http://www.w3.org/2001/XMLSchema#boolean)
]

    """.trimIndent(),
    6 to """

[
(?item http://www.w3.org/1999/02/22-rdf-syntax-ns#type http://www.vstu.ru/poas/code#Item)
(?connection http://www.w3.org/1999/02/22-rdf-syntax-ns#type http://www.vstu.ru/poas/code#Connection)
(?item http://www.vstu.ru/poas/code#is_a ?connection)
(?var436... http://www.vstu.ru/poas/code#var... "currentItem"^^http://www.w3.org/2001/XMLSchema#string)
(?var436... http://www.vstu.ru/poas/code#has ?connection)
(?var440... http://www.vstu.ru/poas/code#var... "symbol"^^http://www.w3.org/2001/XMLSchema#string)
(?var440... http://www.vstu.ru/poas/code#operator ??var439...)
(??var439... http://www.vstu.ru/poas/code#operator_type ??var438...)
(?connection http://www.vstu.ru/poas/code#connection_type ??var443...)
(??var438... http://www.vstu.ru/poas/code#associated_with ??var443...)
(?var440... http://www.vstu.ru/poas/code#operand ??var446...)
(??var446... http://www.vstu.ru/poas/code#linked_with ?connection)
makeSkolem(?var430...)
->
(?var430... http://www.vstu.ru/poas/code#predicate31... ?item)
]

    """.trimIndent(),
    7 to """

[
(?var451... http://www.vstu.ru/poas/code#var... "targetItem"^^http://www.w3.org/2001/XMLSchema#string)
(?var452... http://www.vstu.ru/poas/code#var... "nextItem"^^http://www.w3.org/2001/XMLSchema#string)
(?var451... http://www.vstu.ru/poas/code#reachable_from ?var452...)
makeSkolem(?var450...)
->
(?var450... http://www.vstu.ru/poas/code#predicate32... "true"^^http://www.w3.org/2001/XMLSchema#boolean)
]

    """.trimIndent(),
    8 to """

[
(?var456... http://www.vstu.ru/poas/code#var... "nextItem"^^http://www.w3.org/2001/XMLSchema#string)
(?var456... http://www.vstu.ru/poas/code#was_visited_before ?var455...)
makeSkolem(?var454...)
->
(?var454... http://www.vstu.ru/poas/code#predicate33... ?var455...)
]

    """.trimIndent(),
)

class JenaRulesReasoner(
    private val situation: LearningSituation,
) : OperatorReasoner {

    private val compiler = OperatorJenaCompiler(situation.domain)

    private fun Operator.eval(): List<RDFNode> {
        situation.decisionTreeVariables.forEach { (varName, `object`) ->
            situation.domain.variables.remove(varName)
            situation.domain.variables.add(VariableDef(varName, `object`.objectName))
        }

        val model = DomainRDFWriter.saveDomain(situation.domain)

        val res = compiler.compileExpression(this)
        val rulesString = rules[counter] ?: res.rules

        var inf = ModelFactory.createInfModel(GenericRuleReasoner(listOf()), model)

        val rulesSets = rulesString.split(PAUSE_MARK.trim())
        for (set in rulesSets) {
            val rules = Rule.parseRules(set)
            val reasoner = GenericRuleReasoner(rules)
            inf = ModelFactory.createInfModel(reasoner, inf)
        }
        val prop = inf.getProperty(res.value)
        val objects = inf.listObjectsOfProperty(prop).asSequence().toList()

        return objects
    }

    override fun process(op: AssignProperty) {
        TODO("Not yet implemented")
    }

    override fun process(op: AssignDecisionTreeVar) {
        op.eval()
    }

    override fun process(op: AddRelationshipLink) {
        TODO("Not yet implemented")
    }

    override fun process(op: Block) {
        op.eval()
    }

    override fun process(op: IfThen) {
        TODO("Not yet implemented")
    }

    override fun process(op: With): Any? {
        TODO("Not yet implemented")
    }

    override fun process(op: Compare): EnumValue {
        TODO("Not yet implemented")
    }

    override fun process(op: CompareWithComparisonOperator): Boolean {
        return op.eval().isNotEmpty()
    }

    override fun process(op: GetByCondition): Obj? {
        return op.eval().firstOrNull()?.let {
            ObjectRef(it.toString().replace("http://www.vstu.ru/poas/code#", ""))
        }
    }

    override fun process(op: GetExtreme): Obj? {
        return op.eval().firstOrNull()?.let {
            ObjectRef(it.toString().replace("http://www.vstu.ru/poas/code#", ""))
        }
    }

    override fun process(op: GetClass): Clazz {
        TODO("Not yet implemented")
    }

    override fun process(op: GetPropertyValue): Any {
        return when (val b= op.eval().first().toString()) {
            "http://www.vstu.ru/poas/code#Start" -> {
                EnumValue("TaskStage", "Start")
            }
            "http://www.vstu.ru/poas/code#Main" -> {
                EnumValue("TaskStage", "Main")
            }
            "http://www.vstu.ru/poas/code#Final" -> {
                EnumValue("TaskStage", "Final")
            }
            "false^^http://www.w3.org/2001/XMLSchema#boolean" -> {
                false
            }
            "true^^http://www.w3.org/2001/XMLSchema#boolean" -> {
                true
            }
            "1^^http://www.w3.org/2001/XMLSchema#int" -> {
                1
            }
            "2^^http://www.w3.org/2001/XMLSchema#int" -> {
                2
            }
            else -> {
                TODO()
            }
        }
    }

    override fun process(op: GetByRelationship): Obj {
        TODO("Not yet implemented")
    }

    override fun process(op: Cast): Obj {
        TODO("Not yet implemented")
    }

    override fun process(op: CheckClass): Boolean {
        return op.eval().isNotEmpty()
    }

    override fun process(op: CheckPropertyValue): Boolean {
        return op.eval().isNotEmpty()
    }

    override fun process(op: CheckRelationship): Boolean {
        return op.eval().isNotEmpty()
    }

    override fun process(op: ExistenceQuantifier): Boolean {
        return op.eval().isNotEmpty()
    }

    override fun process(op: ForAllQuantifier): Boolean {
        TODO("Not yet implemented")
    }

    override fun process(op: LogicalAnd): Boolean {
        TODO("Not yet implemented")
    }

    override fun process(op: LogicalNot): Boolean {
        TODO("Not yet implemented")
    }

    override fun process(op: LogicalOr): Boolean {
        TODO("Not yet implemented")
    }

    override fun process(literal: VariableLiteral): Obj {
        TODO("Not yet implemented")
    }

    override fun process(literal: DecisionTreeVarLiteral): Obj {
        TODO("Not yet implemented")
    }

    override fun process(literal: ClassLiteral): Clazz {
        TODO("Not yet implemented")
    }

    override fun process(literal: ObjectLiteral): Obj {
        TODO("Not yet implemented")
    }

    override fun process(literal: BooleanLiteral): Boolean {
        TODO("Not yet implemented")
    }

    override fun process(literal: IntegerLiteral): Int {
        TODO("Not yet implemented")
    }

    override fun process(literal: DoubleLiteral): Double {
        TODO("Not yet implemented")
    }

    override fun process(literal: StringLiteral): String {
        TODO("Not yet implemented")
    }

    override fun process(literal: EnumLiteral): EnumValue {
        TODO("Not yet implemented")
    }

    override fun getObjectsByCondition(condition: Operator, asVar: TypedVariable): List<Obj> {
        return condition.eval().map {
            ObjectRef(it.toString().replace("http://www.vstu.ru/poas/code#", ""))
        }
    }
}