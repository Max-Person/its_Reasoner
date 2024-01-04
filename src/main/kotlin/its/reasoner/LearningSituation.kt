package its.reasoner

import its.model.definition.Domain
import its.model.definition.types.Obj

/**
 * Описание текущей учебной ситуации
 * @param domain описание предметной области текущей ситуации
 * @param decisionTreeVariables имена и значения известных переменных дерева решений
 */
data class LearningSituation(
    val domain: Domain,
    val decisionTreeVariables: MutableMap<String, Obj> = collectDecisionTreeVariables(domain),
) {

    companion object _static{
        @JvmStatic
        fun collectDecisionTreeVariables(domain: Domain): MutableMap<String, Obj> {
            return domain.variables.associate { it.name to Obj(it.valueObjectName) }.toMutableMap()
        }
    }
}