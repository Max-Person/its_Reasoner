package its.reasoner.nodes

import its.model.definition.ThisShouldNotHappen
import its.model.definition.types.Obj
import its.model.expressions.Operator
import its.model.expressions.getUsedVariables
import its.model.nodes.*
import its.model.nodes.visitors.LinkNodeBehaviour
import its.reasoner.LearningSituation
import its.reasoner.operators.OperatorReasoner
import its.reasoner.operators.OperatorReasoner.Companion.evalAs

/**
 * Ризонер дерева решений
 * Описывается как поведение узлов дерева решений, выдающее ответ на конкретный узел
 * @param situation текущая ситуация, описывающая задачу (изменяется ризонером)
 */
class DecisionTreeReasoner private constructor(val situation: LearningSituation) :
    LinkNodeBehaviour<DecisionTreeReasoner.Answer<Any>> {

    private val exprReasoner = OperatorReasoner.defaultReasoner(situation)
    private fun <T> Operator.evalAs(): T = evalAs(exprReasoner)

    override fun process(node: CycleAggregationNode): Answer<BranchResult> {
        val nestedResults = exprReasoner.getObjectsByCondition(node.selectorExpr, node.variable)
            .map { obj ->
                situation.decisionTreeVariables[node.variable.varName] = obj
                val result = node.thoughtBranch.getResult(situation)
                situation.decisionTreeVariables.remove(node.variable.varName)
                result
            }

        return evaluateAggregation(node, nestedResults).asAnswer()
    }

    override fun process(node: WhileCycleNode): Answer<BranchResult> {
        while (node.conditionExpr.evalAs<Boolean>()) {
            val branchResult = node.thoughtBranch.getResult(situation)
            if (branchResult.value != BranchResult.NULL) {
                return branchResult.asAnswer()
            }
        }
        return BranchResult.NULL.asAnswer()
    }

    private fun process(assignment: DecisionTreeVarAssignment): Boolean {
        val value = assignment.valueExpr.evalAs<Obj?>()
        if (value != null) {
            situation.decisionTreeVariables[assignment.variable.varName] = value
            return true
        }
        return false
    }

    override fun process(node: FindActionNode): Answer<Boolean> {
        val found = process(node.varAssignment)
        if (found) {
            node.secondaryAssignments.forEach {
                val secondaryFound = process(it)
                if (!secondaryFound)
                    throw ThisShouldNotHappen()
            }
            return true.asAnswer()
        }
        return false.asAnswer()
    }

    data class FindResult(
        val correct: List<Obj>,
        val errors: Map<FindErrorCategory, List<Obj>>,
    )

    fun searchWithErrors(node: CycleAggregationNode): FindResult {
        val iteratedValues = exprReasoner.getObjectsByCondition(node.selectorExpr, node.variable)

        val errors = mutableMapOf<FindErrorCategory, List<Obj>>()
        for (category in node.errorCategories.sortedBy { it.priority }) {
            val objects = exprReasoner.getObjectsByCondition(category.selectorExpr, category.checkedVariable)
            errors[category] = objects.filter { obj -> errors.values.none { it.contains(obj) } }
        }

        return FindResult(iteratedValues, errors)
    }

    fun processWithErrors(node: FindActionNode): FindResult {
        val isFound = process(node).value
        val allVariables = listOf(node.varAssignment).plus(node.secondaryAssignments)
            .map { it.variable.varName }
            .toSet()

        val errors = mutableMapOf<FindErrorCategory, List<Obj>>()
        for (category in node.errorCategories.sortedBy { it.priority }) {
            if (!isFound && category.selectorExpr.getUsedVariables().intersect(allVariables).isNotEmpty())
                continue

            val objects = exprReasoner.getObjectsByCondition(category.selectorExpr, category.checkedVariable)
            errors[category] = objects.filter { obj -> errors.values.none { it.contains(obj) } }
        }

        val correct = if (isFound) situation.decisionTreeVariables[node.varAssignment.variable.varName] else null
        return FindResult(listOf(correct).filterNotNull(), errors)
    }

    override fun process(node: BranchAggregationNode): Answer<BranchResult> {
        return evaluateAggregation(node, node.thoughtBranches.map { it.getResult(situation) }).asAnswer()
    }

    private fun evaluateAggregation(
        node: AggregationNode,
        nestedResults: List<DecisionTreeEvaluationResult<*>>
    ): AggregatedDecisionTreeEvaluationResult {
        val resultsValues = nestedResults.map { it.value }
        val aggregatedResult = when (node.aggregationMethod) {
            AggregationMethod.AND ->
                if (resultsValues.all { it == BranchResult.NULL })
                    BranchResult.NULL
                else if (resultsValues.all { it == BranchResult.CORRECT || it == BranchResult.NULL })
                    BranchResult.CORRECT
                else
                    BranchResult.ERROR

            AggregationMethod.OR ->
                if (resultsValues.all { it == BranchResult.NULL })
                    BranchResult.NULL
                else if (resultsValues.any { it == BranchResult.CORRECT })
                    BranchResult.CORRECT
                else
                    BranchResult.ERROR

            AggregationMethod.HYP ->
                if (resultsValues.any { it == BranchResult.CORRECT })
                    BranchResult.CORRECT
                else if (resultsValues.any { it == BranchResult.ERROR })
                    BranchResult.ERROR
                else
                    BranchResult.NULL

            AggregationMethod.MUTEX -> resultsValues.singleOrNull { it != BranchResult.NULL } ?: BranchResult.NULL
        }
        return AggregatedDecisionTreeEvaluationResult(
            aggregatedResult,
            node,
            nestedResults,
            situation.decisionTreeVariables.toMap()
        )
    }

    override fun process(node: QuestionNode): Answer<Any> {
        return node.expr.evalAs<Any>().asAnswer()
    }

    override fun processTupleQuestionNode(node: TupleQuestionNode): Answer<List<Any?>> {
        val exprTuple = node.parts.map { it.expr.evalAs<Any>() }
        return (node.outcomes.keys.firstOrNull { it.matches(exprTuple) } ?: exprTuple).asAnswer()
    }

    data class Answer<out T : Any?>(
        val value: T,
        val evaluationResult: DecisionTreeEvaluationResult<*>?,
    )

    companion object {

        private fun DecisionTreeEvaluationResult<*>.asAnswer() =
            Answer(this.value, this)

        private fun <T> T.asAnswer() =
            Answer(this, null)

        @JvmStatic
        fun LinkNode<*>.getAnswerUnion(situation: LearningSituation): Answer<*> {
            return use(DecisionTreeReasoner(situation))
        }

        /**
         * Получить ответ на узел дерева решений
         */
        @JvmStatic
        fun <AnswerType : Any?> LinkNode<AnswerType>.getAnswer(situation: LearningSituation): AnswerType {
            return this.getAnswerUnion(situation).value as AnswerType
        }

        @JvmStatic
        private fun LinkNode<*>.getNext(answer: Any?): DecisionTreeNode? {
            val outcomes = outcomes as Outcomes<Any?>
            if (this is EndingNode && !outcomes.containsKey(answer)) {
                return null
            }
            require(outcomes.containsKey(answer)) {
                "Node $this has no outcome with value '$answer', but such an answer was returned"
            }
            return outcomes[answer]!!.node
        }

        /**
         * Получить корректный следующий узел
         */
        @JvmStatic
        fun LinkNode<*>.correctNext(situation: LearningSituation): DecisionTreeNode? {
            return this.getNext(this.getAnswer(situation))
        }

        /**
         * Получить корректный путь рассуждений в ветви мысли;
         *
         * Корректный путь представляет собой последовательность
         * посещенных на самом верхнем уровне (без ухода во вложенные ветки) узлов
         */
        @JvmStatic
        fun ThoughtBranch.getResult(situation: LearningSituation): DecisionTreeEvaluationResult<*> {
            var curr = this.start
            while (curr is LinkNode<*>) {
                val answer = curr.getAnswerUnion(situation)
                val next = curr.getNext(answer.value)
                if (next == null) {
                    require(answer.evaluationResult != null) {
                        "An evaluation result should have been formed at $curr (Reasoner error)"
                    }
                    return answer.evaluationResult
                }
                curr = next
            }
            require(curr is BranchResultNode) {
                "The final node of the branch '$this' somehow wasn't a BranchResultNode (Reasoner error)"
            }
            curr.actionExpr?.use(OperatorReasoner.defaultReasoner(situation))
            return BasicDecisionTreeEvaluationResult(curr, situation.decisionTreeVariables.toMap())
        }

        /**
         * Прорешать дерево мысли для конкретной ситуации;
         * @see getResults
         */
        @JvmStatic
        fun DecisionTree.solve(situation: LearningSituation): DecisionTreeEvaluationResult<*> {
            variables.forEach { variable ->
                require(situation.decisionTreeVariables.containsKey(variable.varName))
                val obj = situation.decisionTreeVariables[variable.varName]!!.findInOrUnkown(situation.domainModel)
                require(obj.isInstanceOf(variable.className))
            }
            implicitVariables.forEach {
                if (!situation.decisionTreeVariables.containsKey(it.variable.varName)) {
                    DecisionTreeReasoner(situation).process(it)
                }
            }
            return mainBranch.getResult(situation)
        }
    }

    sealed class DecisionTreeEvaluationResult<Node : EndingNode>(
        val value: BranchResult,
        val node: Node,
        val variablesSnapshot: Map<String, Obj>,
    )

    class BasicDecisionTreeEvaluationResult(
        node: BranchResultNode,
        variablesSnapshot: Map<String, Obj>
    ) : DecisionTreeEvaluationResult<BranchResultNode>(node.value, node, variablesSnapshot)

    class AggregatedDecisionTreeEvaluationResult(
        result: BranchResult,
        node: AggregationNode,
        val nestedResults: List<DecisionTreeEvaluationResult<*>>,
        variablesSnapshot: Map<String, Obj>
    ) : DecisionTreeEvaluationResult<AggregationNode>(result, node, variablesSnapshot) {
        val aggregationMethod = node.aggregationMethod
    }
}