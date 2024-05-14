package its.reasoner.nodes

import its.model.definition.Domain
import its.model.expressions.operators.AssignDecisionTreeVar
import its.model.expressions.operators.GetPropertyValue
import its.model.nodes.*
import its.model.nodes.visitors.LinkNodeBehaviour
import its.reasoner.compiler.OperatorJenaCompiler
import its.reasoner.compiler.util.*

data class NodeCompilationResult(
    val children: Map<Any, NodeCompilationResult>?,
    val value: String = "",
    val rules: String = "",
    val ending: BranchResultNode? = null,
)

class DecisionTreeCompiler(domain: Domain) : LinkNodeBehaviour<NodeCompilationResult> {

    private val compiler = OperatorJenaCompiler(domain)

    override fun process(node: QuestionNode): NodeCompilationResult {
        var rules = ""
        val children = mutableMapOf<Any, NodeCompilationResult>()

        node.outcomes.forEach { outcome ->
            if (outcome.node is LinkNode<*>) {
                val res = (outcome.node as LinkNode<*>).use(this)
                rules += res.rules
                if (node.expr is GetPropertyValue && outcome.key == false) {
                    children["GetPropertyValue.false"] = res
                } else {
                    children[outcome.key] = res
                }
            } else {
                val resultNodeCompiled = (outcome.node as BranchResultNode).actionExpr?.use(compiler)
                val res = NodeCompilationResult(
                    null,
                    rules = resultNodeCompiled?.rules.orEmpty(),
                    ending = outcome.node as BranchResultNode
                )
                if (node.expr is GetPropertyValue && outcome.key == false) {
                    children["GetPropertyValue.false"] = res
                } else {
                    children[outcome.key] = res
                }
            }
        }

        val result = compiler.compileExpression(node.expr, false)
        return NodeCompilationResult(
            children,
            result.value,
            result.rules + rules
        )
    }

    override fun process(node: FindActionNode): NodeCompilationResult {
        var rules = ""
        val children = mutableMapOf<Any, NodeCompilationResult>()

        node.outcomes.forEach { outcome ->
            if (outcome.node is LinkNode<*>) {
                val res = (outcome.node as LinkNode<*>).use(this)
                rules += res.rules
                children[outcome.key] = res
            } else {
                val resultNodeCompiled = (outcome.node as BranchResultNode).actionExpr?.use(compiler)
                children[outcome.key] = NodeCompilationResult(
                    null,
                    rules = resultNodeCompiled?.rules.orEmpty(),
                    ending = outcome.node as BranchResultNode
                )
            }
        }

        // Компилируем getByCondition
        val compilerResult = node.varAssignment.valueExpr.semantic().use(compiler)

        // Генерируем имена
        val skolemName = genVariableName()
        val resPredicateName = genPredicateName()

        // Собираем правило
        val rule = """
            [
            ${compilerResult.bodies.first()}
            makeSkolem($skolemName)
            ->
            (${compilerResult.value} $VAR_PREDICATE ${genValue(node.varAssignment.variable.varName)})
            ($skolemName $resPredicateName ${compilerResult.value})
            ]
        """.trimIndent()

        return NodeCompilationResult(
            children,
            resPredicateName,
            compilerResult.rules + rule + PAUSE_MARK + rules
        )
    }

    override fun process(node: CycleAggregationNode): NodeCompilationResult {
        TODO("Not yet implemented")
    }

    override fun process(node: LogicAggregationNode): NodeCompilationResult {
        TODO("Not yet implemented")
    }

    override fun process(node: PredeterminingFactorsNode): NodeCompilationResult {
        TODO("Not yet implemented")
    }

    override fun process(node: WhileAggregationNode): NodeCompilationResult {
        TODO("Not yet implemented")
    }
}