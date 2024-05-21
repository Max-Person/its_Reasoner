import its.model.DomainSolvingModel
import its.model.definition.EnumValueRef
import its.model.definition.rdf.DomainRDFFiller
import its.model.nodes.BranchResultNode
import its.model.nodes.DecisionTreeElement
import its.reasoner.LearningSituation
import its.reasoner.compiler.builtins.registerAllCustomBuiltin
import its.reasoner.compiler.util.genValue
import its.reasoner.nodes.DecisionTreeReasoner._static.getCorrectPath
import its.reasoner.nodes.NodeCompilationResult
import its.reasoner.nodes.OtherStrategyNodeCompilationResult
import its.reasoner.operators.JenaRulesReasoner
import kotlin.system.measureTimeMillis

fun main() {
    registerAllCustomBuiltin()

    val dir = "inputs_adj"

    // Создать модель домена
    val model = DomainSolvingModel(dir)

    // Компилируем
//    val result = (model.decisionTree.mainBranch.start as LinkNode<*>).use(DecisionTreeCompiler(model.domain))
//    val resultOtherStrategy = (model.decisionTree.mainBranch.start as LinkNode<*>).use(
//        DecisionTreeCompilerWithOtherStrategy(model.domain)
//    )
//    val paths= findPaths(result)
//    val endings = findAllEndings(model.decisionTree.mainBranch.start)

//    val skolemName = genVariableName()
//    val resPredicate = genPredicateName()

//    val rulesArray = genRules(resultOtherStrategy, skolemName, resPredicate, endings)
//    val rulesStringOtherStrategy = generateAuxiliaryRules() +
//            PAUSE_MARK + resultOtherStrategy.rules +
//            PAUSE_MARK + rulesArray.values.joinToString("") {
//        "\n[\n$it"
//    }

//    var rulesString = generateAuxiliaryRules() + PAUSE_MARK + result.rules + PAUSE_MARK
//    paths.forEach { (ending, pairs) ->
//        val builder = StringBuilder("\n[\n")
//        pairs.forEach { (value, key) ->
//            when (key) {
//                Key.False -> {
//                    builder.append("noValue(${genVariableName()} $value ${genVariableName()})\n")
//                }
//                Key.Final -> {
//                    builder.append("(${genVariableName()} $value ${genURI(POAS_PREF, "Final")})\n")
//                }
//                Key.GetPropertyValueFalse -> {
//                    builder.append("(${genVariableName()} $value ${genValue(false)})\n")
//                }
//                Key.Main -> {
//                    builder.append("(${genVariableName()} $value ${genURI(POAS_PREF, "Main")})\n")
//                }
//                Key.Start -> {
//                    builder.append("(${genVariableName()} $value ${genURI(POAS_PREF, "Start")})\n")
//                }
//                Key.True -> {
//                    builder.append("(${genVariableName()} $value ${genVariableName()})\n")
//                }
//            }
//        }
//
//        builder.append("makeSkolem($skolemName)\n")
//        builder.append("noValue(${genVariableName()}, $resPredicate)\n")
//        builder.append("->\n")
//        builder.append("($skolemName $resPredicate ${genValue(endings.indexOf(ending))})\n")
//        builder.append("]\n")
//
//        rulesString += builder.toString() + PAUSE_MARK
//    }

//    val rulesSets = rulesString.split(PAUSE_MARK.trim())
//    val parsedRulesSets = rulesSets.map { set ->
//        Rule.parseRules(set)
//    }

//    val rulesSetsOtherStrategy = rulesStringOtherStrategy.split(PAUSE_MARK.trim())
//    val parsedRulesSetsOtherStrategy = rulesSetsOtherStrategy.map { set ->
//        Rule.parseRules(set)
//    }

    // Создать условие конкретных задач
    val situations = obtainModels(dir, model)

    val interpreterTimes = mutableMapOf<Int, Long>()
    val rulesReasoningTimes = mutableMapOf<Int, Long>()
    val compiledTreeReasoningTimes = mutableMapOf<Int, Long>()
    val compiledTreeOtherStrategyReasoningTimes = mutableMapOf<Int, Long>()
    situations.forEach { (modelName, situations) ->
        val (first, second, thirds) = situations
        val (third, fourth) = thirds

        val situationSize = first.domain.objects.sumOf {
            it.relationshipLinks.size + it.definedPropertyValues.size
        }

        println("+++++++++++++++++++++++++++++$modelName+++++++++++++++++++++++++++++")

        println("Interpreter reasoning start: ++++++++++++++++++++++")
        val interpreterResult: BranchResultNode
        val interpreterReasoningTime = measureTimeMillis {
            interpreterResult = model.decisionTree.mainBranch.getCorrectPath(first).last() as BranchResultNode
        }
        interpreterTimes[situationSize] = interpreterReasoningTime
        println("Interpreter reasoning time: $interpreterReasoningTime")

        getDefaultOperatorReasoner = {
            JenaRulesReasoner(it)
        }

        println("Rules reasoning start: ++++++++++++++++++++++")
        val rulesReasonerResult: BranchResultNode
        val rulesReasoningTime = measureTimeMillis {
            rulesReasonerResult = model.decisionTree.mainBranch.getCorrectPath(second).last() as BranchResultNode
        }
        rulesReasoningTimes[situationSize] = rulesReasoningTime
        println("Rules reasoning time: $rulesReasoningTime")

//        val jenaModel = DomainRDFWriter.saveDomain(third.domain)
//        var inf = ModelFactory.createInfModel(GenericRuleReasoner(listOf()), jenaModel)

//        parsedRulesSets.forEach { rules ->
//            val reasoner = GenericRuleReasoner(rules)
//            inf = ModelFactory.createInfModel(reasoner, inf)
//        }

//        println("Compiled tree reasoning start: ++++++++++++++++++++++")
//        val compiledTreeReasonerResult: BranchResultNode
//        val compiledTreeReasoningTime = measureTimeMillis {
//            val prop = inf.getProperty(resPredicate)
//            compiledTreeReasonerResult = endings[inf.listObjectsOfProperty(prop).next().asLiteral().int]
//        }
//        compiledTreeReasoningTimes[situationSize] = compiledTreeReasoningTime
//        println("Compiled tree reasoning time: $compiledTreeReasoningTime")

//        val jenaModelOtherStrategy = DomainRDFWriter.saveDomain(fourth.domain)
//        var infOtherStrategy = ModelFactory.createInfModel(GenericRuleReasoner(listOf()), jenaModelOtherStrategy)

//        parsedRulesSetsOtherStrategy.forEach { rules ->
//            val reasoner = GenericRuleReasoner(rules)
//            infOtherStrategy = ModelFactory.createInfModel(reasoner, infOtherStrategy)
//        }

//        println("Compiled tree other strategy reasoning start: ++++++++++++++++++++++")
//        val compiledTreeReasonerResultOtherStrategy: BranchResultNode
//        val compiledTreeOtherStrategyReasoningTime = measureTimeMillis {
//            val prop = infOtherStrategy.getProperty(resPredicate)
//            compiledTreeReasonerResultOtherStrategy = endings[infOtherStrategy.listObjectsOfProperty(prop).next().asLiteral().int]
//        }
//        compiledTreeOtherStrategyReasoningTimes[situationSize] = compiledTreeOtherStrategyReasoningTime
//        println("Compiled tree other strategy reasoning time: $compiledTreeOtherStrategyReasoningTime")

        require(interpreterResult === rulesReasonerResult)
//        require(interpreterResult === compiledTreeReasonerResult)
//        require(interpreterResult === compiledTreeReasonerResultOtherStrategy)

        println("------------------------------------------------------------------------------")
    }
    println(interpreterTimes.toSortedMap())
    println(rulesReasoningTimes.toSortedMap())
    println(compiledTreeReasoningTimes.toSortedMap())
    println(compiledTreeOtherStrategyReasoningTimes.toSortedMap())
}

