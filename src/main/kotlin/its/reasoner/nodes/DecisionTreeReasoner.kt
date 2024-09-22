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
class DecisionTreeReasoner(val situation: LearningSituation) : LinkNodeBehaviour<Any?> {

    private val exprReasoner = OperatorReasoner.defaultReasoner(situation)
    private fun <T> Operator.evalAs(): T = evalAs(exprReasoner)

    override fun process(node: CycleAggregationNode): Boolean {
        val iteratedValues = exprReasoner.getObjectsByCondition(node.selectorExpr, node.variable)
        var res =
            node.logicalOp == LogicalOp.AND //Исходное значение результата зависит от оператора: true для И, false для ИЛИ
        for(value in iteratedValues){
            situation.decisionTreeVariables[node.variable.varName] = value
            val cur = node.thoughtBranch.getAnswer(situation)
            res = when(node.logicalOp){
                LogicalOp.AND -> res && cur
                LogicalOp.OR -> res || cur
            }
        }
        situation.decisionTreeVariables.remove(node.variable.varName)
        return  res
    }

    override fun process(node: WhileAggregationNode): Boolean {
        var res = node.logicalOp == LogicalOp.AND
        while (node.conditionExpr.evalAs<Boolean>()) {
            val cur = node.thoughtBranch.getAnswer(situation)
            res = when(node.logicalOp){ //FIXME? Уточнить семантику - точно ли данном цикле не надо прерываться если результат известен
                LogicalOp.AND -> res && cur
                LogicalOp.OR -> res || cur
            }
        }
        return res
    }

    private fun process(assignment: DecisionTreeVarAssignment): Boolean {
        val value = assignment.valueExpr.evalAs<Obj?>()
        if (value != null) {
            situation.decisionTreeVariables[assignment.variable.varName] = value
            return true
        }
        return false
    }

    override fun process(node: FindActionNode): Boolean {
        val found = process(node.varAssignment)
        if (found) {
            node.secondaryAssignments.forEach {
                val secondaryFound = process(it)
                if (!secondaryFound)
                    throw ThisShouldNotHappen()
            }
            return true
        }
        return false
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
        val isFound = process(node)
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

    override fun process(node: LogicAggregationNode): Boolean {
        var res = node.logicalOp == LogicalOp.AND
        for(branch in node.thoughtBranches){
            val cur = branch.getAnswer(situation)
            res = when(node.logicalOp){
                LogicalOp.AND -> res && cur
                LogicalOp.OR -> res || cur
            }
        }
        return  res
    }

    override fun process(node: PredeterminingFactorsNode): ThoughtBranch? {
        val predetermining = node.predetermining.singleOrNull { it.key!!.getAnswer(situation) }
        return predetermining?.key
            ?: (node.undetermined ?: throw ThisShouldNotHappen()).key
    }

    override fun process(node: QuestionNode): Any {
        return node.expr.evalAs<Any>()
    }

    override fun processTupleQuestionNode(node: TupleQuestionNode): Any? {
        val exprTuple = node.parts.map { it.expr.evalAs<Any>() }
        return node.outcomes.keys.firstOrNull { it.matches(exprTuple) } ?: exprTuple
    }

    companion object _static{
        /**
         * Получить ответ на узел дерева решений
         */
        @JvmStatic
        fun <AnswerType : Any?> LinkNode<AnswerType>.getAnswer(situation: LearningSituation): AnswerType {
            return use(DecisionTreeReasoner(situation)) as AnswerType
        }

        /**
         * Получить корректный следующий узел
         */
        @JvmStatic
        fun LinkNode<*>.correctNext(situation: LearningSituation): DecisionTreeNode {
            val ans = this.getAnswer(situation)
            val outcomes = outcomes as Outcomes<Any?>
            require(outcomes.containsKey(ans)) { "Node $this has no outcome with value '$ans', but such an answer was returned" }
            return outcomes[ans]!!.node
        }

        /**
         * Получить ответ на ветвь мысли
         */
        @JvmStatic
        fun ThoughtBranch.getAnswer(situation: LearningSituation): Boolean {
            val path = this.getCorrectPath(situation)
            val last = path.last() as BranchResultNode
            return last.value
        }

        /**
         * Получить корректный путь рассуждений в ветви мысли;
         *
         * Корректный путь представляет собой последовательность
         * посещенных на самом верхнем уровне (без ухода во вложенные ветки) узлов
         */
        @JvmStatic
        fun ThoughtBranch.getCorrectPath(situation: LearningSituation): List<DecisionTreeNode> {
            val path = mutableListOf<DecisionTreeNode>()
            var curr = this.start
            path.add(curr)
            while (curr is LinkNode<*>) {
                curr = curr.correctNext(situation)
                path.add(curr)
            }
            val last = path.last()
            require(last is BranchResultNode)
            last.actionExpr?.use(OperatorReasoner.defaultReasoner(situation))
            results?.add(DecisionTreeEvaluationResult(last, situation.decisionTreeVariables.toMap()))
            return path
        }

        @JvmStatic
        private var results : MutableList<DecisionTreeEvaluationResult>? = null

        /**
         * Получить результаты решения ветви;
         *
         * Результаты решения представляют собой последовательность посещенных (на всех уровнях)
         * узлов результатов, а также запись состояния переменных на момент посещения данного узла
         */
        @JvmStatic
        fun ThoughtBranch.getResults(situation: LearningSituation): List<DecisionTreeEvaluationResult> {
            results = mutableListOf()
            this.getCorrectPath(situation)
            val trace = results!!
            results = null
            return trace
        }

        /**
         * Прорешать дерево мысли для конкретной ситуации;
         * @see getResults
         */
        @JvmStatic
        fun DecisionTree.solve(situation: LearningSituation): List<DecisionTreeEvaluationResult> {
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
            return mainBranch.getResults(situation)
        }
    }

    data class DecisionTreeEvaluationResult(
            val node: BranchResultNode,
            val variablesSnapshot: Map<String, Obj>,
    )
}