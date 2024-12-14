package its.reasoner.nodes

import its.model.definition.types.Obj
import its.model.nodes.*

/**
 * Результат вычисления дерева рассуждений [DecisionTree]
 * @param value значение результата
 * @param node узел, в котором возник данный результат
 * @param variablesSnapshot состояние переменных в момент получения результата
 */
sealed class DecisionTreeEvaluationResult<Node : EndingNode>(
    val value: BranchResult,
    val node: Node,
    val variablesSnapshot: Map<String, Obj>,
)

/**
 * Результат в дереве рассуждений, получаемый напрямую из [BranchResultNode]
 * @param node узел результата, создавший данный результат
 * @param variablesSnapshot состояние переменных в момент получения результата
 */
class BasicDecisionTreeEvaluationResult(
    node: BranchResultNode,
    variablesSnapshot: Map<String, Obj>
) : DecisionTreeEvaluationResult<BranchResultNode>(node.value, node, variablesSnapshot)

/**
 * Результат в дереве рассуждений, получаемый из [AggregationNode],
 * и основывающийся на результатах агрегации - [nestedResults]
 * @param value значение результата
 * @param node узел агрегации, создавший данный результат
 * @param nestedResults вложенные результаты агрегации, на основе которых получен данный результат
 * @param variablesSnapshot состояние переменных в момент получения результата
 */
class AggregatedDecisionTreeEvaluationResult(
    value: BranchResult,
    node: AggregationNode,
    val nestedResults: List<DecisionTreeEvaluationResult<*>>,
    variablesSnapshot: Map<String, Obj>
) : DecisionTreeEvaluationResult<AggregationNode>(value, node, variablesSnapshot) {
    /**
     * Метод агрегации, которым получен данный результат
     */
    val aggregationMethod = node.aggregationMethod
}


/**
 * Вспомогательный класс, позволяющий определить некоторую обработку для различных видов [DecisionTreeEvaluationResult]
 * @param T тип возвращаемого обработчиком значения
 */
interface DecisionTreeEvaluationResultsProcessor<T> {
    /**
     * Обработать результат
     */
    fun process(result: DecisionTreeEvaluationResult<*>): T {
        return when (result) {
            is AggregatedDecisionTreeEvaluationResult -> {
                when (result.aggregationMethod) {
                    AggregationMethod.AND -> processAndRes(result)
                    AggregationMethod.OR -> processOrRes(result)
                    AggregationMethod.HYP -> processHypRes(result)
                    AggregationMethod.MUTEX -> processMutexRes(result)
                }
            }

            is BasicDecisionTreeEvaluationResult -> processBasicRes(result)
        }
    }

    /**
     * Обработать [AggregatedDecisionTreeEvaluationResult] с типом [AggregationMethod.AND]
     */
    fun processAndRes(result: AggregatedDecisionTreeEvaluationResult): T

    /**
     * Обработать [AggregatedDecisionTreeEvaluationResult] с типом [AggregationMethod.OR]
     */
    fun processOrRes(result: AggregatedDecisionTreeEvaluationResult): T

    /**
     * Обработать [AggregatedDecisionTreeEvaluationResult] с типом [AggregationMethod.HYP]
     */
    fun processHypRes(result: AggregatedDecisionTreeEvaluationResult): T

    /**
     * Обработать [AggregatedDecisionTreeEvaluationResult] с типом [AggregationMethod.MUTEX]
     */
    fun processMutexRes(result: AggregatedDecisionTreeEvaluationResult): T

    /**
     * Обработать [BasicDecisionTreeEvaluationResult]
     */
    fun processBasicRes(result: BasicDecisionTreeEvaluationResult): T

    companion object {
        /**
         * Пустой обработчик, на случай необходимости передать обработчик как заглушку
         */
        @JvmStatic
        val EMPTY = object : DecisionTreeEvaluationResultsProcessor<Unit> {
            override fun processAndRes(result: AggregatedDecisionTreeEvaluationResult) {}
            override fun processOrRes(result: AggregatedDecisionTreeEvaluationResult) {}
            override fun processHypRes(result: AggregatedDecisionTreeEvaluationResult) {}
            override fun processMutexRes(result: AggregatedDecisionTreeEvaluationResult) {}
            override fun processBasicRes(result: BasicDecisionTreeEvaluationResult) {}
        }
    }
}