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

        var inf = ModelFactory.createInfModel(GenericRuleReasoner(listOf()), model)

        val rulesSets = res.rules.split(PAUSE_MARK.trim())
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
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    override fun process(op: GetClass): Clazz {
        TODO("Not yet implemented")
    }

    override fun process(op: GetPropertyValue): Any {
        return when (op.eval().first().toString()) {
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
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }
}