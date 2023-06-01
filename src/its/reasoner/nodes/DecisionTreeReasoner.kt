package its.reasoner.nodes

import its.model.expressions.types.Obj
import its.model.expressions.types.Types.Boolean
import its.model.expressions.types.Types.isOfValidType
import its.model.nodes.*
import its.model.nodes.visitors.LinkNodeBehaviour
import its.model.nullCheck
import its.reasoner.LearningSituation
import its.reasoner.operators.OperatorReasoner

class DecisionTreeReasoner(val situation: LearningSituation) : LinkNodeBehaviour<Any> {
    override fun process(node: CycleAggregationNode): Boolean {
        val iteratedValues = OperatorReasoner.defaultReasoner(situation).getObjectsByCondition(node.selectorExpr, node.variable.name)
        var res = node.logicalOp == LogicalOp.AND
        for(value in iteratedValues){
            situation.decisionTreeVariables[node.variable.name] = value.resource.localName
            val cur = node.thoughtBranch.getAnswer(situation)
            res = when(node.logicalOp){
                LogicalOp.AND -> res && cur
                LogicalOp.OR -> res || cur
            }
        }
        situation.decisionTreeVariables.remove(node.variable.name)
        return  res
    }

    override fun process(node: WhileAggregationNode): Boolean {
        var res = node.logicalOp == LogicalOp.AND
        while(node.conditionExpr.use(OperatorReasoner.defaultReasoner(situation)) as Boolean){
            val cur = node.thoughtBranch.getAnswer(situation)
            res = when(node.logicalOp){ //FIXME? Уточнить семантику - точно ли данном цикле не надо прерываться если результат известен
                LogicalOp.AND -> res && cur
                LogicalOp.OR -> res || cur
            }
        }
        return res
    }

    override fun process(node: FindActionNode): String {
        val res = node.findCorrect(situation)
        if(res != null){
            situation.decisionTreeVariables[node.variable.name] = res.name
            for(additional in node.additionalVariables){
                situation.decisionTreeVariables[additional.variable.name] = (additional.calcExpr.use(OperatorReasoner.defaultReasoner(situation)) as Obj).name
            }
            return "found"
        }
        else
            return "none"
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

    override fun process(node: PredeterminingFactorsNode): String {
        val predetermining =  node.predetermining.singleOrNull{ it.decidingBranch!!.getAnswer(situation) }
        return predetermining?.key ?: node.undetermined
                .nullCheck("None of the outcomes of PredeterminingFactorsNode $node are applicable")
                .key
    }

    override fun process(node: QuestionNode): Any {
        val value = node.expr.use(OperatorReasoner.defaultReasoner(situation))
        require(value.isOfValidType())
        require(node.type.isInstance(value))
        return value
    }

    companion object _static{
        @JvmStatic
        fun <AnswerType : Any> LinkNode<AnswerType>.getAnswer(situation: LearningSituation): AnswerType {
            return use(DecisionTreeReasoner(situation)) as AnswerType
        }

        @JvmStatic
        fun LinkNode<*>.correctNext(situation: LearningSituation): DecisionTreeNode{
            val ans = this.getAnswer(situation)
            require(this.next.containsKey(ans)){"Node $this has no outcome with value '$ans', but such an answer was returned"}
            return this.next[ans]!!
        }

        @JvmStatic
        fun ThoughtBranch.getAnswer(situation: LearningSituation): Boolean{
            val path = this.getCorrectPath(situation)
            val last = path.last() as BranchResultNode
            require(this.type.isInstance(last.value)){"Значение результата ${last.value} не соответствует типу ветки - ${this.type.simpleName}"}
            require(this.type == Boolean){"Пока что поддерживаются только ветки типа Boolean"}
            return last.value as Boolean
        }

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
            require(last is BranchResultNode){"Ошибка: Последний узел в пути рассуждений не является BranchResultNode"}
            last.actionExpr?.use(OperatorReasoner.defaultReasoner(situation))
            results?.add(DecisionTreeEvaluationResult(last, situation.decisionTreeVariables.toMap()))
            return path
        }

        @JvmStatic
        private var results : MutableList<DecisionTreeEvaluationResult>? = null
        @JvmStatic
        fun ThoughtBranch.getResults(situation: LearningSituation): List<DecisionTreeEvaluationResult> {
            results = mutableListOf()
            this.getCorrectPath(situation)
            val trace = results!!
            results = null
            return trace
        }
    }

    data class DecisionTreeEvaluationResult(
            val node: BranchResultNode,
            val variablesSnapshot : Map<String, String>,
    )
}