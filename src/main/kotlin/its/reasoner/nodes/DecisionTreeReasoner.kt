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

    /**
     * Вспомогательный класс для объединения выходов вычисления узла
     * @param value значение-ответ на узел, есть всегда
     * @param evaluationResult результат вычисления текущей ветви мысли, если текущий узел это [EndingNode]
     */
    data class Answer<out T : Any?>(
        val value: T,
        val evaluationResult: DecisionTreeEvaluationResult<*>?,
    )

    companion object {

        private fun DecisionTreeEvaluationResult<*>.asAnswer() =
            Answer(this.value, this)

        private fun <T> T.asAnswer() =
            Answer(this, null)

        /**
         * Вычислить текущий узел - получить для него ответ, либо готовый результат вычисления
         */
        @JvmStatic
        fun <T : Any?> LinkNode<T>.getAnswerUnion(situation: LearningSituation): Answer<T> {
            return use(DecisionTreeReasoner(situation)) as Answer<T>
        }

        /**
         * Вычислить текущий узел - получить для него либо следующий узел для перехода, либо результат
         * Данный метод гарантирует, что хотя бы одно из двух возвращаемых значений не равно null
         */
        @JvmStatic
        private fun <T : Any?> LinkNode<T>.getNextOrResult(situation: LearningSituation): Pair<DecisionTreeNode?, DecisionTreeEvaluationResult<*>?> {
            val answerUnion = this.getAnswerUnion(situation)
            val answer = answerUnion.value
            val outcomes = outcomes
            if (this is EndingNode && !outcomes.containsKey(answer)) {
                require(answerUnion.evaluationResult != null) {
                    "An evaluation result should have been formed at $this (Reasoner error)"
                }
                return null to answerUnion.evaluationResult
            }
            require(outcomes.containsKey(answer)) {
                "Node $this has no outcome with value '$answer', but such an answer was returned"
            }
            return outcomes[answer]!!.node to answerUnion.evaluationResult
        }

        /**
         * Получить ответ на узел дерева решений
         */
        @JvmStatic
        fun <T : Any?> LinkNode<T>.getAnswer(situation: LearningSituation): T {
            return this.getAnswerUnion(situation).value
        }

        /**
         * Получить корректный следующий узел
         */
        @JvmStatic
        fun <T : Any?> LinkNode<T>.correctNext(situation: LearningSituation): DecisionTreeNode? {
            return this.getNextOrResult(situation).first
        }

        /**
         * Получить результат вычисления ветви дерева мысли
         * @see DecisionTree.solve для прорешивания целого дерева
         * @param situation конкретная учебная ситуация
         * @param resultCallback произвольный обработчик любых (всех) возникающих результатов [DecisionTreeEvaluationResult]
         */
        @JvmStatic
        fun ThoughtBranch.getResult(
            situation: LearningSituation,
            resultCallback: DecisionTreeEvaluationResultsProcessor<*>,
        ): DecisionTreeEvaluationResult<*> {
            var curr = this.start
            while (curr is LinkNode<*>) {
                val (next, evaluationResult) = curr.getNextOrResult(situation)
                if (evaluationResult != null) resultCallback.process(evaluationResult)
                if (next == null) return evaluationResult!!
                curr = next
            }
            require(curr is BranchResultNode) {
                "The final node of the branch '$this' somehow wasn't a BranchResultNode (Reasoner error)"
            }
            curr.actionExpr?.use(OperatorReasoner.defaultReasoner(situation))
            return BasicDecisionTreeEvaluationResult(curr, situation.decisionTreeVariables.toMap())
                .also { resultCallback.process(it) }
        }

        /**
         * @see [ThoughtBranch.getResult]
         */
        @JvmStatic
        fun ThoughtBranch.getResult(situation: LearningSituation): DecisionTreeEvaluationResult<*> {
            return getResult(situation, DecisionTreeEvaluationResultsProcessor.EMPTY)
        }

        /**
         * Прорешать дерево мысли для конкретной ситуации
         * При заходе в дерево проверяет наличие переменных [DecisionTree.variables],
         * и довычисляет [DecisionTree.implicitVariables], если это необходимо.
         * @param situation конкретная учебная ситуация
         * @param resultCallback произвольный обработчик любых (всех) возникающих результатов [DecisionTreeEvaluationResult]
         */
        @JvmStatic
        fun DecisionTree.solve(
            situation: LearningSituation,
            resultCallback: DecisionTreeEvaluationResultsProcessor<*>,
        ): DecisionTreeEvaluationResult<*> {
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
            return mainBranch.getResult(situation, resultCallback)
        }

        /**
         * @see [DecisionTree.solve]
         */
        @JvmStatic
        fun DecisionTree.solve(situation: LearningSituation): DecisionTreeEvaluationResult<*> {
            return solve(situation, DecisionTreeEvaluationResultsProcessor.EMPTY)
        }
    }
}