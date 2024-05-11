import its.model.DomainSolvingModel
import its.model.definition.EnumValueRef
import its.model.definition.rdf.DomainRDFFiller
import its.model.definition.rdf.DomainRDFWriter
import its.model.nodes.BranchResultNode
import its.model.nodes.DecisionTreeElement
import its.model.nodes.LinkNode
import its.reasoner.LearningSituation
import its.reasoner.compiler.builtins.registerAllCustomBuiltin
import its.reasoner.compiler.util.*
import its.reasoner.nodes.DecisionTreeCompiler
import its.reasoner.nodes.DecisionTreeReasoner._static.getCorrectPath
import its.reasoner.nodes.NodeCompilationResult
import its.reasoner.operators.JenaRulesReasoner
import its.reasoner.operators.OperatorReasoner
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner
import org.apache.jena.reasoner.rulesys.Rule
import kotlin.system.measureTimeMillis

var getDefaultOperatorReasoner: (LearningSituation) -> OperatorReasoner = { situation ->
    OperatorReasoner.defaultReasoner(situation)
}

fun main() {
    registerAllCustomBuiltin()

    val dir = "inputs"

    // Создать модель домена
    val model = DomainSolvingModel(dir)

    // Компилируем
    val result = (model.decisionTree.mainBranch.start as LinkNode<*>).use(DecisionTreeCompiler(model.domain))
    val paths= findPaths(result)
    val endings = findAllEndings(model.decisionTree.mainBranch.start)

    val skolemName = genVariableName()
    val resPredicate = genPredicateName()

    var rulesString = generateAuxiliaryRules() + PAUSE_MARK + result.rules + PAUSE_MARK
    paths.forEach { (ending, pairs) ->
        val builder = StringBuilder("\n[\n")
        pairs.forEach { (value, key) ->
            when (key) {
                Key.False -> {
                    builder.append("noValue(${genVariableName()} $value ${genVariableName()})\n")
                }
                Key.Final -> {
                    builder.append("(${genVariableName()} $value ${genURI(POAS_PREF, "Final")})\n")
                }
                Key.GetPropertyValueFalse -> {
                    builder.append("(${genVariableName()} $value ${genValue(false)})\n")
                }
                Key.Main -> {
                    builder.append("(${genVariableName()} $value ${genURI(POAS_PREF, "Main")})\n")
                }
                Key.Start -> {
                    builder.append("(${genVariableName()} $value ${genURI(POAS_PREF, "Start")})\n")
                }
                Key.True -> {
                    builder.append("(${genVariableName()} $value ${genVariableName()})\n")
                }
            }
        }

        builder.append("makeSkolem($skolemName)\n")
        builder.append("noValue(${genVariableName()}, $resPredicate)\n")
        builder.append("->\n")
        builder.append("($skolemName $resPredicate ${genValue(endings.indexOf(ending))})\n")
        builder.append("]\n")

        rulesString += builder.toString() + PAUSE_MARK
    }

    val rulesSets = rulesString.split(PAUSE_MARK.trim())
    val parsedRulesSets = rulesSets.map { set ->
        Rule.parseRules(set)
    }

    // Создать условие конкретных задач
    val situations = obtainModels(dir, model)

    situations.forEach { (modelName, situation) ->
        val interpreterResult: BranchResultNode
        val interpreterReasoningTime = measureTimeMillis {
            interpreterResult = model.decisionTree.mainBranch.getCorrectPath(situation).last() as BranchResultNode
        }

        getDefaultOperatorReasoner = {
            JenaRulesReasoner(it)
        }

        val rulesReasonerResult: BranchResultNode
        val rulesReasoningTime = measureTimeMillis {
            rulesReasonerResult = model.decisionTree.mainBranch.getCorrectPath(situation).last() as BranchResultNode
        }

        val jenaModel = DomainRDFWriter.saveDomain(situation.domain)
        var inf = ModelFactory.createInfModel(GenericRuleReasoner(listOf()), jenaModel)

        parsedRulesSets.forEach { rules ->
            val reasoner = GenericRuleReasoner(rules)
            inf = ModelFactory.createInfModel(reasoner, inf)
        }

        val compiledTreeReasonerResult: BranchResultNode
        val compiledTreeReasoningTime = measureTimeMillis {
            val prop = inf.getProperty(resPredicate)
            compiledTreeReasonerResult = endings[inf.listObjectsOfProperty(prop).next().asLiteral().int]
        }

        assert(interpreterResult === rulesReasonerResult)
        assert(interpreterResult === compiledTreeReasonerResult)

        println(modelName)
        println("Interpreter reasoning time: $interpreterReasoningTime")
        println("Rules reasoning time: $rulesReasoningTime")
        println("Compiled tree reasoning time: $compiledTreeReasoningTime")
        println("------------------------------------------------------------------------------")
    }
}

