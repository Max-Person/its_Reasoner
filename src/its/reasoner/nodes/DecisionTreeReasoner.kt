package its.reasoner.nodes

import its.model.expressions.types.Obj
import its.model.expressions.types.Types.Boolean
import its.model.expressions.types.Types.isOfValidType
import its.model.nodes.*
import its.model.nodes.visitors.LinkNodeBehaviour
import its.reasoner.LearningSituation
import its.reasoner.operators.OperatorReasoner

class DecisionTreeReasoner(val situation: LearningSituation) : LinkNodeBehaviour<Any> {
    override fun process(node: CycleAggregationNode): Boolean {
        TODO("Not yet implemented")
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
        return when(node.logicalOp) {
            LogicalOp.AND -> node.thoughtBranches.all{it.getAnswer(situation)}
            LogicalOp.OR -> node.thoughtBranches.any{it.getAnswer(situation)}
        }
    }

    override fun process(node: PredeterminingFactorsNode): String {
        val predetermining =  node.predetermining.singleOrNull{ it.decidingBranch!!.getAnswer(situation) }
        return predetermining?.key ?: node.undetermined!!.key
    }

    override fun process(node: QuestionNode): Any {
        val value = node.expr.use(OperatorReasoner.defaultReasoner(situation))
        require(value.isOfValidType())
        require(node.type.isInstance(value))
        return value
    }

    companion object _static{
        @JvmStatic
        fun <AnswerType : Any> LinkNode<AnswerType>.getAnswer(situation: LearningSituation): AnswerType{
            return this.use(DecisionTreeReasoner(situation)) as AnswerType
        }

        @JvmStatic
        fun LinkNode<*>.correctNext(situation: LearningSituation): DecisionTreeNode{
            return this.next[this.getAnswer(situation)]!!
        }

        @JvmStatic
        fun ThoughtBranch.getAnswer(situation: LearningSituation): Boolean{
            val path = this.getCorrectPath(situation)
            val last = path.last()
            require(last is BranchResultNode){"Последний узел в пути рассуждений не является BranchResultNode"}
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
            return path
        }
    }
}