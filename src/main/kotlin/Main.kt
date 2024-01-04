import its.model.DomainSolvingModel
import its.model.definition.rdf.DomainRDFFiller
import its.reasoner.LearningSituation
import its.reasoner.nodes.DecisionTreeReasoner._static.solve


fun run() {
    val dir = "..\\inputs\\input_examples" //путь к папке с данными
    //val dir = "inputs\\"

    //Создать модель домена
    val model = DomainSolvingModel(dir)

    //Создать условие конкретной задачи
    val i = 3
    val situationDomain = model.domain.copy()
    DomainRDFFiller.fillDomain(situationDomain, "${dir}\\_$i\\$i.ttl")

    val situation = LearningSituation(situationDomain)

    //решение задачи - от наиболее краткого ответа до наиболее подробного - выбрать одно из трех
//    val answer = DomainModel.decisionTree.main.getAnswer(situation) //Получить тру/фолс ответ
//    val path = DomainModel.decisionTree.main.getCorrectPath(situation) //Получить посещенные узлы на самом верхнем уровне (без ухода во вложенные ветки) - в порядке вычисления
//    val trace = DomainModel.decisionTree.main.getTrace(situation) //Получить посещенные узлы по всему дереву - в порядке полного вычисления

    val results = model.decisionTree.solve(situation)
    results.forEach { println("${it.node.value} : ${it.node.parent.parent}") }

}

fun main(args: Array<String>) {
    run()
}