private fun findPaths(result: NodeCompilationResult): MutableMap<BranchResultNode, MutableList<Pair<String, Key>>> {
    if (result.children == null) {
        return mutableMapOf(result.ending!! to mutableListOf())
    }

    val res = mutableMapOf<BranchResultNode, MutableList<Pair<String, Key>>>()
    result.children.forEach { (key, nodeCompilationResult) ->
        val childResult = findPaths(nodeCompilationResult)
        childResult.forEach {
            it.value.add (0, result.value to when (key) {
                true -> Key.True
                false -> Key.False
                "GetPropertyValue.false" -> Key.GetPropertyValueFalse
                is EnumValueRef -> {
                    when (key.valueName) {
                        "Main" -> Key.Main
                        "Start" -> Key.Start
                        "Final" -> Key.Final
                        else -> TODO()
                    }
                }
                else -> TODO()
            })
        }
        res.putAll(childResult)
    }

    return res
}

private fun findAllEndings(node: DecisionTreeElement): List<BranchResultNode> {
    val res = mutableListOf<BranchResultNode>()
    node.linkedElements.forEach { element ->
        if (element is BranchResultNode) {
            res.add(element)
        }
        res.addAll(findAllEndings(element))
    }
    return res
}

sealed interface Key {

    data object True: Key
    data object False: Key
    data object GetPropertyValueFalse: Key
    data object Start: Key
    data object Main: Key
    data object Final: Key
}

private fun obtainModels(dir: String, model: DomainSolvingModel): Map<String, LearningSituation> = mapOf(
    "Start: 1 -> NotFound" to "${dir}/ttls/start/stage_start_pervy_uzel_Not_found.ttl",
    "Start: 2 -> False" to "${dir}/ttls/start/stage_start_vtoroy_uzel_False.ttl",
    "Start: 2 -> True" to "${dir}/ttls/start/stage_start_vtoroy_uzel_True.ttl",
    "Main: 1 -> True" to "${dir}/ttls/main/stage main (1 узел True).ttl",
    "Main: 2 -> False" to "${dir}/ttls/main/stage main (2 узел False).ttl",
    "Main: 3 -> False" to "${dir}/ttls/main/stage main (3 узел False).ttl",
    "Main: 4 -> False" to "${dir}/ttls/main/stage main (4 узел False).ttl",
    "Main: 5 -> NotFound" to "${dir}/ttls/main/stage main (5 узел Not found).ttl",
    "Main: 6 -> False" to "${dir}/ttls/main/stage main (6 узел False).ttl",
    "Main: 7 -> False" to "${dir}/ttls/main/stage main (7 узел False).ttl",
    "Main: 7 -> True" to "${dir}/ttls/main/stage main (7 узел True).ttl",
    "Final: 1 -> False" to "${dir}/ttls/final/stage final (1 узел False).ttl",
    "Final: 2 -> True" to "${dir}/ttls/final/stage final (1 узел True).ttl",
).map {
    val situationDomain = model.domain.copy()
    DomainRDFFiller.fillDomain(situationDomain, it.value)
    it.key to LearningSituation(situationDomain)
}.toMap()
