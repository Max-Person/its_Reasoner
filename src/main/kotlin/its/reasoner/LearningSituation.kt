package its.reasoner

import its.model.definition.DomainModel
import its.model.definition.types.Obj

/**
 * Описание текущей учебной ситуации
 * @param domainModel описание предметной области текущей ситуации
 * @param decisionTreeVariables имена и значения известных переменных дерева решений
 */
open class LearningSituation(
    val domainModel: DomainModel,
    val decisionTreeVariables: MutableMap<String, Obj> = collectDecisionTreeVariables(domainModel),
) {

    companion object _static{
        @JvmStatic
        fun collectDecisionTreeVariables(domain: DomainModel): MutableMap<String, Obj> {
            return domain.variables.associate { it.name to Obj(it.valueObjectName) }.toMutableMap()
        }
    }
}