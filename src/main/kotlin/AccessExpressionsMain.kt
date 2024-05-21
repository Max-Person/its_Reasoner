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
import its.reasoner.nodes.DecisionTreeCompilerWithOtherStrategy
import its.reasoner.nodes.DecisionTreeReasoner._static.getCorrectPath
import its.reasoner.nodes.NodeCompilationResult
import its.reasoner.nodes.OtherStrategyNodeCompilationResult
import its.reasoner.operators.JenaRulesReasoner
import its.reasoner.operators.OperatorReasoner
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner
import org.apache.jena.reasoner.rulesys.Rule
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
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
    val resultOtherStrategy = (model.decisionTree.mainBranch.start as LinkNode<*>).use(DecisionTreeCompilerWithOtherStrategy(model.domain))
    val paths= findPaths(result)
    val endings = findAllEndings(model.decisionTree.mainBranch.start)

    val skolemName = genVariableName()
    val resPredicate = genPredicateName()

    val rulesArray = genRules(resultOtherStrategy, skolemName, resPredicate, endings)
    val rulesStringOtherStrategy = generateAuxiliaryRules() +
            PAUSE_MARK + resultOtherStrategy.rules +
            PAUSE_MARK + rulesArray.values.joinToString("") {
        "\n[\n$it"
    }

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

    rulesString = """
        
        [
        (?var1 http://www.w3.org/2000/01/rdf-schema#subClassOf ?var2)
        noValue(?var2, http://www.w3.org/2000/01/rdf-schema#subClassOf, ?var3)
        noValue(?var2, http://www.vstu.ru/poas/code#subClassOf_numeration...)
        makeUniqueID(?var4)
        ->
        (?var2 http://www.vstu.ru/poas/code#subClassOf_numeration... ?var4)
        ]

        [
        (?var1 http://www.w3.org/2000/01/rdf-schema#subClassOf ?var2)
        noValue(?var1, http://www.vstu.ru/poas/code#subClassOf_numeration...)
        (?var2 http://www.vstu.ru/poas/code#subClassOf_numeration... ?var3)
        makeUniqueID(?var4)
        strConcat(?var3, ".", ?var4, ?var5)
        ->
        (?var1 http://www.vstu.ru/poas/code#subClassOf_numeration... ?var5)
        ]

        <pause>


        [
        (?var103... http://www.vstu.ru/poas/code#var... "stage"^^http://www.w3.org/2001/XMLSchema#string) 
        (?var103... http://www.vstu.ru/poas/code#current_stage ?var102...) 
        makeSkolem(?var101...)
        ->
        (?var101... http://www.vstu.ru/poas/code#predicate14... ?var102...)
        ]

        [
        (?var74... http://www.vstu.ru/poas/code#var... "currentItem"^^http://www.w3.org/2001/XMLSchema#string) 
        (?var75... http://www.vstu.ru/poas/code#var... "targetItem"^^http://www.w3.org/2001/XMLSchema#string) 
        equal(?var74...,?var75...) 
        makeSkolem(?var73...)
        ->
        (?var73... http://www.vstu.ru/poas/code#predicate8... "true"^^http://www.w3.org/2001/XMLSchema#boolean)
        ]

        [
        (?var66... http://www.vstu.ru/poas/code#var... "symbol"^^http://www.w3.org/2001/XMLSchema#string) 
        (?var66... http://www.vstu.ru/poas/code#operator ??var65...)
        (??var65... http://www.vstu.ru/poas/code#operator_type ??var64...)
        (?var70... http://www.vstu.ru/poas/code#var... "currentItem"^^http://www.w3.org/2001/XMLSchema#string) 
        (?var70... http://www.vstu.ru/poas/code#item_type ??var69...)
        (??var64... http://www.vstu.ru/poas/code#can_be_applied_to ??var69...)
        makeSkolem(?var63...)
        ->
        (?var63... http://www.vstu.ru/poas/code#predicate7... "true"^^http://www.w3.org/2001/XMLSchema#boolean)
        ]

        [
        (?var50... http://www.vstu.ru/poas/code#var... "symbol"^^http://www.w3.org/2001/XMLSchema#string) 
        (?var50... http://www.vstu.ru/poas/code#operand ??var49...)
        (??var49... http://www.vstu.ru/poas/code#operand_type ??var48...)
        (?var55... http://www.vstu.ru/poas/code#var... "symbol"^^http://www.w3.org/2001/XMLSchema#string) 
        (?var55... http://www.vstu.ru/poas/code#operator ??var54...)
        (??var54... http://www.vstu.ru/poas/code#operator_type ??var53...)
        (?var59... http://www.vstu.ru/poas/code#var... "currentItem"^^http://www.w3.org/2001/XMLSchema#string) 
        (?var59... http://www.vstu.ru/poas/code#item_type ??var58...)
        (??var48... http://www.vstu.ru/poas/code#compatible_subj ?var62...)
        (?var62... http://www.vstu.ru/poas/code#compatible_obj_0 ??var53...)
        (?var62... http://www.vstu.ru/poas/code#compatible_obj_1 ??var58...)
        makeSkolem(?var47...)
        ->
        (?var47... http://www.vstu.ru/poas/code#predicate6... "true"^^http://www.w3.org/2001/XMLSchema#boolean)
        ]

        [
        (?item http://www.w3.org/1999/02/22-rdf-syntax-ns#type http://www.vstu.ru/poas/code#Item) 
        #isReachable(?var32...,http://www.w3.org/2000/01/rdf-schema#subClassOf,,"true"^^http://www.w3.org/2001/XMLSchema#boolean) 
        (?connection http://www.w3.org/1999/02/22-rdf-syntax-ns#type http://www.vstu.ru/poas/code#Connection) 
        #isReachable(?var33...,http://www.w3.org/2000/01/rdf-schema#subClassOf,,"true"^^http://www.w3.org/2001/XMLSchema#boolean) 
        (?item http://www.vstu.ru/poas/code#is_a ?connection)
        (?var35... http://www.vstu.ru/poas/code#var... "currentItem"^^http://www.w3.org/2001/XMLSchema#string) 
        (?var35... http://www.vstu.ru/poas/code#has ?connection)
        (?var39... http://www.vstu.ru/poas/code#var... "symbol"^^http://www.w3.org/2001/XMLSchema#string) 
        (?var39... http://www.vstu.ru/poas/code#operator ??var38...)
        (??var38... http://www.vstu.ru/poas/code#operator_type ??var37...)
        (?connection http://www.vstu.ru/poas/code#connection_type ??var42...)
        (??var37... http://www.vstu.ru/poas/code#associated_with ??var42...)
        (?var45... http://www.vstu.ru/poas/code#var... "targetItem"^^http://www.w3.org/2001/XMLSchema#string) 
        (?var45... http://www.vstu.ru/poas/code#reachable_from ?item)
        makeSkolem(?var31...)
        ->
        (?var31... http://www.vstu.ru/poas/code#predicate5... "true"^^http://www.w3.org/2001/XMLSchema#boolean)
        ]
                    [
                    (?var12... http://www.vstu.ru/poas/code#var... "stage"^^http://www.w3.org/2001/XMLSchema#string) 
        (?var12... http://www.vstu.ru/poas/code#current_stage ?var11...) 
        equal(?var11...,http://www.vstu.ru/poas/code#Main) 
        (?item http://www.w3.org/1999/02/22-rdf-syntax-ns#type http://www.vstu.ru/poas/code#Item) 
        #isReachable(?var13...,http://www.w3.org/2000/01/rdf-schema#subClassOf,,"true"^^http://www.w3.org/2001/XMLSchema#boolean) 
        (?connection http://www.w3.org/1999/02/22-rdf-syntax-ns#type http://www.vstu.ru/poas/code#Connection) 
        #isReachable(?var14...,http://www.w3.org/2000/01/rdf-schema#subClassOf,,"true"^^http://www.w3.org/2001/XMLSchema#boolean) 
        (?item http://www.vstu.ru/poas/code#is_a ?connection)
        (?var16... http://www.vstu.ru/poas/code#var... "currentItem"^^http://www.w3.org/2001/XMLSchema#string) 
        (?var16... http://www.vstu.ru/poas/code#has ?connection)
        (?var20... http://www.vstu.ru/poas/code#var... "symbol"^^http://www.w3.org/2001/XMLSchema#string) 
        (?var20... http://www.vstu.ru/poas/code#operator ??var19...)
        (??var19... http://www.vstu.ru/poas/code#operator_type ??var18...)
        (?connection http://www.vstu.ru/poas/code#connection_type ??var23...)
        (??var18... http://www.vstu.ru/poas/code#associated_with ??var23...)
        (?var27... http://www.vstu.ru/poas/code#var... "symbol"^^http://www.w3.org/2001/XMLSchema#string) 
        (?var27... http://www.vstu.ru/poas/code#operand ??var26...)
        (??var26... http://www.vstu.ru/poas/code#linked_with ?connection)

                    makeSkolem(?var30...)
                    ->
                    (?item http://www.vstu.ru/poas/code#var... "nextItem"^^http://www.w3.org/2001/XMLSchema#string)
                    (?var30... http://www.vstu.ru/poas/code#predicate4... ?item)
                    ]
        <pause>


        [
        (?var8... http://www.vstu.ru/poas/code#var... "targetItem"^^http://www.w3.org/2001/XMLSchema#string) 
        (?var9... http://www.vstu.ru/poas/code#var... "nextItem"^^http://www.w3.org/2001/XMLSchema#string) 
        (?var8... http://www.vstu.ru/poas/code#reachable_from ?var9...)
        makeSkolem(?var7...)
        ->
        (?var7... http://www.vstu.ru/poas/code#predicate3... "true"^^http://www.w3.org/2001/XMLSchema#boolean)
        ]

        [
        (?var6... http://www.vstu.ru/poas/code#var... "nextItem"^^http://www.w3.org/2001/XMLSchema#string) 
        (?var6... http://www.vstu.ru/poas/code#was_visited_before ?var5...) 
        makeSkolem(?var4...)
        ->
        (?var4... http://www.vstu.ru/poas/code#predicate2... ?var5...)
        ]

        [
        (?parentItem http://www.w3.org/1999/02/22-rdf-syntax-ns#type http://www.vstu.ru/poas/code#Item) 
        #isReachable(?var92...,http://www.w3.org/2000/01/rdf-schema#subClassOf,,"true"^^http://www.w3.org/2001/XMLSchema#boolean) 
        (?item http://www.vstu.ru/poas/code#contained_in ?parentItem)
        makeSkolem(?var94...)
        combine(?var95..., ?item, ?parentItem)
        ->
        (?var94... http://www.vstu.ru/poas/code#predicate11... ?var95...)
        ]

        <pause>

                    [
                    (?var84... http://www.vstu.ru/poas/code#var... "stage"^^http://www.w3.org/2001/XMLSchema#string) 
        (?var84... http://www.vstu.ru/poas/code#current_stage ?var83...) 
        equal(?var83...,http://www.vstu.ru/poas/code#Start) 
        (?item http://www.w3.org/1999/02/22-rdf-syntax-ns#type http://www.vstu.ru/poas/code#Item) 
        #isReachable(?var85...,http://www.w3.org/2000/01/rdf-schema#subClassOf,,"true"^^http://www.w3.org/2001/XMLSchema#boolean) 
        (?connection http://www.w3.org/1999/02/22-rdf-syntax-ns#type http://www.vstu.ru/poas/code#Connection) 
        #isReachable(?var86...,http://www.w3.org/2000/01/rdf-schema#subClassOf,,"true"^^http://www.w3.org/2001/XMLSchema#boolean) 
        (?item http://www.vstu.ru/poas/code#is_a ?connection)
        (?var89... http://www.vstu.ru/poas/code#var... "symbol"^^http://www.w3.org/2001/XMLSchema#string) 
        (?var89... http://www.vstu.ru/poas/code#operand ??var88...)
        (??var88... http://www.vstu.ru/poas/code#linked_with ?connection)
        (?parentItem http://www.w3.org/1999/02/22-rdf-syntax-ns#type http://www.vstu.ru/poas/code#Item) 
        #isReachable(?var92...,http://www.w3.org/2000/01/rdf-schema#subClassOf,,"true"^^http://www.w3.org/2001/XMLSchema#boolean) 
        (?var94... http://www.vstu.ru/poas/code#predicate11... ?var96...) 
        checkCombined("false"^^http://www.w3.org/2001/XMLSchema#boolean, ?var94..., http://www.vstu.ru/poas/code#predicate11..., ?item)
                    makeSkolem(?var97...)
                    ->
                    (?item http://www.vstu.ru/poas/code#var... "nextItem"^^http://www.w3.org/2001/XMLSchema#string)
                    (?var97... http://www.vstu.ru/poas/code#predicate12... ?item)
                    ]
        <pause>


        [
        (?var80... http://www.vstu.ru/poas/code#var... "targetItem"^^http://www.w3.org/2001/XMLSchema#string) 
        (?var81... http://www.vstu.ru/poas/code#var... "nextItem"^^http://www.w3.org/2001/XMLSchema#string) 
        (?var80... http://www.vstu.ru/poas/code#reachable_from ?var81...)
        makeSkolem(?var79...)
        ->
        (?var79... http://www.vstu.ru/poas/code#predicate10... "true"^^http://www.w3.org/2001/XMLSchema#boolean)
        ]

        [
        (?var99... http://www.vstu.ru/poas/code#var... "targetItem"^^http://www.w3.org/2001/XMLSchema#string) 
        (?var100... http://www.vstu.ru/poas/code#var... "currentItem"^^http://www.w3.org/2001/XMLSchema#string) 
        equal(?var99...,?var100...) 
        makeSkolem(?var98...)
        ->
        (?var98... http://www.vstu.ru/poas/code#predicate13... "true"^^http://www.w3.org/2001/XMLSchema#boolean)
        ]

        <pause>


        [
        (?var273... http://www.vstu.ru/poas/code#predicate14... http://www.vstu.ru/poas/code#Main)
        noValue(?var274... http://www.vstu.ru/poas/code#predicate8... ?var275...)
        (?var276... http://www.vstu.ru/poas/code#predicate7... ?var277...)
        (?var278... http://www.vstu.ru/poas/code#predicate6... ?var279...)
        (?var280... http://www.vstu.ru/poas/code#predicate5... ?var281...)
        (?var282... http://www.vstu.ru/poas/code#predicate4... ?var283...)
        (?var284... http://www.vstu.ru/poas/code#predicate3... ?var285...)
        (?var286... http://www.vstu.ru/poas/code#predicate2... "false"^^http://www.w3.org/2001/XMLSchema#boolean)
        makeSkolem(?var272...)
        noValue(?var287..., http://www.vstu.ru/poas/code#predicate25...)
        ->
        (?var272... http://www.vstu.ru/poas/code#predicate25... "0"^^http://www.w3.org/2001/XMLSchema#integer)
        ]

        <pause>


        [
        (?var288... http://www.vstu.ru/poas/code#predicate14... http://www.vstu.ru/poas/code#Main)
        noValue(?var289... http://www.vstu.ru/poas/code#predicate8... ?var290...)
        (?var291... http://www.vstu.ru/poas/code#predicate7... ?var292...)
        (?var293... http://www.vstu.ru/poas/code#predicate6... ?var294...)
        (?var295... http://www.vstu.ru/poas/code#predicate5... ?var296...)
        (?var297... http://www.vstu.ru/poas/code#predicate4... ?var298...)
        (?var299... http://www.vstu.ru/poas/code#predicate3... ?var300...)
        (?var301... http://www.vstu.ru/poas/code#predicate2... ?var302...)
        makeSkolem(?var272...)
        noValue(?var303..., http://www.vstu.ru/poas/code#predicate25...)
        ->
        (?var272... http://www.vstu.ru/poas/code#predicate25... "1"^^http://www.w3.org/2001/XMLSchema#integer)
        ]

        <pause>


        [
        (?var304... http://www.vstu.ru/poas/code#predicate14... http://www.vstu.ru/poas/code#Main)
        noValue(?var305... http://www.vstu.ru/poas/code#predicate8... ?var306...)
        (?var307... http://www.vstu.ru/poas/code#predicate7... ?var308...)
        (?var309... http://www.vstu.ru/poas/code#predicate6... ?var310...)
        (?var311... http://www.vstu.ru/poas/code#predicate5... ?var312...)
        (?var313... http://www.vstu.ru/poas/code#predicate4... ?var314...)
        noValue(?var315... http://www.vstu.ru/poas/code#predicate3... ?var316...)
        makeSkolem(?var272...)
        noValue(?var317..., http://www.vstu.ru/poas/code#predicate25...)
        ->
        (?var272... http://www.vstu.ru/poas/code#predicate25... "2"^^http://www.w3.org/2001/XMLSchema#integer)
        ]

        <pause>


        [
        (?var318... http://www.vstu.ru/poas/code#predicate14... http://www.vstu.ru/poas/code#Main)
        noValue(?var319... http://www.vstu.ru/poas/code#predicate8... ?var320...)
        (?var321... http://www.vstu.ru/poas/code#predicate7... ?var322...)
        (?var323... http://www.vstu.ru/poas/code#predicate6... ?var324...)
        (?var325... http://www.vstu.ru/poas/code#predicate5... ?var326...)
        noValue(?var327... http://www.vstu.ru/poas/code#predicate4... ?var328...)
        makeSkolem(?var272...)
        noValue(?var329..., http://www.vstu.ru/poas/code#predicate25...)
        ->
        (?var272... http://www.vstu.ru/poas/code#predicate25... "3"^^http://www.w3.org/2001/XMLSchema#integer)
        ]

        <pause>


        [
        (?var330... http://www.vstu.ru/poas/code#predicate14... http://www.vstu.ru/poas/code#Main)
        noValue(?var331... http://www.vstu.ru/poas/code#predicate8... ?var332...)
        (?var333... http://www.vstu.ru/poas/code#predicate7... ?var334...)
        (?var335... http://www.vstu.ru/poas/code#predicate6... ?var336...)
        noValue(?var337... http://www.vstu.ru/poas/code#predicate5... ?var338...)
        makeSkolem(?var272...)
        noValue(?var339..., http://www.vstu.ru/poas/code#predicate25...)
        ->
        (?var272... http://www.vstu.ru/poas/code#predicate25... "4"^^http://www.w3.org/2001/XMLSchema#integer)
        ]

        <pause>


        [
        (?var340... http://www.vstu.ru/poas/code#predicate14... http://www.vstu.ru/poas/code#Main)
        noValue(?var341... http://www.vstu.ru/poas/code#predicate8... ?var342...)
        (?var343... http://www.vstu.ru/poas/code#predicate7... ?var344...)
        noValue(?var345... http://www.vstu.ru/poas/code#predicate6... ?var346...)
        makeSkolem(?var272...)
        noValue(?var347..., http://www.vstu.ru/poas/code#predicate25...)
        ->
        (?var272... http://www.vstu.ru/poas/code#predicate25... "5"^^http://www.w3.org/2001/XMLSchema#integer)
        ]

        <pause>


        [
        (?var348... http://www.vstu.ru/poas/code#predicate14... http://www.vstu.ru/poas/code#Main)
        noValue(?var349... http://www.vstu.ru/poas/code#predicate8... ?var350...)
        noValue(?var351... http://www.vstu.ru/poas/code#predicate7... ?var352...)
        makeSkolem(?var272...)
        noValue(?var353..., http://www.vstu.ru/poas/code#predicate25...)
        ->
        (?var272... http://www.vstu.ru/poas/code#predicate25... "6"^^http://www.w3.org/2001/XMLSchema#integer)
        ]

        <pause>


        [
        (?var354... http://www.vstu.ru/poas/code#predicate14... http://www.vstu.ru/poas/code#Main)
        (?var355... http://www.vstu.ru/poas/code#predicate8... ?var356...)
        makeSkolem(?var272...)
        noValue(?var357..., http://www.vstu.ru/poas/code#predicate25...)
        ->
        (?var272... http://www.vstu.ru/poas/code#predicate25... "7"^^http://www.w3.org/2001/XMLSchema#integer)
        ]

        <pause>


        [
        (?var358... http://www.vstu.ru/poas/code#predicate14... http://www.vstu.ru/poas/code#Start)
        (?var359... http://www.vstu.ru/poas/code#predicate12... ?var360...)
        (?var361... http://www.vstu.ru/poas/code#predicate10... ?var362...)
        makeSkolem(?var272...)
        noValue(?var363..., http://www.vstu.ru/poas/code#predicate25...)
        ->
        (?var272... http://www.vstu.ru/poas/code#predicate25... "8"^^http://www.w3.org/2001/XMLSchema#integer)
        ]

        <pause>


        [
        (?var364... http://www.vstu.ru/poas/code#predicate14... http://www.vstu.ru/poas/code#Start)
        (?var365... http://www.vstu.ru/poas/code#predicate12... ?var366...)
        noValue(?var367... http://www.vstu.ru/poas/code#predicate10... ?var368...)
        makeSkolem(?var272...)
        noValue(?var369..., http://www.vstu.ru/poas/code#predicate25...)
        ->
        (?var272... http://www.vstu.ru/poas/code#predicate25... "9"^^http://www.w3.org/2001/XMLSchema#integer)
        ]

        <pause>


        [
        (?var370... http://www.vstu.ru/poas/code#predicate14... http://www.vstu.ru/poas/code#Start)
        noValue(?var371... http://www.vstu.ru/poas/code#predicate12... ?var372...)
        makeSkolem(?var272...)
        noValue(?var373..., http://www.vstu.ru/poas/code#predicate25...)
        ->
        (?var272... http://www.vstu.ru/poas/code#predicate25... "10"^^http://www.w3.org/2001/XMLSchema#integer)
        ]

        <pause>


        [
        (?var374... http://www.vstu.ru/poas/code#predicate14... http://www.vstu.ru/poas/code#Final)
        noValue(?var375... http://www.vstu.ru/poas/code#predicate13... ?var376...)
        makeSkolem(?var272...)
        noValue(?var377..., http://www.vstu.ru/poas/code#predicate25...)
        ->
        (?var272... http://www.vstu.ru/poas/code#predicate25... "11"^^http://www.w3.org/2001/XMLSchema#integer)
        ]

        <pause>


        [
        (?var378... http://www.vstu.ru/poas/code#predicate14... http://www.vstu.ru/poas/code#Final)
        (?var379... http://www.vstu.ru/poas/code#predicate13... ?var380...)
        makeSkolem(?var272...)
        noValue(?var381..., http://www.vstu.ru/poas/code#predicate25...)
        ->
        (?var272... http://www.vstu.ru/poas/code#predicate25... "12"^^http://www.w3.org/2001/XMLSchema#integer)
        ]
        
    """.trimIndent()

    val rulesSets = rulesString.split(PAUSE_MARK.trim())
    val parsedRulesSets = rulesSets.map { set ->
        Rule.parseRules(set)
    }

    val rulesSetsOtherStrategy = rulesStringOtherStrategy.split(PAUSE_MARK.trim())
    val parsedRulesSetsOtherStrategy = rulesSetsOtherStrategy.map { set ->
        Rule.parseRules(set)
    }

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
            val memory = measureMemoryUsage {
                interpreterResult = model.decisionTree.mainBranch.getCorrectPath(first).last() as BranchResultNode
            }
            println(memory / 1024F / 1024)
        }
        interpreterTimes[situationSize] = interpreterReasoningTime
        println("Interpreter reasoning time: $interpreterReasoningTime")

        getDefaultOperatorReasoner = {
            JenaRulesReasoner(it)
        }

        println("Rules reasoning start: ++++++++++++++++++++++")
        val rulesReasonerResult: BranchResultNode
        val rulesReasoningTime = measureTimeMillis {
            val memory = measureMemoryUsage {
                rulesReasonerResult = model.decisionTree.mainBranch.getCorrectPath(second).last() as BranchResultNode
            }
            println(memory / 1024F / 1024)
        }
        rulesReasoningTimes[situationSize] = rulesReasoningTime
        println("Rules reasoning time: $rulesReasoningTime")

        val jenaModel = DomainRDFWriter.saveDomain(third.domain)
        var inf = ModelFactory.createInfModel(GenericRuleReasoner(listOf()), jenaModel)

        parsedRulesSets.forEach { rules ->
            val reasoner = GenericRuleReasoner(rules)
            inf = ModelFactory.createInfModel(reasoner, inf)
        }

        println("Compiled tree reasoning start: ++++++++++++++++++++++")
        val compiledTreeReasonerResult: BranchResultNode
        val compiledTreeReasoningTime = measureTimeMillis {
            val memory = measureMemoryUsage {
                val prop = inf.getProperty(resPredicate)
                compiledTreeReasonerResult = endings[inf.listObjectsOfProperty(prop).next().asLiteral().int]
            }
            println(memory / 1024F / 1024)
        }
        compiledTreeReasoningTimes[situationSize] = compiledTreeReasoningTime
        println("Compiled tree reasoning time: $compiledTreeReasoningTime")

        val jenaModelOtherStrategy = DomainRDFWriter.saveDomain(fourth.domain)
        var infOtherStrategy = ModelFactory.createInfModel(GenericRuleReasoner(listOf()), jenaModelOtherStrategy)

        parsedRulesSetsOtherStrategy.forEach { rules ->
            val reasoner = GenericRuleReasoner(rules)
            infOtherStrategy = ModelFactory.createInfModel(reasoner, infOtherStrategy)
        }

