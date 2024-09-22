import its.model.DomainSolvingModel
import its.model.definition.loqi.DomainLoqiBuilder
import its.reasoner.LearningSituation
import its.reasoner.nodes.DecisionTreeReasoner._static.getAnswer
import its.reasoner.nodes.DecisionTreeReasoner._static.getCorrectPath
import its.reasoner.nodes.DecisionTreeReasoner._static.getResults
import its.reasoner.nodes.DecisionTreeReasoner._static.solve
import java.io.File

/**
 * Пример использования библиотеки для решения задач, описанных в формате its_DomainModel
 */
fun main(args: Array<String>) {
    //путь к папке с данными
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

    //   Решение задачи - от наиболее краткого ответа до наиболее подробного - выбрать одно из трех
    //1. Получить тру/фолс ответ (значение финального узла BranchResultNode)
    val answer = model.decisionTree.mainBranch.getAnswer(situation)
    //2. Получить все посещенные узлы на самом верхнем уровне (без ухода во вложенные ветки) - в порядке вычисления
    val path = model.decisionTree.mainBranch.getCorrectPath(situation)
    //3. Получить посещенные узлы результатов (BranchResultNode) по всему дереву - в порядке полного вычисления
    val resultsA = model.decisionTree.mainBranch.getResults(situation)
    val resultsB = model.decisionTree.solve(situation)

//    println(resultsB)

}
