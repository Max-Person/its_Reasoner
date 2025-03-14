package its.reasoner.nodes

import its.model.ValueTuple
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
class DecisionTreeReasoner(val situation: LearningSituation) : LinkNodeBehaviour<DecisionTreeTraceElement<*, *>> {

    private val exprReasoner = OperatorReasoner.defaultReasoner(situation)
    private fun <T> Operator.evalAs(): T = evalAs(exprReasoner)

    override fun process(node: CycleAggregationNode): AggregationDecisionTreeTraceElement<Obj> {
        val branchTracesMap = exprReasoner.getObjectsByCondition(node.selectorExpr, node.variable)
            .associateWith { obj ->
                situation.decisionTreeVariables[node.variable.varName] = obj
                val result = node.thoughtBranch.solve(situation)
                situation.decisionTreeVariables.remove(node.variable.varName)
                result
            }

        return aggregationTraceElement(node, branchTracesMap)
    }

    override fun process(node: WhileCycleNode): WhileCycleDecisionTreeTraceElement {
        val branchTraces = mutableListOf<DecisionTreeTrace>()
        while (node.conditionExpr.evalAs<Boolean>()) {
            val trace = node.thoughtBranch.solve(situation)
            branchTraces.add(trace)
            if (trace.branchResult != BranchResult.NULL) {
                break
            }
        }
        return WhileCycleDecisionTreeTraceElement(
            node,
            branchTraces.lastOrNull()?.branchResult ?: BranchResult.NULL,
            situation.decisionTreeVariables.toMap(),
            branchTraces
        )
    }

    private fun process(assignment: DecisionTreeVarAssignment): Boolean {
        val value = assignment.valueExpr.evalAs<Obj?>()
        if (value != null) {
            situation.decisionTreeVariables[assignment.variable.varName] = value
            return true
        }
        return false
    }

    override fun process(node: FindActionNode): LinkDecisionTreeTraceElement<Boolean> {
        val isFound = process(node.varAssignment)
        if (isFound) {
            node.secondaryAssignments.forEach {
                val secondaryFound = process(it)
                if (!secondaryFound)
                    throw ThisShouldNotHappen()
            }
        }
        return linkNodeTraceElement(node, isFound)
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
        val isFound = process(node).nodeResult
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

    override fun process(node: BranchAggregationNode): AggregationDecisionTreeTraceElement<ThoughtBranch> {
        return aggregationTraceElement(
            node,
            node.thoughtBranches.associateWith { it.solve(situation) }
        )
    }

    private fun evaluateAggregation(
        aggregationMethod: AggregationMethod,
        nestedResults: Collection<BranchResult>
    ): BranchResult {
        return when (aggregationMethod) {
            AggregationMethod.AND ->
                if (nestedResults.all { it == BranchResult.NULL })
                    BranchResult.NULL
                else if (nestedResults.all { it == BranchResult.CORRECT || it == BranchResult.NULL })
                    BranchResult.CORRECT
                else
                    BranchResult.ERROR

            AggregationMethod.OR ->
                if (nestedResults.all { it == BranchResult.NULL })
                    BranchResult.NULL
                else if (nestedResults.any { it == BranchResult.CORRECT })
                    BranchResult.CORRECT
                else
                    BranchResult.ERROR

            AggregationMethod.HYP ->
                if (nestedResults.any { it == BranchResult.CORRECT })
                    BranchResult.CORRECT
                else if (nestedResults.any { it == BranchResult.ERROR })
                    BranchResult.ERROR
                else
                    BranchResult.NULL

            AggregationMethod.MUTEX -> nestedResults.singleOrNull { it != BranchResult.NULL } ?: BranchResult.NULL
        }
    }

    override fun process(node: QuestionNode): LinkDecisionTreeTraceElement<Any> {
        val value = node.expr.evalAs<Any>()
        return linkNodeTraceElement(node, value)
    }

    override fun processTupleQuestionNode(node: TupleQuestionNode): LinkDecisionTreeTraceElement<ValueTuple> {
        val exprTuple = ValueTuple(node.parts.map { it.expr.evalAs<Any>() })
        return linkNodeTraceElement(
            node,
            node.outcomes.keys.firstOrNull { it.matches(exprTuple) } ?: exprTuple
        )
    }

    private fun <AnswerType : Any> linkNodeTraceElement(
        node: LinkNode<AnswerType>,
        nodeResult: AnswerType,
    ) = LinkDecisionTreeTraceElement(node, nodeResult, situation.decisionTreeVariables.toMap())

    private fun <BranchInfo : Any> aggregationTraceElement(
        node: AggregationNode,
        branchTraceMap: Map<BranchInfo, DecisionTreeTrace>
    ) = AggregationDecisionTreeTraceElement(
        node,
        evaluateAggregation(node.aggregationMethod, branchTraceMap.values.map { it.branchResult }),
        situation.decisionTreeVariables.toMap(),
        branchTraceMap
    )

    companion object {

        /**
         * Вычислить текущий узел - получить для него ответ, либо готовый результат вычисления
         */
        @JvmStatic
        fun <T : Any> LinkNode<T>.execute(situation: LearningSituation): DecisionTreeTraceElement<T, *> {
            return use(DecisionTreeReasoner(situation)) as DecisionTreeTraceElement<T, *>
        }

        /**
         * Получить ответ на узел дерева решений
         */
        @JvmStatic
        fun <T : Any> LinkNode<T>.getAnswer(situation: LearningSituation): T {
            return this.execute(situation).nodeResult
        }

        @JvmStatic
        private fun <T : Any> LinkNode<T>.getNextAny(answer: Any): DecisionTreeNode? {
            return this.getNextNode(answer as T)
        }

        /**
         * Получить корректный следующий узел
         */
        @JvmStatic
        fun <T : Any> LinkNode<T>.correctNext(situation: LearningSituation): DecisionTreeNode? {
            return this.getNextNode(this.getAnswer(situation))
        }

        /**
         * Прорешать ветвь мысли и получить трассу ее выполнение
         * @see DecisionTree.solve для прорешивания целого дерева
         */
        @JvmStatic
        fun ThoughtBranch.solve(situation: LearningSituation): DecisionTreeTrace {
            var curr = this.start
            val traceElements = mutableListOf<DecisionTreeTraceElement<*, *>>()
            while (curr is LinkNode<*>) {
                val traceElement = curr.execute(situation)
                traceElements.add(traceElement)

                val answer = traceElement.nodeResult
                val next = curr.getNextAny(answer)
                if (next == null) {
                    require(answer is BranchResult) {
                        if (curr is EndingNode)
                            "An evaluation result should have been formed at $curr (Reasoner error)"
                        else
                            "Node $curr has no outcome with value '$answer', but such an answer was returned"
                    }
                    return DecisionTreeTrace(traceElements)
                }
                curr = next
            }
            require(curr is BranchResultNode) {
                "The final node of the branch '$this' somehow wasn't a BranchResultNode (Reasoner error)"
            }
            curr.actionExpr?.use(OperatorReasoner.defaultReasoner(situation))
            traceElements.add(BranchResultDecisionTreeTraceElement(curr, situation.decisionTreeVariables.toMap()))
            return DecisionTreeTrace(traceElements)
        }

        /**
         * Прорешать дерево мысли для конкретной ситуации
         * При заходе в дерево проверяет наличие переменных [DecisionTree.variables],
         * и довычисляет [DecisionTree.implicitVariables], если это необходимо
         */
        @JvmStatic
        fun DecisionTree.solve(situation: LearningSituation): DecisionTreeTrace {
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
            return mainBranch.solve(situation)
        }
    }
}