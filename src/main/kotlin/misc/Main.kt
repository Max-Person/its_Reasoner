package misc

import its.model.DomainSolvingModel
import its.model.definition.loqi.DomainLoqiBuilder
import its.reasoner.LearningSituation
import its.reasoner.nodes.DecisionTreeReasoner.Companion.solve
import java.io.File

/**
 * Пример использования библиотеки для решения задач, описанных в формате its_DomainModel
 */
fun main(args: Array<String>) { //путь к папке с данными
    val dir = "../inputs/input_examples_expressions_prod"

    //Создать модель домена
    val model = DomainSolvingModel(dir, DomainSolvingModel.BuildMethod.LOQI)

    //Создать условие конкретной задачи
    val i = 2
    val situationDomain = DomainLoqiBuilder.buildDomain(File("$dir/questions/s_$i.loqi").bufferedReader())

    //Получить полное описание ситуации и провалидировать его
    situationDomain.add(model.getMergedTagDomain("c++"))
    situationDomain.validateAndThrow()

    val situation = LearningSituation(situationDomain)

    //Решение задачи
    val result = model.decisionTree.solve(situation)

}
