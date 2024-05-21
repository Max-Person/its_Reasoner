package its.reasoner.nodes

import its.model.definition.Domain
import its.model.definition.EnumValueRef
import its.model.expressions.operators.*
import its.model.nodes.*
import its.model.nodes.visitors.LinkNodeBehaviour
import its.reasoner.compiler.OperatorJenaCompiler
import its.reasoner.compiler.util.*

data class OtherStrategyNodeCompilationResult(
    val children: Map<Any, OtherStrategyNodeCompilationResult>?,
    val bodies: Map<Any, String>?,
    val rules: String = "",
    val ending: BranchResultNode? = null,
)

class DecisionTreeCompilerWithOtherStrategy(domain: Domain) : LinkNodeBehaviour<OtherStrategyNodeCompilationResult> {

    private val compiler = OperatorJenaCompiler(domain)

    override fun process(node: QuestionNode): OtherStrategyNodeCompilationResult {
        var rules = ""
        val children = mutableMapOf<Any, OtherStrategyNodeCompilationResult>()
        val bodies = mutableMapOf<Any, String>()

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
                val res = OtherStrategyNodeCompilationResult(
                    null,
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

        node.outcomes.forEach { outcome ->
            when (node.expr) {
                is CompareWithComparisonOperator -> {
                    when (outcome.key) {
                        true -> {
                            val compilationResult = node.expr.use(compiler)
                            bodies[outcome.key] = compilationResult.bodies.first()
                            rules = compilationResult.rules + rules
                        }

                        false -> {
                            val compilationResult = LogicalNot(node.expr).semantic().use(compiler)
                            bodies[outcome.key] = compilationResult.bodies.first()
                            rules = compilationResult.rules + rules
                        }

                        else -> {
                            TODO()
                        }
                    }
                }

                is GetPropertyValue -> {
                    when (outcome.key) {
                        true -> {
                            val compilationResult = node.expr.use(compiler)
                            bodies[outcome.key] = compilationResult.bodies.first() + genEqualPrim(
                                compilationResult.value,
                                genValue(true)
                            )
                            rules = compilationResult.rules + rules
                        }

                        false -> {
                            val compilationResult = node.expr.use(compiler)
                            bodies["GetPropertyValue.false"] = compilationResult.bodies.first() + genEqualPrim(
                                compilationResult.value,
                                genValue(false)
                            )
                            rules = compilationResult.rules + rules
                        }

                        is EnumValueRef -> {
                            when ((outcome.key as EnumValueRef).valueName) {
                                "Main" -> {
                                    val compilationResult = node.expr.use(compiler)
                                    bodies[outcome.key] = compilationResult.bodies.first() + genEqualPrim(
                                        compilationResult.value,
                                        genURI(POAS_PREF, "Main")
                                    )
                                    rules = compilationResult.rules + rules
                                }

                                "Start" -> {
                                    val compilationResult = node.expr.use(compiler)
                                    bodies[outcome.key] = compilationResult.bodies.first() + genEqualPrim(
                                        compilationResult.value,
                                        genURI(POAS_PREF, "Start")
                                    )
                                    rules = compilationResult.rules + rules
                                }

                                "Final" -> {
                                    val compilationResult = node.expr.use(compiler)
                                    bodies[outcome.key] = compilationResult.bodies.first() + genEqualPrim(
                                        compilationResult.value,
                                        genURI(POAS_PREF, "Final")
                                    )
                                    rules = compilationResult.rules + rules
                                }

                                else -> {
                                    TODO()
                                }
                            }
                        }

                        else -> {
                            TODO()
                        }
                    }
                }

                is CheckRelationship -> {
                    when (outcome.key) {
                        true -> {
                            val compilationResult = node.expr.use(compiler)
                            bodies[outcome.key] = compilationResult.bodies.first()
                            rules = compilationResult.rules + rules
                        }

                        false -> {
                            val result = compiler.compileExpression(node.expr, false)
                            bodies[outcome.key] = "noValue(${genVariableName()} ${result.value} ${genVariableName()})\n"
                            rules = result.rules + rules
                        }

                        else -> {
                            TODO()
                        }
                    }
                }

                is ExistenceQuantifier -> {
                    when (outcome.key) {
                        true -> {
                            val compilationResult = node.expr.use(compiler)
                            bodies[outcome.key] = compilationResult.bodies.first()
                            rules = compilationResult.rules + rules
                        }

                        false -> {
                            val result = compiler.compileExpression(node.expr, false)
                            bodies[outcome.key] = "noValue(${genVariableName()} ${result.value} ${genVariableName()})\n"
                            rules = result.rules + rules
                        }

                        else -> {
                            TODO()
                        }
                    }
                }

                else -> {
                    TODO()
                }
            }
        }

        return OtherStrategyNodeCompilationResult(
            children,
            bodies,
            rules
        )
    }

    override fun process(node: FindActionNode): OtherStrategyNodeCompilationResult {
        var rules = ""
        val children = mutableMapOf<Any, OtherStrategyNodeCompilationResult>()
        val bodies = mutableMapOf<Any, String>()

        node.outcomes.forEach { outcome ->
            if (outcome.node is LinkNode<*>) {
                val res = (outcome.node as LinkNode<*>).use(this)
                rules += res.rules
                children[outcome.key] = res
            } else {
                val resultNodeCompiled = (outcome.node as BranchResultNode).actionExpr?.use(compiler)
                children[outcome.key] = OtherStrategyNodeCompilationResult(
                    null,
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

        node.outcomes.forEach { outcome ->
            when (outcome.key) {
                true -> {
                    bodies[true] = "($skolemName $resPredicateName ${genVariableName()})\n"
                }
                false -> {
                    bodies[false] = "noValue($skolemName $resPredicateName ${genVariableName()})\n"
                }
            }
        }

        return OtherStrategyNodeCompilationResult(
            children,
            bodies,
            compilerResult.rules + rule + PAUSE_MARK + rules
        )
    }

    override fun process(node: CycleAggregationNode): OtherStrategyNodeCompilationResult {
        TODO("Not yet implemented")
    }

    override fun process(node: LogicAggregationNode): OtherStrategyNodeCompilationResult {
        TODO("Not yet implemented")
    }

    override fun process(node: PredeterminingFactorsNode): OtherStrategyNodeCompilationResult {
        TODO("Not yet implemented")
    }

    override fun process(node: WhileAggregationNode): OtherStrategyNodeCompilationResult {
        TODO("Not yet implemented")
    }
}