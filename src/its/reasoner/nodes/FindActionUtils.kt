package its.reasoner.nodes

import its.model.expressions.getUsedVariables
import its.model.expressions.types.Obj
import its.model.nodes.FindActionNode
import its.reasoner.LearningSituation
import its.reasoner.operators.OperatorReasoner

data class FindResult(
    val correct : Obj?,
    val errors : Map<FindActionNode.FindErrorCategory, List<Obj>>,
)

fun FindActionNode.findCorrect(situation : LearningSituation) : Obj? {
    val correct = try {
        selectorExpr.use(OperatorReasoner.defaultReasoner(situation)) as Obj
    } catch (e: NoSuchElementException) {
        null
    }
    return correct
}

fun FindActionNode.findWithErrors(situation : LearningSituation) : FindResult{
    val correct = findCorrect(situation)

    val errors = mutableMapOf<FindActionNode.FindErrorCategory, List<Obj>>()
    for(category in errorCategories){
        if(correct == null && category.selectorExpr.getUsedVariables().intersect(this.allDeclaredVariables()).isNotEmpty())
            continue

        val objects = OperatorReasoner.defaultReasoner(situation).getObjectsByCondition(category.selectorExpr, "checked")
        errors[category] = objects.filter { obj -> errors.values.none{it.contains(obj)} }
    }
    return FindResult(correct, errors)
}