//        println("Compiled tree other strategy reasoning start: ++++++++++++++++++++++")
//        val compiledTreeReasonerResultOtherStrategy: BranchResultNode
//        val compiledTreeOtherStrategyReasoningTime = measureTimeMillis {
//            val prop = infOtherStrategy.getProperty(resPredicate)
//            compiledTreeReasonerResultOtherStrategy = endings[infOtherStrategy.listObjectsOfProperty(prop).next().asLiteral().int]
//        }
//        compiledTreeOtherStrategyReasoningTimes[situationSize] = compiledTreeOtherStrategyReasoningTime
//        println("Compiled tree other strategy reasoning time: $compiledTreeOtherStrategyReasoningTime")

        require(interpreterResult === rulesReasonerResult)
        require(interpreterResult === compiledTreeReasonerResult)
//        require(interpreterResult === compiledTreeReasonerResultOtherStrategy)

        println("------------------------------------------------------------------------------")
    }
    println(interpreterTimes.toSortedMap().keys)
    println(interpreterTimes.toSortedMap().values)
    println(rulesReasoningTimes.toSortedMap().values)
    println(compiledTreeReasoningTimes.toSortedMap().values)
    println(compiledTreeOtherStrategyReasoningTimes.toSortedMap().values)
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

private fun genRules(
    result: OtherStrategyNodeCompilationResult,
    skolemName: String,
    resPredicateName: String,
    endings: List<BranchResultNode>,
): MutableMap<BranchResultNode, String> {
    if (result.children == null) {
        return mutableMapOf(result.ending!! to """
            ->
            ($skolemName $resPredicateName ${genValue(endings.indexOf(result.ending))})
            ]
        """.trimIndent())
    }

    val res = mutableMapOf<BranchResultNode, String>()
    result.children.forEach { (key, nodeCompilationResult) ->
        val childResult = genRules(nodeCompilationResult, skolemName, resPredicateName, endings)
        val tmp = childResult.map { (ending, body) ->
            ending to (result.bodies?.get(key)!! + body)
        }.toMap()
        res.putAll(tmp)
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

private fun obtainModels(dir: String, model: DomainSolvingModel): Map<String, Triple<LearningSituation, LearningSituation, Pair<LearningSituation, LearningSituation>>> = mapOf(
//    "Start 1x2" to "${dir}/ttls/measure/tmpStart1x2.ttl",
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
//    "Main 2x5" to "${dir}/ttls/measure/tmpMain2x5.ttl",-------------
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
    "Main 4x4" to "${dir}/ttls/measure/tmpMain4x4.ttl",
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

@OptIn(ExperimentalContracts::class)
private fun measureMemoryUsage(block: () -> Unit): Long {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    // Создаем экземпляр Runtime
    val runtime = Runtime.getRuntime()

    // Вызываем сборщик мусора для более точного измерения
    runtime.gc()

    // Запоминаем начальное использование памяти
    val startMemory = runtime.totalMemory() - runtime.freeMemory()

    // Вызываем функцию, использование памяти которой мы хотим измерить
    block()

    // Запоминаем конечное использование памяти
    val endMemory = runtime.totalMemory() - runtime.freeMemory()

    // Вычисляем разницу в использовании памяти
    val memoryUsed = endMemory - startMemory

    return memoryUsed
}