private fun obtainModels(dir: String, model: DomainSolvingModel): Map<String, Triple<LearningSituation, LearningSituation, Pair<LearningSituation, LearningSituation>>> = mapOf(
    "1" to "${dir}/1.ttl",
    "2" to "${dir}/2.ttl",
    "3" to "${dir}/3.ttl",
    "4" to "${dir}/4.ttl",
    "5" to "${dir}/5.ttl",
    "6" to "${dir}/6.ttl",
//    "7" to "${dir}/7.ttl",
//    "8" to "${dir}/8.ttl",
//    "Main 1x2" to "${dir}/ttls/measure/tmpMain1x2.ttl",
//    "Final 1x2" to "${dir}/ttls/measure/tmpFinal1x2.ttl",

//    "Start 1x3" to "${dir}/ttls/measure/tmpStart1x3.ttl",
//    "Main 1x3" to "${dir}/ttls/measure/tmpMain1x3.ttl",
//    "Final 1x3" to "${dir}/ttls/measure/tmpFinal1x3.ttl",

//    "Start 1x4" to "${dir}/ttls/measure/tmpStart1x4.ttl",
//    "Main 1x4" to "${dir}/ttls/measure/tmpMain1x4.ttl",
//    "Final 1x4" to "${dir}/ttls/measure/tmpFinal1x4.ttl",

//    "Start 1x5" to "${dir}/ttls/measure/tmpStart1x5.ttl",
//    "Main 1x5" to "${dir}/ttls/measure/tmpMain1x5.ttl",
//    "Final 1x5" to "${dir}/ttls/measure/tmpFinal1x5.ttl",

//    "Start 2x2" to "${dir}/ttls/measure/tmpStart2x2.ttl",
//    "Main 2x2" to "${dir}/ttls/measure/tmpMain2x2.ttl",
//    "Final 2x2" to "${dir}/ttls/measure/tmpFinal2x2.ttl",

//    "Start 2x3" to "${dir}/ttls/measure/tmpStart2x3.ttl",
//    "Main 2x3" to "${dir}/ttls/measure/tmpMain2x3.ttl",
//    "Final 2x3" to "${dir}/ttls/measure/tmpFinal2x3.ttl",

//    "Start 3x2" to "${dir}/ttls/measure/tmpStart3x2.ttl",
//    "Main 3x2" to "${dir}/ttls/measure/tmpMain3x2.ttl",
//    "Final 3x2" to "${dir}/ttls/measure/tmpFinal3x2.ttl",

//    "Start 2x4" to "${dir}/ttls/measure/tmpStart2x4.ttl",
//    "Main 2x4" to "${dir}/ttls/measure/tmpMain2x4.ttl",
//    "Final 2x4" to "${dir}/ttls/measure/tmpFinal2x4.ttl",

//    "Start 4x2" to "${dir}/ttls/measure/tmpStart4x2.ttl",
//    "Main 4x2" to "${dir}/ttls/measure/tmpMain4x2.ttl",
//    "Final 4x2" to "${dir}/ttls/measure/tmpFinal4x2.ttl",

//    "Start 3x3" to "${dir}/ttls/measure/tmpStart3x3.ttl",
//    "Main 3x3" to "${dir}/ttls/measure/tmpMain3x3.ttl",
//    "Final 3x3" to "${dir}/ttls/measure/tmpFinal3x3.ttl",

//    "Start 2x5" to "${dir}/ttls/measure/tmpStart2x5.ttl",
//    "Main 2x5" to "${dir}/ttls/measure/tmpMain2x5.ttl",
//    "Final 2x5" to "${dir}/ttls/measure/tmpFinal2x5.ttl",

//    "Start 4x3" to "${dir}/ttls/measure/tmpStart4x3.ttl",
//    "Main 4x3" to "${dir}/ttls/measure/tmpMain4x3.ttl",
//    "Final 4x3" to "${dir}/ttls/measure/tmpFinal4x3.ttl",

//    "Start 3x4" to "${dir}/ttls/measure/tmpStart3x4.ttl",
//    "Main 3x4" to "${dir}/ttls/measure/tmpMain3x4.ttl",
//    "Final 3x4" to "${dir}/ttls/measure/tmpFinal3x4.ttl",

//    "Start 3x5" to "${dir}/ttls/measure/tmpStart3x5.ttl",
//    "Main 3x5" to "${dir}/ttls/measure/tmpMain3x5.ttl",
//    "Final 3x5" to "${dir}/ttls/measure/tmpFinal3x5.ttl",

//    "Start 4x4" to "${dir}/ttls/measure/tmpStart4x4.ttl",
//    "Main 4x4" to "${dir}/ttls/measure/tmpMain4x4.ttl",
//    "Final 4x4" to "${dir}/ttls/measure/tmpFinal4x4.ttl",

//    "Start: 1 -> NotFound" to "${dir}/ttls/start/stage_start_pervy_uzel_Not_found.ttl",
//    "Start: 2 -> False" to "${dir}/ttls/start/stage_start_vtoroy_uzel_False.ttl",
//    "Start: 2 -> True" to "${dir}/ttls/start/stage_start_vtoroy_uzel_True.ttl",
//    "Main: 1 -> True" to "${dir}/ttls/main/stage main (1 узел True).ttl",
//    "Main: 2 -> False" to "${dir}/ttls/main/stage main (2 узел False).ttl",
//    "Main: 3 -> False" to "${dir}/ttls/main/stage main (3 узел False).ttl",
//    "Main: 4 -> False" to "${dir}/ttls/main/stage main (4 узел False).ttl",
//    "Main: 5 -> NotFound" to "${dir}/ttls/main/stage main (5 узел Not found).ttl",
//    "Main: 6 -> False" to "${dir}/ttls/main/stage main (6 узел False).ttl",
//    "Main: 7 -> False" to "${dir}/ttls/main/stage main (7 узел False).ttl",
//    "Main: 7 -> True" to "${dir}/ttls/main/stage main (7 узел True).ttl",
//    "Final: 1 -> False" to "${dir}/ttls/final/stage final (1 узел False).ttl",
//    "Final: 2 -> True" to "${dir}/ttls/final/stage final (1 узел True).ttl",
).map {
    val situationDomain = model.domain.copy()
    DomainRDFFiller.fillDomain(situationDomain, it.value)
    it.key to Triple(LearningSituation(situationDomain), LearningSituation(situationDomain), LearningSituation(situationDomain) to LearningSituation(situationDomain))
}.toMap()
