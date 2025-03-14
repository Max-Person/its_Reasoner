package its.reasoner.nodes

import its.model.definition.types.Obj
import its.model.nodes.*

/**
 * Трасса выполнения одной ветви дерева решения.
 * Состоит из [DecisionTreeTraceElement].
 *
 * Итоговый результат ветви всегда определяется по последнему элементу в трасе.
 */
class DecisionTreeTrace(
    traceElements: List<DecisionTreeTraceElement<*, *>>,
) : List<DecisionTreeTraceElement<*, *>> by traceElements.toList() {

    init {
        require(traceElements.isNotEmpty()) {
            "A DecisionTreeTrace cannot be empty"
        }
        require(
            traceElements.last().nodeResult is BranchResult
                    && traceElements.last().node is EndingNode
        ) {
            "The final trace element of a DecisionTreeTrace must contain a branch result"
        }
    }

    /**
     * Узел, на котором завершилось выполнение ветви
     */
    val resultingNode: DecisionTreeNode
        get() = this.last().node

    /**
     * Состояние переменных на момент завершения выполнения ветви
     */
    val finalVariableSnapshot: Map<String, Obj>
        get() = this.last().variablesSnapshot

    /**
     * Итоговый результат выполнения ветви
     */
    val branchResult: BranchResult
        get() = this.last().nodeResult as BranchResult

    /**
     * Выполнялся ли узел в данной ветви или во вложенных ветвях
     */
    fun containsWithNested(node: DecisionTreeNode): Boolean {
        return this.any { traceElement ->
            traceElement.node == node
                    || traceElement.nestedTraces().any { nestedTrace -> nestedTrace.containsWithNested(node) }
        }
    }
}

/**
 * Элемент трассы выполнения дерева решений [DecisionTreeTrace] - соответствует выполнению одного узла в ветви мысли.
 *
 * @param node узел, соответствующий данному шагу.
 * @param nodeResult результат выполнения узла.
 *   Это либо ответ на узел в случае [LinkNode] либо результат ветви [BranchResult] в случае [BranchResultNode]
 *   (Для узлов агрегации ответ и результат являются одним и тем же значением).
 * @param variablesSnapshot состояние переменных на момент *завершения* выполнения данного шага.
 */
sealed class DecisionTreeTraceElement<Result : Any, Node : DecisionTreeNode>(
    val node: Node,
    val nodeResult: Result,
    val variablesSnapshot: Map<String, Obj>,
) {
    /**
     * Трассы вложенных для данного шага ветвей, если они есть
     */
    open fun nestedTraces(): Collection<DecisionTreeTrace> {
        return listOf()
    }
}

/**
 * [DecisionTreeTraceElement] для обычных [LinkNode] (типа вопросов и действий).
 * Обратите внимание, что для сложных узлов используются другие типы элементов
 * - см. [AggregationDecisionTreeTraceElement] и [WhileCycleDecisionTreeTraceElement].
 */
class LinkDecisionTreeTraceElement<AnswerType : Any>(
    node: LinkNode<AnswerType>,
    nodeResult: AnswerType,
    variablesSnapshot: Map<String, Obj>,
) : DecisionTreeTraceElement<AnswerType, LinkNode<AnswerType>>(node, nodeResult, variablesSnapshot)

/**
 * [DecisionTreeTraceElement] для [AggregationDecisionTreeTraceElement]
 * @param BranchInfo тип данных, определяющий одну ветку агрегации:
 *   для [BranchAggregationNode] это сама ветка [ThoughtBranch],
 *   а для [CycleAggregationNode] это [Obj], соответствующий конкретной итерации цикла.
 *
 * @param branchTraceMap мапа вложенных ветвей агрегации к их трассам их выполнения
 */
class AggregationDecisionTreeTraceElement<BranchInfo : Any>(
    node: AggregationNode,
    nodeResult: BranchResult,
    variablesSnapshot: Map<String, Obj>,
    val branchTraceMap: Map<BranchInfo, DecisionTreeTrace>
) : DecisionTreeTraceElement<BranchResult, AggregationNode>(node, nodeResult, variablesSnapshot) {

    override fun nestedTraces(): Collection<DecisionTreeTrace> {
        return branchTraceMap.values
    }
}

/**
 * [DecisionTreeTraceElement] для [WhileCycleNode]
 *
 * @param branchTraceList трассы выполнений тела цикла
 */
class WhileCycleDecisionTreeTraceElement(
    node: WhileCycleNode,
    nodeResult: BranchResult,
    variablesSnapshot: Map<String, Obj>,
    val branchTraceList: List<DecisionTreeTrace>
) : DecisionTreeTraceElement<BranchResult, WhileCycleNode>(node, nodeResult, variablesSnapshot) {

    override fun nestedTraces(): Collection<DecisionTreeTrace> {
        return branchTraceList
    }
}

/**
 * [DecisionTreeTraceElement] для [BranchResultNode]
 */
class BranchResultDecisionTreeTraceElement(
    node: BranchResultNode,
    variablesSnapshot: Map<String, Obj>,
) : DecisionTreeTraceElement<BranchResult, BranchResultNode>(node, node.value, variablesSnapshot)
